# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, socket, logging
from math import log
from time import time
from collections import deque
from functools import partial
from itertools import chain
from threading import local as ThreadLocal
from httplib import HTTPConnection, ImproperConnectionState, HTTPException
from xml.etree.ElementTree import dump

from flickrapi import FlickrAPI, FlickrError
from igraph import Graph

from tags.scrape.object import Node, NodeSample, Producer
from tags.scrape.util import (StateError, intern_force, infer_arcs, repr_call,
  enumerate_cb, exec_unique, union_ind, geo_prog_range, invert_seq, edge_array,
  graph_copy, undirect_and_simplify)


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
				if gid not in gumap:
					gumap[gid] = [nsid]
				else:
					gumap[gid].append(nsid)

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

		#[s.get("id") for s in self.photosets_getList(user_id=nsid).getchildren()[0].getchildren()]
		for r in x.run_to_results_any(partial(self.photosets_getPhotos, photoset_id=sid) for sid in sets):
			pset = r.getchildren()[0]
			sid = pset.get("id")
			spmap[sid] = [p.get("id") for p in pset.getchildren()]
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
			photos = [p.get("id") for p in chain(stream, faves)]
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
			ppdb[gid] = [p.get("id") for p in photos]

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
		#for u in socgr.vs["id"]:
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


	def invertProducerMap(self, ppdb, pcdb):
		"""
		Calculates an inverse map from the given producer-photo database.

		@param ppdb: an open database of {producer:[photo]}
		@param pcdb: an open database of {photo:[producer]} - this must have
		       been opened with writeback=True (see shelve docs for details)
		"""
		if pcdb.writeback is not True:
			raise ValueError("[pcdb] must have writeback=True")

		def syncer(i, (prod, photos)):
			pcdb.sync()

		for i, (prod, photos) in enumerate_cb(ppdb.iteritems(), syncer, every=0x10000):
			for phid in photos:
				if phid not in pcdb:
					pcdb[phid] = [prod]
				else:
					pcdb[phid].append(prod)
		pcdb.sync()

		LOG.info("context db: inverted %s producers to %s photos" % (len(ppdb), len(pcdb)))


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


