# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, os, time
from itertools import izip, chain, takewhile


###############################################################################
# Computations
###############################################################################

from math import fabs
FLOAT = sys.float_info


def geo_prog(base, ratio, inverse=False):
	val = base
	if inverse:
		while True:
			yield val
			val /= ratio
	else:
		while True:
			yield val
			val *= ratio


def geo_prog_range(lo, hi, ratio, thru=None):
	"""
	Returns a geometric progression between the given ranges.

	@param lo: low endpoint (inclusive)
	@param hi: high endpoint (inclusive)
	@param ratio: ratio of sequence
	@param through: sequence will pass through this number; defaults to low
	       endpoint.
	"""
	if thru is None:
		thru = lo

	if lo > hi:
		raise ValueError()

	if not (lo <= thru <= hi):
		raise ValueError()

	ratio = float(ratio)
	if -1 < ratio < 1:
		ratio = 1/ratio

	hiseq = list(takewhile(lambda x: lo <= x <= hi, geo_prog(thru, ratio)))
	loseq = list(takewhile(lambda x: lo <= x <= hi, geo_prog(thru, ratio, True)))

	loseq.reverse()
	return loseq + hiseq[1:]


def geo_mean(a, b):
	"""
	Returns the geometric mean of a, b, ie. sqrt(ab)
	"""
	return (a*b)**0.5


def union_ind(it, total=None):
	"""
	Returns the union of some probabilities, assuming they are all independent.
	"""
	if not it:
		return 0
	elif total:
		total = float(total)
		return total*(1.0 - reduce(lambda x,y : x*y, ((total-i)/total for i in it), 1.0))
	else:
		return 1.0 - reduce(lambda x,y : x*y, (1.0-i for i in it), 1.0)


def f1_score(real, test, ix=None):
	"""
	Returns the F1-score of two sets. (see wikipedia:F1_score)

	@param ix: size of intersection; if this is None then <real>, <test> must
	       be sets; otherwise all three parameters should be integers.
	"""
	if ix is None:
		ix = len(real.intersection(test))
		precision = float(ix)/len(test)
		recall = float(ix)/len(real)
	else:
		precision = float(ix)/test
		recall = float(ix)/real
	return float(2*precision*recall)/(precision+recall)


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


def iterconverge(func, range=(-FLOAT.max, FLOAT.max), init=None, eps=FLOAT.epsilon, maxsteps=0x100):
	"""
	Iterates a function until it converges, or raise ValueError if it diverges
	(does not converge within the number of steps).

	@param func: function to iterate
	@param range: a (lo, hi) tuple; raise if the value goes outside this
	@param init: initial value (default (lo+hi)/2*(1+sys.float.epsilon))
	@param eps: if **relative** difference between two successive iterations is
	       smaller than this, stop iterating and return the arithmetic average
	       of the two
	@param maxsteps: maximum number of steps to run; will raise ValueError if
	       value hasn't converge by then (default 0x100)
	"""
	lo, hi = range
	if lo >= hi:
		raise ValueError("lo < hi, but %s >= %s" % (lo, hi))

	lo, hi = float(lo), float(hi)
	if init is None:
		init = (lo+hi)/2*(1.0+FLOAT.epsilon)

	if eps < 0:
		eps = -eps

	if eps < FLOAT.epsilon:
		raise ValueError("not advisable to have eps < sys.float_info.epsilon (= %s)" % FLOAT.epsilon)

	k = init
	for i in xrange(0, maxsteps):
		ok = k
		k = func(ok)
		if not lo <= k <= hi:
			raise ValueError("value went outside %s: %s" % (range, k))
		if fabs(k/ok-1) <= eps if ok != 0 else k <= FLOAT.min:
			return (k+ok)/2

	raise ValueError("did not converge in %s steps: init=%s, range=%s, eps=%s, prev=%s, last=%s" % (maxsteps, init, range, eps, ok, k))


###############################################################################
# Data structures, collections
###############################################################################

from math import log
from random import random
from array import array
from igraph import Graph, IN
from bisect import bisect_left


class azip(object):

	__slots__ = ("args")


	def __init__(self, *args):
		self.args = args

	def __iter__(self):
		return izip(*self.args)

	def __len__(self):
		return len(self.args[0])


