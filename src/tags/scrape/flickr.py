# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys

from flickrapi import FlickrAPI
from tags.scrape.object import ID, IDSample


class SafeFlickrAPI(FlickrAPI):


	def __init__(self, token=None, store_token=False, cache=True, *args, **kwargs):
		FlickrAPI.__init__(self, token=token, store_token=store_token, cache=cache, *args, **kwargs)
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

				print >>sys.stderr, "retrying FlickrAPI call %s(%s) in %.4fs due to: %s" % (attrib, args, 1.2**i, err)
				sleep(1.2**i)
				i = i+1 if i < 40 else 0

		return wrapper


	def getNSID(self, n):
		return self.people_findByUsername(username=n).getchildren()[0].get("nsid")


	def makeID(self, nsid):
		out = self.contacts_getPublicList(user_id=nsid).getchildren()[0].getchildren()
		return ID(nsid, dict([(elem.get("nsid"), 0 if int(elem.get("ignored")) else 1) for elem in out]))


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

		s = IDSample()
		q = [self.getNSID(seed)]

		while len(s) < size:
			id = next(s, q)
			if id is not None: print >>sys.stderr, "sample: %s/%s (added %s)" % (len(s), size, id)

		s.build()
		return s

