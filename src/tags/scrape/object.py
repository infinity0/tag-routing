# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from array import array
from igraph import Graph, IN

from tags.scrape.util import intern_force, sort_v, union_ind, edge_array, infer_arcs


NID = "id" # label for node id in graphs
NAT = "height" # label for node attribute in graphs
AAT = "weight" # label for arc attribute in graphs


class StateError(RuntimeError):
	pass


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


	def build(self, keep, node_attr=None):
		"""
		Build a graph out of the sample.

		Definitions: an "explicit" node is one which has been explicited added
		to the sample with add_node(). A "dangling" node is one referenced by
		the out-dicts of other nodes, but not explicitly added to the sample
		(and implicitly has no out-dict of its own).

		The total number of explicit nodes will be stored in self.order, and
		if kept (see <keep>), the total number of dangling nodes will be stored
		in self.extra.

		@param keep: Whether to keep or discard dangling nodes. Dangling nodes
		       will have vertex ids greater than explicit nodes.
		@param node_attr: If keep is True, this sets the attribute for dangling
		       nodes. If this is callable, the return value is used. If this
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

		v_id = []
		v_attr = []
		arc_s, arc_t, edges = edge_array(len(self._node))
		e_attr = array('d')

		# init nodes
		for (i, node) in enumerate(self._node.itervalues()):
			self.idmap[node.id] = i
			assert len(v_id) == i
			v_id.append(node.id)
			v_attr.append(node.attr)

		self.order = j = len(self._node)
		# init edges
		for (i, node) in enumerate(self._node.itervalues()):
			for (dst, attr) in node.out.iteritems():
				if dst in self._node:
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
		if keep: self.extra = j - self.order
		del self._node

		# igraph can't handle utf-8 output, see launchpad bug #545663
		for (i, id) in enumerate(v_id):
			if type(id) == unicode:
				v_id[i] = id.encode("utf-8")

		va = {NID: v_id}
		if any(a is not None for a in v_attr):
			va[NAT] = v_attr

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


	def __init__(self, prodgr=None, vid=None):
		"""
		Creates a new producer attached to the given graphs. The graphs must
		contain the given vertex id.

		@param dset: own document set
		@param prodgr: producer graph
		@param vid: vertex id of node in the content graph
		"""
		self.prodgr = prodgr
		self.vid = vid
		self.nsid = prodgr.vs[vid][NID] if prodgr else None

		self.docgr = None
		self.id_d = None
		self.it_t = None

		# OPT HIGH move all of these into a "graph" instance
		self.tag = None # {tag:attr}
		self.cover = None
		self.arcs = None


	def initContent(self, dset, ptdb):
		"""
		Initialises the doc-tag graph from the given document set and the given
		doc-tag database.

		@param dset: A set of documents
		@param ptdb: An open database of {doc:[tag]}
		"""
		def outdict(doc):
			tags = ptdb[doc]
			attr = len(tags)**-0.5 if tags else 0 # formula pulled out of my ass
			# however it follows the principle of "more tags there are, less important each one is"
			# TODO HIGH decide whether this is actually a good idea, or just use a constant 1
			return dict((tag, attr) for tag in tags)

		ss = NodeSample().add_nodes(Node(doc, outdict(doc)) for doc in dset)
		self.docgr = ss.build(True)

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


	def makeCover(self):
		"""
		Returns a set of tags which covers all documents.

		This implementation uses a greedy heuristic which attempts to minimise
		the size of the set (finding the absolute minimum is NP-complete).

		FIXME HIGH this causes problems when eg. every photo has one tag. fix
		the algorithm to make the cover be x deep (atm it is 1 deep), where x
		is the 1/2 mean tags per doc, or something...

		@return: proportion of tags it took to cover all documents, or 0 if
		         there are no tags. (lower is better)
		"""
		g = self.docgr
		tidbase = len(self.id_d)
		# use union_ind to weigh tags
		# justification: roughly, 1 match out of any is satisfactory
		tscore = list(union_ind(*g.es.select(g.adjacent(id, IN))[AAT]) for id in xrange(tidbase, tidbase+len(self.id_t)))

		left = set(xrange(0, len(self.id_d)))
		cover = []
		for id, attr in sort_v(enumerate(tscore), reverse=True):
			tid = tidbase + id
			left.difference_update(g.predecessors(tid))
			cover.append(tid)
			if len(left) == 0: break

		self.tscore = tscore # TODO NORM store in graph as doubles
		self.cover = cover
		return len(self.cover) / float(len(self.id_t)) if len(self.id_t) > 0 else 0


	def coverTags(self):
		raise NotImplementedError()


	def createTGraph(self, net_g):
		raise NotImplementedError()


	def createIndex(self, net_h):
		raise NotImplementedError()