class lazydict(dict):

	__slots__ = ()

	def __getitem__(self, key):
		val = dict.__getitem__(self, key)
		if callable(val):
			val = val()
			self[key] = val
		return val

	def eval_all(self):
		for k in self:
			self.__getitem__(k)

	def __reduce_ex__(self, proto):
		# needed so pickle works properly
		self.eval_all()
		return dict.__reduce_ex__(self, proto)



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


def freq(it):
	"""
	Return a frequency histogram for the items in the iterable.
	"""
	cmap = {}
	for i in it:
		cmap[i] = cmap[i]+1 if i in cmap else 1
	return cmap


def split_asc(list, cuts):
	"""
	Split an ascending list of integers by the given integer cut points (which
	must also be ascending).
	"""
	if not list:
		return [[]] * len(cuts)
	ind = [bisect_left(list, cut) for cut in cuts]
	return [list[lo:hi] for lo, hi in izip(ind, ind[1:]+[len(list)])]


def invert(it, fkey, fval=lambda x: x):
	"""
	Invert a sequence by some given key, val metrics.

	@param fkey: callable which retrieves the key
	@param fval: callable which retrieves the values
	"""
	map = {}
	for i in it:
		k = fkey(i)
		if k in map:
			map[k].append(fval(i))
		else:
			map[k] = [fval(i)]
	return map


def invert_seq(seq):
	return invert(enumerate(seq), lambda (i, o): o, lambda (i, o): i)


def invert_map(map):
	return invert(map.iteritems(), lambda (k, v): v, lambda (k, v): k)


def invert_multimap(kvvit, rmap=None):
	"""
	@param kvvit: an iterator of (key,[items])
	@param rmap: map to populate (defaults to a new empty map)
	"""
	if rmap is None:
		rmap = {}
	for k, vs in kvvit:
		for v in vs:
			if v in rmap:
				rmap[v].append(k)
			else:
				rmap[v] = [k]
	return rmap


def sort_v(kvit, reverse=False):
	"""
	Returns a list of (k, v) tuples, sorted by v.

	@param kvit: an iterator of (k, v) tuples
	"""
	return ((k, v) for (v, k) in sorted(((v, k) for k, v in kvit), reverse=reverse))


def edge_array(items, attr=None, inverse=False):
	"""
	Creates efficient c-array containers for holding edges.

	@param items: number of possible items; if this is small enough the array
	       will be a short array rather than a int array
	@param attr: type of attribute (see docs for array module); if this is None
	       then no attribute array will be created
	@param inverse: whether to invert source/target nodes
	@return: (arc_s, arc_t, edges[, attr]) - arc_* are empty arrays, edges is a
	         zip of the two, and attr is an empty array which is only included
	         if <attr> was not None
	"""
	arc_s = array('H') if items <= 65536 else array('i')
	arc_t = array('H') if items <= 65536 else array('i')
	edges = azip(arc_t, arc_s) if inverse else azip(arc_s, arc_t)
	return (arc_s, arc_t, edges) if attr is None else (arc_s, arc_t, edges, array(attr))


def infer_arcs(mem, total, inverse=False, ratio=None):
	"""
	Return a list of directed weighted edges between related sets. A source set
	is "related to" a target set if their intersection is significantly better
	than expected of a random target set of the same size. (Note that this is
	asymmetric.)

	OPT HIGH this could be made a LOT quicker (for large data sets) if we have
	an inverse item:sets map, because then we wouldn't have to iterate across
	all sets looking for a match.

	@param mem: [[item]] - list of item-sets
	@param total: number of possible items; this is assumed to be correct
	@param inverse: whether to return the inverse relationship
	@param ratio: minimum ratio of (subject node):(intersection) at which to
	       infer the existence of arcs; by default this is log(1+total)
	@return: iterable of ((source index, target index), arc weight)
	"""
	if total < 0:
		raise ValueError("total must be positive: %s" % total)

	if not ratio:
		ratio = log(1+total)
	mlen = len(mem)
	arc_s, arc_t, edges, arc_a = edge_array(mlen, 'd', inverse)

	for sid in xrange(0, mlen):
		smem = mem[sid]
		slen = len(smem)

		for tid in xrange(sid+1, mlen):
			tmem = mem[tid]
			tlen = len(tmem)

			imem = set(smem) & set(tmem)
			ilen = len(imem)
			if ilen == 0: continue
			rilen = ratio*ilen

			# keep arc only if intersection is big enough

			#a = ilen/float(slen) # don't use this since (assume) graph is not dense and * faster than /
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


