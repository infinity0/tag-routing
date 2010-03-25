# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from time import time

from flickrapi import FlickrAPI, FlickrError
from xml.etree.ElementTree import dump

from threading import local as ThreadLocal
from httplib import HTTPConnection, ImproperConnectionState, HTTPException
import socket

from futures import ThreadPoolExecutor
from functools import partial

from tags.scrape.object import Node, NodeSample


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
		if lv <= SafeFlickrAPI.verbose:
			print >>sys.stderr, "%.4f | %s | %s" % (time(), lv, msg)
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


	def scrapeUserSets(self, nsid, x):

		sets = self.photosets_getList(user_id=nsid).getchildren()[0].getchildren()
		phids = set()
		ptmap = {}

		# TODO LOW this API call uses per_page/page args but ignore for now, 500 is plenty
		# TODO HIGH have a limit on the number of photos scraped. some people have >9000 photos

		# FIXME NORM handle FlickrErrors
		for r in x.run_to_results(partial(self.photosets_getPhotos, photoset_id=pset.get("id")) for pset in sets):
			photos = r.getchildren()[0].getchildren()
			phids.update(set(p.get("id") for p in photos))

		def run(photo_id=None):
			r = self.tags_getListPhoto(photo_id=photo_id)
			return r, photo_id

		# FIXME NORM handle FlickrErrors
		for r, phid in x.run_to_results(partial(run, photo_id=id) for id in phids):
			tags = r.getchildren()[0].getchildren()[0].getchildren()
			self.log("photo: got %s tags (%s)" % (len(tags), phid), 4)
			# TODO NOW generate {tag:attr} instead of [tag]
			# TODO NOW shorten geo tags to nearest degree, eg. "geo:lon=132.453516" -> "geo:lon=132"
			ptmap[phid] = dict((intern_force(tag.get("raw")), 1) for tag in tags)

		return ptmap


	def scrapePhotos(self, users, conc_m=16, conc_w=0):
		"""
		Scrape photos of the given users.

		@return ({user:[photo]}, {photo:[tag]})
		"""
		if not conc_w: conc_w = conc_m*3

		upmap = {} # user: [photo]
		ptmap = {} # photo: [tag]

		# TODO HIGH re-write this to use people_getPublicPhotos

		# managers need to be handled by a different executor from the workers
		# otherwise it deadlocks when the executor fills up with manager tasks
		# since worker tasks don't get a chance to run
		with ThreadPoolExecutor(max_threads=conc_m) as x:
			with ThreadPoolExecutor(max_threads=conc_w) as x2:

				def run(x2, nsid, i):
					ptmap = self.scrapeUserSets(nsid, x2)
					return ptmap, nsid, i

				for ptm, nsid, i in x.run_to_results(partial(run, x2, nsid, i) for i, nsid in enumerate(users)):
					upmap[nsid] = ptm.keys()
					ptmap.update(ptm)
					self.log("photo sample: %s/%s (added user %s)" % (i+1, len(users), nsid), 1)

		return upmap, ptmap


	def scrapeUserPools(self, nsid, x):
		"""
		Scrapes photos in group pools of the given user

		@return {group:[photo]}
		"""
		gpmap = {}

		def run(gid):
			try:
				r = self.groups_pools_getPhotos(group_id=gid, user_id=nsid)
			except FlickrError, e:
				if FlickrError_code(e) == 2:
					r = None
				else:
					raise
			return r, gid

		gs = self.people_getPublicGroups(user_id=nsid).getchildren()[0].getchildren()
		for r, gid in x.run_to_results(partial(run, g.get("nsid")) for g in gs):
			if r is None: continue
			photos = r.getchildren()[0].getchildren()
			gpmap[gid] = [p.get("id") for p in photos]
			self.log("group: got %s photos (%s)" % (len(photos), gid), 4)

		return gpmap


	def scrapeGroups(self, users, conc_m=16, conc_w=0):
		"""
		Scrapes all groups of the given users.

		@return {group:([users],[photos])}
		"""
		g2map = {}

		if not conc_w: conc_w = conc_m*3

		with ThreadPoolExecutor(max_threads=conc_m) as x:
			with ThreadPoolExecutor(max_threads=conc_m) as x2:

				def run(x2, nsid, i):
					gpmap = self.scrapeUserPools(nsid, x2)
					return gpmap, nsid, i

				for gpm, nsid, i in x.run_to_results(partial(run, x2, nsid, i) for i, nsid in enumerate(users)):
					for gid, ps in gpm.iteritems():
						if gid not in g2map:
							g2map[gid] = ([nsid], ps)
						else:
							g2map[gid][0].append(nsid)
							g2map[gid][1].extend(ps)
					self.log("group sample: %s/%s (added user %s)" % (i+1, len(users), nsid), 1)

		return g2map


def intern_force(sss):
	if type(sss) == str:
		return sss
	elif type(sss) == unicode:
		return sss.encode("utf-8")
	else:
		raise TypeError("%s not unicode or string" % sss)


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
