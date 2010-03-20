# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys

from pydot import Node, Edge, Graph, Dot


class StateError(Exception):
	pass


class IDSample():

	def __init__(self):
		self.node = {}
		self.complete = False

	def __contains__(self, id):
		return id in self.node

	def __len__(self):
		return len(self.node)

	def add_node(self, n):
		"""
		@param n: ID to add
		"""
		if self.complete:
			raise StateError("sample not finalised")

		if n.id in self.node:
			raise KeyError

		self.node[n.id] = n

	def finalise(self):
		"""
		Finalises the sample by discarding edges to nodes not part of the sample.
		@return: The discarded edges
		"""
		if self.complete: return

		removed = []
		for id in self.node.itervalues():
			for dst in id.out.keys(): ## make a copy since we want to remove
				if dst not in self.node:
					removed.append(Edge(str(id.id), str(dst), label=str(id.out.pop(dst))))

		self.complete = True
		return removed

	def build_graph(self):
		"""
		Outputs the sample in .dot format
		@return: string representation of graph in dot format
		"""
		if not self.complete:
			raise StateError("sample not finalised")

		g = Dot(graph_type='digraph')

		for id in self.node.itervalues():
			g.add_node(Node(str(id.id)))
			for (dst, attr) in id.out.iteritems():
				g.add_edge(Edge(str(id.id), str(dst), label=str(attr)))

		return g


class ID():

	def __init__(self, id, out):
		"""
		@param id: Unique identifier
		@param out: { id: weight } of friends
		"""
		self.id = id
		self.out = out


if __name__ == "__main__":
	ss = IDSample()
	ss.add_node(ID(1, {2:1, 4:2}))
	ss.add_node(ID(2, {3:1, 1:2}))
	ss.add_node(ID(3, {4:1, 2:2}))
	ss.add_node(ID(4, {1:1, 3:2, 5:3, 6:4}))
	#ss.add_node(ID(5, {1:1, 3:2}))
	r = ss.finalise()
	print " ".join([e.to_string() for e in r])
	print ss.build_graph().to_string()
	sys.exit(0)
