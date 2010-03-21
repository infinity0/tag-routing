# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys

from flickrapi import FlickrAPI
from tags.scrape.object import ID, IDSample


def initAPI(api_key, api_sec, token=None):

	if token:
		ff = FlickrAPI(api_key, api_sec, token=token, store_token=False, cache=False)
	else:

		ff = FlickrAPI(api_key, api_sec, store_token=False, cache=True)
		(token, frob) = ff.get_token_part_one(perms='read')
		if not token:
			print "A browser window should have opened asking you to authorise this program."
			raw_input("Press ENTER when you have done so... ")
		ff.get_token_part_two((token, frob))

	return ff

def getNSID(ff, n):
	return ff.people_findByUsername(username=n).getchildren()[0].get("nsid")

def makeID(ff, nsid):
	out = ff.contacts_getPublicList(user_id=nsid).getchildren()[0].getchildren()
	return ID(nsid, dict([(elem.get("nsid"), 0 if int(elem.get("ignored")) else 1) for elem in out]))

def scrapeID(ff, seed, size):

	if type(size) != int:
		raise TypeError

	def next(ss, qq):
		id = qq.pop(0)
		if id in ss: return None
		node = makeID(ff, id)
		qq.extend(node.out.keys())
		ss.add_node(node)
		return id

	s = IDSample()
	q = [getNSID(ff, seed)]

	while len(s) < size:
		id = next(s, q)
		if id is not None: print >>sys.stderr, "sample: %s/%s (added %s)" % (len(s), size, id)

	s.build()
	return s
