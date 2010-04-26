# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, re, os
from functools import partial
from itertools import chain
from collections import namedtuple
from ast import literal_eval

from zlib import compress, decompress
from tempfile import TemporaryFile
from igraph import Graph, IN, OUT

from tags.scrape.state import StateError, state_req, state_not, state_next
from tags.scrape.util import (intern_force, geo_mean, union_ind, f1_score,
  sort_v, edge_array, infer_arcs, iterconverge, representatives, graph_copy,
  callable_wrap, TMP_RAM)


NID = "id" # label for node id in graphs
NAT = "height" # label for node attribute in graphs
AAT = "weight" # label for arc attribute in graphs
NAA = "score" # label for arc attributes from a node


class NodeSample(object):

	def __init__(self, nodes=[]):
		self.order = None
		self.extra = None
		self.graph = None

		# nodes might be a generator, in which case we can't iterate over it twice
		self._list = list(nodes)
		self._keys = set(node.id for node in self._list)
		if len(self._keys) != len(self._list):
			raise ValueError("duplicate node(s)")

	def __contains__(self, id):
		return id in self._keys

	def __len__(self):
		return len(self._list)


	def add_node(self, node):
		"""
		@param node: Node to add
		"""
		if self.graph is not None:
			raise StateError("sample already finalised")

		if node.id in self._keys:
			raise KeyError

		self._keys.add(node.id)
		self._list.append(node)
		return self


	def build(self, keep_dangle=False, bipartite=False, node_attr=None, inverse=False, complete=True):
		"""
		Build a graph out of the sample.

		Definitions: an "explicit" node is one which has been explicited added
		to the sample with add_node(). A "dangling" node is one referenced by
		the out-dicts of other nodes, but not explicitly added to the sample
		(and implicitly has no out-dict of its own).

		The total number of explicit nodes will be stored in self.order, and
		if kept (see <keep_dangle>), the total number of dangling nodes will be
		stored in self.extra.

		@param keep_dangle: whether to keep dangling nodes; if so, these nodes
		       will have vertex ids greater than explicit nodes. <bipartite>
		       and <node_attr> will only have an effect if this is True.
		@param bipartite: whether to raise an error if an explicit node points
		       to another explicit node
		@param node_attr: this is passed through callable_wrap() to give a
		       mapping from dangling nodes to node attributes
		@param inverse: whether to invert edge directions
		@param complete: whether to store the built graph in self.graph and
		       discard the cache. it will then be impossible to add new nodes,
		       and future calls to this method will always return self.graph
		@return: The built graph
		"""
		if self.graph is not None: return self.graph

		if keep_dangle:
			attr_cb = callable_wrap(node_attr)

		# init nodes
		v_id = [node.id for node in self._list]
		v_attr = [node.attr for node in self._list]
		id_v = dict((node.id, i) for i, node in enumerate(self._list))
		self.order = j = len(self._list)

		# init edges
		arc_s, arc_t, edges, e_attr = edge_array(j, 'd', inverse)

		for (i, node) in enumerate(self._list):
			for (dst, attr) in node.out.iteritems():
				if dst in self._keys:
					if keep_dangle and bipartite:
						raise ValueError("non-bipartite graph: %s - %s" % (node.id, dst))
					arc_s.append(i)
					arc_t.append(id_v[dst])
					e_attr.append(attr)

				elif keep_dangle:
					if dst in id_v:
						x = id_v[dst]
					else:
						x = id_v[dst] = j
						v_id.append(dst)
						v_attr.append(attr_cb(dst))
						j += 1

					arc_s.append(i)
					arc_t.append(x)
					e_attr.append(attr)
				else:
					pass

		assert j == len(id_v) == len(v_id) == len(v_attr)
		if keep_dangle:
			self.extra = j - self.order

		# igraph can't handle utf-8 output, see launchpad bug #545663
		for (i, id) in enumerate(v_id):
			if type(id) == unicode:
				v_id[i] = id.encode("utf-8")

		# prepare attributes
		va = {NID: v_id}
		if any(a is not None for a in v_attr):
			va[NAT] = v_attr

		# build graph
		gg = Graph(n=j, directed=True, vertex_attrs=va)
		gg.add_edges(edges)
		gg.es[AAT] = e_attr

		if complete:
			self.graph = gg
			self._keys = None
			self._list = None

		return gg



