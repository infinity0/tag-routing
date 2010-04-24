#!/usr/bin/python
# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, logging, code, os, re
from time import time, ctime
from itertools import chain

from igraph import Graph
from xml.etree.ElementTree import dump

from tags.scrape.util import futures_patch_nonblocking
futures_patch_nonblocking()
#from tags.scrape.contextprocess import multiprocessing_patch_nonblocking
#multiprocessing_patch_nonblocking()
logging.basicConfig(format="%(asctime)s.%(msecs)03d | %(levelno)02d | %(message)s", datefmt="%s")

from tags.scrape.flickr import SafeFlickrAPI
from tags.scrape.sample import SampleGenerator, SampleWriter, SampleStats
from tags.scrape.object import NodeSample, Node, QueryReport
from tags.scrape.util import signal_dump, dict_load, dict_save, read_chapters
from tags.scrape.cache import shelve_attach_cache

NAME = "scrape.py"
VERSION = 0.01
LOG = logging.getLogger(__name__)


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
			LOG.info("%s at %s" % (Scraper.rounds[round].desc, ctime()))
			ret = f(*args)
			LOG.info('Round "%s" completed in %.4fs' % (round, time()-t))
			return ret


class Round(object):

	def __init__(self, desc, dep, out):
		self.desc = desc
		self.dep = dep
		self.out = out


