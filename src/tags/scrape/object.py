# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from igraph import Graph, IN, OUT
from functools import partial
from itertools import chain

from tags.scrape.util import StateError, intern_force, sort_v, union_ind, edge_array, infer_arcs, iterconverge, representatives


NID = "id" # label for node id in graphs
NAT = "height" # label for node attribute in graphs
AAT = "weight" # label for arc attribute in graphs


class NodeSample():


	def __init__(self, f=None):
		self._node = {}
		self.idmap = {}
		self.order = None
		self.extra = None
		if not f:
			self.graph = None
		else:
			self.graph = Graph.Read(f)


	def __contains__(self, id):
		return id in self._node


	def __len__(self):
		return len(self._node)


	def add_nodes(self, nodes):
		if self.graph is not None:
			raise StateError("sample already finalised")

		self._node = dict((node.id, node) for node in nodes)
		return self


	def add_node(self, node):
		"""
		@param node: Node to add
		"""
		if self.graph is not None:
			raise StateError("sample already finalised")

		if node.id in self._node:
			raise KeyError

		self._node[node.id] = node
		return self


	def build(self, keep, bipartite=False, node_attr=None, inverse=False):
		"""
		Build a graph out of the sample.

		Definitions: an "explicit" node is one which has been explicited added
		to the sample with add_node(). A "dangling" node is one referenced by
		the out-dicts of other nodes, but not explicitly added to the sample
		(and implicitly has no out-dict of its own).

		The total number of explicit nodes will be stored in self.order, and
		if kept (see <keep>), the total number of dangling nodes will be stored
		in self.extra.

		@param keep: whether to keep or discard dangling nodes; dangling nodes
		       will have vertex ids greater than explicit nodes
		@param bipartite: if this is True, then raise an error if an explicit
		       node points to another explicit node
		@param node_attr: if keep is True, this sets the attribute for dangling
		       nodes. if this is callable or a dictionary, the node id is input
		       to it and the output is used as the value; otherwise it is
		       treated as a constant value for all nodes
		@param inverse: Whether to invert edge directions
		@return: The built graph
		"""
		if self.graph is not None: return self.graph

		if not keep:
			pass
		elif callable(node_attr):
			attr_cb = node_attr
		elif hasattr(node_attr, "__getitem__"):
			attr_cb = lambda id: node_attr[id]
		else:
			attr_cb = lambda id: node_attr

		# init nodes
		v_id, v_attr, idmap = zip(*((id, node.attr, (id, i)) for i, (id, node) in enumerate(self._node.iteritems()))) if self._node else ([], [], [])
		v_id, v_attr = list(v_id), list(v_attr)
		self.idmap = dict(idmap)
		self.order = j = len(self._node)

		# init edges
		arc_s, arc_t, edges, e_attr = edge_array(len(self._node), 'd', inverse)

		for (i, node) in enumerate(self._node.itervalues()):
			for (dst, attr) in node.out.iteritems():
				if dst in self._node:
					if keep and bipartite:
						raise ValueError("non-bipartite graph: %s - %s" % (node.id, dst))
					arc_s.append(i)
					arc_t.append(self.idmap[dst])
					e_attr.append(attr)

				elif keep:
					if dst in self.idmap:
						x = self.idmap[dst]
					else:
						x = self.idmap[dst] = j
						v_id.append(dst)
						v_attr.append(attr_cb(dst))
						j += 1

					arc_s.append(i)
					arc_t.append(x)
					e_attr.append(attr)
				else:
					pass

		assert j == len(self.idmap) == len(v_id) == len(v_attr)
		if keep:
			self.extra = j - self.order
		del self._node

		# igraph can't handle utf-8 output, see launchpad bug #545663
		for (i, id) in enumerate(v_id):
			if type(id) == unicode:
				v_id[i] = id.encode("utf-8")

		# prepare attributes
		va = {NID: v_id}
		if any(a is not None for a in v_attr):
			va[NAT] = v_attr

		# build graph
		self.graph = Graph(n=j, directed=True, vertex_attrs=va)
		self.graph.add_edges(edges)
		self.graph.es[AAT] = e_attr
		return self.graph


class Node():


	def __init__(self, id, out, attr=None):
		"""
		@param id: Unique identifier
		@param out: { id: arc-attribute } of out-neighbours
		@param attr: Node-attribute
		"""
		self.id = id
		self.out = out
		self.attr = attr


