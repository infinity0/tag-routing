# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, socket
from time import time
from functools import partial
from itertools import chain
from threading import local as ThreadLocal
from httplib import HTTPConnection, ImproperConnectionState, HTTPException
from xml.etree.ElementTree import dump

from flickrapi import FlickrAPI, FlickrError
from futures import ThreadPoolExecutor
from igraph import Graph

from tags.scrape.object import Node, NodeSample, Producer
from tags.scrape.util import intern_force


class SafeFlickrAPI(FlickrAPI):

	verbose = 0

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
					if FlickrError_code(e) == 0:
						err = e
					else:
						raise
				except (URLError, IOError, ImproperConnectionState, HTTPException), e:
					err = e

				self.log("FlickrAPI: wait %.4fs to retry %s(%s) due to %s" % (1.2**i, attrib, args, repr(err)), 2)
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
				self.log("connection opened: %s" % FlickrAPI.flickr_host, 3)

			self.thr.conn.request("POST", FlickrAPI.flickr_rest_form, post_data,
				{"Content-Type": "application/x-www-form-urlencoded"})
			reply = self.thr.conn.getresponse().read()

		except (ImproperConnectionState, socket.error), e:
			self.log("connection error: %s" % repr(e), 3)
			self.thr.conn.close()
			del self.thr.conn
			raise

		# Store in cache, if we have one
		if self.cache is not None:
			self.cache.set(post_data, reply)

		return reply


	def log(self, msg, lv):
		# TODO HIGH use the logging module
		if lv <= SafeFlickrAPI.verbose:
			print >>sys.stderr, "%.4f | %s | %s" % (time(), lv, msg)
			sys.stderr.flush()


	def log2(self, msg, lv):
		# TODO HIGH use the logging module
		if lv <= SafeFlickrAPI.verbose:
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
				self.log("id sample: %s/%s (added %s)" % (len(s), size, id), 1)

		s.build(False)
		return s


	def getSetPhotos(self, sets, x):
		"""
		Gets sets of a given user and all photos belonging to it

		@param sets: an iterable of set ids
		@param x: an executor to execute calls in parallel
		@return: {set:[photos]}
		"""
		spmap = {}

		#[s.get("id") for s in self.photosets_getList(user_id=nsid).getchildren()[0].getchildren()]
		for r in x.run_to_results(partial(self.photosets_getPhotos, photoset_id=sid) for sid in sets):
			pset = r.getchildren()[0]
			sid = pset.get("id")
			spmap[sid] = [p.get("id") for p in pset.getchildren()]
			self.log("set: got %s photos (%s)" % (len(pset), sid), 4)

		return spmap


	def commitPhotoTags(self, photos, ptdb, conc_m=36):
		"""
		Gets the tags of all the given photos and saves these to a database

		@param photos: an iterable of photo ids
		@param ptdb: an open database of {photo:[tag]}
		@param x: an executor to execute calls in parallel
		"""
		def run(phid, i):
			r = None if phid in ptdb else self.tags_getListPhoto(photo_id=phid)
			return r, phid, i

		with ThreadPoolExecutor(max_threads=conc_m) as x:
			for r, phid, i in x.run_to_results(partial(run, phid, i) for i, phid in enumerate(photos)):
				if r is None: continue
				photo = r.getchildren()[0]
				# TODO NOW shorten geo tags to nearest degree, eg. "geo:lon=132.453516" -> "geo:lon=132"
				ptdb[phid] = [intern_force(tag.get("raw")) for tag in photo.getchildren()[0].getchildren()]
				self.log2("photo db: %s/%s (added %s tags for %s)" % (i+1, len(photos), len(photo.getchildren()[0]), phid), 2)

		self.log("photo db: %s photos added" % (len(photos)), 1)


	def scrapePhotos(self, users, conc_m=36):
		"""
		Scrape photos of the given users.

		@return: {user:[photo]}
		"""
		upmap = {}

		def run(nsid, i):
			# TODO NORM uses per_page/page args; set a sensible default limit
			photos = self.people_getPublicPhotos(user_id=nsid, per_page=256).getchildren()[0].getchildren()
			return photos, nsid, i

		# managers need to be handled by a different executor from the workers
		# otherwise it deadlocks when the executor fills up with manager tasks
		# since worker tasks don't get a chance to run
		with ThreadPoolExecutor(max_threads=conc_m) as x:
			for photos, nsid, i in x.run_to_results(partial(run, nsid, i) for i, nsid in enumerate(users)):
				upmap[nsid] = [p.get("id") for p in photos]
				self.log2("photo sample: %s/%s (added user %s)" % (i+1, len(users), nsid), 1)

		self.log("photo sample: %s users added" % (len(users)), 1)
		return upmap


	def getGroupPools(self, groups, nsid, x):
		"""
		Gets photos in group pools of the given user

		@param groups: an iterable of group ids
		@return: {group:[photo]}
		"""
		gpmap = {}

		def run(gid):
			try:
				r = self.groups_pools_getPhotos(group_id=gid, user_id=nsid, per_page=256)
			except FlickrError, e:
				if FlickrError_code(e) == 2:
					r = None
				else:
					raise
			return r, gid

		for r, gid in x.run_to_results(partial(run, gid) for gid in groups):
			if r is None: continue
			photos = r.getchildren()[0].getchildren()
			gpmap[gid] = [p.get("id") for p in photos]
			#self.log("group: got %s photos (%s)" % (len(photos), gid), 4)

		return gpmap


	def scrapeGroups(self, users, upmap, conc_m=8, conc_w=0):
		"""
		Scrapes all groups of the given users.

		@param upmap: dict of {user:[photos]} - this will be updated if new photos are found
		@return: {group:([users],[photos])}
		"""
		g2map = {}
		ppp = set() # list of photos to retrieve tags for

		if not conc_w: conc_w = conc_m*3

		with ThreadPoolExecutor(max_threads=conc_m) as x:
			with ThreadPoolExecutor(max_threads=conc_w) as x2:

				def run(x2, nsid, i):
					gs = self.people_getPublicGroups(user_id=nsid, per_page=256).getchildren()[0].getchildren()
					gpmap = self.getGroupPools((g.get("nsid") for g in gs), nsid, x2)
					return gpmap, nsid, i

				for gpm, nsid, i in x.run_to_results(partial(run, x2, nsid, i) for i, nsid in enumerate(users)):
					for gid, ps in gpm.iteritems():
						if gid not in g2map:
							g2map[gid] = ([nsid], ps)
						else:
							g2map[gid][0].append(nsid)
							g2map[gid][1].extend(ps)

					self.log2("group sample: %s/%s (added user %s)" % (i+1, len(users), nsid), 1)

		# ignore groups with 1 user, 0 photos
		for gid in g2map.keys():
			users, photos = g2map[gid]
			if len(users) <= 1 and len(photos) <= 0:
				del g2map[gid]

		self.log("group sample: added %s users" % (len(users)), 1)
		return g2map


