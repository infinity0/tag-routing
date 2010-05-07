# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, logging, code, os
from itertools import chain
from collections import namedtuple

from igraph import Graph
from xml.etree.ElementTree import dump

from tags.eval.util import futures_patch_nonblocking
futures_patch_nonblocking()
#from tags.eval.contextprocess import multiprocessing_patch_nonblocking
#multiprocessing_patch_nonblocking()

from tags.eval.crawl.flickr import SafeFlickrAPI
from tags.eval.sample import SampleGenerator, SampleWriter, SampleStats
from tags.eval.objects import NodeSample, Node, QueryReport
from tags.eval.util import signal_dump, dict_load, dict_save, read_chapters, db_open
from tags.eval.util.cache import shelve_attach_cache

LOG = logging.getLogger(__name__)


Round = namedtuple('Round', 'desc dep out')


class Evaluation(object):

	_rounds = [
		("social", Round("Scraping social network", [], ["soc.graphml"])),
		("group", Round("Scraping groups", ["social"], ["group-user.map"])),
		("photo", Round("Scraping photos", ["group"], ["prod-doc.db"]+["group-user.map"]+["soc.graphml"])),
		("inv_pd", Round("Inverting producer-document mapping", ["photo"], ["doc-prod.db"])),
		("tag", Round("Scraping tags", ["photo"], ["doc-tag.db", "doc-tag.len"])),
		("inv_dt", Round("Inverting document-tag mapping", ["tag"], ["tag-doc.db"])),
		("cluster", Round("Scraping clusters", ["tag"], ["tag-cluster.db"])),
		("generate", Round("Generating data", ["inv_pd", "cluster"], ["p_idx.db", "idx.graphml", "communities.map", "p_tgr.db", "tgr.graphml"])),
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
		return "[Evaluation console]\n>>> locals().keys()\n%r\n>>> self.ff\n%r" % (sorted(local.keys()), self.ff)


	def fp_o(self, name):
		fn = os.path.join(self.base, name)
		fp = open(fn, 'w')
		self.respush(fn, fp, 'w')
		return fp


	def fp_i(self, name):
		fn = os.path.join(self.base, name)
		fp = open(fn)
		self.respush(fn, fp, 'r')
		return fp


	def fp_exists(self, name):
		fn = os.path.join(self.base, name)
		return os.path.exists(fn)


	def df(self, name):
		return os.path.join(self.base, "%s.db" % name)


	def db(self, name, writeback=False, lrusize=0):
		dbf = os.path.join(self.base, "%s.db" % name)
		db = db_open(dbf, writeback)

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
		socgr.write_graphml(self.fp_o("soc.graphml"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_group(self):
		"""
		Scrape the group network from the social network.
		"""
		users = Graph.Read(self.fp_i("soc.graphml")).vs["id"]

		gumap = self.ff.scrapeGroups(users)
		dict_save(gumap, self.fp_o("group-user.map"))

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_photo(self):
		"""
		Scrape photos of the collected producers.
		"""
		socgr = Graph.Read(self.fp_i("soc.graphml"))
		gumap = dict_load(self.fp_i("group-user.map"))

		pddb = self.db("prod-doc")
		self.ff.commitUserPhotos(socgr.vs["id"], pddb)
		self.ff.commitGroupPhotos(gumap, pddb)

		self.ff.pruneProducers(socgr, gumap, pddb)
		socgr.write_graphml(self.fp_o("soc.graphml"))
		dict_save(gumap, self.fp_o("group-user.map"))

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
		print >>self.fp_o("doc-tag.len"), len(dtdb)

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
		socgr = Graph.Read(self.fp_i("soc.graphml"))
		gumap = dict_load(self.fp_i("group-user.map"))

		pddb = self.db("prod-doc")
		dppb = self.db("doc-prod")
		dtdb = self.db("doc-tag")
		tcdb = self.db("tag-cluster")

		phdb = self.db("p_idx", lrusize=self.cache)
		phsb = self.db("p_idx_s")
		pgdb = self.db("p_tgr", lrusize=self.cache)
		pgsb = self.db("p_tgr_s")

		FILE_IDX = "idx.graphml"
		FILE_CMM = "communities.map"
		FILE_TGR = "tgr.graphml"
		FILE_PTB = "ptb.graphml"
		FILE_PTB_U = "ptables.map"

		sg = SampleGenerator(socgr, gumap, pddb, dppb, dtdb, tcdb, phdb, phsb, pgdb, pgsb)

		# indexes
		if not self.fp_exists(FILE_IDX):
			sg.generateIndexes()
			sg.prodgr.write(self.fp_o(FILE_IDX))
		else:
			sg.prodgr = Graph.Read(self.fp_i(FILE_IDX))

		# communities
		if not self.fp_exists(FILE_CMM):
			sg.generateCommunities()
			dict_save(dict(enumerate(sg.comm)), self.fp_o(FILE_CMM))
		else:
			sg.comm = [v for k, v in sorted(dict_load(self.fp_i(FILE_CMM)).iteritems())]

		# tgraphs
		if not self.fp_exists(FILE_TGR):
			sg.generateTGraphs()
			sg.sprdgr.write(self.fp_o(FILE_TGR))
		else:
			sg.sprdgr = Graph.Read(self.fp_i(FILE_TGR))

		# ptables
		if not self.fp_exists(FILE_PTB):
			sg.generatePTables()
			sg.ptabgr.write(self.fp_o(FILE_PTB))
			dict_save(sg.ptbmap, self.fp_o(FILE_PTB_U))
		else:
			sg.ptabgr = Graph.Read(self.fp_i(FILE_PTB))
			sg.ptbmap = dict_load(self.fp_i(FILE_PTB_U))

		LOG.info("generation complete; don't forget to run `postgen -d %s`" % self.base)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())


	def round_writeall(self):
		"""
		Write objects from the generated data.
		"""
		socgr = Graph.Read(self.fp_i("soc.graphml"))
		gumap = dict_load(self.fp_i("group-user.map"))

		totalsize = int(self.fp_i("doc-tag.len").read())
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
		socgr = Graph.Read(self.fp_i("soc.graphml"))
		gumap = dict_load(self.fp_i("group-user.map"))

		pddb = self.db("prod-doc")
		dppb = self.db("doc-prod")
		dtdb = self.db("doc-tag")
		tddb = self.db("tag-doc")
		tcdb = self.db("tag-cluster")
		totalsize = int(self.fp_i("doc-tag.len").read())

		phdb = self.db("p_idx")
		phsb = self.db("p_idx_s")
		pgdb = self.db("p_tgr")
		pgsb = self.db("p_tgr_s")

		ptabgr = Graph.Read(self.fp_i("ptb.graphml"))
		prodgr = Graph.Read(self.fp_i("idx.graphml"))
		sprdgr = Graph.Read(self.fp_i("tgr.graphml"))

		stats = SampleStats(pddb, dppb, dtdb, tddb, totalsize, ptabgr, prodgr, sprdgr)

		reports = []
		for arg in args:
			with open(os.path.join(self.dir_res, arg)) as fp:
				reports.append(QueryReport.from_chapters(read_chapters(fp)))

		stats.printReports(reports, pretty=self.pretty)

		if self.interact: code.interact(banner=self.banner(locals()), local=locals())