class Node(namedtuple('Node', 'id out attr')):

	__slots__ = ()
	def __new__(cls, id, out, attr=None):
		"""
		@param id: Unique identifier
		@param out: { id: arc-attribute } of out-neighbours
		@param attr: Node-attribute
		"""
		return super(Node, cls).__new__(cls, id, out, attr)



class ProducerSample(object):

	def __init__(self, phdb, pgdb):
		self.pgdb = pgdb
		self.phdb = phdb

	def makeTGraphNode(self, g, tag):
		"""
		Make a Node object out of a tag in a tgraph.
		"""
		return Node(tag, dict((g.vs[e.target][NID], g.es[e.index][AAT])
		  for e in g.es.select(g.adjacent(g.vs.select(id=tag)[0].index))), g.vs.select(id=tag)[0][NAT])



P_NEW, P_CONTENT, P_SCORES, P_ARC = 0, 1, 2, 3
E_NOTNEW = "initContent already called"
E_NEW = "initContent not yet called"
E_NOTARC = "initProdArcs not yet called"
E_ARC = "initProdArcs already called"
E_SCORE = "inferScores not yet called"
EE_SCORE = {P_NEW:E_SCORE, P_CONTENT:E_SCORE, P_ARC:E_ARC}
EE_CONTENT = {P_NEW:E_NEW, P_ARC:E_ARC}

