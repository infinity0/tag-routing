#!/usr/bin/python

import sys

from tags.scrape.flickr import SafeFlickrAPI
from tags.scrape.object import IDSample, ID
from xml.etree.ElementTree import dump

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

	scr = Scraper(**kwargs)
	f = getattr(scr, "scrape_%s" % round)

	if (args[0].lower() == "help"):

		print >>sys.stderr, fmt_pydoc(f.__doc__)
		return 0

	else:

		print >>sys.stderr, "Scraping %s..." % ROUNDS[round][0]
		return f(*args)


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
		gg = ss.build()

		gg.write_graphml(self.outfp("soc.graphml"))
		gg.write_dot(self.outfp("soc.dot"))

		return 0


	def scrape_photo(self, socf):
		"""
		Scrape photos and collect their tags

		@param socf: GraphML file describing the social network to get photos of.
		"""
		ss = IDSample(socf)
		gg = ss.graph;

		import code
		code.interact(local=locals())

		#for nsid in gg.vs["id"]:
		#	print nsid


if __name__ == "__main__":

	from optparse import OptionParser, OptionGroup, IndentedHelpFormatter
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Scrapes data from flickr. ROUND is one of: soc photo",
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

	if len(args) < 1:
		config.print_help()
		sys.exit(2)
	else:
		sys.exit(main(*args, **opts.__dict__))