def graph_copy(g):
	return Graph(len(g.vs), g.get_edgelist(), g.is_directed(), dict((attr, g[attr]) for attr in g.attributes()),
	  dict((attr, g.vs[attr]) for attr in g.vertex_attributes()), dict((attr, g.es[attr]) for attr in g.edge_attributes()))


def graph_prune_arcs(graph, vlist):
	"""
	Prunes arcs from the given graph. A node is only allowed to have incoming
	arcs from nodes before them in the given <vlist>.

	OPT HIGH current complexity is O(n) in len(vlist)
	"""
	if len(set(vlist)) != len(vlist):
		raise ValueError("duplicate node ids: %r" % vlist)

	cut = []
	for vid in xrange(0, len(graph.vs)):
		try:
			allowed = set(vlist[0:vlist.index(vid)])
		except ValueError:
			allowed = set()
		cut.extend(eid for eid in graph.adjacent(vid, IN) if graph.es[eid].source not in allowed)
	graph.delete_edges(cut)
	return graph


def undirect_and_simplify(g, combiners={}, count_attr=None, sum_attrs={}):
	"""
	Returns a copy of the give graph with duplicate and directed edges combined
	into single undirected edges. Loops are discarded.

	@param combiners: a map of edge attribute names to combiner functions, that
	       should take a sequence of (source, target, attribute) triples, or
	       (source, target) pairs if the attribute does not exist, and returns
	       an attribute value for the unified edge. edge attributes that are
	       present in the graph, but not present in this map, are discarded
	@param count_attr: whether to create a new attribute with the given name
	       that holds the number of edges present in the subject graph
	@param sum_attrs: a map of attribute names to divisors; this will create
	       combiners which return the sum of existing attribute values, divided
	       by the respective divisor
	"""

	if count_attr:
		combiners[count_attr] = lambda seq: len(seq)

	for attr, div in sum_attrs.iteritems():
		if attr not in g.edge_attributes():
			raise ValueError("%s not an edge attribute" % attr)
		combiners[attr] = lambda seq: sum(tup[2] for tup in seq) / div

	unified = {}
	edges = []
	for i, (vs, vt) in enumerate(g.get_edgelist()):
		if vs == vt:
			continue
		elif (vs, vt) in unified:
			unified[vs, vt].append(i)
		else:
			eid = [i]
			unified[vs, vt] = eid
			unified[vt, vs] = eid
			edges.append((vs, vt))

	e_attr = dict((attr,
	  [comb([(e.source, e.target, e[attr]) for e in g.es.select(unified[vs, vt])]) for (vs, vt) in edges]
	  if attr in g.edge_attributes() else
	  [comb([(e.source, e.target) for e in g.es.select(unified[vs, vt])]) for (vs, vt) in edges]
	) for attr, comb in combiners.iteritems())

	return Graph(len(g.vs), edges, False, dict((attr, g[attr]) for attr in g.attributes()),
	  dict((attr, g.vs[attr]) for attr in g.vertex_attributes()), e_attr)


def representatives(cand, items=None, prop=0, thres=float("inf"), cover=0):
	"""
	Infers a set of representatives from a set of candidates, each of whom has
	a rating, and a set of items for which they are responsible.

	This implementation will give priority to candidates with higher ratings,
	whilst trying to fit the criteria defined by the parameters.

	@param cand: map of {candidate:(rating,items)}
	@param items: all items - if this is given it will expected to be correct;
	       otherwise it will be calculated automatically
	@param thres: select everyone above this rating (or lower)
	@param prop: select this proportion of candidates (or higher)
	@param cover: ensure every item has this many representatives (or higher)
	@return: a 2-tuple (reps, params), where reps are the selected candidates,
	         and params is a dict containing the actual values of the input
	         predicates.
	"""
	if not cand:
		return [], {"thres": thres, "prop": prop, "cover": cover}

	if not 0 <= prop <= 1:
		raise ValueError()

	if cover < 0:
		raise ValueError()

	if items is None:
		items = []
		for cid, (rating, its) in cand.iteritems():
			items.extend(its)

	reps = []
	minl = len(cand) * float(prop)
	covered = dict((i, 0) for i in items)
	left = set(covered.iterkeys()) if cover > 0 else set()

	for cid, (rating, its) in sort_v(cand.iteritems(), reverse=True):
		if len(reps) > minl and rating <= thres and not left:
			break

		reps.append(cid)
		for it in its:
			covered[it] += 1
			if covered[it] == cover:
				left.remove(it)

	return reps, lazydict([("thres", rating), ("prop", lambda: len(reps)/float(len(cand))), ("cover", lambda: min(covered.itervalues()))])


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
TMP_RAM = "/dev/shm" if os.access("/dev/shm", os.W_OK) else None


