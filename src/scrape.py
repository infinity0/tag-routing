#!/usr/bin/python

import sys, logging, code
from time import time, ctime
from itertools import chain

from tags.scrape.util import futures_patch_nonblocking
futures_patch_nonblocking()
logging.basicConfig(format="%(asctime)s.%(msecs)03d | %(levelno)02d | %(message)s", datefmt="%s")

from igraph import Graph
from tags.scrape.flickr import SafeFlickrAPI, FlickrSample
from tags.scrape.object import NodeSample, Node
from tags.scrape.util import signal_dump, dict_load, dict_save
from xml.etree.ElementTree import dump

NAME = "scrape.py"
VERSION = 0.01


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

	import tags.scrape.flickr

	signal_dump()
	tags.scrape.flickr.LOG.setLevel(kwargs.pop("verbose"))

	with Scraper(**kwargs) as scr:

		f = getattr(scr, "round_%s" % round)

		if len(args) > 0 and args[0].lower() == "help":
			print >>sys.stderr, fmt_pydoc(f.__doc__)
			return 0

		else:
			t = time()
			print >>sys.stderr, "%s at %s" % (Scraper.rounds[round].desc, ctime())
			ret = f(*args)
			print >>sys.stderr, "completed in %.4fs" % (time()-t)
			return ret


class Round():

	def __init__(self, desc, dep=[], out=[]):
		self.desc = desc
		self.dep = dep
		self.out = out