class Producer(object):
	"""
	This makes use of the NAA attribute in two different ways:

	- the NAA of a tag T, measures how relevant the tag is to this Producer.
	  ie. tag-arcs to this Producer should have this NAA as its AAT.
	- the NAA of a producer P, aggregates all tag arcs to P. ie. in the overall
	  index graph, this Producer should point to P with this NAA as its AAT.
	"""

	__slots__ = ("nsid", "state", "_docgr", "__docgr", "__t_naa", "id_d", "id_t", "id_p",
	  "base_d", "base_t", "base_s", "base_p", "rep_d", "rpp_d", "rep_t", "rpp_t",
	)

	def __init__(self, nsid):
		"""
		Creates a new producer
		"""
		self.nsid = nsid
		self.state = P_NEW

		self._docgr = None # field
		self.__docgr = None # cache
		self.__t_naa = None # cache

		self.id_d = None # {doc:vid}
		self.id_t = None # {tag:vid}
		self.id_p = None # {pid:vid}

		self.base_d = None # first doc id
		self.base_t = None # first tag id
		self.base_s = None # first newtag id
		self.base_p = None # first producer id

		self.rep_d = None # representative docs
		self.rpp_d = None # representative docs
		self.rep_t = None # representative tags
		self.rpp_t = None # representative tags

	@property
	def docgr(self):
		if self.__docgr is not None:
			with TemporaryFile(dir=TMP_RAM) as fp:
				fp.write(decompress(self.__docgr))
				fp.seek(0)
				self._docgr = Graph.Read_GraphML(fp)
				print "marshalling graph for %s" % self.nsid
				self.__docgr = None
				self.__t_naa = None
		return self._docgr

	@docgr.setter
	def docgr(self, val):
		self._docgr = val
		self.__docgr = None
		self.__t_naa = None

	@docgr.deleter
	def docgr(self):
		assert False # prevent deletion

	def __getstate__(self, level=3):
		if self.__docgr is None and self._docgr is not None:
			tag_scores = self._docgr.vs[NAA]
			with TemporaryFile(dir=TMP_RAM) as fp:
				self._docgr.write_graphml(fp)
				fp.seek(0)
				graph_bytes = compress(fp.read(), level)
		else:
			assert (self.__docgr is None and self.__t_naa is None) or (self.__docgr is not None and self.__t_naa is not None)
			graph_bytes = self.__docgr
			tag_scores = self.__t_naa

		return (self.nsid, self.state, graph_bytes, self.id_d, self.id_t, self.id_p,
		  self.base_d, self.base_t, self.base_s, self.base_p,
		  self.rep_d, self.rpp_d, self.rep_t, self.rpp_t,
		  tag_scores
		)

	def __setstate__(self, state):
		(self.nsid, self.state, self.__docgr, self.id_d, self.id_t, self.id_p,
		  self.base_d, self.base_t, self.base_s, self.base_p,
		  self.rep_d, self.rpp_d, self.rep_t, self.rpp_t,
		  self.__t_naa
		) = state #if len(state) == 15 else state + (None,) # uncomment to perform back-compat maintenance


	@state_req(P_NEW, E_NOTNEW)
	@state_next(P_CONTENT)
	def initContent(self, dset, dtdb, store_node_attr=False):
		"""
		Initialises the doc-tag graph from the given document set and the given
		doc-tag database.

		@param dset: a collection of documents
		@param dtdb: an open database of {doc:[tag]}
		@param store_node_attr: whether to calculate and store node attributes
		"""
		def outdict(doc):
			tags = dtdb[doc]
			attr = len(tags)**-0.5 if tags else 0 # formula pulled out of my ass
			# however it follows the principle of "more tags there are, less important each one is"
			# TODO HIGH decide whether this is actually a good idea, or just use a constant 1
			return dict((tag, attr) for tag in tags)

		ss = NodeSample(Node(doc, outdict(doc)) for doc in set(dset))
		g = ss.build(keep_dangle=True, bipartite=True, inverse=True)
		g.vs[NAT] = [float(d)/ss.order for d in g.outdegree()]
		self.docgr = g

		id_d = {} # {doc:id}
		id_t = {} # {tag:id}
		for vid, id in enumerate(g.vs[NID]):
			if vid < ss.order:
				id_d[id] = vid
			else:
				id_t[id] = vid
		assert len(id_d) == ss.order and len(id_t) == ss.extra

		self.id_d = id_d
		self.id_t = id_t
		self.base_d = 0
		self.base_t = ss.order


	def drange(self):
		return xrange(0, len(self.id_d))

	def trange(self):
		return xrange(self.base_t, self.base_t + len(self.id_t))

	def srange(self):
		return xrange(self.base_s, self.base_p)

	def prange(self):
		return xrange(self.base_p, self.base_p + len(self.id_p))

	def size(self):
		"""
		Returns the number of documents.
		"""
		return self.base_t


	@state_not(P_NEW, E_NEW)
	def tagsForDoc(self, doc):
		"""
		Returns a map of tags to their doc-tag weights, for the given doc.

		@param doc: this can either be a graph id or a doc.
		"""
		if self.docgr is None:
			raise StateError("initContent not called yet")

		if doc in self.id_d:
			id = self.id_d[doc]
		elif doc in self.drange():
			id = doc
		else:
			raise ValueError()

		g = self.docgr
		eseq = g.es.select(g.adjacent(id, IN))
		return dict((g.vs[e.source][NID], e[AAT]) for e in eseq)


	@state_not(P_NEW, E_NEW)
	def docsForTag(self, tag):
		"""
		Returns a map of docs to their doc-tag weights, for the given tag.

		@param tag: this can either be a graph id or a tag.
		"""
		if self.docgr is None:
			raise StateError("initContent not called yet")

		if tag in self.id_t:
			id = self.id_t[tag]
		elif tag in self.trange():
			id = tag
		else:
			raise ValueError()

		g = self.docgr
		eseq = g.es.select(g.adjacent(id, OUT))
		return dict((g.vs[e.target][NID], e[AAT]) for e in eseq)


	@state_not((P_NEW, P_ARC), EE_CONTENT)
	@state_next(P_SCORES)
	def inferScores(self, init=0.5):
		"""
		Infer scores for docs and tags.

		DOCUMENT more detail
		"""
		g = self.docgr

		# doc-tag weight is P(t|d)
		# tags and docs are considered as bags of meaning
		# a producer = union of tags = union of docs

		# Infer P(t|this) = union_ind(P(t|d) over all d attached to t)
		#
		# Justification: roughly, 1 match out of any is satisfactory. We have
		# no further information so assume P(t|d) independent over d.
		sc_t = list(union_ind(g.es.select(g.adjacent(id, OUT))[AAT]) for id in self.trange())

		# Infer P(d|this) = union_ind(P(d|t) over all t attached to d)
		#
		# We assume that P(d|this) = P(d). This is NOT theoretically sound, but
		# it doesn't matter because this heuristic is only used within this
		# producer, to rank documents. (In reality, P(d|this) >> P(d).)
		#
		# We rewrite P(d|t) in terms of P(t|d); this results in a formula with
		# P(d) on both sides; we use iterconverge to find a non-zero solution.
		#
		# Special case: if there is only 1 tag, its weight is 1.0, and its arc
		# weight is 1.0, then iteration will always return the inital value.
		# So we'll arbitrarily choose init=0.5 by default.
		sc_d = []
		def scoreDoc(id, k):
			eseq = g.es.select(g.adjacent(id, IN))
			try:
				return union_ind(k*e[AAT]/sc_t[e.source-self.base_t] for e in eseq)
			except IndexError:
				print list(e.source for e in eseq)
				raise
		for id in self.drange():
			sc_d.append(iterconverge(partial(scoreDoc, id), (0,1), init, eps=2**-32, maxsteps=0x40))

		self.docgr.vs[NAA] = sc_d + sc_t


	@state_not((P_NEW, P_ARC), EE_CONTENT)
	def debugScores(self, fp=sys.stderr):
		"""
		Tests the scoreNodes() algorithm for different values of <init>.

		@param fp: the stream on which to output the table
		"""
		res = {}
		for i in [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]:
			self.inferScores(i)
			res[i] = self.docgr.vs.select()[NAA][:]
			print >>fp, "%.12f" % i,

		print >>fp, ""
		print >>fp, "-" * (15*11-1)
		from math import fabs

		for c in self.drange():
			k = -1
			for i in [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]:
					o = k
					k = res[i][c]
					if fabs(k/o-1) <= sys.float_info.epsilon if o != 0 else k <= sys.float_info.min:
						print >>fp, "%-14s" % "same",
					else:
						print >>fp, "%.12f" % res[i][c],
			print >>fp, ""


	@state_req(P_SCORES, EE_SCORE)
	def repDoc(self, store_res=False, prop=0.25, thres=0.96, cover=1):
		"""
		Generates a set of representative docs for this producer. The results
		are stored in self.rep_d and also returned, and the result parameters
		are stored in self.rpp_d (if <store_res> is True).

		This method takes the same parameters as representatives()

		@param store_res: whether to store the result parameters
		"""
		g = self.docgr
		cand = dict((g.vs[id][NID], (g.vs[id][NAA], g.predecessors(id))) for id in self.drange())
		self.rep_d, rpp_d = representatives(cand, self.trange(), prop, thres, cover)
		if store_res:
			self.rpp_d = rpp_d
		return self.rep_d


	@state_req(P_SCORES, EE_SCORE)
	def repTag(self, store_res=False, prop=0.25, thres=0.50, cover=1):
		"""
		Generates a set of representative tags for this producer. The results
		are stored in self.rep_t and also returned, and the result parameters
		are stored in self.rpp_t (if <store_res> is True).

		This method takes the same parameters as representatives()

		@param store_res: whether to store the result parameters
		"""
		g = self.docgr
		cand = dict((g.vs[id][NID], (g.vs[id][NAA], g.successors(id))) for id in self.trange())
		self.rep_t, rpp_t = representatives(cand, self.drange(), prop, thres, cover)
		if store_res:
			self.rpp_t = rpp_t
		return self.rep_t


	@state_req((P_SCORES, P_ARC), E_SCORE)
	def tagScore(self, tag):
		"""
		Returns the score for a tag.

		@param tags: [tag]
		@return: {tag:score}
		"""
		if tag not in self.id_t:
			return 0
		id = self.id_t[tag]
		return (self.docgr.vs[id][NAA] if self.__t_naa is None else self.__t_naa[id]) or 0.0


	def __scoreTags(self, tags):
		"""
		DOCUMENT
		"""
		doc = set(chain(*(self.docgr.successors(v.index) for v in self.docgr.vs.select(self.id_t[tag] for tag in tags))))
		return float(len(doc)) / self.size()


	@state_req(P_SCORES, EE_SCORE)
	@state_next(P_ARC)
	def initProdArcs(self, pmap, has_tags=False):
		"""
		Initialises the doc-tag graph with the given prod<-tag arcs.

		@param pmap: {nsid:ProducerRelation} map
		"""
		g = self.docgr

		if has_tags and NAT not in g.vertex_attributes():
			raise ValueError("<has_tags> specified but initContent was not called with <store_node_attr=False>")

		self.base_s = len(g.vs)
		# add tags in pmap that aren't in self.id_t
		newtags = list(set(chain(*((tag for tag in rel.arcs.iterkeys() if tag not in self.id_t) for rel in pmap.itervalues()))))
		self.id_t.update((tag, self.base_s+i) for i, tag in enumerate(newtags))
		g.add_vertices(len(newtags))
		g.vs[self.base_s:][NID] = newtags

		# init nodes
		self.base_p = len(self.id_d) + len(self.id_t)
		self.id_p = dict((nsid, self.base_p+i) for i, nsid in enumerate(pmap.iterkeys()))

		# add node attributes for tags
		# TODO NORM atm this just adds srange(), maybe we should mix in values
		# for other tags too?
		if has_tags:
			rtags = {}
			for nsid, rel in pmap.iteritems():
				for rtag, tags in rel.tags.iteritems():
					if rtag in rtags:
						rtags[rtag].update(tags)
					else:
						rtags[rtag] = set(tags)
			n_attr = [self.__scoreTags(rtags[tag]) for tag in g.vs.select(self.srange())[NID]]
			g.vs[self.base_s:][NAT] = n_attr

		# init arcs
		arc_s, arc_t, edges, e_attr = edge_array(len(self.id_t), 'd')
		for i, (nsid, rel) in enumerate(pmap.iteritems()):
			pid = self.base_p+i
			for tag, attr in rel.arcs.iteritems():
				arc_s.append(self.id_t[tag])
				arc_t.append(pid)
				e_attr.append(attr)

		# add all to graph
		g.add_vertices(len(pmap))
		g.vs[self.base_p:][NID] = list(pmap.iterkeys())
		g.vs[self.base_p:][NAA] = [rel.attr for rel in pmap.itervalues()]
		eend = len(g.es)
		g.add_edges(edges)
		g.es[eend:][AAT] = e_attr


	@state_req(P_ARC, E_NOTARC)
	def createTGraph(self, totalsize, pgdb, display=False, node_attr={
	  "style": ("filled", "filled"),
	  "fillcolor":("firebrick1", "limegreen"),
	  "shape":("ellipse","doublecircle"),
	}):
		"""
		Creates a graph representing this producer as a tgraph.

		@param totalsize: total number of documents in the entire world
		@param pgdb: an open database of {prid:Producer} (for tgraphs)
		@param display: whether to generate for display (adds attributes to
		       pretty up the graph)
		@param node_attr: {attr:(tag,prod)} node attributes for graphviz; each
		       attribute should be mapped to a (tag,prod) pair that holds the
		       attribute value for the respective type of node; this only has
		       an effect if <display> is True
		       an effect if <display> is True
		"""

		# estimate total size from producer's own perspective
		# the formula is pulled out of my ass but should give passable results
		# - neighbours are not independent => total lower than this
		# - neighbours are not entire network => total higher than this
		total = union_ind(chain([self.size()], (pgdb[self.docgr.vs[pid]["id"]].size() for pid in self.prange())), totalsize)
		# print "producer %s (%s): total size of network estimated to be %s (actual %s)" % (self.nsid, self.size(), total, totalsize)

		gg = graph_copy(self.docgr)
		del gg.vs[NAA]
		gg["base_t"] = 0
		gg["base_g"] = self.base_p - self.base_t

		# node-attrs for prange
		gg.vs[self.base_p:][NAT] = [pgdb[gg.vs[pid][NID]].size()/float(total) for pid in self.prange()]

		# infer arcs between tags
		mem = [filter(lambda id: id in self.drange(), gg.successors(tid)) for tid in self.trange()]
		edges, arc_a = infer_arcs(mem, total)

		gg.delete_vertices(self.drange())
		gg.add_edges(edges)
		#assert gg.es[-len(edges):][AAT] == [None] * len(edges)
		gg.es[-len(edges):][AAT] = arc_a

		if display:
			gg.vs["label"] = gg.vs[NID]
			del gg.vs[NID]
			for attr, val in node_attr.iteritems():
				gg.vs[attr] = [val[0] for i in self.drange()] + [val[1] for i in self.trange()] + [val[2] for i in self.prange()]

		return gg


	@state_req(P_ARC, E_NOTARC)
	def createIndex(self, display=False, node_attr={
	  "style": ("filled", "filled", "filled"),
	  "fillcolor":("deepskyblue", "firebrick1", "limegreen"),
	  "shape":("box", "ellipse","doublecircle"),
	}):
		"""
		Creates a graph representing this producer as an index.

		@param display: whether to generate for display (adds attributes to
		       pretty up the graph)
		@param node_attr: {attr:(doc,tag,prod)} node attributes for graphviz;
		       each attribute should be mapped to a (doc,tag,prod) triple that
		       holds the attribute value for the respective type of node; this
		       only has an effect if <display> is True
		"""
		gg = graph_copy(self.docgr)
		del gg.vs[NAA]
		gg["base_d"] = self.base_d
		gg["base_t"] = self.base_t
		gg["base_h"] = self.base_p

		if display:
			gg.vs["label"] = gg.vs[NID]
			del gg.vs[NID]
			for attr, val in node_attr.iteritems():
				gg.vs[attr] = [val[0] for i in self.drange()] + [val[1] for i in self.trange()] + [val[2] for i in self.prange()]

		return gg



