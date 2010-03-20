#!/usr/bin/python

import sys

from tags.scrape.flickr import initAPI, getNSID, makeID, scrapeID
from tags.scrape.object import IDSample, ID
from xml.etree.ElementTree import dump

NAME = "scrape.py"
VERSION = 0.01


def main(seed, size, **opts):

	size = int(size)

	ff = initAPI(**opts)
	ss = scrapeID(ff, seed, size)
	gg = ss.build_graph()

	print gg.to_string()

	return 0


if __name__ == "__main__":

	from optparse import OptionParser, OptionGroup, IndentedHelpFormatter
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [SEED] [SIZE]",
	  description = "...",
	  version = VERSION,
	  formatter = IndentedHelpFormatter(max_help_position=25)
	)

	config.add_option("-k", "--api-key", type="string", metavar="APIKEY",
	  help = "Flickr API key")
	config.add_option("-s", "--api-sec", type="string", metavar="SECRET",
	  help = "Flickr API secret")
	config.add_option("-t", "--token", type="string", metavar="TOKEN",
	  help = "Flickr API authentication token")

	(opts, args) = config.parse_args()

	sys.exit(main(*args, **opts.__dict__))
