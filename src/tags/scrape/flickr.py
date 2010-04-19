# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, socket, logging, os
from math import log
from time import time
from collections import deque
from functools import partial
from itertools import chain, izip
from threading import local as ThreadLocal
from httplib import HTTPConnection, ImproperConnectionState, HTTPException
from xml.etree.ElementTree import dump

from flickrapi import FlickrAPI, FlickrError
from igraph import Graph

from tags.scrape.object import Node, NodeSample, Producer, ProducerSample, NID, AAT
from tags.scrape.util import (intern_force, union_ind, geo_prog_range, infer_arcs,
  edge_array, graph_copy, undirect_and_simplify, invert_seq, invert_multimap,
  enumerate_cb, exec_unique, repr_call)


LOG = logging.getLogger(__name__)
LOG.setLevel(1)


class SafeFlickrAPI(FlickrAPI):

	def __init__(self, api_key, secret=None, token=None, store_token=False, cache=False, **kwargs):
		FlickrAPI.__init__(self, api_key, secret=secret, token=token, store_token=store_token, cache=cache, **kwargs)

		# Thread-local HTTPConnection, see __flickr_call
		self.thr = ThreadLocal()

		if token: return
		(token, frob) = self.get_token_part_one(perms='read')
		if not token:
			print "A browser window should have opened asking you to authorise this program."
			raw_input("Press ENTER when you have done so... ")
		self.get_token_part_two((token, frob))


	def __getattr__(self, attrib):
		from urllib2 import URLError
		from flickrapi.exceptions import FlickrError
		from time import sleep

		handler = FlickrAPI.__getattr__(self, attrib)

		def wrapper(**args):
			i = 0
			while True:
				err = None

				try:
					return handler(**args)
				except FlickrError, e:
					code = FlickrError_code(e)
					if code == 0 or code == 112: # FIXME LOW only when "unknown" is returned as the method called
						err = e
					else:
						log = LOG.warning if code > 99 else LOG.debug
						log("SafeFlickrAPI: ABORT %s due to %r" % (repr_call(attrib, **args), e))
						raise
				except (URLError, IOError, ImproperConnectionState, HTTPException), e:
					err = e

				LOG.warning("SafeFlickrAPI: wait %.4fs to retry %s due to %r" % (1.2**i, repr_call(attrib, **args), err))
				sleep(1.2**i)
				i = i+1 if i < 20 else 0

		return wrapper


	def __flickr_call(self, **kwargs):
		# Use persistent HTTP connections through a thread-local socket
		from flickrapi import LOG

		LOG.debug("Calling %s" % kwargs)

		post_data = self.encode_and_sign(kwargs)

		# Return value from cache if available
		if self.cache and self.cache.get(post_data):
			return self.cache.get(post_data)

		# Thread-local persistent connection
		try:
			if "conn" not in self.thr.__dict__:
				self.thr.conn = HTTPConnection(FlickrAPI.flickr_host)
				LOG.debug("connection opened: %s" % FlickrAPI.flickr_host)

			self.thr.conn.request("POST", FlickrAPI.flickr_rest_form, post_data,
				{"Content-Type": "application/x-www-form-urlencoded"})
			reply = self.thr.conn.getresponse().read()

		except (ImproperConnectionState, socket.error), e:
			LOG.debug("connection error: %s" % repr(e))
			self.thr.conn.close()
			del self.thr.conn
			raise

		# Store in cache, if we have one
		if self.cache is not None:
			self.cache.set(post_data, reply)

		return reply

	# Make private method visible
	data_walker = FlickrAPI._FlickrAPI__data_walker


	def getNSID(self, n):
		return self.people_findByUsername(username=n).getchildren()[0].get("nsid")


	def makeID(self, nsid):
		out = self.contacts_getPublicList(user_id=nsid).getchildren()[0].getchildren()
		return Node(nsid, dict((elem.get("nsid"), 0 if int(elem.get("ignored")) else 1) for elem in out))


	def scrapeIDs(self, seed, size):

		if type(size) != int:
			raise TypeError

		def next(ss, qq):
			id = qq.popleft()
			if id in ss: return None
			node = self.makeID(id)
			qq.extend(node.out.keys())
			ss.add_node(node)
			return id

		s = NodeSample()
		q = deque([self.getNSID(seed)])

		while len(s) < size:
			id = next(s, q)
			if id is not None:
				LOG.info("id sample: %s/%s (added %s)" % (len(s), size, id))

		s.build(False)
		return s


	def scrapeGroups(self, users):
		"""
		Scrapes all groups of the given users.

		@return: {group:[user]}
		"""
		gumap = {}

		def run(nsid):
			groups = self.people_getPublicGroups(user_id=nsid).getchildren()[0].getchildren()
			return groups

		def post(nsid, i, groups):
			for g in groups:
				gid = g.get("nsid")
				if gid in gumap:
					gumap[gid].append(nsid)
				else:
					gumap[gid] = [nsid]

		exec_unique(users, gumap, run, post, "gid sample db", LOG.info)
		return gumap


	def getSetPhotos(self, sets, x):
		"""
		Gets sets of a given user and all photos belonging to it

		@param sets: an iterable of set ids
		@param x: an executor to execute calls in parallel
		@return: {set:[photo]}
		"""
		spmap = {}

		#[s.get(NID) for s in self.photosets_getList(user_id=nsid).getchildren()[0].getchildren()]
		for r in x.run_to_results_any(partial(self.photosets_getPhotos, photoset_id=sid) for sid in sets):
			pset = r.getchildren()[0]
			sid = pset.get(NID)
			spmap[sid] = [p.get(NID) for p in pset.getchildren()]
			LOG.debug("set: got %s photos (%s)" % (len(pset), sid), 6)

		return spmap


	def commitPhotoTags(self, photos, ptdb):
		"""
		Gets the tags of the given photos and saves these to a database

		@param photos: a list of photo ids
		@param ptdb: an open database of {photo:[tag]}
		@param conc_m: max concurrent threads to run
		"""
		def run(phid):
			tags = self.tags_getListPhoto(photo_id=phid).getchildren()[0].getchildren()[0].getchildren()
			return tags

		def post(phid, i, tags):
			# filter out "machine-tags"
			ptdb[phid] = [intern_force(tag.text) for tag in tags if tag.text and ":" not in tag.text]

		exec_unique(photos, ptdb, run, post, "photo-tag db", LOG.info)


	def commitUserPhotos(self, users, ppdb):
		"""
		Gets the photos of the given users and saves these to a database

		@param users: a list of user ids
		@param ppdb: an open database of {producer:[photo]}
		@param conc_m: max concurrent threads to run
		"""
		if type(users) != set and len(users) > 16: users = set(users) # efficient membership test
		def run(nsid):
			# OPT HIGH decide whether we want this many, or whether "faves" only will do
			stream = list(self.data_walker(self.people_getPublicPhotos, user_id=nsid, per_page=500))
			faves = list(p for p in self.data_walker(self.favorites_getPublicList, user_id=nsid, per_page=500) if p.get("owner") in users)
			return stream, faves

		def post(nsid, i, (stream, faves)):
			photos = [p.get(NID) for p in chain(stream, faves)]
			if len(photos) >= 4096:
				LOG.info("producer db (user): got %s photos for user %s" % (len(photos), nsid))
			ppdb[nsid] = photos

		exec_unique(users, ppdb, run, post, "producer db (user)", LOG.info)


	def commitGroupPhotos(self, gumap, ppdb):
		"""
		Gets the photos of the given pools and saves these to a database

		@param gumap: a map of {group:[user]}
		@param ppdb: an open database of {producer:[photo]}
		@param conc_m: max concurrent threads to run
		"""
		def run(gid):
			try:
				photos = list(chain(*(self.data_walker(self.groups_pools_getPhotos, group_id=gid, user_id=nsid, per_page=500) for nsid in gumap[gid])))
			except FlickrError, e:
				if FlickrError_code(e) == 2:
					photos = []
				else:
					raise
			return photos

		def post(gid, i, photos):
			if len(photos) >= 4096:
				LOG.info("producer db (group): got %s photos for group %s" % (len(photos), gid))
			ppdb[gid] = [p.get(NID) for p in photos]

		exec_unique(gumap, ppdb, run, post, "producer db (group)", LOG.info)


	def pruneProducers(self, socgr, gumap, ppdb, cutoff=1):
		"""
		Removes producers with less than the given number of photos.

		@param socgr: graph of users
		@param groups: list of groups
		@param ppdb: an open database of {producer:[photo]}
		@param cutoff: producers with this many photos or less will be pruned
		       (default 1)
		"""
		# TODO NORM maybe also prune groups with >n users

		#FIXME HIGH if we prune users, then we also need to prune groups that
		#point to this user
		delu = []
		#for u in socgr.vs[NID]:
		#	if u in ppdb:
		#		if len(ppdb[u]) > cutoff:
		#			continue
		#		del ppdb[u]
		#	delu.append(u)

		delg = []
		for g in gumap:
			if g in ppdb:
				if len(ppdb[g]) > cutoff:
					continue
				del ppdb[g]
			delg.append(g)

		#socgr.delete_vertices([v.index for v in socgr.vs.select(id_in=set(delu))])
		for g in delg: del gumap[g]

		LOG.info("producer db: pruned %s users, %s groups" % (len(delu), len(delg)))


	def invertMap(self, kvdb, vkdb, name):
		"""
		Calculates an inverse map from the given producer-photo database.

		@param kvdb: an open database of {key:[item]}
		@param vkdb: an open database of {item:[key]} - this must have been
		       opened with writeback=True (see shelve docs for details)
		"""
		if vkdb.writeback is not True:
			raise ValueError("[vkdb] must have writeback=True")

		def syncer(i, (key, items)):
			vkdb.sync()

		for i, (key, items) in enumerate_cb(kvdb.iteritems(), syncer, every=0x10000):
			for item in items:
				if item in vkdb:
					vkdb[item].append(key)
				else:
					vkdb[item] = [key]
		vkdb.sync()

		LOG.info("%s db: inverted %s keys to %s items" % (name, len(kvdb), len(vkdb)))


	def commitTagClusters(self, tags, tcdb):
		"""
		Gets the clusters of all the given tags and saves these to a database

		@param tags: a list of tags
		@param tcdb: an open database of {tag:[cluster]}
		@param conc_m: max concurrent threads to run
		"""
		def run(tag):
			try:
				# FIXME HIGH verify that this does the right thing for unicode tags
				# atm all evidence points to flickr not doing clustering anaylses for them...
				clusters = self.tags_getClusters(tag=tag).getchildren()[0].getchildren()
			except FlickrError, e:
				if FlickrError_code(e) == 1:
					clusters = []
				else:
					raise
			return clusters

		def post(tag, i, clusters):
			tcdb[tag] = [[intern_force(t.text) for t in cluster.getchildren()] for cluster in clusters]

		exec_unique(tags, tcdb, run, post, "cluster db", LOG.info)


