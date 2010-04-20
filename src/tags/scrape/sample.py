# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, logging, os
from itertools import chain, izip
from igraph import Graph

from tags.scrape.object import Producer, ProducerSample, TagInfo, NID, AAT, P_ARC
from tags.scrape.util import (union_ind, geo_prog_range, infer_arcs, edge_array,
  graph_copy, undirect_and_simplify, invert_seq, invert_multimap, exec_unique)


LOG = logging.getLogger(__name__)


class SampleGenerator(object):

	def __init__(self, socgr, gumap, ppdb, pcdb, ptdb, tcdb, phdb, phsb, pgdb, pgsb):
		"""
		Create a new SampleGenerator from the given arguments

		@param socgr: social network graph
		@param gumap: {group:[user]} map
		@param ppdb: an open database of {producer:[photo]}
		@param pcdb: an open database of {photo:[producer]}
		@param ptdb: an open database of {photo:[tag]}
		@param tcdb: an open database of {tag:[cluster]}
		@param phdb: an open database of {nsid:Producer} (for indexes)
		@param phsb: an open database of {nsid:Producer.state} (for indexes)
		@param pgdb: an open database of {prid:Producer} (for tgraphs)
		@param pgsb: an open database of {prid:Producer.state} (for tgraphs)
		"""
		self.socgr = socgr
		self.gumap = gumap

		self.ppdb = ppdb
		self.pcdb = pcdb
		self.ptdb = ptdb
		self.tcdb = tcdb

		self.phdb = phdb
		self.phsb = phsb
		self.pgdb = pgdb
		self.pgsb = pgsb

		self.prodgr = None
		self.sprdgr = None
		self.ptabgr = None
		self.ptbmap = None
		self.comm = None


	def generateIndexes(self):
		"""
		DOCUMENT
		"""
		name = "indexes"

		# generate Producer objects
		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(self.ppdb[nsid], self.ptdb)
			prod.inferScores()
			prod.repDoc()
			prod.repTag()
			self.phdb[nsid] = prod
			self.phsb[nsid] = prod.state
		exec_unique(self.ppdb.iterkeys(), self.phsb, run_p, None, "%s db: producers" % name, LOG.info)

		# generate content arcs between producers
		def run_r(nsid):
			prod = self.phdb[nsid]
			if prod.state != P_ARC:
				rels = self.inferRelProds(prod)
				pmap_a = dict((rnsid, self.inferProdArc(prod, self.phdb[rnsid])) for rnsid in rels)
				prod.initProdArcs(pmap_a)
				self.phdb[nsid] = prod
			self.phsb[nsid] = prod.state
		# OPT HIGH the lambda is inefficient, we should store this state in a smaller+faster db
		exec_unique(self.phdb.iterkeys(), lambda nsid: self.phsb[nsid] >= P_ARC,
		  run_r, None, "%s db: relations" % name, LOG.info, steps=0x10000)

		total = len(self.phdb)
		lab_p, id_p = zip(*(("%s (%s)\\n%s" % (nsid, prod.size(), '\\n'.join(prod.rep_t[0:4])),
		  (nsid, i)) for i, (nsid, prod) in enumerate(self.phdb.iteritems()))) if self.phdb else ([], [])
		id_p = dict(id_p)

		# generate producer graph
		arc_s, arc_t, edges = edge_array(total)
		for i, prod in enumerate(self.phdb.itervalues()):
			for nsid in prod.docgr.vs.select(prod.prange())[NID]:
				arc_s.append(i)
				arc_t.append(id_p[nsid])

		sz = [log(prod.size()) for prod in self.phdb.itervalues()]
		v_attr = {NID: list(self.phdb.iterkeys()), "label": lab_p, "height": sz, "width": sz}

		self.prodgr = Graph(total, edges=list(edges), directed=True, vertex_attrs=v_attr)
		LOG.info("%s db: generated producer graph" % name)


	def inferRelProds(self, prod):
		"""
		Infer a set of related producers for the given producer.

		This implementation uses selectProdsForPhotos() on the producer's
		representative photos.
		"""
		rel = self.selectProdsForPhotos(prod.rep_d)
		rel.discard(prod.nsid)
		return rel


	def selectProdsForPhotos(self, photos):
		"""
		Selects a set of "related" producers from all the producers that hold
		any of the photos in the given set.
		"""
		rel = set()
		for phid in photos:
			rel.update(self.pcdb[phid])
		return rel


	def inferProdArc(self, prod_s, prod_t, show_tag=False):
		"""
		Infer arcs and their weights between the given producers.

		This implementation uses selectTagsFromClusters() on the producers'
		representative tags.

		@param prod_s: source producer
		@param prod_t: target producer
		@param show_tag: whether to return tag_a
		@return: (arc_a, tag_a) DOCUMENT
		"""
		rtags, htags = self.selectTagsFromClusters(prod_s.rep_t, prod_t.rep_t)
		arc_a = dict((rtag, prod_t.tagScore(rtag)) for rtag in rtags)
		arc_a.update(htags)
		return (arc_a, rtags) if show_tag else arc_a


	def selectTagsFromClusters(self, tset_s, tset_t):
		"""
		Selects tags from the intersection between each cluster for a source
		tag, and the target tagset. The representatives of the cluster are also
		selected, if the intersection is large enough.

		@param tset_s: source tag-set
		@param tset_t: target tag-set
		@return: (rtags, htags), where rtags = {rtag:[tag]} associates tags on
		         the target side to related tags on the source side, and htags
		         = {htag:e_attr} associates "high-level" tags (which might not
		         exist on the target side) to appropriate arc-attributes.
		"""
		#LOG.debug("III enter selectTagsFromClusters: %s %s" % (len(tset_s), len(tset_t)))
		rtags = {}
		htags = {}
		if type(tset_t) != set:
			tset_t = set(tset_t)

		for tag in tset_s:
			for cluster in self.tcdb[tag]:
				tset_x = tset_t.intersection(cluster)

				# add intersection to rtags
				for rtag in tset_x:
					if rtag in rtags:
						rtags[rtag].append(tag)
					else:
						rtags[rtag] = [tag]

				# if intersection is big enough, add "representative" tags of
				# this cluster to htags
				if 3*len(tset_x) > len(cluster): # TWEAK
					# on flickr, this is the first 3 tags
					attr = len(tset_x)/float(len(cluster))
					for rtag in cluster[0:3]:
						if rtag in htags:
							htags[rtag].append(attr)
							rtags[rtag].append(tag)
						else:
							htags[rtag] = [attr]
							rtags[rtag] = [tag]

		#LOG.debug("XXX exit selectTagsFromClusters: %s %s" % (len(tset_s), len(tset_t)))
		return rtags, dict((htag, union_ind(attrs)) for htag, attrs in htags.iteritems())


	def generateTGraphs(self):
		"""
		DOCUMENT
		"""
		name = "tgraphs"

		# generate docsets for new producers
		self.comm = self.selectCommunities()
		pmap = dict(("%04d" % i, pset) for i, pset in enumerate(self.comm))

		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(set(chain(*(self.ppdb[self.prodgr.vs[p][NID]] for p in pmap[nsid]))), self.ptdb, True)
			prod.inferScores()
			prod.repTag(cover=0) # TWEAK
			self.pgdb[nsid] = prod
			self.pgsb[nsid] = prod.state
		exec_unique(pmap, self.pgsb, run_p, None, "%s db: producers" % name, LOG.info)

		tot_p = len(self.prodgr.vs)
		tot_s = len(self.comm)
		edges, arc_a = infer_arcs(self.comm, tot_p, ratio=2*log(1+tot_p)) # TWEAK # relax for tgraphs

		id_p = dict(("%04d" % i, i) for i in xrange(0, tot_s))
		self.sprdgr = Graph(tot_s, list(edges), directed=True,
		  vertex_attrs={NID:list("%04d" % i for i in xrange(0, tot_s)), "label":[len(com) for com in self.comm]})
		g = self.sprdgr
		#g.write_dot("sprdgr.dot")
		#g.write("sprdgr.graphml")
		LOG.info("%s db: generated producer graph" % name)

		# generate content arcs between producers
		# FIXME HIGH this uses up too much memory :/
		import gc
		#gc.set_debug(gc.DEBUG_LEAK)
		def run_r(nsid):
			prod = self.pgdb[nsid]
			if prod.state != P_ARC:
				rprod = g.vs.select(g.successors(id_p[nsid]))[NID]
				pmap = [(rnsid, self.inferProdArc(prod, self.pgdb[rnsid], show_tag=True)) for rnsid in rprod]
				self.pgdb.sync()
				pmap_a, pmap_t = izip(*(((rnsid, arc_a), (rnsid, node_a)) for rnsid, (arc_a, node_a) in pmap)) if pmap else ([], [])
				prod.initProdArcs(dict(pmap_a), dict(pmap_t))
				del pmap, pmap_a, pmap_t
				gc.collect()
				self.pgdb[nsid] = prod
			self.pgsb[nsid] = prod.state
		# OPT HIGH the lambda is inefficient, we should store this state in a smaller+faster db
		exec_unique(self.pgdb.iterkeys(), lambda nsid: self.pgsb[nsid] >= P_ARC,
		  run_r, None, "%s db: relations" % name, LOG.info, steps=0x10000)


	def selectCommunities(self):
		"""
		Generates a bunch of communities using various community detection
		algorithms on the underlying producer graph, and selects the non-tiny
		ones.
		"""
		comm = set()

		'''
		# adds communities generated with "label propagation" until no new
		# communities are generated for 3 rounds in a row. this is shit because
		# it'll generate a lot of similar (ie. redundant) communities with size
		# in the midrange
		unchanged = 0
		while unchanged < 3:
			mem = self.prodgr.community_label_propagation().membership
			coms = list(frozenset(community) for community in invert_seq(mem).itervalues())
			unchanged = unchanged+1 if all(com in comm for com in coms) else 0
			comm.update(coms)
		'''

		'''
		# aggregate communities using "label propagation" algorithm
		# currently this segfaults, igraph bug.
		# also maybe do this several times and generate several aggregates
		mem = self.prodgr.community_label_propagation().membership
		agg = []
		agg.append(self.prodgr.community_label_propagation().membership)
		agg.append(self.prodgr.community_label_propagation().membership)
		lll = zip(*agg)
		ddd = dict((o, i) for i, o in enumerate(set(lll)))
		self.prodgr.community_label_propagation(initial=[ddd[o] for o in lll])
		'''

		# select communities from dendrograms generated by a bunch of algorithms
		dgrams = [
		  undirect_and_simplify(self.prodgr).community_fastgreedy(),
		  self.prodgr.community_edge_betweenness(directed=True),
		]
		gg = undirect_and_simplify(self.prodgr)
		gg.delete_vertices(gg.vs.select(_degree=0)) # walktrap impl can't handle islands
		dgrams.append(gg.community_walktrap())

		def int_unique(flseq):
			"""
			Turns a sorted list of floats into a sorted list of unique ints
			"""
			it = iter(flseq)
			o = round(it.next())
			yield int(o)

			while True:
				i = round(it.next())
				if i != o:
					yield int(i)
				o = i

		from igraph.core import InternalError
		from igraph.statistics import power_law_fit
		ratio = power_law_fit([prod.size() for prod in self.phdb.itervalues()], 6)

		for vxd in dgrams:
			default_cut = len(vxd)
			for n in int_unique(geo_prog_range(2, len(gg.vs)/4, ratio, default_cut)):
				try:
					mem = vxd.cut(n)
				except InternalError:
					continue
				comm.update(frozenset(community) for community in invert_seq(mem).itervalues())

		total = len(self.prodgr.vs)
		# don't generate too big or too small
		return [com for com in comm if log(total) <= len(com) <= total/log(total)]


	def generatePTables(self):
		"""
		DOCUMENT
		"""
		name = "ptables"

		id_u = dict((nsid, vid) for vid, nsid in enumerate(self.socgr.vs[NID]))

		base_h = len(id_u)
		lab_h, id_h = zip(*((nsid, (nsid, base_h+vid)) for vid, nsid in enumerate(self.gumap.iterkeys()))) if self.gumap else ([], [])
		id_h = dict(id_h)

		base_g = len(id_h) + base_h
		lab_g, id_g = zip(*((nsid, (nsid, base_g+vid)) for vid, nsid in enumerate(self.sprdgr.vs[NID]))) if len(self.sprdgr.vs) > 0 else ([], [])
		id_g = dict(id_g)

		ptabgr = graph_copy(self.socgr)

		edges = set()
		# add arcs to self
		edges.update((vid, vid) for vid in xrange(0, base_h))
		# add arcs to indexes
		for nsid, users in self.gumap.iteritems():
			hvid = id_h[nsid]
			edges.update((id_u[user], hvid) for user in users)
		# add arcs to tgraphs
		for i, hvids in enumerate(self.comm):
			gvid = base_g + i
			for nsid in self.prodgr.vs.select(hvids)[NID]:
				if nsid in id_u:
					continue
				for user in self.gumap[nsid]:
					edges.add((id_u[user], gvid))

		ptabgr.add_vertices(len(id_h) + len(id_g))
		eend = len(ptabgr.es)
		ptabgr.add_edges(edges)
		ptabgr.es[eend:][AAT] = [0.5] * len(edges)
		ptabgr.es.select(ptabgr.get_eid(vid, vid) for vid in xrange(0, base_h))[AAT] = [1.0] * base_h
		ptabgr.vs[base_h:][NID] = lab_h + lab_g
		ptabgr["base_z"] = 0
		ptabgr["base_h"] = base_h
		ptabgr["base_g"] = base_g
		self.ptabgr = ptabgr

		# for easy access / human readability
		ugmap = dict((nsid, set(gnsid)) for nsid, gnsid in invert_multimap(self.gumap.iteritems(),
		  dict((nsid, []) for nsid in self.socgr.vs[NID])).iteritems())
		for i, pvid in enumerate(self.comm):
			spid = self.sprdgr.vs[i][NID]
			for nsid in self.prodgr.vs.select(pvid)[NID]:
				if nsid in ugmap:
					continue
				for uid in self.gumap[nsid]:
					ugmap[uid].add(spid)
		self.ptbmap = ugmap



