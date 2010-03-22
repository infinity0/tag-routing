#!/usr/bin/python

import sys

from tags.scrape.flickr import SafeFlickrAPI
from tags.scrape.object import IDSample, ID
from xml.etree.ElementTree import dump

NAME = "scrape.py"
VERSION = 0.01


def main(seed, size, output=None, **opts):

	size = int(size)

	ff = SafeFlickrAPI(**opts)
	ss = ff.scrapeIDs(seed, size)
	gg = ss.build()

	gg.write_graphml(open("%s.graphml" % output, 'w') if output else sys.stdout)
	gg.write_dot(open("%s.dot" % output, 'w') if output else sys.stdout)

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
	config.add_option("-s", "--secret", type="string", metavar="SECRET",
	  help = "Flickr API secret")
	config.add_option("-t", "--token", type="string", metavar="TOKEN",
	  help = "Flickr API authentication token")
	config.add_option("-o", "--output", type="string", metavar="OUTPUT",
	  help = "Output file prefix (extensions will be added to it)")

	(opts, args) = config.parse_args()

	if len(args) < 2:
		config.print_help()
		sys.exit(2)
	else:
		sys.exit(main(*args, **opts.__dict__))
