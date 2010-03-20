# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from pydot import Node, Edge, Graph, Dot


class StateError(Exception):
	pass


class Sample():

	def __init__(self):
		self.dangling = set()
		self.node = {}
		self.complete = False

	def add_node(self, n):
		"""
		@param n: ID to add
		"""
		if self.complete:
			raise StateError

		if n.id in self.node:
			raise KeyError

		self.node[n.id] = n
		self.dangling.discard(n.id)

		d = set(n.out.keys())
		d.difference_update(self.node.keys())
		self.dangling.update(d)

	def finalise(self):
		if self.complete: return

		removed = []
		for id in self.node.itervalues():
			for rid in self.dangling:
				if rid in id.out:
					removed.append(Edge(str(id.id), str(rid), attr=str(id.out.pop(rid))))

		self.complete = True
		return removed

	def output_dot(self, fn):
		"""
		Outputs the sample in .dot format
		@param fn: filename to output
		"""
		if not self.complete:
			raise StateError

		g = Dot(graph_type='digraph')

		for id in self.node.itervalues():
			for (dst, attr) in id.out.iteritems():
				g.add_edge(Edge(str(id.id), str(dst), attr=str(attr)))

		print >>open(fn, 'w'), g.to_string()


class ID():

	def __init__(self, id, out):
		"""
		@param id: Unique identifier
		@param out: { id: weight } of friends
		"""
		self.id = id
		self.out = out


def run_tests(argv):
	#return 0
	ss = Sample()
	ss.add_node(ID(1, {2:1, 4:2}))
	ss.add_node(ID(2, {3:1, 1:2}))
	ss.add_node(ID(3, {4:1, 2:2}))
	ss.add_node(ID(4, {1:1, 3:2, 5:3, 6:4}))
	#ss.add_node(ID(5, {1:1, 3:2}))
	r = ss.finalise()
	print " ".join([e.to_string() for e in r])
	ss.output_dot("test.dot")
	return 0

if __name__ == "__main__":
	sys.exit(run_tests(sys.argv))
