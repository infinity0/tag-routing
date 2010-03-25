# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, traceback, signal, os
from random import random
from math import log, exp


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

#from pygments import highlight
#from pygments.lexers import PythonLexer
#from pygments.formatters import HtmlFormatter

#def colour_stack_trace(trace):
#	return highlight(trace, PythonLexer(), HtmlFormatter(
#	  full=False,
#	  # style="native",
#	  noclasses=True,
#	))


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

