# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys

import igraph
from igraph import Graph

from tags.scrape.util import sort_v


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

		edges = []
		v_id = []
		v_attr = []
		e_attr = []

		for (i, node) in enumerate(self._node.itervalues()):
			self._idmap[node.id] = i
			assert len(v_id) == i
			v_id.append(node.id)
			v_attr.append(node.attr)

		j = len(self._node)

		for (i, node) in enumerate(self._node.itervalues()):
			for (dst, attr) in node.out.iteritems():
				if dst in self._node:
					edges.append((i, self._idmap[dst]))
					e_attr.append(attr)
				elif keep:
					if dst in self._idmap:
						x = self._idmap[dst]
					else:
						x = self._idmap[dst] = j
						v_id.append(dst)
						v_attr.append(node_attr)
						j += 1

					edges.append((i, x))
					e_attr.append(attr)
				else:
					pass

		assert j == len(self._idmap) == len(v_id) == len(v_attr)
		del self._node

		va = {"id": v_id}
		ea = {"weight": e_attr}

		for (i, id) in enumerate(v_id):
			if type(id) == unicode:
				v_id[i] = id.encode("utf-8")

		if any(a is not None for a in v_attr):
			va["height"] = v_attr

		self.graph = Graph(n=j, edges=edges, directed=True, vertex_attrs=va, edge_attrs=ea)
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

	def __init__(self, id, dset=set(), arc_s={}, arc_d={}):
		self.id = id # own identity
		self.dset = dset # own document set
		self.arc_s = arc_s # social links
		self.arc_d = arc_d # content links


	def createTGraph(self, net_g):
		raise NotImplemented()


	def createIndex(self, net_h):
		raise NotImplemented()


	def invertMap(self, ptdb):
		"""
		Given a photo:[tag] database, generate an inverse index and a cover set
		for this producer.

		@return: ({tag:{photo:attr}}, [tag])
		"""
		inv = {} # {tag:{photo:attr}}

		for d in self.dset:
			ts = ptdb[d]
			attr = len(ts)**-0.5 # formula pulled out of my ass
			# however it follows the principle of "more tags there are, less important each one is"
			# TODO HIGH decide whether this is actually a good idea, or just use a constant 1
			for t in ts:
				if t not in inv:
					inv[t] = {d:attr}
				else:
					inv[t][d] = attr

		left = set(self.dset)
		cover = []
		for t, a in sort_v(((t, sum(dm.itervalues())) for t, dm in inv.iteritems()), reverse=True):
			left.difference_update(inv[t])
			cover.append(t)
			if len(left) == 0: break

		return inv, cover