class SampleGenerator(object):


	def __init__(self, socgr, gumap, ppdb, pcdb, ptdb, tcdb, phdb, pgdb):
		"""
		Create a new SampleGenerator from the given arguments

		@param socgr: social network graph
		@param gumap: {group:[user]} map
		@param ppdb: an open database of {producer:[photo]}
		@param pcdb: an open database of {photo:[producer]}
		@param ptdb: an open database of {photo:[tag]}
		@param tcdb: an open database of {tag:[cluster]}
		@param phdb: an open database of {nsid:Producer} (for indexes)
		@param pgdb: an open database of {prid:Producer} (for tgraphs)
		"""
		self.socgr = socgr
		self.gumap = gumap
		self.ppdb = ppdb
		self.pcdb = pcdb
		self.ptdb = ptdb
		self.tcdb = tcdb
		self.phdb = phdb
		self.pgdb = pgdb

		self.prodgr = None
		self.sprdgr = None
		self.ptabgr = None
		self.ptbmap = None
		self.comm = None


	def generateIndexes(self):
		"""
		DOCUMENT
		"""
		name = "indexes"

		# generate Producer objects
		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(self.ppdb[nsid], self.ptdb)
			prod.inferScores()
			prod.repDoc()
			prod.repTag()
			self.phdb[nsid] = prod
		exec_unique(self.ppdb.iterkeys(), self.phdb, run_p, None, "%s db: producers" % name, LOG.info)

		# generate content arcs between producers
		def run_r(nsid):
			prod = self.phdb[nsid]
			pmap_a = dict((rnsid, self.inferProdArc(prod, self.phdb[rnsid])) for rnsid in self.inferRelProds(prod))
			prod.initProdArcs(pmap_a)
			self.phdb[nsid] = prod
		# OPT HIGH the lambda is inefficient, we should store this state in a smaller+faster db
		exec_unique(self.phdb.iterkeys(), lambda nsid: self.phdb[nsid].base_p is not None,
		  run_r, None, "%s db: relations" % name, LOG.info, steps=0x10000)

		total = len(self.phdb)
		lab_p, id_p = zip(*(("%s (%s)\\n%s" % (nsid, prod.size(), '\\n'.join(prod.rep_t[0:4])),
		  (nsid, i)) for i, (nsid, prod) in enumerate(self.phdb.iteritems()))) if self.phdb else ([], [])
		id_p = dict(id_p)

		# generate producer graph
		arc_s, arc_t, edges = edge_array(total)
		for i, prod in enumerate(self.phdb.itervalues()):
			for nsid in prod.docgr.vs.select(prod.prange())[NID]:
				arc_s.append(i)
				arc_t.append(id_p[nsid])

		sz = [log(prod.size()) for prod in self.phdb.itervalues()]
		v_attr = {NID: list(self.phdb.iterkeys()), "label": lab_p, "height": sz, "width": sz}

		self.prodgr = Graph(total, edges=list(edges), directed=True, vertex_attrs=v_attr)
		LOG.info("%s db: generated producer graph" % name)


	def inferRelProds(self, prod):
		"""
		Infer a set of related producers for the given producer.

		This implementation uses selectProdsForPhotos() on the producer's
		representative photos.
		"""
		rel = self.selectProdsForPhotos(prod.rep_d)
		rel.discard(prod.nsid)
		return rel


	def selectProdsForPhotos(self, photos):
		"""
		Selects a set of "related" producers from all the producers that hold
		any of the photos in the given set.
		"""
		rel = set()
		for phid in photos:
			rel.update(self.pcdb[phid])
		return rel


	def inferProdArc(self, prod_s, prod_t, show_tag=False):
		"""
		Infer arcs and their weights between the given producers.

		This implementation uses selectTagsFromClusters() on the producers'
		representative tags.

		@param prod_s: source producer
		@param prod_t: target producer
		@param show_tag: whether to return tag_a
		@return: (arc_a, tag_a) DOCUMENT
		"""
		rtags, htags = self.selectTagsFromClusters(prod_s.rep_t, prod_t.rep_t)
		arc_a = dict((rtag, prod_t.tagScore(rtag)) for rtag in rtags)
		arc_a.update(htags)
		return (arc_a, rtags) if show_tag else arc_a


	def selectTagsFromClusters(self, tset_s, tset_t):
		"""
		Selects tags from the intersection between each cluster for a source
		tag, and the target tagset. The representatives of the cluster are also
		selected, if the intersection is large enough.

		@param tset_s: source tag-set
		@param tset_t: target tag-set
		@return: (rtags, htags), where rtags = {rtag:[tag]} associates tags on
		         the target side to related tags on the source side, and htags
		         = {htag:e_attr} associates "high-level" tags (which might not
		         exist on the target side) to appropriate arc-attributes.
		"""
		LOG.debug("III enter selectTagsFromClusters: %s %s" % (len(tset_s), len(tset_t)))
		rtags = {}
		htags = {}
		if type(tset_t) != set:
			tset_t = set(tset_t)

		for tag in tset_s:
			for cluster in self.tcdb[tag]:
				tset_x = tset_t.intersection(cluster)

				# add intersection to rtags
				for rtag in tset_x:
					if rtag in rtags:
						rtags[rtag].append(tag)
					else:
						rtags[rtag] = [tag]

				# if intersection is big enough, add "representative" tags of
				# this cluster to htags
				if 3*len(tset_x) > len(cluster): # TWEAK
					# on flickr, this is the first 3 tags
					attr = len(tset_x)/float(len(cluster))
					for rtag in cluster[0:3]:
						if rtag in htags:
							htags[rtag].append(attr)
							rtags[rtag].append(tag)
						else:
							htags[rtag] = [attr]
							rtags[rtag] = [tag]

		LOG.debug("XXX exit selectTagsFromClusters: %s %s" % (len(tset_s), len(tset_t)))
		return rtags, dict((htag, union_ind(attrs)) for htag, attrs in htags.iteritems())


	def generateTGraphs(self):
		"""
		DOCUMENT
		"""
		name = "tgraphs"

		# generate docsets for new producers
		self.comm = self.selectCommunities()
		pmap = dict(("%04d" % i, pset) for i, pset in enumerate(self.comm))

		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(set(chain(*(self.ppdb[self.prodgr.vs[p][NID]] for p in pmap[nsid]))), self.ptdb, True)
			prod.inferScores()
			prod.repTag(cover=0) # TWEAK
			self.pgdb[nsid] = prod
		exec_unique(pmap, self.pgdb, run_p, None, "%s db: producers" % name, LOG.info)

		tot_p = len(self.prodgr.vs)
		tot_s = len(self.comm)
		edges, arc_a = infer_arcs(self.comm, tot_p, ratio=2*log(1+tot_p)) # TWEAK # relax for tgraphs

		id_p = dict(("%04d" % i, i) for i in xrange(0, tot_s))
		self.sprdgr = Graph(tot_s, list(edges), directed=True,
		  vertex_attrs={NID:list("%04d" % i for i in xrange(0, tot_s)), "label":[len(com) for com in self.comm]})
		g = self.sprdgr
		#g.write_dot("sprdgr.dot")
		#g.write("sprdgr.graphml")
		LOG.info("%s db: generated producer graph" % name)

		# generate content arcs between producers
		# FIXME HIGH this uses up too much memory :/
		import gc
		#gc.set_debug(gc.DEBUG_LEAK)
		def run_r(nsid):
			prod = self.pgdb[nsid]
			rprod = g.vs.select(g.successors(id_p[nsid]))[NID]
			pmap = [(rnsid, self.inferProdArc(prod, self.pgdb[rnsid], show_tag=True)) for rnsid in rprod]
			self.pgdb.sync()
			pmap_a, pmap_t = izip(*(((rnsid, arc_a), (rnsid, node_a)) for rnsid, (arc_a, node_a) in pmap)) if pmap else ([], [])
			prod.initProdArcs(dict(pmap_a), dict(pmap_t))
			del pmap, pmap_a, pmap_t
			gc.collect()
			self.pgdb[nsid] = prod
		# OPT HIGH the lambda is inefficient, we should store this state in a smaller+faster db
		exec_unique(self.pgdb.iterkeys(), lambda nsid: self.pgdb[nsid].base_p is not None,
		  run_r, None, "%s db: relations" % name, LOG.info, steps=0x10000)


	def selectCommunities(self):
		"""
		Generates a bunch of communities using various community detection
		algorithms on the underlying producer graph, and selects the non-tiny
		ones.
		"""
		comm = set()

		'''
		# adds communities generated with "label propagation" until no new
		# communities are generated for 3 rounds in a row. this is shit because
		# it'll generate a lot of similar (ie. redundant) communities with size
		# in the midrange
		unchanged = 0
		while unchanged < 3:
			mem = self.prodgr.community_label_propagation().membership
			coms = list(frozenset(community) for community in invert_seq(mem).itervalues())
			unchanged = unchanged+1 if all(com in comm for com in coms) else 0
			comm.update(coms)
		'''

		'''
		# aggregate communities using "label propagation" algorithm
		# currently this segfaults, igraph bug.
		# also maybe do this several times and generate several aggregates
		mem = self.prodgr.community_label_propagation().membership
		agg = []
		agg.append(self.prodgr.community_label_propagation().membership)
		agg.append(self.prodgr.community_label_propagation().membership)
		lll = zip(*agg)
		ddd = dict((o, i) for i, o in enumerate(set(lll)))
		self.prodgr.community_label_propagation(initial=[ddd[o] for o in lll])
		'''

		# select communities from dendrograms generated by a bunch of algorithms
		dgrams = [
		  undirect_and_simplify(self.prodgr).community_fastgreedy(),
		  self.prodgr.community_edge_betweenness(directed=True),
		]
		gg = undirect_and_simplify(self.prodgr)
		gg.delete_vertices(gg.vs.select(_degree=0)) # walktrap impl can't handle islands
		dgrams.append(gg.community_walktrap())

		def int_unique(flseq):
			"""
			Turns a sorted list of floats into a sorted list of unique ints
			"""
			it = iter(flseq)
			o = round(it.next())
			yield int(o)

			while True:
				i = round(it.next())
				if i != o:
					yield int(i)
				o = i

		from igraph.core import InternalError
		from igraph.statistics import power_law_fit
		ratio = power_law_fit([prod.size() for prod in self.phdb.itervalues()], 6)

		for vxd in dgrams:
			default_cut = len(vxd)
			for n in int_unique(geo_prog_range(2, len(gg.vs)/4, ratio, default_cut)):
				try:
					mem = vxd.cut(n)
				except InternalError:
					continue
				comm.update(frozenset(community) for community in invert_seq(mem).itervalues())

		total = len(self.prodgr.vs)
		# don't generate too big or too small
		return [com for com in comm if log(total) <= len(com) <= total/log(total)]


	def generatePTables(self):
		"""
		DOCUMENT
		"""
		name = "ptables"

		id_u = dict((nsid, vid) for vid, nsid in enumerate(self.socgr.vs[NID]))

		base_h = len(id_u)
		lab_h, id_h = zip(*((nsid, (nsid, base_h+vid)) for vid, nsid in enumerate(self.gumap.iterkeys()))) if self.gumap else ([], [])
		id_h = dict(id_h)

		base_g = len(id_h) + base_h
		lab_g, id_g = zip(*((nsid, (nsid, base_g+vid)) for vid, nsid in enumerate(self.sprdgr.vs[NID]))) if len(self.sprdgr.vs) > 0 else ([], [])
		id_g = dict(id_g)

		ptabgr = graph_copy(self.socgr)

		edges = set()
		# add arcs to self
		edges.update((vid, vid) for vid in xrange(0, base_h))
		# add arcs to indexes
		for nsid, users in self.gumap.iteritems():
			hvid = id_h[nsid]
			edges.update((id_u[user], hvid) for user in users)
		# add arcs to tgraphs
		for i, hvids in enumerate(self.comm):
			gvid = base_g + i
			for nsid in self.prodgr.vs.select(hvids)[NID]:
				if nsid in id_u:
					continue
				for user in self.gumap[nsid]:
					edges.add((id_u[user], gvid))

		ptabgr.add_vertices(len(id_h) + len(id_g))
		eend = len(ptabgr.es)
		ptabgr.add_edges(edges)
		ptabgr.es[eend:][AAT] = [0.5] * len(edges)
		ptabgr.es.select(ptabgr.get_eid(vid, vid) for vid in xrange(0, base_h))[AAT] = [1.0] * base_h
		ptabgr.vs[base_h:][NID] = lab_h + lab_g
		ptabgr["base_z"] = 0
		ptabgr["base_h"] = base_h
		ptabgr["base_g"] = base_g
		self.ptabgr = ptabgr

		# for easy access / human readability
		ugmap = dict((nsid, set(gnsid)) for nsid, gnsid in invert_multimap(self.gumap,
		  dict((nsid, []) for nsid in self.socgr.vs[NID])).iteritems())
		for i, pvid in enumerate(self.comm):
			spid = self.sprdgr.vs[i][NID]
			for nsid in self.prodgr.vs.select(pvid)[NID]:
				if nsid in ugmap:
					continue
				for uid in self.gumap[nsid]:
					ugmap[uid].add(spid)
		self.ptbmap = ugmap



