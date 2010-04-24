# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
from multiprocessing.pool import Pool
from multiprocessing.process import Process
from traceback import extract_tb, format_list
from functools import partial
from time import sleep


class ExecutionException(Exception):

	def __init__(self, cause_info=None, extract=False):
		"""
		Wrap an exception that occured in a seperate thread or process.
		See PEP 3134 for inspiration.

		@param cause_info: output of sys.exc_info() when exception is caught.
		@param pickle: whether to extract the traceback data, so that this
		       Exception can be pickled
		"""
		etype, exception, info = cause_info
		if extract: info = extract_tb(info)
		self.__cause_info__ = (etype, exception, info)

	def exc_info(self):
		return self.__cause_info__

	def __str__(self):
		etype, exception, info = self.__cause_info__
		if not type(info) == list: info = extract_tb(info)
		return "Caused by:\n%s%s: %s" % ("".join(format_list(info)), etype.__name__, exception)

	def __reduce__(self):
		# extract=False because we've either already extracted it; or we haven't, in which case pickling will fail anyway
		return (ExecutionException, (self.__cause_info__, False))


class ContextProcess(Process):

	def __init__(self, **kwargs):
		"""
		@param init_target: unbound instance method to call at process start
		@param wrap_target: unbound instance method to wrap worker function
		@param term_target: unbound instance method to call at process finish
		"""
		self._init_target = kwargs.pop("init_target", lambda self: None).__get__(self, Process)
		self._wrap_target = kwargs.pop("wrap_target", lambda self: None).__get__(self, Process)
		self._term_target = kwargs.pop("term_target", lambda self: None).__get__(self, Process)
		Process.__init__(self, **kwargs)

	def __enter__(self):
		return self

	def __exit__(self, type, value, traceback):
		self.close()

	def run(self):
		self._init_target()
		if self._wrap_target:
			self._target = self._wrap_target(self._target)
		try:
			return Process.run(self)
		finally:
			self._term_target()


class ContextPool(Pool):

	def __init__(self, processes=None, initializer=None, initargs=(), process=None):
		"""
		@param process: Process subclass to use
		"""
		if process is not None:
			self.Process = process
		Pool.__init__(self, processes, initializer, initargs)

	def __enter__(self):
		return self

	def __exit__(self, type, value, traceback):
		self.close()



def ctx_process(init_target, wrap_target=None, term_target=None):
	return partial(ContextProcess, init_target=init_target, wrap_target=wrap_target, term_target=term_target)


def ctx_worker(self, worker):
	"""
	A wrapper around multiprocessing.pool.worker() which treats each input work
	function as an unbound instance method. This method is bound to the Process
	instance, then given to multiprocessing.pool.worker() to run.
	"""
	if worker is None: return None
	def ctx_worker(inqueue, outqueue, initializer=None, initargs=()):
		oget = inqueue.get
		def get():
			task = oget()
			if task is None: return None
			job, i, func, args, kwds = task
			return job, i, exec_wrap(func.__get__(self, self.__class__), True), list(args), kwds
		inqueue.get = get #.__get__(inqueue, inqueue.__class__)
		return worker(inqueue, outqueue, initializer, initargs)
	return ctx_worker


def exec_wrap(func, pickle=False):
	"""
	Decorate a function to wrap all exceptions in a ExecutionException.
	"""
	def wrap(*args, **kwargs):
		try:
			func(*args, **kwargs)
		except Exception:
			raise ExecutionException(sys.exc_info(), pickle)
	return wrap


def multiprocessing_patch_nonblocking():
	"""
	THIS IS A HACK, and I don't even know exactly how it works. Sometimes you
	will get OSError when acquiring the replacement locks. But at least ctrl-C
	works... DOCUMENT
	"""
	from multiprocessing.synchronize import Condition, RLock, Lock, Semaphore, BoundedSemaphore, Event
	import threading
	threading._Condition = Condition
	threading._RLock = RLock
	threading.Lock = Lock
	threading._Semaphore = Semaphore
	threading._BoundedSemaphore = BoundedSemaphore
	threading._Event = Event


if __name__ == "__main__":
	multiprocessing_patch_nonblocking()

	def f(self, x):
		for i in xrange(x):
			print "%s %s" % (self.name, i)
			sleep(1)

	def init(self):
		print "init %s" % self.name

	def term(self):
		print "term %s" % self.name

	pool = ContextPool(processes=2, process=ctx_process(init, ctx_worker, term))
	for res in pool.imap_unordered(f, [2, 4]):
		print res

	fn = "test.db"
	def db_init(self):
		import shelve
		self.db = shelve.open(fn)
		print "%s opened by %s " % (fn, self.name)

	def db_term(self):
		self.db.close()
		print "db closed by %s " % (self.name)

	def db_f(self, rng):
		for i in rng:
			print i, ':', self.db[str(i)]
			sleep(1)

	pool = ContextPool(processes=2, process=ctx_process(db_init, ctx_worker, db_term))
	res1 = pool.apply_async(db_f, [(1, 7, 8)])
	res2 = pool.apply_async(db_f, [(9, 5, 1)])

	sleep(4)
	print res1.get()
	print res2.get()

