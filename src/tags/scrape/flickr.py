# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, socket, logging
from time import time
from array import array
from functools import partial
from itertools import chain
from threading import local as ThreadLocal
from httplib import HTTPConnection, ImproperConnectionState, HTTPException
from xml.etree.ElementTree import dump

from flickrapi import FlickrAPI, FlickrError
from futures import ThreadPoolExecutor
from igraph import Graph

from tags.scrape.object import Node, NodeSample, Producer
from tags.scrape.util import intern_force, infer_arcs, repr_call, enumerate_cb


LOG = logging.getLogger(__name__)
LOG.setLevel(1)

DEFAULT_CONC_MAX = 36


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


	def log2(self, msg, lv):
		# TODO HIGH use the logging module
		print >>sys.stderr, "%.4f | %s | %s                  \r" % (time(), lv, msg),
		sys.stderr.flush()


	def getNSID(self, n):
		return self.people_findByUsername(username=n).getchildren()[0].get("nsid")


	def makeID(self, nsid):
		out = self.contacts_getPublicList(user_id=nsid).getchildren()[0].getchildren()
		return Node(nsid, dict((elem.get("nsid"), 0 if int(elem.get("ignored")) else 1) for elem in out))


	def scrapeIDs(self, seed, size):

		if type(size) != int:
			raise TypeError

		def next(ss, qq):
			id = qq.pop(0)
			if id in ss: return None
			node = self.makeID(id)
			qq.extend(node.out.keys())
			ss.add_node(node)
			return id

		s = NodeSample()
		q = [self.getNSID(seed)]

		while len(s) < size:
			id = next(s, q)
			if id is not None:
				LOG.info("id sample: %s/%s (added %s)" % (len(s), size, id))

		s.build(False)
		return s


	def scrapeGroups(self, users, conc_m=DEFAULT_CONC_MAX):
		"""
		Scrapes all groups of the given users.

		@return: {group:[users]}
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

		self.execAllUnique(users, gumap, "gid sample db", run, post, conc_m)
		return gumap


	def getSetPhotos(self, sets, x):
		"""
		Gets sets of a given user and all photos belonging to it

		@param sets: an iterable of set ids
		@param x: an executor to execute calls in parallel
		@return: {set:[photos]}
		"""
		spmap = {}

		#[s.get("id") for s in self.photosets_getList(user_id=nsid).getchildren()[0].getchildren()]
		for r in x.run_to_results_any(partial(self.photosets_getPhotos, photoset_id=sid) for sid in sets):
			pset = r.getchildren()[0]
			sid = pset.get("id")
			spmap[sid] = [p.get("id") for p in pset.getchildren()]
			LOG.debug("set: got %s photos (%s)" % (len(pset), sid), 6)

		return spmap


	def execAllUnique(self, items, done, name, run, post, conc_m=DEFAULT_CONC_MAX, assume_unique=False):
		"""
		Gets the tags of all the given photos and saves these to a database

		@param items: a list of items
		@param done: a collection of done items (can be a database or dict)
		@param name: name of the batch, used in logging messages
		@param run: job for worker threads; takes (item) parameter
		@param post: job for main thread; takes (item, i, res) parameters
		@param conc_m: max concurrent worker threads to run
		@param assume_unique: whether to assume [items] is unique
		"""
		if not assume_unique and type(items) != set and type(items) != dict:
			items = set(items)
		tasks = [partial(lambda it: (it, run(it)), it) for it in items if it not in done]
		total = len(tasks)
		LOG.info("%s: %s submitted, %s accepted" % (name, len(items), total))

		i = -1
		with ThreadPoolExecutor(max_threads=conc_m) as x:
			for i, (it, res) in enumerate_cb(x.run_to_results_any(tasks),
			  LOG.info, "%s: %%(i1)s/%s %%(it)s" % (name, total), expected_length=total):
				post(it, i, res)

		LOG.info("%s: %s submitted, %s accepted, %s completed" % (name, len(items), total, (i+1)))


	def commitPhotoTags(self, photos, ptdb, conc_m=DEFAULT_CONC_MAX):
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
			ptdb[phid] = [intern_force(tag.text) for tag in tags if ":" not in tag.text]

		self.execAllUnique(photos, ptdb, "photo-tag db", run, post, conc_m)


	def commitUserPhotos(self, users, ppdb, conc_m=DEFAULT_CONC_MAX):
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

		self.execAllUnique(users, ppdb, "producer db (user)", run, post, conc_m)


	def commitGroupPhotos(self, gumap, ppdb, conc_m=DEFAULT_CONC_MAX):
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

		self.execAllUnique(gumap, ppdb, "producer db (group)", run, post, conc_m)


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


	def invertProducerMap(self, ppdb, pcdb, conc_m=DEFAULT_CONC_MAX):
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


	def commitTagClusters(self, tags, tcdb, conc_m=DEFAULT_CONC_MAX):
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

		self.execAllUnique(tags, tcdb, "cluster db", run, post, conc_m)


class FlickrSample():


	def __init__(self, graph, ptdb, upmap, g2map):
		"""
		Create a new FlickrSample from the given arguments

		@param graph: [input] social network graph
		@param ptdb: an open database of {photo:[tag]}
		@param upmap: {user:[photo]}
		@param g2map: {group:([user],[photo])}
		"""

		if len(upmap) != len(graph.vs):
			raise ValueError("graph / upmap size not same")
			# don't need to check nsids are the same since we do that implicitly
			# when we create self.ps

		if not hasattr(ptdb, "sync"): # we don't actually use sync(), we just want a type-check that works most of the type
			raise TypeError("not a database")

		self.ptdb = ptdb

		self.id_u = {} # map of nsid:graphid
		self.id_g = {} # map of nsid:graphid

		# add existing social links between users and groups

		for (i, v) in enumerate(graph.vs):
			self.id_u[v["id"]] = i
			v["isgroup"] = False

		graph.add_vertices(len(g2map))
		edges = []
		for (i, (gnsid, (users, photos))) in enumerate(g2map.iteritems()):
			gid = i + len(upmap)
			self.id_g[gnsid] = gid
			graph.vs[gid]["id"] = gnsid
			graph.vs[gid]["isgroup"] = True

			for nsid in users:
				uid = self.id_u[nsid]
				edges.extend([(uid, gid), (gid, uid)])
				# TODO HIGH weights for these
		graph.add_edges(edges)
		del edges

		# graph of social links between producers
		self.gs = graph

		# graph of content links between producers
		self.gd = Graph(len(self.gs.vs), directed=True)

		# {nsid:Producer}
		self.ps = dict((nsid, Producer(dset, self.gs, self.gd, vid)) for nsid, vid, dset in
			chain(((nsid, self.id_u[nsid], pset) for nsid, pset in upmap.iteritems()),
				  ((nsid, self.id_g[nsid], gr[1]) for nsid, gr in g2map.iteritems())))

		#print self.gs.summary()
		for p in self.ps.itervalues():
			p.invertMap(self.ptdb)
			p.inferTagArcs()

		#self.inferGroupArcs()


	def inferGroupArcs(self):
		"""
		Given a sample of groups, infer arcs between them.
		"""
		# FIXME HIGH make sure this can only be run at the appropriate time

		gidbase = len(self.id_u)
		gidsize = len(self.id_g)

		mem = [self.gs.successors(gidbase+ogid) for ogid in xrange(0, gidsize)] # users
		#mem = [self.ps[self.gs.vs["id"][gidbase+ogid]].dset for ogid in xrange(0, gidsize)] # photos
		#mem = [self.ps[self.gs.vs["id"][gidbase+ogid]].tag.values() for ogid in xrange(0, gidsize)] # tags
		edges, arc_a = infer_arcs(mem, gidbase)

		# add all edges at once, since we need successors() to remain free of group-producers
		# this is also a lot faster for igraph
		self.gs.add_edges((s+gidbase, t+gidbase) for s, t in edges)

		added = len(arc_a)
		poss = gidsize*gidsize
		print "%s group-group arcs added (/%s, ~%.4f) between %s groups" % (added, poss, float(added)/poss, gidsize)


	def generateSuperProducer(self): #producer_graph, seed, size
		# TODO NORM optional, possibly this is necessary for good tgraph generation
		#Given a graph of producers, generate a superproducer from the seed group
		#and the given size, based on clustering analysis
		raise NotImplementedError()


	def generateSuperProducers(self): #producer_graph
		# TODO NORM
		#Given a graph of producers, generate superproducers following a power-law
		#distribution
		#- then make social links between these supergroups and the normal groups
		#- TODO HOW??
		raise NotImplementedError()


	def generateContentArcs(self):
		"""
		Generate content arcs between producers
		"""
		raise NotImplementedError()


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