class Scraper(object):

	_rounds = [
		("social", Round("Scraping social network", [], ["soc.graphml"])),
		("group", Round("Scraping groups", ["social"], ["group-user.map"])),
		("photo", Round("Scraping photos", ["group"], ["prod-doc.db"]+["group-user.map"]+["soc.graphml"])),
		("inv_pd", Round("Inverting producer-document mapping", ["photo"], ["doc-prod.db"])),
		("tag", Round("Scraping tags", ["photo"], ["doc-tag.db", "doc-tag.len"])),
		("inv_dt", Round("Inverting document-tag mapping", ["tag"], ["tag-doc.db"])),
		("cluster", Round("Scraping clusters", ["tag"], ["tag-cluster.db"])),
		("generate", Round("Generating data", ["inv_pd", "cluster"], ["p_idx.db", "p_tgr.db"])),
		("writeall", Round("Writing objects", ["generate"], [])),
		("examine", Round("Examine data", [], [])),
	]
	rounds = dict(_rounds)
	roundlist = [k for k, r in _rounds]


	def __init__(self, base, api_key, secret, token=None, interact=False, cache=0, pretty=False):
		self.base = base
		self.ff = SafeFlickrAPI(api_key, secret, token)
		self.interact = bool(interact)
		# TODO NORM use a decorator to do this, somehow...
		# needs to be able to access previous stack frame's locals!
		self.cache = int(cache)
		self.pretty = bool(pretty)

		if not os.path.isdir(str(base)):
			raise ValueError("not a directory: %s" % base)

		self.dir_idx = os.path.join(base, "idx")
		self.dir_tgr = os.path.join(base, "tgr")
		self.dir_res = os.path.join(base, "res")

		for path in [self.base, self.dir_idx, self.dir_tgr, self.dir_res]:
			if not os.path.isdir(path):
				os.mkdir(path)
		self.res = {}


	def __enter__(self):
		return self


	def __exit__(self, type, value, traceback):
		for path, res in self.res.iteritems():
			res.close()
			LOG.info("%s closed" % (path))


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
			LOG.info("%s closed" % (path))
		self.res[path] = res
		LOG.info("%s opened (%s)" % (path, mode))


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
		dict_save(gumap, self.outfp("group-user.map"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_photo(self):
		"""
		Scrape photos of the collected producers.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("group-user.map"))

		pddb = self.db("prod-doc")
		self.ff.commitUserPhotos(socgr.vs["id"], pddb)
		self.ff.commitGroupPhotos(gumap, pddb)

		self.ff.pruneProducers(socgr, gumap, pddb)
		socgr.write_graphml(self.outfp("soc.graphml"))
		dict_save(gumap, self.outfp("group-user.map"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_invertp(self):
		"""
		Invert the producer-photo mapping.
		"""
		pddb = self.db("prod-doc")
		dppb = self.db("doc-prod", writeback=True)

		self.ff.invertMap(pddb, dppb, "context")

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_tag(self):
		"""
		Scrape tags of the collected photos.
		"""
		pddb = self.db("prod-doc")
		dtdb = self.db("doc-tag")

		photos = chain(*pddb.itervalues())
		self.ff.commitPhotoTags(photos, dtdb)
		print >>self.outfp("doc-tag.len"), len(dtdb)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_invertt(self):
		"""
		Invert the photo-tag mapping.
		"""
		dtdb = self.db("doc-tag")
		tddb = self.db("tag-doc", writeback=True)

		self.ff.invertMap(dtdb, tddb, "tag-photo")

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_cluster(self):
		"""
		Scrape clusters of the collected tags.
		"""
		dtdb = self.db("doc-tag")
		tcdb = self.db("tag-cluster")

		tags = chain(*dtdb.itervalues())
		self.ff.commitTagClusters(tags, tcdb)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_generate(self):
		"""
		Generate objects from the scraped data.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("group-user.map"))

		pddb = self.db("prod-doc")
		dppb = self.db("doc-prod")
		dtdb = self.db("doc-tag")
		tcdb = self.db("tag-cluster")

		phdb = self.db("p_idx", lrusize=self.cache)
		phsb = self.db("p_idx_s")
		pgdb = self.db("p_tgr", lrusize=self.cache)
		pgsb = self.db("p_tgr_s")

		sg = SampleGenerator(socgr, gumap, pddb, dppb, dtdb, tcdb, phdb, phsb, pgdb, pgsb)
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
		gumap = dict_load(self.infp("group-user.map"))

		totalsize = int(self.infp("doc-tag.len").read())
		phdb = self.db("p_idx")
		pgdb = self.db("p_tgr")

		ss = SampleWriter(phdb, pgdb, totalsize)
		ss.writeIndexes(self.dir_idx)
		ss.writeTGraphs(self.dir_tgr)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_examine(self, *args):
		"""
		Examine objects through the python interactive interpreter.
		"""
		socgr = Graph.Read(self.infp("soc.graphml"))
		gumap = dict_load(self.infp("group-user.map"))

		pddb = self.db("prod-doc")
		dppb = self.db("doc-prod")
		dtdb = self.db("doc-tag")
		tddb = self.db("tag-doc")
		tcdb = self.db("tag-cluster")
		totalsize = int(self.infp("doc-tag.len").read())

		phdb = self.db("p_idx")
		phsb = self.db("p_idx_s")
		pgdb = self.db("p_tgr")
		pgsb = self.db("p_tgr_s")

		ptabgr = Graph.Read(self.infp("ptb.graphml"))
		prodgr = Graph.Read(self.infp("idx.graphml"))
		sprdgr = Graph.Read(self.infp("tgr.graphml"))

		stats = SampleStats(pddb, dppb, dtdb, tddb, totalsize, ptabgr, prodgr, sprdgr)

		reports = []
		for arg in args:
			with open(os.path.join(self.dir_res, arg)) as fp:
				reports.append(QueryReport.from_chapters(read_chapters(fp)))

		stats.printReports(reports, pretty=self.pretty)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())



if __name__ == "__main__":

	from optparse import OptionParser
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Scrapes data from flickr. ROUND is one of: %s." % ", ".join(Scraper.roundlist),
	  version = VERSION,
	)

	exstr = " (See ./flickr.key.example for an example)" if os.path.exists("./flickr.key.example") else ""

	config.add_option("-d", "--base", type="string", metavar="DIR",
	  help = "Base output directory")
	config.add_option("-k", "--key", type="string", metavar="FILE", default="flickr.key",
	  help = 'File with Flickr API authentication details. Each line must read "key: value", '
	         'and the file must define keys "api_key" and "secret", and optionally "token".%s' % exstr)
	config.add_option("-i", "--interact", action="store_true", dest="interact",
	  help = "Go into interactive mode after performing a round, to examine the objects created")
	config.add_option("-c", "--cache", type="int", metavar="SIZE", default=0,
	  help = "Cache size for database objects (only sometimes used, eg. pgdb, phdb in round 'generate')")
	config.add_option("-v", type="int", metavar="LEVEL", default=100,
	  help = 'Verbosity level (1-50; 1 most verbose, 20 standard)')
	config.add_option("-p", "--pretty", action="store_true", dest="pretty",
	  help = "Pretty print (only for some outputs)")

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