class ProducerRelation(namedtuple('ProducerRelation', 'attr arcs tags')):

	__slots__ = ()
	def __new__(cls, attr, arcs, tags=None):
		"""
		@param attr: score of Producer (see Producer doc for details)
		@param arcs: a map of {rtag:attr}
		@param tags: a map of {rtag:tags} with the same key as <arcs>, or None
		"""
		return super(ProducerRelation, cls).__new__(cls, attr, arcs, tags)



class TagInfo(namedtuple('TagInfo', 'tag docs rtag prod worldsize')):

	RANK = ["rtag", "prod"]
	SCORE = ["precision", "recall", "intersect", "f1_score"]

	__slots__ = ()
	def __new__(cls, tag, docs=None, rtag=None, prod=None, worldsize=None):
		"""
		TODO NORM should support more advanced techniques that take into
		account the weights given to each tag-doc relationship.

		@param docs: a list of documents for this tag
		@param rtag: a map of {rtag:(ritx,rtotal)} defining related tags
		@param prod: a map of {nsid:(ritx,rtotal)} defining related producers
		@param worldsize: total number of documents in the world, if known
		"""
		return super(TagInfo, cls).__new__(cls, tag, docs or [], rtag or {}, prod or {}, worldsize)

	def __str__(self):
		return '"%s": %s documents%s, %s related tags, %s related producers' % (self.tag, len(self.docs),
		  " (out of %s total)" % self.worldsize if self.worldsize else "", len(self.rtag), len(self.prod))

	def __repr__(self):
		return "TagInfo(%r, %r, %r, %r, %r)" % (self.tag, self.docs, self.rtag, self.prod, self.worldsize)

	def rel_size(self):
		return float(len(self.docs))/self.worldsize

	def build_node(self):
		out = dict((k, s) for k, (s, t) in self.by_precision(self.rtag))
		return Node(self.tag, out, self.rel_size())

	def rank_matches(self, rel, score):
		if rel not in TagInfo.RANK:
			raise ValueError("bad rel (%s); must be one of %s" % (rel, ", ".join(TagInfo.RANK)))
		if score not in TagInfo.SCORE:
			raise ValueError("bad score (%s); must be one of %s" % (score, ", ".join(TagInfo.SCORE)))
		return getattr(self, "by_"+score)(getattr(self, rel))

	def by_precision(self, map):
		"""
		@return: a sorted list of {rtag:(precision,rtotal)} for related tags,
		         where precision = intersect/rtag.total
		"""
		return list(sort_v(((k, (float(ix)/tot, tot)) for k, (ix, tot) in map.iteritems()), reverse=True))

	def by_recall(self, map):
		"""
		@return: a sorted list of {rtag:(recall,rtotal)}, for related tags,
		         where recall = intersect/tag.total
		"""
		return list(sort_v(((k, (float(ix)/len(self.docs), tot)) for k, (ix, tot) in map.iteritems()), reverse=True))

	def by_intersect(self, map):
		"""
		@return: a sorted list of {rtag:intersection}, for related tags.
		"""
		return list(sort_v(((k, ix) for k, (ix, tot) in map.iteritems()), reverse=True))

	def by_f1_score(self, map):
		"""
		@return: a sorted list of {rtag:f1score}, for related tags.
		"""
		return list(sort_v(((k, f1_score(len(self.docs), tot, ix)) for k, (ix, tot) in map.iteritems()), reverse=True))

	def f1_score(self, results):
		"""
		@return: F1 score between the tag's documents and the results set.
		"""
		return f1_score(set(self.docs), set(results))



