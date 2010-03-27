#!/usr/bin/python

import sys, shelve
from time import time, ctime
from itertools import chain

from tags.scrape.flickr import SafeFlickrAPI, FlickrSample
from tags.scrape.object import NodeSample, Node
from tags.scrape.util import signal_dump, dict_load, dict_save
from xml.etree.ElementTree import dump

NAME = "scrape.py"
VERSION = 0.01

ROUNDS = {
"social":
	("Scraping social network", [".soc.graphml", ".soc.dot"]),
"photo":
	("Scraping photos", [".up.dict"]),
"group":
	("Scraping groups", [".g2.dict"]),
"generate":
	("Generating data", []),
}


def first_nonwhite(line):
	i = 0
	for c in line:
		if c != ' ':
			break
		i += 1
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

	signal_dump()

	SafeFlickrAPI.verbose = kwargs.pop("verbose")
	scr = Scraper(**kwargs)

	if (round == "interact"):
		return scr.interact()

	f = getattr(scr, "round_%s" % round)

	if len(args) > 0 and args[0].lower() == "help":
		print >>sys.stderr, fmt_pydoc(f.__doc__)
		return 0

	else:
		t = time()
		print >>sys.stderr, "%s at %s" % (ROUNDS[round][0], ctime())
		ret = f(*args)
		print >>sys.stderr, "completed in %.4fs" % (time()-t)
		return ret


class Scraper():


	def __init__(self, api_key, secret, token, output="scrape", database="."):
		self.ff = SafeFlickrAPI(api_key, secret, token)
		self.out = output
		self.dbp = database
		self.ptdb = None


	def outfp(self, suffix):
		return open("%s.%s" % (self.out, suffix), 'w')


	def infp(self, suffix):
		return open("%s.%s" % (self.out, suffix))


	def db(self):
		if self.ptdb is None:
			self.ptdb = shelve.open("%s/%s.pt.db" % (self.dbp, self.out))
			print >>sys.stderr, "%s/%s.pt.db opened" % (self.dbp, self.out)
		return self.ptdb


	def round_social(self, seed, size):
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


	def round_photo(self):
		"""
		Scrape photos and collect their tags.

		Round "social" must already have been executed
		"""
		s0 = NodeSample(self.infp("soc.graphml"))

		upmap = self.ff.scrapePhotos(s0.graph.vs["id"])
		dict_save(upmap, self.outfp("up.dict"))

		photos = set(i for i in chain(*upmap.itervalues()))
		del upmap
		self.ff.commitPhotoTags(photos, self.db())


	def round_group(self):
		"""
		Scrape groups and collect their photos.

		Round "photo" must already have been executed
		"""
		s0 = NodeSample(self.infp("soc.graphml"))
		upmap = dict_load(self.infp("up.dict"))

		g2map = self.ff.scrapeGroups(s0.graph.vs["id"], upmap)
		dict_save(g2map, self.outfp("g2.dict"))
		dict_save(upmap, self.outfp("up.dict"))

		photos = set(i for i in chain(*(p for u, p in g2map.itervalues())))
		del g2map, upmap
		self.ff.commitPhotoTags(photos, self.db())


	def round_generate(self):
		"""
		Generate objects from the scraped data.

		Round "group" must already have been executed.
		"""
		graph = NodeSample(self.infp("soc.graphml")).graph
		upmap = dict_load(self.infp("up.dict"))
		g2map = dict_load(self.infp("g2.dict"))

		ss = FlickrSample(graph, self.db(), upmap, g2map)
		import code; code.interact(local=locals())


	def interact(self):
		"""
		Start up a python interpreter with access to this Scraper
		"""
		import code
		code.interact(banner="[Scraper interactive console]\n>>> self\n%r" % self, local=locals())


if __name__ == "__main__":

	from optparse import OptionParser, OptionGroup, IndentedHelpFormatter
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Scrapes data from flickr. ROUND is one of: %s" % ROUNDS.keys(),
	  version = VERSION,
	  formatter = IndentedHelpFormatter(max_help_position=25)
	)

	config.add_option("-o", "--output", type="string", metavar="OUTPUT", default="scrape",
	  help = "Output file prefix (extensions will be added to it)")
	config.add_option("-b", "--database", type="string", metavar="DATABASE", default=".",
	  help = "Path to the photo-tag database (default .)")
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
