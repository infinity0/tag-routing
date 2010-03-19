#!/usr/bin/python
# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from flickrapi import FlickrAPI
from xml.etree.ElementTree import dump

def initAPI(api_key, api_sec, token=None):

	if token:
		ff = FlickrAPI(api_key, api_sec, token=token, store_token=False, cache=True)
	else:

		ff = FlickrAPI(api_key, api_sec, store_token=False, cache=True)
		(token, frob) = ff.get_token_part_one(perms='read')
		if not token:
			print "A browser window should have opened asking you to authorise this program."
			raw_input("Press ENTER when you have done so... ")
		ff.get_token_part_two((token, frob))

	return ff

if __name__ == "__main__":
	ff = initAPI(*sys.argv[1:])

	a = ff.tags_getRelated(tag="dune")
	dump(a)

