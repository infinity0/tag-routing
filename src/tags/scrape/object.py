# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from array import array

import igraph
from igraph import Graph

from tags.scrape.util import intern_force, sort_v, union_ind, edge_array, infer_arcs


class StateError(RuntimeError):
	pass


class NodeSample():

	def __init__(self, f=None):
		self._node = {}
		self._idmap = {}
		if not f:
			self.graph = None
		else:
			self.graph = igraph.read(f)

	def __contains__(self, id):
		return id in self._node

	def __len__(self):
		return len(self._node)

	def add_node(self, node):
		"""
		@param node: Node to add
		"""
		if self.graph is not None:
			raise StateError("sample already finalised")

		if node.__class__ != Node:
			print node
			raise TypeError

		if node.id in self._node:
			raise KeyError

		self._node[node.id] = node

	def add_nodes(self, nodes):
		i = 0
		for node in nodes:
			self.add_node(node)
			i += 1
		return i

	def build(self, keep, node_attr=None):
		"""
		Build a graph out of the sample

		@param keep: Whether to keep or discard dangling nodes (referenced by
		       other nodes, but not explicitly added to this sample).
		@param node_attr: If keep is True, this will be set as the attribute
		       for the dangling nodes.
		@return: The built graph
		"""
		if self.graph is not None: return self.graph

		arc_s, arc_t, edges = edge_array(len(self._node))
		v_id = []
		v_attr = []
		e_attr = array('d')

		for (i, node) in enumerate(self._node.itervalues()):
			self._idmap[node.id] = i
			assert len(v_id) == i
			v_id.append(node.id)
			v_attr.append(node.attr)

		j = len(self._node)

		for (i, node) in enumerate(self._node.itervalues()):
			for (dst, attr) in node.out.iteritems():
				if dst in self._node:
					arc_s.append(i)
					arc_t.append(self._idmap[dst])
					e_attr.append(attr)
				elif keep:
					if dst in self._idmap:
						x = self._idmap[dst]
					else:
						x = self._idmap[dst] = j
						v_id.append(dst)
						v_attr.append(node_attr)
						j += 1

					arc_s.append(i)
					arc_t.append(x)
					e_attr.append(attr)
				else:
					pass

		assert j == len(self._idmap) == len(v_id) == len(v_attr)
		del self._node

		va = {"id": v_id}

		for (i, id) in enumerate(v_id):
			if type(id) == unicode:
				v_id[i] = id.encode("utf-8")

		if any(a is not None for a in v_attr):
			va["height"] = v_attr

		self.graph = Graph(n=j, directed=True, vertex_attrs=va)
		self.graph.add_edges(edges)
		self.graph.es["weight"] = e_attr
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


	def __init__(self, dset=[], gs=None, gd=None, vid=None):
		"""
		Creates a new producer attached to the given graphs. The graphs must
		contain the given vertex id.

		@param dset: own document set
		@param gs: social graph
		@param gd: content graph
		@param vid: vertex id of node
		"""
		self.gs = gs
		self.gd = gd
		self.vid = vid
		self.nsid = gs.vs[vid]["id"]
		self.dset = [intern_force(d) for d in dset]
		# OPT HIGH move all of these into a "graph" instance
		self.inv = None # {tag:[doc]}
		self.tag = None # {tag:attr}
		self.cover = None
		self.arcs = None


	def createTGraph(self, net_g):
		raise NotImplementedError()


	def createIndex(self, net_h):
		raise NotImplementedError()


	def invertMap(self, ptdb):
		"""
		Given a photo:[tag] database, generate an inverse index and a cover set
		for this producer.

		@return: proportion of tags it took to cover all documents, or 0 if
		         there are no tags. (lower is better)
		"""
		inv = {}

		for d in self.dset:
			ts = ptdb[d]
			if not ts: continue
			attr = len(ts)**-0.5 # formula pulled out of my ass
			# however it follows the principle of "more tags there are, less important each one is"
			# TODO HIGH decide whether this is actually a good idea, or just use a constant 1
			for t in ts:
				t = intern_force(t)
				if t not in inv:
					inv[t] = {d:attr}
				else:
					inv[t][d] = attr

		# use union_ind to weigh tags - roughly, 1 match out of any is satisfactory
		tag = dict((t, union_ind(*dm.itervalues())) for t, dm in inv.iteritems())
		#tag = dict((t, sum(dm.itervalues())) for t, dm in inv.iteritems())

		left = set(self.dset)
		cover = []
		for t, a in sort_v(tag.iteritems(), reverse=True):
			left.difference_update(inv[t])
			cover.append(t)
			if len(left) == 0: break

		self.inv = inv
		self.tag = tag
		self.cover = cover
		return len(self.cover) / float(len(self.inv)) if len(self.inv) > 0 else 0


	def inferTagArcs(self):
		key = self.inv.keys()
		val = self.inv.values()
		items = len(self.dset)

		edges, arc_a = infer_arcs(val, items, inverse=True)

		arcs = {}
		for i, (s, t) in enumerate(edges):
			sk = key[s]
			tk = key[t]
			if sk not in arcs:
				arcs[sk] = {tk:arc_a[i]}
			else:
				arcs[sk][tk] = arc_a[i]

		self.arcs = arcs
		return len(edges), len(self.inv)**2

