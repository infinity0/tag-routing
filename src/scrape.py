#!/usr/bin/python
# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, logging, code, os, re
from time import time, ctime
from itertools import chain

from igraph import Graph
from xml.etree.ElementTree import dump

from tags.scrape.util import futures_patch_nonblocking
futures_patch_nonblocking()
logging.basicConfig(format="%(asctime)s.%(msecs)03d | %(levelno)02d | %(message)s", datefmt="%s")

from tags.scrape.flickr import SafeFlickrAPI
from tags.scrape.sample import SampleGenerator, SampleWriter, SampleStats
from tags.scrape.object import NodeSample, Node, Results
from tags.scrape.util import signal_dump, dict_load, dict_save, read_chapters
from tags.scrape.lrudict import shelve_attach_cache

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

	signal_dump()
	logging.getLogger("").setLevel(kwargs.pop("v"))

	with Scraper(**kwargs) as scr:

		f = getattr(scr, "round_%s" % round)

		if len(args) > 0 and args[0].lower() == "help":
			rinfo = Scraper.rounds[round]
			print >>sys.stderr, fmt_pydoc(f.__doc__)
			print >>sys.stderr, "These rounds must already have been executed: %s" % ", ".join(rinfo.dep)
			print >>sys.stderr, "These files will be written to: %s" % ", ".join(os.path.join(kwargs["base"], ext) for ext in rinfo.out)
			return 0

		else:
			t = time()
			print >>sys.stderr, "%s at %s" % (Scraper.rounds[round].desc, ctime())
			ret = f(*args)
			print >>sys.stderr, "completed in %.4fs" % (time()-t)
			return ret


class Round(object):

	def __init__(self, desc, dep, out):
		self.desc = desc
		self.dep = dep
		self.out = out


