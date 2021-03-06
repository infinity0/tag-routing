# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, logging, os
from math import log, exp
from itertools import chain, izip
from igraph import Graph, IN, OUT, ALL
from random import sample

from tags.eval.objects import (Node, NodeSample, NID, NAA, NAT, AAT, P_ARC,
  Producer, ProducerSample, ProducerRelation, TagInfo, IDInfo, AddrSchemeEval)
from tags.eval.util import (union_ind, geo_prog_range, split_asc, sort_v,
  infer_arcs, edge_array, graph_prune_arcs, undirect_and_simplify, freq,
  invert_seq, invert_multimap, write_align_column, exec_unique)

LOG = logging.getLogger(__name__)


class SampleGenerator(object):

	def __init__(self, socgr, gumap, pddb, dppb, dtdb, tcdb, phdb, phsb, pgdb, pgsb):
		"""
		Create a new SampleGenerator from the given arguments

		@param socgr: social network graph
		@param gumap: {group:[user]} map
		@param pddb: an open database of {producer:[doc]}
		@param dppb: an open database of {doc:[producer]}
		@param dtdb: an open database of {doc:[tag]}
		@param tcdb: an open database of {tag:[cluster]}
		@param phdb: an open database of {nsid:Producer} for indexes
		@param phsb: an open database of {nsid:Producer.state} for indexes
		@param pgdb: an open database of {prid:Producer} for tgraphs
		@param pgsb: an open database of {prid:Producer.state} for tgraphs
		"""
		self.socgr = socgr
		self.gumap = gumap

		self.pddb = pddb
		self.dppb = dppb
		self.dtdb = dtdb
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
			prod.initContent(self.pddb[nsid], self.dtdb)
			prod.inferScores()
			prod.repDoc()
			prod.repTag()
			self.phdb[nsid] = prod
			self.phsb[nsid] = prod.state
		exec_unique(self.pddb.iterkeys(), self.phsb, run_p, None, "%s db: producers" % name, LOG.info)

		# generate content arcs between producers
		def run_r(nsid):
			prod = self.phdb[nsid]
			if prod.state != P_ARC:
				rels = self.inferRelProds(prod)
				pmap = dict((rnsid, ProducerRelation(rattr, self.inferProdArc(prod,
				  self.phdb[rnsid]))) for rnsid, rattr in rels.iteritems())
				prod.initProdArcs(pmap)
				self.phdb[nsid] = prod
			self.phsb[nsid] = prod.state
		exec_unique(self.phdb.iterkeys(), lambda nsid: self.phsb[nsid] >= P_ARC, run_r, None,
		  "%s db: relations" % name, LOG.info, steps=0x10000)

		total = len(self.phdb)
		lab_p, id_p = zip(*(("%s (%s)\\n%s" % (nsid, prod.size(), '\\n'.join(prod.rep_t[0:4])),
		  (nsid, i)) for i, (nsid, prod) in enumerate(self.phdb.iteritems()))) if self.phdb else ([], [])
		id_p = dict(id_p)

		# generate producer graph
		arc_s, arc_t, edges, score = edge_array(total, 'd')
		for i, prod in enumerate(self.phdb.itervalues()):
			for vx in prod.docgr.vs.select(prod.prange()):
				arc_s.append(i)
				arc_t.append(id_p[vx[NID]])
				score.append(vx[NAA])

		sz = [log(1+prod.size()) for prod in self.phdb.itervalues()]
		v_attr = {NID: list(self.phdb.iterkeys()), "label": lab_p, NAT: sz, AAT: sz}

		self.prodgr = Graph(total, edges=list(edges), directed=True, vertex_attrs=v_attr, edge_attrs={AAT: score})
		LOG.info("%s db: generated producer graph" % name)


	def inferRelProds(self, prod):
		"""
		Infer a set of related producers for the given producer.

		This implementation selects producers that hold the documents in the
		representative documents set of the given source producer.
		"""
		rel = invert_multimap((doc, self.dppb[doc]) for doc in prod.rep_d)
		if rel:
			del rel[prod.nsid] # if it's not empty, then it must refer back to itself
		return dict((nsid, float(len(docs))/len(self.pddb[nsid])) for nsid, docs in rel.iteritems())


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

		tot_s = len(self.comm)
		id_p = dict(("%04d" % i, i) for i in xrange(0, tot_s))

		# generate docsets for new producers
		def run_p(nsid):
			prod = Producer(nsid)
			prod.initContent(set(chain(*(self.pddb[self.prodgr.vs[p][NID]] for p in self.comm[id_p[nsid]]))), self.dtdb, True)
			prod.inferScores()
			prod.repTag(cover=0) # TWEAK
			self.pgdb[nsid] = prod
			self.pgsb[nsid] = prod.state
		exec_unique(id_p, self.pgsb, run_p, None, "%s db: producers" % name, LOG.info)

		tot_p = len(self.prodgr.vs)
		edges, arc_a = infer_arcs(self.comm, tot_p, ratio=2*log(1+tot_p)) # TWEAK # relax for tgraphs
		self.sprdgr = Graph(tot_s, list(edges), directed=True,
		  vertex_attrs={NID:list("%04d" % i for i in xrange(0, tot_s)), "label":[len(com) for com in self.comm]})
		g = self.sprdgr
		LOG.info("%s db: generated producer graph" % name)

		# generate content arcs between producers
		def run_r(nsid):
			prod = self.pgdb[nsid]
			if prod.state != P_ARC:
				rprod = g.vs.select(g.successors(id_p[nsid]))[NID]
				pmap = dict((rnsid, ProducerRelation(None,
				  *self.inferProdArc(prod, self.pgdb[rnsid], show_tag=True))) for rnsid in rprod)
				prod.initProdArcs(pmap, has_tags=True)
				self.pgdb[nsid] = prod
			self.pgsb[nsid] = prod.state
		exec_unique(self.pgdb.iterkeys(), lambda nsid: self.pgsb[nsid] >= P_ARC, run_r, None,
		  "%s db: relations" % name, LOG.info, steps=0x10000)


	def generateCommunities(self):
		"""
		Generates a bunch of communities using various community detection
		algorithms on the underlying producer graph, and selects the non-tiny
		ones.
		"""
		comm = set()

		# aggregate communities using "label propagation" algorithm.
		# without aggregation, multiple runs will generate a lot of similar
		# (ie. redundant) communities with size in the midrange
		labels = zip(*(self.prodgr.community_label_propagation().membership for i in xrange(0, 4)))
		ddd = dict((o, i) for i, o in enumerate(set(labels)))
		mem = self.prodgr.community_label_propagation(initial=[ddd[o] for o in labels],
		  fixed=[False]*self.prodgr.vcount()).membership # fixed=[False]*V workaround igraph bug #570902
		comm.update(frozenset(community) for community in invert_seq(mem).itervalues())
		# TODO repeat this several times?

		# select communities from dendrograms generated by a bunch of algorithms
		dgrams = [
		  undirect_and_simplify(self.prodgr).community_fastgreedy(),
		  #self.prodgr.community_edge_betweenness(directed=True), # this has atrocious performance
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
		self.comm = [list(com) for com in comm if log(1+total) <= len(com) <= total/log(1+total)]
		return self.comm


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

		ptabgr = self.socgr.copy()

		edges = set()
		# add arcs to self
		edges.update((vid, vid) for vid in xrange(0, base_h))
		# add arcs to indexes
		for nsid, users in self.gumap.iteritems():
			hvid = id_h[nsid]
			edges.update((id_u[user], hvid) for user in users)
		# add arcs to tgraphs
		phmap = {}
		for i, hvids in enumerate(self.comm):
			gvid = base_g + i
			for hid in self.prodgr.vs.select(hvids)[NID]:
				if hid in id_u:
					continue
				for user in self.gumap[hid]:
					if user in phmap:
						phmap[user].add(gvid)
					else:
						phmap[user] = set([gvid])
		# only add some of these, to prevent a user linking to eg. 400 tgraphs
		for user, gvids in phmap.iteritems():
			pvid = id_u[user]
			for gvid in sample(gvids, int(len(gvids)**0.5)):
				edges.add((pvid, gvid))

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



FMT_EXT = "%s.graphmlz"
FMT_UNW = "%s.json.gz"

class SampleWriter(ProducerSample):

	def __init__(self, phdb, pgdb, totalsize):
		ProducerSample.__init__(self, phdb, pgdb)
		self.totalsize = totalsize


	def writeIndexes(self, dir):
		def run(nsid):
			prod = self.phdb[nsid]
			g = prod.createIndex()
			g.write(os.path.join(dir, FMT_EXT % nsid))
		exec_unique(self.phdb.iterkeys(), lambda nsid: os.path.exists(os.path.join(dir, FMT_EXT % nsid)),
		  run, None, "indexes db: object files", LOG.info)


	def writeTGraphs(self, dir):
		def run(nsid):
			prod = self.pgdb[nsid]
			g = prod.createTGraph(self.totalsize, self.pgdb)
			g.write(os.path.join(dir, FMT_EXT % nsid))
		exec_unique(self.pgdb.iterkeys(), lambda nsid: os.path.exists(os.path.join(dir, FMT_EXT % nsid)),
		  run, None, "tgraphs db: object files", LOG.info)


	def unwrapTGraph(self, dir, gid):
		LOG.debug("unwrap tgraph %s: reading" % gid)
		fn = os.path.join(dir, FMT_EXT % gid)
		g = Graph.Read(fn)
		self.unwrap(dir, g, dict((v[NID], v[NAT]) for v in g.vs), gid, xrange(g.vcount()), self.makeTGraphTuple)


	def unwrapIndex(self, dir, hid, skip_lower_than=0):
		LOG.debug("unwrap index %s: reading" % hid)
		fn = os.path.join(dir, FMT_EXT % hid)
		if skip_lower_than and os.path.getsize(fn) < skip_lower_than:
			LOG.info("unwrap index %s: skipping (%s < %s)" % (hid, os.path.getsize(fn), skip_lower_than))
			return

		h = Graph.Read(fn)
		tags = xrange(h["base_t"], h["base_h"])
		self.unwrap(dir, h, [v[NID] for v in h.vs.select(tags)], hid, tags, self.makeIndexTuple)


	def unwrap(self, dir, g, nodes, gid, vrange, mktuple):
		from simplejson import dump
		from zlib import crc32
		from gzip import GzipFile

		sz = int(log(1+g.vcount())**2/16)
		mask = 0x2**sz-1
		maskfmt = "%%0%dx" % (((sz-1)>>2)+1)

		bdir = os.path.join(dir, gid)
		if not os.path.isdir(bdir):
			os.mkdir(bdir)

		LOG.debug("unwrap %s: writing attributes and nodes" % gid)
		attributes = {"mask":mask}
		attributes.update((attr, g[attr]) for attr in g.attributes())
		with open(os.path.join(bdir, FMT_UNW % "attributes"), 'wb') as fp:
			dump(attributes, GzipFile(fileobj=fp))
		with open(os.path.join(bdir, FMT_UNW % "nodes"), 'wb') as fp:
			dump(nodes, GzipFile(fileobj=fp))

		LOG.debug("unwrap %s: making buckets" % gid)
		buckets = {}
		for vid in vrange:
			tag, tup = mktuple(g, vid)
			hash = maskfmt % (crc32(tag)&mask)
			if hash not in buckets:
				buckets[hash] = {}
			buckets[hash][tag] = tup

		LOG.debug("unwrap %s: writing buckets" % gid)
		for bucket, contents in buckets.iteritems():
			with open(os.path.join(bdir, FMT_UNW % bucket), 'wb') as fp:
				dump(contents, GzipFile(fileobj=fp))

		LOG.info("unwrap %s: complete" % gid)


AAT_A = "logweight" # additive arc-attribute
AAT_AD = "logdist" # additive arc-attribute (distance measure)


class SampleStats(object):

	def __init__(self, pddb, dppb, dtdb, tddb, totalsize, ptabgr, prodgr, sprdgr):
		self.pddb = pddb
		self.dppb = dppb
		self.dtdb = dtdb
		self.tddb = tddb
		self.totalsize = totalsize
		self.ptabgr = ptabgr
		self.prodgr = prodgr
		self.sprdgr = sprdgr

		self.id_p = dict((intern(nsid), vid) for vid, nsid in enumerate(self.ptabgr.vs[NID]))
		self.id_h = dict((intern(nsid), vid) for vid, nsid in enumerate(self.prodgr.vs[NID]))


	def degreePlot(self, g, fp=None, type=ALL):
		gen = sorted(freq(g.degree(type=type)).iteritems())
		if not fp: return list(gen)
		for k, v in gen:
			print >>fp, k, v


	def getTagInfo(self, tag):
		"""
		DOCUMENT

		@return: (documents, totalsize, [rtag:(intersect,total)])
		"""
		docs = self.tddb[tag]
		rel = dict((tag, (len(intersect), len(self.tddb[tag]))) for tag, intersect in
		  invert_multimap((doc, self.dtdb[doc]) for doc in docs).iteritems())
		prod = dict((prod, (len(ps), len(self.pddb[prod]))) for prod, ps in
		  invert_multimap((doc, self.dppb[doc]) for doc in docs).iteritems())
		return TagInfo(tag, docs, rel, prod, self.totalsize)


	def getIDInfo(self, id):
		"""
		DOCUMENT
		"""
		gr_p = self.ptabgr
		gr_h = self.prodgr

		out = gr_p.successors(self.id_p[id])
		soc, idx, tgr = split_asc(out, (gr_p["base_z"], gr_p["base_h"], gr_p["base_g"]))
		idx_h = [self.id_h[nsid] for nsid in gr_p.vs.select(idx)[NID]]
		rel_h = gr_h.vs.select(set(chain(*(gr_h.successors(id) for id in idx_h))))[NID]
		return IDInfo(id, gr_p.vs.select(soc)[NID], gr_p.vs.select(tgr)[NID], gr_p.vs.select(idx)[NID], rel_h)
		'''
		- closeness centrality w.r.t. index graph:
		  - closeness(set) = sum[v in G] { 2 ^ -mean_path_len(set, v) }
		  - mean_path_len(set, v) = weighted_mean[s in set] { path_len(s, v) }
		  - set is just ptable(id).indexes
		TODO put weights on edges in idx.graphml
		'''


	def closeness(self, id, tag=None):
		"""
		Return the closeness between the set of indexes pointed to by an ID's
		ptable, and the set of indexes pointing to a tag (or the entire graph,
		if the tag is None).

		DOCUMENT. for now, see evaluate.html
		"""
		g = self.prodgr

		idi = self.getIDInfo(id)
		src = sorted([self.id_h[nsid] for nsid in set(idi.idx+idi.soc)])
		dst = range(0, len(g.vs)) if tag is None else sorted([
		  self.id_h[nsid] for nsid in self.getTagInfo(tag).prod.iterkeys()])

		srcout = len(set(chain(*(g.successors(i) for i in src))))
		dstin = len(set(chain(*(g.predecessors(i) for i in dst))))

		if AAT_A not in g.es:
			g.es[AAT_A] = [-log(attr) for attr in g.es[AAT]]
			#print g.es[AAT_A]

		if not src: return 0.0
		dstcl = []
		for lengths in g.vs.select(dst).shortest_paths(weights=AAT_A, mode=IN):
			#print [lengths[i] for i in src]
			dstcl.append(sum(exp(-lengths[i]) for i in src)/len(src))
			#dstcl.append(sum(exp(-lengths[i]) for i in src))
			#dstcl.append(exp(-min(lengths[i] for i in src)))

		return (srcout, dstin), (max(dstcl), sum(dstcl), sum(dstcl)/len(dstcl)), dstcl


	def getAllCloseness(self):
		g = self.ptabgr
		return dict((nsid, self.closeness(nsid)) for nsid in g.vs.select(xrange(0, g["base_h"]))[NID])


	def evaluateScheme(self, scheme):
		"""
		Evaluate the given address scheme against the perfect address scheme
		from the complete data of the world.
		"""
		prune = scheme.copy()
		prune.delete_vertices(v.index for v in prune.vs.select(lambda vx: vx[NAA] is None))

		ss = NodeSample()
		for tag in prune.vs[NID]:
			ss.add_node(self.getTagInfo(tag).build_node())

		MAX = float("inf")
		def dist(arc, graph):
			return -log(arc[AAT]*graph.vs[arc.target][NAT]/graph.vs[arc.source][NAT])
		def nattr(dist):
			return exp(-dist)

		# build address scheme of input tags
		local = ss.build(complete=False)
		assert None not in local.vs[NAT]
		local.es[AAT_AD] = [dist(arc, local) for arc in local.es]
		path = local.shortest_paths(0, weights=AAT_AD)[0]
		graph_prune_arcs(local, [k for k,v in sort_v(enumerate(path))])
		local.vs[NAA] = [nattr(d) for d in path]

		# build address scheme of n tags from world data, where n = len(input tags)
		# OPT LOW this rebuilds the entire graph each time, not optimal, but means
		# we can just use already-existing implementation of dijkstra from igraph
		sw = NodeSample()
		tinfo = self.getTagInfo(prune.vs[0][NID])
		sw.add_node(tinfo.build_node())
		visit = set([0]) # visited nodes, sw vids
		trail = [(0, 0.0)] # trail of visited nodes, ss vids
		for i in xrange(0, len(prune.vs)-1): # n-1 because root already added
			for rtag in tinfo.rtag.iterkeys():
				if rtag not in sw:
					sw.add_node(self.getTagInfo(rtag).build_node())
			world = sw.build(complete=False)
			world.es[AAT_AD] = [dist(arc, world) for arc in world.es]
			path = world.shortest_paths(0, weights=AAT_AD)[0]

			# get next tag in world addr scheme
			npath = [MAX if i in visit else v for i, v in enumerate(path)]
			d = min(npath)
			index = npath.index(d)
			tinfo = self.getTagInfo(world.vs[index][NID])
			visit.add(index)
			if tinfo.tag not in ss:
				trail.append((len(ss), d))
				ss.add_node(tinfo.build_node())
			else:
				trail.append((prune.vs[NID].index(tinfo.tag),d)) # OPT LOW

		world = ss.build()
		assert len(trail) == len(local.vs)
		graph_prune_arcs(world, [vid for vid, dist in trail])
		trail = dict(trail)
		world.vs[NAA] = [nattr(trail[i]) if i in trail else None for i in xrange(0, len(world.vs))]

		return AddrSchemeEval(prune, local, world)


	def printReports(self, reports, eaddr=False, pretty=False, steplo=0x08, closeness=None, fp=sys.stdout):
		lines = [['close', 'steps', 'precision', 'recall', 'f1_score']]
		if eaddr:
			lines[0].append('addr_scheme_score')
		if closeness is None:
			closeness = self.closeness

		tincache = {}
		total = len(reports)
		i = 1
		for rep in reports:
			close = closeness(rep.id, rep.tag)

			if rep.tag in tincache:
				tinfo = tincache[rep.tag]
			else:
				tinfo = self.getTagInfo(rep.tag)
				tincache[rep.tag] = tinfo

			for s, stepr in sorted(rep.steps.iteritems()):
				if s < steplo: continue
				line = [str(close), str(s)] + list(tinfo.score_triple(stepr.results))
				if eaddr:
					line.append(self.evaluateScheme(stepr.scheme).score_world())
				lines.append(line)
			LOG.info("%s/%s reports processed" % (i, total))
			i += 1

		if pretty:
			lines.insert(1, None)
			write_align_column(lines, len(lines[0]), fp=fp)
		else:
			print >>fp, '#',
			for line in lines:
				print >>fp, ' '.join(str(v) for v in line)