class SampleWriter(ProducerSample):


	def __init__(self, phdb, pgdb, totalsize):
		ProducerSample.__init__(self, phdb, pgdb)
		self.totalsize = totalsize


	def writeIndexes(self, base):
		def run(nsid):
			prod = self.phdb[nsid]
			g = prod.createIndex()
			g.write(os.path.join(base, "%s.graphml" % nsid))
		exec_unique(self.phdb.iterkeys(), lambda nsid: os.path.exists(os.path.join(base, "%s.graphml" % nsid)),
		  run, None, "indexes db: object files", LOG.info)


	def writeTGraphs(self, base):
		def run(nsid):
			prod = self.pgdb[nsid]
			g = prod.createTGraph(self.totalsize, self.pgdb)
			g.write(os.path.join(base, "%s.graphml" % nsid))
		exec_unique(self.pgdb.iterkeys(), lambda nsid: os.path.exists(os.path.join(base, "%s.graphml" % nsid)),
		  run, None, "tgraphs db: object files", LOG.info)




def FlickrError_code(e):
	if e.args[0][:6] == "Error:":
		return int(e.args[0].split(':', 2)[1].strip())
	else:
		return None


# Overwrite private method
# FIXME LOW This is technically a hack since it leaves FlickrAPI unusable
# because it doesn't have self.thr
FlickrAPI._FlickrAPI__flickr_call = SafeFlickrAPI._SafeFlickrAPI__flickr_call

# Don't bother looking up host every time
#FlickrAPI.flickr_host = socket.gethostbyname(FlickrAPI.flickr_host)