class Scraper(object):

	_rounds = [
		("social", Round("Scraping social network", [], ["soc.graphml"])),
		("group", Round("Scraping groups", ["social"], ["gu.map"])),
		("photo", Round("Scraping photos", ["group"], ["pp.db"]+["gu.map"]+["soc.graphml"])),
		("invertp", Round("Inverting producer mapping", ["photo"], ["pc.db"])),
		("tag", Round("Scraping tags", ["photo"], ["pt.db", "pt.len"])),
		("invertt", Round("Inverting tag mapping", ["tag"], ["tp.db"])),
		("cluster", Round("Scraping clusters", ["tag"], ["tc.db"])),
		("generate", Round("Generating data", ["invertp", "cluster"], ["ph.db", "pg.db"])),
		("writeall", Round("Writing objects", ["generate"], [])),
		("examine", Round("Examine data", [], [])),
	]
	rounds = dict(_rounds)
	roundlist = [k for k, r in _rounds]


	def __init__(self, api_key, secret, token=None, base="scrape", interact=False, cache=0):
		self.ff = SafeFlickrAPI(api_key, secret, token)
		self.res = {}
		self.base = base
		self.interact = interact
		self.cache = int(cache)

		self.dir_idx = os.path.join(base, "idx")
		self.dir_tgr = os.path.join(base, "tgr")
		self.dir_res = os.path.join(base, "res")

		for path in [self.base, self.dir_idx, self.dir_tgr, self.dir_res]:
			if not os.path.isdir(path):
				os.mkdir(path)


	def __enter__(self):
		return self


	def __exit__(self, type, value, traceback):
		for path, res in self.res.iteritems():
			res.close()
			print >>sys.stderr, "%s closed" % (path)


	def banner(self, local):
		return "[Scraper interactive console]\n>>> locals().keys()\n%r\n>>> self.ff\n%r" % (sorted(local.keys()), self.ff)


	def outfp(self, name):
		fn = os.path.join(self.base, name)
		fp = open(fn, 'w')
		self.respush(fn, fp, 'w')
		return fp


	def infp(self, name):
		fn = os.path.join(self.base, name)
		fp = open(fn)
		self.respush(fn, fp, 'r')
		return fp


	def db(self, name, writeback=False, lrusize=0):
		dbf = os.path.join(self.base, "%s.db" % name)

		try:
			from dbsqlite import SQLFileShelf
			db = SQLFileShelf(dbf, writeback=writeback)
		except:
			try:
				from shelve import BsdDbShelf
				from bsddb import btopen
				db = BsdDbShelf(btopen(dbf), writeback=writeback)
			except Exception:
				import shelve
				db = shelve.open(dbf, writeback=writeback)

		lrusize = int(lrusize)
		if lrusize:
			shelve_attach_cache(db, lrusize)

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

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_group(self):
		"""
		Scrape the group network from the social network.
		"""
		users = Graph.Read(self.infp("soc.graphml")).vs["id"]

		gumap = self.ff.scrapeGroups(users)
		dict_save(gumap, self.outfp("gu.map"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_photo(self):
		"""
		Scrape photos of the collected producers.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("gu.map"))

		ppdb = self.db("pp")
		self.ff.commitUserPhotos(socgr.vs["id"], ppdb)
		self.ff.commitGroupPhotos(gumap, ppdb)

		self.ff.pruneProducers(socgr, gumap, ppdb)
		socgr.write_graphml(self.outfp("soc.graphml"))
		dict_save(gumap, self.outfp("gu.map"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_invertp(self):
		"""
		Invert the producer-photo mapping.
		"""
		ppdb = self.db("pp")
		pcdb = self.db("pc", writeback=True)

		self.ff.invertMap(ppdb, pcdb, "context")

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_tag(self):
		"""
		Scrape tags of the collected photos.
		"""
		ppdb = self.db("pp")
		ptdb = self.db("pt")

		photos = chain(*ppdb.itervalues())
		self.ff.commitPhotoTags(photos, ptdb)
		print >>self.outfp("pt.len"), len(ptdb)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_invertt(self):
		"""
		Invert the photo-tag mapping.
		"""
		ptdb = self.db("pt")
		tpdb = self.db("tp", writeback=True)

		self.ff.invertMap(ptdb, tpdb, "tag-photo")

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_cluster(self):
		"""
		Scrape clusters of the collected tags.
		"""
		ptdb = self.db("pt")
		tcdb = self.db("tc")

		tags = chain(*ptdb.itervalues())
		self.ff.commitTagClusters(tags, tcdb)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_generate(self):
		"""
		Generate objects from the scraped data.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("gu.map"))

		ppdb = self.db("pp")
		pcdb = self.db("pc")
		ptdb = self.db("pt")
		tcdb = self.db("tc")

		phdb = self.db("ph", lrusize=self.cache)
		phsb = self.db("phs")
		pgdb = self.db("pg", lrusize=self.cache)
		pgsb = self.db("pgs")

		sg = SampleGenerator(socgr, gumap, ppdb, pcdb, ptdb, tcdb, phdb, phsb, pgdb, pgsb)
		sg.generateIndexes()
		sg.prodgr.write(self.outfp("idx.graphml"))
		sg.generateTGraphs()
		sg.sprdgr.write(self.outfp("tgr.graphml"))
		sg.generatePTables()
		sg.ptabgr.write(self.outfp("ptb.graphml"))

		dict_save(sg.ptbmap, self.outfp("ptables.map"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_writeall(self):
		"""
		Write objects from the generated data.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("gu.map"))

		totalsize = int(self.infp("pt.len").read())
		phdb = self.db("ph")
		pgdb = self.db("pg")

		ss = SampleWriter(phdb, pgdb, totalsize)
		ss.writeIndexes(self.dir_idx)
		ss.writeTGraphs(self.dir_tgr)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_examine(self, *args):
		"""
		Examine objects through the python interactive interpreter.
		"""
		try:
			socgr = Graph.Read(self.infp("soc.graphml"))
			gumap = dict_load(self.infp("gu.map"))

			ppdb = self.db("pp")
			pcdb = self.db("pc")
			ptdb = self.db("pt")
			tpdb = self.db("tp")
			tcdb = self.db("tc")
			totalsize = int(self.infp("pt.len").read())

			phdb = self.db("ph")
			phsb = self.db("phs")
			pgdb = self.db("pg")
			pgsb = self.db("pgs")

			ptabgr = Graph.Read(self.infp("ptb.graphml"))
			prodgr = Graph.Read(self.infp("idx.graphml"))
			sprdgr = Graph.Read(self.infp("tgr.graphml"))

			stats = SampleStats(ppdb, pcdb, ptdb, tpdb, totalsize, ptabgr, prodgr, sprdgr)

			results = []
			for arg in args:
				with open(os.path.join(self.dir_res, arg)) as fp:
					results.append(Results.from_chapters(read_chapters(fp)))

		except IOError:
			pass
		finally:
			if hasattr(pgdb.cache, "report_stats"): print pgdb.cache.report_stats()
			if hasattr(phdb.cache, "report_stats"): print phdb.cache.report_stats()

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())
		else: print >>sys.stderr, "cli param parsing not implemented yet; use -i to enter interactive mode"



if __name__ == "__main__":

	from optparse import OptionParser
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Scrapes data from flickr. ROUND is one of: %s." % ", ".join(Scraper.roundlist),
	  version = VERSION,
	)

	exstr = " (See ./flickr.key.example for an example)" if os.path.exists("./flickr.key.example") else ""

	config.add_option("-d", "--base", type="string", metavar="DIR", default="scrape",
	  help = "Base output directory")
	config.add_option("-c", "--cache", type="int", metavar="SIZE", default=0,
	  help = "Cache size for database objects (only sometimes used, eg. pgdb, phdb in round 'generate')")
	config.add_option("-i", "--interact", action="store_true", dest="interact",
	  help = "Go into interactive mode after performing a round, to examine the objects created")
	config.add_option("-k", "--key", type="string", metavar="FILE", default="flickr.key",
	  help = 'File with Flickr API authentication details. Each line must read "key: value", '
	         'and the file must define keys "api_key" and "secret", and optionally "token".%s' % exstr)
	config.add_option("-v", type="int", metavar="LEVEL", default=0,
	  help = "Verbosity level (1-50; 1 most verbose)")

	(opts, args) = config.parse_args()

	kwargs = opts.__dict__
	with open(opts.key) as fp:
		try:
			keys = dict_load(fp)
			for k in ["api_key", "secret"]:
				if k in keys:
					kwargs[k] = keys[k]
				else:
					raise ValueError('key "%s" not found' % k)
			for k in ["token"]:
				if k in keys:
					kwargs[k] = keys[k]
		except ValueError, e:
			print >>sys.stderr, "bad keyfile format in %s: %r" % (opts.key, e)
			sys.exit(1)

	del opts.key

	if len(args) < 1:
		config.print_help()
		sys.exit(2)
	else:
		sys.exit(main(*args, **kwargs))