def repr_call(fname, *args, **kwargs):
	return "%s(%s)" % (fname, ", ".join(chain((repr(arg) for arg in args), ("%s=%r" % (k,v) for k, v in kwargs.iteritems()))))


def read_chapters(fp=sys.stdin):
	chp = []
	sec = []
	cur = []
	for line in fp:
		if line[0] == '\f':
			if cur: sec.append(cur)
			cur = [line[1:]]
			if sec: chp.append(sec)
			sec = []
		elif line[0] == '\n':
			if cur: sec.append(cur)
			cur = []
		else:
			cur.append(line)
	if cur: sec.append(cur)
	if sec: chp.append(sec)
	return chp


def write_histogram(it, header=None, fp=sys.stdout, reverse=False):
	if header:
		print >>fp, "# %s" % header
	for k, v in sorted(freq(it).iteritems(), reverse=reverse):
		print >>fp, k, v


def write_align_column(lines, cols, fp=sys.stdout, sep=' | ', csep='-', xsep='-+-'):
	csz = tuple(max(len(str(line[i])) for line in lines if line is not None) for i in xrange(0, cols))
	fmt = tuple(("%%%ds" % sz) for sz in csz)
	for line in lines:
		if line is None:
			print >>fp, xsep.join(csep*csz[i] for i in xrange(0, cols))
		else:
			print >>fp, sep.join(fmt[i] % line[i] for i in xrange(0, cols))


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
		key, val = p[0].strip(), p[1].strip()
		try: key = literal_eval(key)
		except (ValueError, SyntaxError): pass
		try: val = literal_eval(val)
		except (ValueError, SyntaxError): pass
		return key, val
	return dict(parse_pair(l) for l in fp.readlines())


def db_open(dbf, writeback=False):
	try:
		from dbsqlite import SQLFileShelf
		return SQLFileShelf(dbf, writeback=writeback)
	except:
		try:
			from shelve import BsdDbShelf
			from bsddb import btopen
			return BsdDbShelf(btopen(dbf), writeback=writeback)
		except Exception:
			import shelve
			return shelve.open(dbf, writeback=writeback)


def db_clean(db_i, start=None, fp=sys.stderr):
	rem, tot = 0, 0
	it = db_i.iterkeys()
	if start:
		if start not in db_i:
			raise ValueError("start %s not in db_i" % start)
		for key in it:
			if key == start:
				break
		print >>fp, "starting cleanup from key %s (exclusive)" % start

	for key in it:
		try:
			print >>fp, "                \raccess key %s" % key, # no line break
			val = db_i[key]
		except Exception:
			del db_i[key]
			print >>fp, "                \rremoved key %s" % key
			rem += 1
		tot += 1
	print >>fp, "                \rfinished cleanup; %s/%s keys removed" % (rem, tot)


def db_copy(db_i, db_o, fp=sys.stderr, skip_done=True):
	rem, tot = 0, 0
	for key in db_i.iterkeys():
		if skip_done and key in db_o:
			continue
		try:
			db_o[key] = db_i[key]
			print >>fp, "                \rcopied key %s" % key, # no line break
		except Exception:
			print >>fp, "                \rfailed key %s" % key
			rem += 1
		tot += 1
	print >>fp, "                \rfinished copy; %s/%s keys failed" % (rem, tot)


###############################################################################
# IPC, execution management, threads, signals, etc
###############################################################################

from traceback import format_stack
from functools import partial
from itertools import ifilterfalse

DEFAULT_MAX_THREADS = 36
def max_procs():
	try:
		from multiprocessing import cpu_count
		c = cpu_count()
		return c-2 if c>3 else 2 if c==3 else 1
	except:
		return 1
DEFAULT_MAX_PROCS = max_procs()


def callable_wrap(item):
	"""
	Return a callable of one parameter, from a callable, map, or constant.
	"""
	if callable(item):
		return item
	elif hasattr(item, "__getitem__"):
		return lambda k: item[k]
	else:
		return lambda k: item


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
				callback(message % lazydict({'i':i, 'i1':i+1, 'item':item}.items() + [('it', lambda: repr_s.repr(item))]))
			else:
				callback(i, item)
			current += every
		yield i, item