class IDInfo(namedtuple('IDInfo', 'id soc tgr idx rel_h')):

	__slots__ = ()

	def nb_size(self):
		"""
		@return: number of nodes up to one hop away in the indexes graph
		"""
		return len(self.rel_h)



StepReport = namedtuple('StepReport', 'scheme results')

class QueryReport(namedtuple('QueryReport', 'id tag steps')):

	__slots__ = ()

	@classmethod
	def from_chapters(cls, chapters):
		intro = chapters.pop(0)
		id, tag = re.search(r"\[(.*?):(.*?)\]", intro[0][0]).groups()

		steps = {}
		for head, addr, res in chapters:
			step = int(re.search(r"step (\d+):", head[0]).group(1))

			nodes = []
			for line in addr[2:]:
				index, tag, score, pred, path = line.split(' | ')

				index = len(nodes) if index[0] == '#' else int(index)
				tag = tag.strip()
				score = None if score.strip() == "null" else float(score)
				pred = [int(n) for n in pred.split(',')] if pred.strip() else []
				#path = [int(n) for n in path.split('->')] # ignore for now, we don't need it

				if index != len(nodes):
					raise ValueError()

				nodes.append((tag, score, pred))

			tags, scores, preds = zip(*nodes) if nodes else ([], [], [])
			edges = list(chain(*([(pred, i) for pred in pp] for i, pp in enumerate(preds))))

			res = [str(doc) for doc in literal_eval(res[0].split(':')[1].strip())]
			g_addr = Graph(len(nodes), edges, directed=True, vertex_attrs={NID: tags, NAA: scores})

			steps[step] = StepReport(g_addr, res)

		return QueryReport(id, tag, steps)



class AddrSchemeEval(namedtuple('AddrSchemeEval', 'subj local world')):

	__slots__ = ()

	def score_local(self):
		"""
		Return the jaccard index (intsx/union) of the arcs of the subject and
		local schemes.
		"""
		a = set(self.local.get_edgelist())
		b = set(self.subj.get_edgelist())
		div = len(a|b)
		return float(len(a&b))/div if div != 0 else 0

	def score_world(self):
		"""
		Return the jaccard index (intsx/union) of the arcs of the subject and
		world schemes.
		"""
		a = set(self.local.get_edgelist())
		b = set(self.subj.get_edgelist())
		div = len(a|b)
		return float(len(a&b))/div if div != 0 else 0


