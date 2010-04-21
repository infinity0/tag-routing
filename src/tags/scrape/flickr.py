# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, socket, logging
from collections import deque
from functools import partial
from itertools import chain
from threading import local as ThreadLocal
from urllib2 import URLError
from httplib import HTTPConnection, ImproperConnectionState, HTTPException
from xml.etree.ElementTree import dump
from xml.parsers.expat import ExpatError

from flickrapi import FlickrAPI, FlickrError

from tags.scrape.object import Node, NodeSample, NID
from tags.scrape.util import (intern_force, enumerate_cb, exec_unique, repr_call)


LOG = logging.getLogger(__name__)


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
				except ExpatError, e:
					LOG.warning("SafeFlickrAPI: ABORT %s due to %r" % (repr_call(attrib, **args), e))
					raise

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

# Fix up pyexpat not being able to handle bad unicode chars
# code from http://bugs.python.org/issue5166
import re, flickrapi
_char_tail = u'%s-%s' % (unichr(0x10000), unichr(min(sys.maxunicode, 0x10FFFF))) if sys.maxunicode > 0x10000 else ''
_nontext_sub = re.compile(ur'[^\x09\x0A\x0D\x20-\uD7FF\uE000-\uFFFC%s]' % _char_tail, re.U).sub

def replace_nontext(text, replacement=u'\uFFFD'):
	return _nontext_sub(replacement, text)

def parse_etree(self, rest_xml):
	try:
		return FlickrAPI.parse_etree(self, rest_xml)
	except ExpatError:
		rest_xml = replace_nontext(rest_xml.decode("utf-8"), '_').encode("utf-8")
		return FlickrAPI.parse_etree(self, rest_xml)
flickrapi.rest_parsers['etree'] = parse_etree

