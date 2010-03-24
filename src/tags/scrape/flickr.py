# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys

from flickrapi import FlickrAPI, FlickrError
from xml.etree.ElementTree import dump
from futures import ThreadPoolExecutor
from functools import partial
from tags.scrape.object import Node, NodeSample


class SafeFlickrAPI(FlickrAPI):

	verbose = 0

	def __init__(self, api_key, secret=None, token=None, store_token=False, cache=True, **kwargs):
		FlickrAPI.__init__(self, api_key, secret=secret, token=token, store_token=store_token, cache=cache, **kwargs)
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
					if e.message[:6] == "Error:":
						code = int(e.message.split(':', 2)[1].strip())

						if code == 0:
							err = e

						else:
							raise
					else:
						raise
				except URLError, e:
					err = e
				except IOError, e:
					err = e

				self.log("retrying FlickrAPI call %s(%s) in %.4fs due to: %s" % (attrib, args, 1.2**i, err), 2)
				sleep(1.2**i)
				i = i+1 if i < 40 else 0

		return wrapper


	def log(self, msg, lv):
		if lv <= SafeFlickrAPI.verbose:
			print >>sys.stderr, msg


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
				self.log("sample: %s/%s (added %s)" % (len(s), size, id), 1)

		s.build(False)
		return s


	def scrapeSets(self, nsid):

		sets = self.photosets_getList(user_id=nsid).getchildren()[0].getchildren()
		phids = set()
		tagmap = {}

		with ThreadPoolExecutor(max_threads=16) as x:

			# TODO LOW this API call uses per_page/page args but ignore for now, 500 is plenty
			# FIXME NORM handle FlickrErrors
			res = x.run_to_results(partial(self.photosets_getPhotos, photoset_id=pset.get("id")) for pset in sets)

			for r in res:
				photos = r.getchildren()[0].getchildren()
				phids.update(set(p.get("id") for p in photos))

			def wrap(photo_id=None):
				r = self.tags_getListPhoto(photo_id=photo_id)
				self.log("got tags for photo %s" % photo_id, 4)
				return r, photo_id

			# FIXME NORM handle FlickrErrors
			res = x.run_to_results(partial(wrap, photo_id=id) for id in phids)

			for r, phid in res:
				# TODO NOW generate {tag:attr} instead of [tag]
				tagmap[phid] = dict((tag.get("raw"), 1) for tag in r.getchildren()[0].getchildren()[0].getchildren())

		self.log("got photos for user %s" % nsid, 1)
		return tagmap

