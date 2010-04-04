# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, os, time
from itertools import izip, chain


###############################################################################
# Computations
###############################################################################


def geo_mean(a, b):
	"""
	Returns the geometric mean of a, b, ie. sqrt(ab)
	"""
	return (a*b)**0.5


def union_ind(*args):
	"""
	Returns the union of some probabilities, assuming they are all independent.
	"""
	return 1.0 - reduce(lambda x,y : x*y, (1.0-i for i in args))


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


###############################################################################
# Data structures, collections
###############################################################################

from random import random
from array import array


def choice_dist(dist, total=None):
	"""
	Pick a random key from the map according to the distribution defined by it

	@param dist: Map of keys to their relative weights
	@param total: Sum of all weights. If this is None, it will be calculated
	"""
	if total is None: total = sum(dist.itervalues())

	rand = random() * total
	running = 0.0

	# OPT NORM use some combo of binary search and caching the cumulative list
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


def sort_v(kvit, reverse=False):
	"""
	Returns a list of (k, v) tuples, sorted by v.
	"""
	return ((k, v) for (v, k) in sorted(((v, k) for k, v in kvit), reverse=reverse))


class azip():

	def __init__(self, *args):
		self.args = args

	def __iter__(self):
		return izip(*self.args)

	def __len__(self):
		return len(self.args[0])


def edge_array(items, inverse=False):
	"""
	Creates efficient c-array containers for holding edges.

	@param items: number of possible items; if this is small enough the array
	       will be a short array rather than a int array
	@param inverse: whether to invert source/target nodes
	@return: (arc_s, arc_t, edges) - arc_* are empty arrays, and edges is a zip
	         of the two, that can be iterated through once.
	"""
	arc_s = array('H') if items <= 65536 else array('i')
	arc_t = array('H') if items <= 65536 else array('i')
	edges = azip(arc_t, arc_s) if inverse else azip(arc_s, arc_t)
	return arc_s, arc_t, edges


def infer_arcs(mem, items, inverse=False):
	"""
	Return a list of directed weighted edges between related sets. A source set
	is "related to" a target set if their intersection is significantly better
	than expected of a random target set of the same size. (Note that this
	relationship is asymmetric.)

	@param mem: [[item]] - list of item-sets
	@param items: number of possible items; this is assumed to be correct
	@param inverse: whether to return the inverse relationship
	@return: iterable of ((source index, target index), arc weight)
	"""
	r = items**0.5 if items > 0 else 0 # FIXME LOW hack, if items == 0 then mlen must be 0
	mlen = len(mem)
	arc_s, arc_t, edges = edge_array(mlen, inverse)
	arc_a = array('d')

	for sid in xrange(0, mlen):
		smem = mem[sid]
		slen = len(smem)

		for tid in xrange(sid+1, mlen):
			tmem = mem[tid]
			tlen = len(tmem)

			imem = set(smem) & set(tmem)
			ilen = len(imem)
			if ilen == 0: continue
			rilen = r*ilen

			# keep arc only if (significantly) better than independent intersections

			#a = ilen/float(slen) # don't use this since graph is not dense and * faster than /
			#if a > r:
			if rilen > slen:
				arc_s.append(sid)
				arc_t.append(tid)
				arc_a.append(ilen/float(slen))
				#arc_a.append(a)

			#a = ilen/float(tlen)
			#if a > r:
			if rilen > tlen:
				arc_s.append(tid)
				arc_t.append(sid)
				arc_a.append(ilen/float(tlen))
				#arc_a.append(a)

	return edges, arc_a


###############################################################################
# IO, data formats, etc
###############################################################################

from ast import literal_eval
from repr import Repr

repr_s = Repr()
repr_s.maxlevel = 3
repr_s.maxdict = 2
repr_s.maxlist = 2
repr_s.maxtuple = 2
repr_s.maxset = 2
repr_s.maxfrozenset = 2
repr_s.maxdeque = 2
repr_s.maxarray = 2
repr_s.maxlong = 20
repr_s.maxstring = 20
repr_s.maxother = 15


def repr_call(fname, *args, **kwargs):
	return "%s(%s)" % (fname, ", ".join(chain((repr(arg) for arg in args), ("%s=%r" % (k,v) for k, v in kwargs.iteritems()))))


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


###############################################################################
# IPC, execution management, threads, signals, etc
###############################################################################

from traceback import format_stack
from functools import partial


def enumerate_cb(iterable, callback, message=None, steps=0x100, every=None, expected_length=None):
	"""
	Enumerates an iterable, with a callback for every n iterations (default 1).

	@param iterable: the iterable to enumerate
	@param callback: the callback. this should take a single string input if
	       <message> is non-empty, otherwise a (i, item) tuple
	@param steps: the number of callbacks to make, distributed evenly amongst
	       the items (<iterable> must also override __len__ in this case, or
	       else <expected_length> must be set); however, if <every> is set, all
	       of this will be ignored
	@param every: interval at which to call the callback
	@param message: callback message; a string which will have %(i), %(i1),
	       %(it), %(item), respectively substituted with the count, the count
	       plus 1, a short representation of the item, the item.
	"""
	if every is None:
		if hasattr(iterable, "__len__"):
			every = len(iterable)/float(steps)
		else:
			every = float(expected_length)/steps

	elif every <= 0:
		raise ValueError("[every] must be greater than 0")

	current = 0.0
	for i, item in enumerate(iterable):
		if i >= current:
			if message:
				callback(message % {'i':i, 'i1':i+1, 'it':repr_s.repr(item), 'item':item})
			else:
				callback(i, item)
			current += every
		yield i, item