class Scraper():

	_rounds = [
		("social", Round("Scraping social network", [], [".soc.graphml", ".soc.dot"])),
		("group", Round("Scraping groups", ["social"], [".gu.dict"])),
		("photo", Round("Scraping photos", ["group"], [".pp.db"])),
		("invert", Round("Inverting producer mapping", ["photo"], [".pc.db"])),
		("tag", Round("Scraping tags", ["photo"], [".pt.db"])),
		("cluster", Round("Scraping clusters", ["tag"], [".tc.db"])),
		("generate", Round("Generating data", [])),
	]
	rounds = dict(_rounds)
	roundlist = [k for k, r in _rounds]


	def __init__(self, api_key, secret, token, output="scrape", database=".", interact=False):
		self.ff = SafeFlickrAPI(api_key, secret, token)
		self.res = {}
		self.out = output
		self.dbp = database
		self.interact = interact
		self.banner = "[Scraper interactive console]\n>>> self\n%r\n>>> self.ff\n%r" % (self, self.ff)


	def __enter__(self):
		return self


	def __exit__(self, type, value, traceback):
		for path, res in self.res.iteritems():
			res.close()
			print >>sys.stderr, "%s closed" % (path)


	def outfp(self, suffix):
		fn = "%s.%s" % (self.out, suffix)
		fp = open(fn, 'w')
		self.respush(fn, fp, 'w')
		return fp


	def infp(self, suffix):
		fn = "%s.%s" % (self.out, suffix)
		fp = open(fn)
		self.respush(fn, fp, 'r')
		return fp


	def db(self, suffix, writeback=False):
		dbf = "%s/%s.%s.db" % (self.dbp, self.out, suffix)
		try:
			from shelve import BsdDbShelf
			from bsddb import btopen
			db = BsdDbShelf(btopen(dbf), writeback=writeback)
		except Exception:
			import shelve
			db = shelve.open(dbf, writeback=writeback)
		self.respush(dbf, db, 'rw')
		return db


	def respush(self, path, res, mode):
		if path in self.res:
			self.res.pop(path).close()
			print >>sys.stderr, "%s closed" % (path)
		self.res[path] = res
		print >>sys.stderr, "%s opened (%s)" % (path, mode)


	def round_social(self, seed, size):
		"""
		Scrape the social network using breadth-search.

		@param seed: Seed identity
		@param size: Number of identities to scrape
		"""
		size = int(size)

		socgr = self.ff.scrapeIDs(seed, size).graph
		socgr.write_graphml(self.outfp("soc.graphml"))
		socgr.write_dot(self.outfp("soc.dot"))

		if self.interact: code.interact(banner=self.banner, local=locals())


	def round_group(self):
		"""
		Scrape the group network from the social network.

		Round "social" must already have been executed.
		"""
		users = Graph.Read(self.infp("soc.graphml")).vs["id"]

		gumap = self.ff.scrapeGroups(users)
		dict_save(gumap, self.outfp("gu.dict"))

		if self.interact: code.interact(banner=self.banner, local=locals())


	def round_photo(self):
		"""
		Scrape photos of the collected producers.

		Round "group" must already have been executed.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("gu.dict"))

		ppdb = self.db("pp")
		self.ff.commitUserPhotos(socgr.vs["id"], ppdb)
		self.ff.commitGroupPhotos(gumap, ppdb)

		self.ff.pruneProducers(socgr, gumap, ppdb)
		socgr.write_graphml(self.outfp("soc.graphml"))
		socgr.write_dot(self.outfp("soc.dot"))
		dict_save(gumap, self.outfp("gu.dict"))

		if self.interact: code.interact(banner=self.banner, local=locals())


	def round_invert(self):
		"""
		Invert the producer-photo mapping.

		Round "photo" must already have been executed.
		"""
		ppdb = self.db("pp")
		pcdb = self.db("pc", writeback=True)

		self.ff.invertProducerMap(ppdb, pcdb)

		if self.interact: code.interact(banner=self.banner, local=locals())


	def round_tag(self):
		"""
		Scrape tags of the collected photos.

		Round "photo" must already have been executed.
		"""
		ppdb = self.db("pp")
		ptdb = self.db("pt")

		photos = chain(*ppdb.itervalues())
		self.ff.commitPhotoTags(photos, ptdb)

		if self.interact: code.interact(banner=self.banner, local=locals())


	def round_cluster(self):
		"""
		Scrape clusters of the collected tags.

		Round "tag" must already have been executed.
		"""
		ptdb = self.db("pt")
		tcdb = self.db("tc")

		tags = chain(*ptdb.itervalues())
		self.ff.commitTagClusters(tags, tcdb)

		if self.interact: code.interact(banner=self.banner, local=locals())


	def round_generate(self):
		"""
		Generate objects from the scraped data.

		Round "group" must already have been executed.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("gu.dict"))

		ppdb = self.db("pp")
		pcdb = self.db("pc")
		ptdb = self.db("pt")
		tcdb = self.db("tc")

		ss = FlickrSample(socgr, gumap, ppdb, ptdb, pcdb, tcdb)

		if self.interact: code.interact(banner=self.banner, local=locals())


if __name__ == "__main__":

	from optparse import OptionParser, OptionGroup, IndentedHelpFormatter
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Scrapes data from flickr. ROUND is one of: %s" % Scraper.roundlist,
	  version = VERSION,
	  formatter = IndentedHelpFormatter(max_help_position=25)
	)

	config.add_option("-o", "--output", type="string", metavar="OUTPUT", default="scrape",
	  help = "Output file prefix (extensions will be added to it)")
	config.add_option("-b", "--database", type="string", metavar="DATABASE", default=".",
	  help = "Path to the photo-tag database (default .)")
	config.add_option("-i", "--interact", action="store_true", dest="interact",
	  help = "Go into interactive mode after performing a round, to examine the objects created")
	config.add_option("-k", "--api-key", type="string", metavar="APIKEY",
	  help = "Flickr API key")
	config.add_option("-s", "--secret", type="string", metavar="SECRET",
	  help = "Flickr API secret")
	config.add_option("-t", "--token", type="string", metavar="TOKEN",
	  help = "Flickr API authentication token")
	config.add_option("-v", "--verbose", type="int", metavar="VERBOSE", default=0,
	  help = "Verbosity level (1 to 50, 1 most verbose)")

	(opts, args) = config.parse_args()

	if len(args) < 1:
		config.print_help()
		sys.exit(2)
	else:
		sys.exit(main(*args, **opts.__dict__))
