#!/usr/bin/python

import sys

from tags.scrape.flickr import SafeFlickrAPI
from tags.scrape.object import NodeSample, Node
from xml.etree.ElementTree import dump
from time import time, ctime

NAME = "scrape.py"
VERSION = 0.01

ROUNDS = {
"soc":
	("social network", [".soc.graphml", ".soc.dot"]),
"photo":
	("photos", [".doc.graphml"]),
}


def first_nonwhite(line):
	i = 0
	for i, c in enumerate(line):
		if c != ' ':
			break
	return i


def fmt_pydoc(sss):
	lines = sss.split("\n")

	while not lines[0]:
		lines.pop(0)

	if len(lines) > 0:
		indent = first_nonwhite(lines[0].replace('\t', '    '))

		for (i, line) in enumerate(lines):
			sline = line.replace('\t', '    ')
			iii = first_nonwhite(sline)
			lines[i] = sline[iii:] if iii < indent else sline[indent:]

	return "\n".join(lines)


def main(round, *args, **kwargs):

	SafeFlickrAPI.verbose = kwargs.pop("verbose")

	scr = Scraper(**kwargs)
	f = getattr(scr, "scrape_%s" % round)

	if (args[0].lower() == "help"):

		print >>sys.stderr, fmt_pydoc(f.__doc__)
		return 0

	else:

		t = time()
		print >>sys.stderr, "Scraping %s at %s" % (ROUNDS[round][0], ctime())
		ret = f(*args)
		print >>sys.stderr, "completed in %.4fs" % (time()-t)
		return ret


class Scraper():


	def __init__(self, api_key, secret, token, output="scrape"):
		self.ff = SafeFlickrAPI(api_key, secret, token)
		self.out = output


	def outfp(self, suffix):
		return open("%s.%s" % (self.out, suffix), 'w') if self.out else sys.stdout


	def scrape_soc(self, seed, size):
		"""
		Scrape the social network using breadth-search.

		@param seed: Seed identity
		@param size: Number of identities to scrape
		"""

		size = int(size)

		ss = self.ff.scrapeIDs(seed, size)
		gg = ss.graph

		gg.write_graphml(self.outfp("soc.graphml"))
		gg.write_dot(self.outfp("soc.dot"))

		return 0


	def scrape_photo(self, socf):
		"""
		Scrape photos and collect their tags

		@param socf: GraphML file describing the social network to get photos of.
		"""
		s0 = NodeSample(socf)
		g0 = s0.graph;

		ss = self.ff.scrapeUserPhotos(g0.vs["id"])
		gg = ss.graph

		gg.write_graphml(self.outfp("doc.graphml"))
		gg.write_dot(self.outfp("doc.dot"))


if __name__ == "__main__":

	from optparse import OptionParser, OptionGroup, IndentedHelpFormatter
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Scrapes data from flickr. ROUND is one of: soc photo",
	  version = VERSION,
	  formatter = IndentedHelpFormatter(max_help_position=25)
	)

	config.add_option("-o", "--output", type="string", metavar="OUTPUT", default="scrape",
	  help = "Output file prefix (extensions will be added to it)")
	config.add_option("-k", "--api-key", type="string", metavar="APIKEY",
	  help = "Flickr API key")
	config.add_option("-s", "--secret", type="string", metavar="SECRET",
	  help = "Flickr API secret")
	config.add_option("-t", "--token", type="string", metavar="TOKEN",
	  help = "Flickr API authentication token")
	config.add_option("-v", "--verbose", type="int", metavar="VERBOSE", default=0,
	  help = "Verbosity level")

	(opts, args) = config.parse_args()

	if len(args) < 1:
		config.print_help()
		sys.exit(2)
	else:
		sys.exit(main(*args, **opts.__dict__))