class SampleWriter(ProducerSample):

	def __init__(self, phdb, pgdb, totalsize):
		ProducerSample.__init__(self, phdb, pgdb)
		self.totalsize = totalsize


	def writeIndexes(self, base):
		def run(nsid):
			prod = self.phdb[nsid]
			g = prod.createIndex()
			g.write(os.path.join(base, "%s.graphml" % nsid))
		exec_unique(self.phdb.iterkeys(), lambda nsid: os.path.exists(os.path.join(base, "%s.graphml" % nsid)),
		  run, None, "indexes db: object files", LOG.info)


	def writeTGraphs(self, base):
		def run(nsid):
			prod = self.pgdb[nsid]
			g = prod.createTGraph(self.totalsize, self.pgdb)
			g.write(os.path.join(base, "%s.graphml" % nsid))
		exec_unique(self.pgdb.iterkeys(), lambda nsid: os.path.exists(os.path.join(base, "%s.graphml" % nsid)),
		  run, None, "tgraphs db: object files", LOG.info)


class SampleStats(object):

	def __init__(self, ptdb, tpdb, totalsize):
		self.ptdb = ptdb
		self.tpdb = tpdb
		self.totalsize = totalsize


	def getTagInfo(self, tag):
		"""
		DOCUMENT

		@return: (photos, totalsize, [rtag:(intersect,total)])
		"""
		photos = self.tpdb[tag]
		inv = invert_multimap((pid, self.ptdb[pid]) for pid in photos)
		rel = dict((tag, (len(intersect), len(self.tpdb[tag]))) for tag, intersect in inv.iteritems())
		return TagInfo(tag, photos, rel, self.totalsize)


	def getIDInfo(self, id):
		"""
		DOCUMENT
		"""
		pass


	def getIDTagInfo(self, id, tag):
		"""
		DOCUMENT
		"""
		pass