class FlickrProducer(Producer):

	def __init__(self, id, user, photos):
		"""
		Creates a new Producer from a flickr entity

		@param id: identity string
		@param user: whether this is a user-producer or a group-producer
		"""
		Producer.__init__(self, id, set(photos))
		self.user = True if user else False


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

		for (i, (gnsid, (users, photos))) in enumerate(g2map.iteritems()):
			gid = i + len(upmap)
			self.id_g[gnsid] = gid
			graph.vs[gid]["id"] = gnsid
			graph.vs[gid]["isgroup"] = True

			for nsid in users:
				uid = self.id_u[nsid]
				graph.add_edges([(uid, gid), (gid, uid)])
				# TODO HIGH weights for these

		# graph of social links between producers
		self.gs = graph

		# graph of content links between producers
		self.gd = Graph(len(self.gs.vs), directed=True)

		# {nsid:Producer}
		self.ps = dict((nsid, Producer(dset, self.gs, self.gd, vid)) for nsid, vid, dset in
			chain(((nsid, self.id_u[nsid], pset) for nsid, pset in upmap.iteritems()),
				  ((nsid, self.id_g[nsid], gr[1]) for nsid, gr in g2map.iteritems())))

		#print self.gs.summary()
		self.inferGroupArcs()


	def inferGroupArcs(self):
		"""
		Given a sample of groups, infer arcs between them.
		"""
		# FIXME HIGH make sure this can only be run at the appropriate time

		gidbase = len(self.id_u)
		gidsize = len(self.id_g)

		r = len(self.id_u)**0.5
		edges = []

		for sgid in xrange(gidbase, gidbase+gidsize):
			sgnsid = self.gs.vs[sgid]["id"]
			smem = self.gs.successors(sgid)

			for tgid in xrange(sgid+1, gidbase+gidsize):
				tgnsid = self.gs.vs[tgid]["id"]
				tmem = self.gs.successors(tgid)
				imem = set(smem) & set(tmem)

				# keep arc only if (significantly?) better than independent intersections
				# we use directed rather than undirected arcs since we want to be able to
				# consider asymmetric relationships

				if r * len(imem) > len(smem):
					edges.append((sgid, tgid))

				if r * len(imem) > len(tmem):
					edges.append((tgid, sgid))

		# add all edges at once, since we need successors() to remain free of group-producers
		# this is also a lot faster for igraph
		self.gs.add_edges(edges)

		poss = gidsize*gidsize
		print "%s group-group arcs added (/%s, ~%.4f) between %s groups, fuzz = %.4f = 1/sqrt(users)" % (
			len(edges), poss, float(len(edges))/poss, gidsize, 1/r)


	def createAllObjects(self): #producer_graph
		# TODO NOW
		#Given a graph of producers, generate tgraphs from the narrow peak, and
		#indexes from the fat tail.

		#ie. 80-20 rule, but actually decide a precise way of doing these.
		#- half of area-under-graph for each?
		raise NotImplemented()


	def generateSuperProducer(self): #producer_graph, seed, size
		# TODO NORM optional, possibly this is necessary for good tgraph generation
		#Given a graph of producers, generate a superproducer from the seed group
		#and the given size, based on clustering analysis
		raise NotImplemented()


	def generateSuperProducers(self): #producer_graph
		# TODO NORM
		#Given a graph of producers, generate superproducers following a power-law
		#distribution
		#- then make social links between these supergroups and the normal groups
		#- TODO HOW??
		raise NotImplemented()


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