def exec_unique(items, done, run, post=lambda it, i, res: None, name="exec_unique",
  logcb=lambda line: sys.stderr.write("%s\n", line), logcb_p=None, workers=None,
  init_target=None, term_target=None, assume_unique=False, **kwargs):
	"""
	Executes the given jobs in parallel, excluding jobs that have already been
	done.

	NOTE: this method will load the entire <items> into a set at the start, so
	don't give it an infinite generator or a iteritems() over a huge database.

	@param items: a collection of to-do items
	@param done: a collection of done items (overrides __contains__), or a
	       callable that takes a single <item> parameter
	@param name: name of the batch, used in logging messages
	@param run: job for worker threads; takes (item) parameter
	@param post: job for main thread; takes (item, i, res) parameters
	@param logcb: a logging callback for overall info
	@param logcb_p: a logging callback for progress info (defaults to overall)
	@param workers: if this is 0 or None, the caller's thread is used. if this
	       is >0 or True, threads are used. if this is <0 or False, processes
	       are used. the magnitude determines the number of workers; or if it
	       is a boolean, then the default number for that worker type is used
	@param max_procs: max worker processes to run; this is superseded by
	       <max_threads>; if both are None then the caller's thread is used
	@param assume_unique: whether to assume <item> is unique
	"""
	#print "got here 0"
	if not assume_unique and type(items) != set and type(items) != dict:
		items = set(items)
	#print "got here 1"
	todo = list(ifilterfalse(done, items)) if callable(done) else [it for it in items if it not in done]
	#print "got here 2"
	total = len(todo)
	#print "got here 3"
	kwargs["expected_length"] = total
	logcb("%s: %s submitted, %s accepted" % (name, len(items), total))

	if logcb_p is None: logcb_p = logcb
	if post is None: post = lambda it, i, res: None

	if workers is True:
		workers = DEFAULT_MAX_THREADS
	elif workers is False:
		workers = -DEFAULT_MAX_PROCS
	elif workers is None:
		workers = 0
	workers = int(workers)

	i = -1
	if workers > 0:
		from futures import ThreadPoolExecutor
		with ThreadPoolExecutor(max_threads=max_threads) as x:
			for i, (it, res) in enumerate_cb(x.run_to_results_any(partial(lambda it: (it, run(it)), it) for it in todo),
			  logcb_p, "%s: %%(i1)s/%s %%(it)s" % (name, total), **kwargs):
				post(it, i, res)

	elif workers < 0:
		from contextprocess import ContextPool, ctx_process, ctx_worker
		with ContextPool(processes=-workers, process=ctx_process(init_target, ctx_worker, term_target)) as pool:
			for i, (it, res) in enumerate_cb(pool.imap_unordered(lambda it: (it, run(it)), todo),
			  logcb_p, "%s: %%(i1)s/%s %%(it)s" % (name, total), **kwargs):
				post(it, i, res)

	else:
		for i, (it, res) in enumerate_cb(((it, run(it)) for it in todo),
		  logcb_p, "%s: %%(i1)s/%s %%(it)s" % (name, total), **kwargs):
			post(it, i, res)

	logcb("%s: %s submitted, %s accepted, %s completed" % (name, len(items), total, (i+1)))


def thread_dump(hash=False):
	"""
	Generates a thread dump.

	@param hash: Output a hash of the stack trace.
	"""
	from hashlib import md5
	from binascii import crc32
	code = ["# Thread dump @ %.4f\n" % time.time()]
	if not hash:
		code = ["#"*79+"\n", code[0], "#"*79+"\n", "\n"]
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
	# yes, this is a total hack but that is because python threading is so shit
	# (and so is python-futures) and i don't have time to do anything better
	from Queue import Queue
	from futures._base import ExecutionException
	def run_to_results_any(self, calls):

		res_queue = Queue()
		res_queue.not_empty = LooseCondition(res_queue.mutex)
		res_queue.not_full = LooseCondition(res_queue.mutex)

		def run(call):
			try:
				res = call()
			except Exception, e:
				res = ExecutionException(sys.exc_info())
			res_queue.put(res)
			return res

		def get(res):
			if type(res) == ExecutionException:
				raise res
			else:
				return res

		fs = self.run_to_futures((partial(run, call) for call in calls), return_when=RETURN_IMMEDIATELY)

		yielded = 0
		while yielded < len(fs):
			yield get(res_queue.get())
			yielded += 1
	futures._base.Executor.run_to_results_any = run_to_results_any

