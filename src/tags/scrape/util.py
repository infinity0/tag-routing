# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, traceback, signal, os
from random import random
from math import log, exp
from ast import literal_eval

def choice_dist(dist, total=None):
	"""
	Pick a random key from the map according to the distribution defined by it

	@param dist: Map of keys to their relative weights
	@param total: Sum of all weights. If this is None, it will be calculated
	"""
	if total is None: total = sum(dist.itervalues())

	rand = random() * total
	running = 0.0

	for k, w in dist.iteritems():
		running += w
		if running > rand:
			return k


def count_iter(iter):
	"""
	Returns a frequency histogram for the items in the iterable
	"""
	cmap = {}
	for it in iter:
		cmap[it] = cmap[it]+1 if it in cmap else 1
	return cmap


def geo_mean(a, b):
	"""
	Returns the geometric mean of a, b, ie. sqrt(ab)
	"""
	return (a*b)**0.5


def thread_dump():
	"""
	Generates a thread dump.

	from http://bzimmer.ziclix.com/2008/12/17/python-thread-dumps/
	"""
	code = []
	for threadId, stack in sys._current_frames().items():
		code.append("\n# ThreadID: %s" % threadId)
		for filename, lineno, name, line in traceback.extract_stack(stack):
			code.append('File: "%s", line %d, in %s' % (filename, lineno, name))
			if line:
				code.append("  %s" % (line.strip()))

	return "\n".join(code)


def signal_dump(sig=signal.SIGUSR1, fp=sys.stderr):
	"""
	Registers a signal-handler which thread dumps to the given stream.

	@param sig: Signal to trap, default USR1
	@param fp: File object, default sys.stderr
	"""
	def handle(signum, frame):
		print >>fp, thread_dump()
		fp.flush()

	signal.signal(sig, handle)


def dict_save(d, fp=sys.stdout):
	"""
	Saves a dictionary to disk. Format: "k: v\n"* where k, v are data literals

	Uses repr() to do the hard work.
	"""
	for k, v in d.iteritems():
		fp.write("%r: %r\n" % (k, v))


def dict_load(fp=sys.stdin):
	"""
	Loads a dictionary to disk. Format: "k: v\n"* where k, v are data literals

	Uses ast.literal_eval to do the hard work.
	"""
	def parse_pair(line):
		p = line.split(':', 1)
		return literal_eval(p[0]), literal_eval(p[1].strip())
	return dict(parse_pair(l) for l in fp.readlines())


def intern_force(sss):
	"""
	Interns the given string, or its UTF-8 encoding if it's a unicde string.
	"""
	if type(sss) == str:
		return sss
	elif type(sss) == unicode:
		return sss.encode("utf-8")
	else:
		raise TypeError("%s not unicode or string" % sss)


def sort_v(kvit, reverse=False):
	"""
	Returns a list of (k, v) tuples, sorted by v.
	"""
	return ((k, v) for (v, k) in sorted(((v, k) for k, v in kvit), reverse=reverse))


def union_ind(*args):
	"""
	Returns the union of some probabilities, assuming they are all independent.
	"""
	return 1.0 - reduce(lambda x,y : x*y, (1.0-i for i in args))