class Producer():


	def __init__(self, nsid):
		"""
		Creates a new producer
		"""
		self.nsid = nsid
		self.prodgr = None
		self.vid = None

		self.docgr = None
		self.id_d = None # {doc:vid}
		self.id_t = None # {tag:vid}
		self.id_p = None # {pid:vid}

		self.base_d = None # first doc id
		self.base_t = None # first tag id
		self.base_s = None # first newtag id
		self.base_p = None # first producer id

		self.rep_d = None # representative docs
		self.rep_t = None # representative tags

		# OPT HIGH move all of these into a "graph" instance
		self.arc_t = None


	def attachGraph(self, prodgr=None, vid=None):
		"""
		attached to the given graphs. The graphs must
		contain the given vertex id.

		@param prodgr: producer graph
		@param vid: vertex id of node in the content graph
		"""
		if prodgr.vs[vid][NID] != self.nsid:
			raise ValueError("nsid in graph doesn't match")

		self.prodgr = prodgr
		self.vid = vid


	def initContent(self, dsrc, ptdb):
		"""
		Initialises the doc-tag graph from the given document set and the given
		doc-tag database.

		@param dsrc: A source of documents. This can either be a collection of
		       documents, or be a map associating the producer's id to such a
		       collection.
		@param ptdb: An open database of {doc:[tag]}
		"""
		if self.docgr is not None:
			raise StateError("initContent already called")

		dset = dsrc[self.nsid] if self.nsid in dsrc else dsrc

		def outdict(doc):
			tags = ptdb[doc]
			attr = len(tags)**-0.5 if tags else 0 # formula pulled out of my ass
			# however it follows the principle of "more tags there are, less important each one is"
			# TODO HIGH decide whether this is actually a good idea, or just use a constant 1
			return dict((tag, attr) for tag in tags)

		ss = NodeSample().add_nodes(Node(doc, outdict(doc)) for doc in dset)
		self.docgr = ss.build(True, bipartite=True, inverse=True)

		id_d = {} # {doc:id}
		id_t = {} # {tag:id}
		for id, vid in ss.idmap.iteritems():
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


	def prange(self):
		return xrange(self.base_p, self.base_p + len(self.id_p))


	def size(self):
		"""
		Returns the number of documents.
		"""
		return self.base_t


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


	def inferScores(self, init=0.5):
		"""
		Infer scores for docs and tags.

		DOCUMENT more detail
		"""
		if self.docgr is None:
			raise StateError("initContent not called yet")

		if self.base_p is not None:
			raise StateError("initProdArcs already called")

		g = self.docgr

		# doc-tag weight is P(t|d)
		# tags and docs are considered as bags of meaning
		# a producer = union of tags = union of docs

		# Infer P(t|this) = union_ind(P(t|d) over all d attached to t)
		#
		# Justification: roughly, 1 match out of any is satisfactory. We have
		# no further information so assume P(t|d) independent over d.
		sc_t = list(union_ind(*g.es.select(g.adjacent(id, OUT))[AAT]) for id in self.trange())

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
				return union_ind(*(k * e[AAT] / sc_t[e.source-self.base_t] for e in eseq))
			except IndexError:
				print list(e.source for e in eseq)
				raise
		for id in self.drange():
			sc_d.append(iterconverge(partial(scoreDoc, id), (0,1), init, eps=2**-32, maxsteps=0x40))

		self.docgr.vs["score"] = sc_d + sc_t


	def debugScores(self, fp=sys.stderr):
		"""
		Tests the scoreNodes() algorithm for different values of <init>.

		@param fp: the stream on which to output the table
		"""
		res = {}
		for i in [0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]:
			self.inferScores(i)
			res[i] = self.docgr.vs.select()["score"][:]
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


	def representatives(self, doc=False, tag=False):
		"""
		Generates a set of representative ([doc], [tag]) for this producer.

		If neither parameter is given, assumes both are True.

		DOCUMENT more detail
		@param doc: whether to generate representative docs (default False)
		@param tag: whether to generate representative tags (default False)
		"""
		if self.docgr is None:
			raise StateError("initContent not called yet")

		if "score" not in self.docgr.vs.attribute_names():
			raise StateError("inferScores not called yet")

		if self.base_p is not None:
			raise StateError("initProdArcs already called")

		if not doc and not tag:
			doc, tag = True, True

		g = self.docgr

		if tag:
			cand_t = dict((g.vs[id][NID], (g.vs[id]["score"], g.successors(id))) for id in self.trange())
			items_t = self.drange()
			rep_t = representatives(cand_t, items_t, prop=0.25, thres=0.5, cover=1)
			self.rep_t = rep_t[0]

		if doc:
			cand_d = dict((g.vs[id][NID], (g.vs[id]["score"], g.predecessors(id))) for id in self.drange())
			items_d = self.trange()
			rep_d = representatives(cand_d, items_d, prop=0.25, thres=0.96, cover=1)
			self.rep_d = rep_d[0]


	def tagScores(self, tags):
		"""
		Returns the scores for a bunch of tags.

		@param tags: [tag]
		@return: {tag:score}
		"""
		if self.docgr is None:
			raise StateError("initContent not called yet")

		if "score" not in self.docgr.vs.attribute_names():
			raise StateError("inferScores not called yet")

		return dict((tag, self.docgr.vs[self.id_t[tag]]["score"]) for tag in tags)


	def initProdArcs(self, prodmap):
		"""
		Initialises the doc-tag graph with the given prod<-tag arcs

		@param prodmap: {nsid:{tag:attr}} map
		"""
		if self.docgr is None:
			raise StateError("initContent not called yet")

		if self.base_p is not None:
			raise StateError("initProdArcs already called")

		# add tags in prodmap that aren't in self.id_t
		self.base_s = len(self.docgr.vs)
		newtags = list(set(chain(*((tag for tag in tmap.iterkeys() if tag not in self.id_t) for tmap in prodmap.itervalues()))))
		self.id_t.update((tag, self.base_s+i) for i, tag in enumerate(newtags))
		self.docgr.add_vertices(len(newtags))
		self.docgr.vs[self.base_s:][NAT] = newtags

		# init nodes
		self.base_p = len(self.id_d) + len(self.id_t)
		self.id_p = dict((nsid, self.base_p+i) for i, nsid in enumerate(prodmap.iterkeys()))

		# init arcs
		arc_s, arc_t, edges, e_attr = edge_array(len(self.id_t), 'd')
		for i, (nsid, tmap) in enumerate(prodmap.iteritems()):
			pid = self.base_p+i
			for tag, attr in tmap.iteritems():
				arc_s.append(self.id_t[tag])
				arc_t.append(pid)
				e_attr.append(attr)

		# add all to graph
		self.docgr.add_vertices(len(prodmap))
		self.docgr.vs[self.base_p:][NID] = list(prodmap.iterkeys())
		eend = len(self.docgr.es)
		self.docgr.add_edges(edges)
		self.docgr.es[eend:][AAT] = e_attr


	def createTGraph(self, net_g):
		raise NotImplementedError()


	def createIndex(self, net_h):
		raise NotImplementedError()

