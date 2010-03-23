# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys

import igraph
from igraph import Graph


class StateError(Exception):
	pass


class IDSample():

	def __init__(self, f=None):
		self.node = {}
		if not f:
			self.graph = None
			self.idmap = {}
		else:
			self.graph = igraph.read(f)

	def __contains__(self, id):
		return id in self.node

	def __len__(self):
		return len(self.node)

	def add_node(self, n):
		"""
		@param n: ID to add
		"""
		if self.graph is not None:
			raise StateError("sample already finalised")

		if n.id in self.node:
			raise KeyError

		self.node[n.id] = n

	def build(self):
		"""
		Finalises the sample by discarding edges to nodes not part of the
		sample, and builds a graph out of it.

		@return: The built graph
		"""
		if self.graph is not None: return self.graph

		edges = []
		v_id = []
		e_wgt = []

		for (i, id) in enumerate(self.node.iterkeys()):
			self.idmap[id] = i
			assert len(v_id) == i
			v_id.append(id)

		for (i, node) in enumerate(self.node.itervalues()):
			for (dst, attr) in node.out.items(): ## make a copy since we want to remove
				if dst in self.node:
					edges.append((i, self.idmap[dst]))
					e_wgt.append(attr)
					pass
				else:
					del node.out[dst]

		self.graph = Graph(n=len(self.node), edges=edges, directed=True, vertex_attrs={"id":v_id}, edge_attrs={"weight":e_wgt})
		return self.graph


class ID():

	def __init__(self, id, out):
		"""
		@param id: Unique identifier
		@param out: { id: weight } of friends
		"""
		self.id = id
		self.out = out


class PhotoSample():
	pass


class Photo():
	pass