def thread_dump(hash=False):
	"""
	Generates a thread dump.

	@param hash: Output a hash of the stack trace.
	"""
	from hashlib import md5
	from binascii import crc32
	code = ["# Thread dump @ %.4f\n" % time.time()]
	for thid, stack in sorted(sys._current_frames().items()):
		lines = format_stack(stack)
		if hash:
			code.append("%08x | %s | %s\n" % (thid,
			    md5("".join(lines)).hexdigest()[8:24],
			    #"%08x" % (crc32("".join(lines))&0xffffffff),
			    lines[-1].split("\n", 1)[0].strip()))
		else:
			code.append("# thread %08x:\n" % thid)
			code.extend(lines)
			code.append("\n")
	return "".join(code)


try:
	THREAD_DUMP_FILE = open(os.environ["THREAD_DUMP_FILE"], 'a')
except (KeyError, IOError):
	THREAD_DUMP_FILE = sys.stderr

def signal_dump(fp=THREAD_DUMP_FILE):
	"""
	Registers a signal-handler which thread dumps to the given stream. If no
	stream is given, the environment variable THREAD_DUMP_FILE is used, or
	stderr if that is not given.

	@param sig: Signal to trap, default USR1
	@param fp: File object, default sys.stderr
	"""
	from signal import signal, SIGUSR1, SIGUSR2
	def handle(signum, frame):
		print >>fp, thread_dump(True if signum == SIGUSR2 else False)
		fp.flush()

	print >>fp, "\n## Thread dump session started at %.4f by %s (pid %s)\n" % (time.time(), sys.argv[0], os.getpid())
	signal(SIGUSR1, handle)
	signal(SIGUSR2, handle)


def futures_patch_nonblocking(response_interval=0.25, verbose=False):
	"""
	Prevents certain python-futures features from blocking signal handling.
	ONLY USE IF NO THREADS (except main) EVER WRITE TO DISK, ETC.

	This must be called *before* any module from python-futures is imported.

	Stuff patched so far:
	- Future.result() blocking process signals
	- thread._python_exit being registered as a handler, which blocks exit

	Additional stuff
	- Executor.run_to_results_any() yields results in any order, depending on
	  which completes first.

	Note that this entire suite of patches is a HACK HACK HACK and depends on
	implementation details of python-futures, Queue and threading. Tested on
	python-2.6 and python-futures svn@r67.

	@param response_interval: patch said features so they don't block threads
	       longer than this
	@param verbose: be verbose
	"""
	# prevent futures.thread from setting _python_exit
	# this is safe only if threads aren't writing to files
	import atexit
	old = atexit.register
	def lol(i):
		from futures.thread import _python_exit
		if i == _python_exit:
			if verbose: print >>sys.stderr, "futures_patch_nonblocking: DENIED atexit.register(%r)" % i
		else:
			if verbose: print >>sys.stderr, "futures_patch_nonblocking: allowed atexit.register(%r)" % i
			old(i)
		#import traceback
		#traceback.print_stack()
	atexit.register = lol
	from futures import ThreadPoolExecutor
	atexit.register = old

	import futures._base
	from futures._base import (CANCELLED, CANCELLED_AND_NOTIFIED, FINISHED,
	    RETURN_IMMEDIATELY, FIRST_COMPLETED, CancelledError, TimeoutError, Future)

	# override _Condition to prevent it from waiting too long.
	# THIS IS ONLY SAFE if the code that uses it is coded correctly (ie. inside
	# a loop, as per the while !cond { convar.wait(); } idiom. in particular,
	# Future.result does not do this, which is why we have to override the
	# entire fucking function below
	from threading import _Condition
	class LooseCondition(_Condition):
		def wait(self, timeout=None):
			if timeout is None or timeout > response_interval:
				timeout = response_interval
			_Condition.wait(self, timeout)

	# rewrite Future.result() to periodically wakeup when waiting for a result
	def result(self, timeout=None):
		self._condition.acquire()
		try:
			if self._state in [CANCELLED, CANCELLED_AND_NOTIFIED]:
				raise CancelledError()
			elif self._state == FINISHED:
				return Future._Future__get_result(self)

			if timeout is None:
				while self._state != FINISHED:
					self._condition.wait(response_interval)
			else:
				self._condition.wait(timeout)

			if self._state in [CANCELLED, CANCELLED_AND_NOTIFIED]:
				raise CancelledError()
			elif self._state == FINISHED:
				return Future._Future__get_result(self)
			else:
				raise TimeoutError()
		finally:
			self._condition.release()
	futures._base.Future.result = result

	# add a method to Executor to return first result completed for any input,
	# rather than wait for the next input's result
	from Queue import Queue
	def run_to_results_any(self, calls):

		res_queue = Queue()
		res_queue.not_empty = LooseCondition(res_queue.mutex)
		res_queue.not_full = LooseCondition(res_queue.mutex)

		def run(call):
			res = call()
			res_queue.put(res)
			return res

		fs = self.run_to_futures((partial(run, call) for call in calls), return_when=RETURN_IMMEDIATELY)

		yielded = 0
		while yielded < len(fs):
			yield res_queue.get()
			yielded += 1
	futures._base.Executor.run_to_results_any = run_to_results_any