class FlickrSample(object):


	def __init__(self, socgr, gumap, ppdb, pcdb, ptdb, tcdb, phdb, pgdb):
		"""
		Create a new FlickrSample from the given arguments

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


	def generateIndexes(self):

		name = "indexes"

		# generate Producer objects
		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(self.ppdb[nsid], self.ptdb)
			prod.inferScores()
			prod.repDoc()
			prod.repTag()
			self.phdb[nsid] = prod
		exec_unique(self.ppdb, self.phdb, run_p, None, "%s db: producers" % name, LOG.info)

		# generate content arcs between producers
		def run_r((nsid, prod)):
			prodmap = dict((rnsid, self.inferProdArc(prod, self.phdb[rnsid])) for rnsid in self.inferRelProds(prod))
			prod.initProdArcs(prodmap)
			self.phdb[nsid] = prod
		exec_unique(self.phdb.iteritems(), lambda (nsid, prod): prod.base_p is not None,
		  run_r, None, "%s db: relations" % name, LOG.info)

		total = len(self.phdb)
		lab_p, id_p = zip(*(("%s\\n%s" % (prod.size(), '\\n'.join(prod.rep_t[0:4])),
		  (nsid, i)) for i, (nsid, prod) in enumerate(self.phdb.iteritems()))) if self.phdb else ([], [])
		id_p = dict(id_p)

		# generate producer graph
		arc_s, arc_t, edges = edge_array(total)
		for i, prod in enumerate(self.phdb.itervalues()):
			for nsid in prod.docgr.vs.select(prod.prange())["id"]:
				arc_s.append(i)
				arc_t.append(id_p[nsid])

		sz = [log(prod.size()) for prod in self.phdb.itervalues()]
		v_attr = {"id": list(self.phdb.iterkeys()), "label": lab_p, "height": sz, "width": sz}

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


	def inferProdArc(self, prod_s, prod_t):
		"""
		Infer arcs and their weights between the given producers.

		This implementation uses selectTagsFromClusters() on the producers'
		representative tags.

		@param prod_s: source producer
		@param prod_t: target producer
		"""
		tags, hitags = self.selectTagsFromClusters(prod_s.rep_t, prod_t.rep_t)
		prodmap = prod_t.tagScores(tags)
		prodmap.update(hitags)
		return prodmap


	def selectTagsFromClusters(self, tset_s, tset_t):
		"""
		Selects tags from the intersection between each cluster for a source
		tag, and the target tagset. The representatives of the cluster are also
		selected, if the intersection is large enough.

		@param tset_s: source tag-set
		@param tset_t: target tag-set
		@return: ([tags], {hitag:weight}) where tags is a subset of tset_t, and
		         hitags are more "general" tags that describe tset_t, but which
		         are not necessarily contained in it
		"""
		tags = set()
		hitags = {}
		if type(tset_t) != set:
			tset_t = set(tset_t)

		for tag in tset_s:
			for cluster in self.tcdb[tag]:
				tset_x = tset_t.intersection(cluster)
				tags.update(tset_x)

				if 3*len(tset_x) > len(cluster):
					# if intersection is big enough, link to "representative" tags of cluster
					# on flickr, this is the first 3 tags
					attr = len(tset_x)/float(len(cluster))
					for rt in cluster[0:3]:
						if rt in hitags:
							hitags[rt].append(attr)
						else:
							hitags[rt] = [attr]

		# FIXME HIGH rethink whether these weights are theoretically sound
		return tags, dict((tag, union_ind(*attrs)) for tag, attrs in hitags.iteritems())


	def generateTGraphs(self):

		name = "tgraphs"

		# generate docsets for new producers
		sprd = self.selectCommunities()
		pmap = dict(("%04d" % i, pset) for i, pset in enumerate(sprd))

		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(set(chain(*(self.ppdb[self.prodgr.vs[p]["id"]] for p in pmap[nsid]))), self.ptdb)
			prod.inferScores()
			prod.repTag(cover=0) # TWEAK
			self.pgdb[nsid] = prod
		exec_unique(pmap, self.pgdb, run_p, None, "%s db: producers" % name, LOG.info)

		edges, arc_a = infer_arcs(sprd, len(self.prodgr.vs), ratio=3) # TWEAK

		id_p = dict(("%04d" % i, i) for i in xrange(0, len(sprd)))
		self.sprdgr = Graph(len(sprd), list(edges), directed=True,
		  vertex_attrs={"id":list("%04d" % i for i in xrange(0, len(sprd))), "label":[len(com) for com in sprd]})
		g = self.sprdgr
		#g.write_dot("sprdgr.dot")
		#g.write("sprdgr.graphml")
		LOG.info("%s db: generated producer graph" % name)

		# generate content arcs between producers
		# FIXME NOW this still uses up far too much memory :/
		import gc
		#gc.set_debug(gc.DEBUG_LEAK)
		def run((nsid, prod)):
			prodmap = dict((rnsid, self.inferProdArc(prod, self.pgdb[rnsid])) for rnsid in g.vs.select(g.successors(id_p[nsid]))["id"])
			self.pgdb.sync()
			prod.initProdArcs(prodmap)
			del prodmap
			gc.collect()
			self.pgdb[nsid] = prod
		exec_unique(self.pgdb.iteritems(), lambda (nsid, prod): prod.base_p is not None,
		  run, None, "%s db: relations" % name, LOG.info)


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

		return [com for com in comm if len(com) > len(self.prodgr.vs)**0.5]


	def createAllObjects(self): #producer_graph
		# TODO NOW
		#Given a graph of producers, generate tgraphs from the narrow peak, and
		#indexes from the fat tail.

		#ie. 80-20 rule, but actually decide a precise way of doing these.
		#- half of area-under-graph for each?
		raise NotImplementedError()



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
