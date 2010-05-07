# Released under GPLv2 or later. See http://www.gnu.org/ for details.

from tags.eval.util import callable_wrap


class StateError(RuntimeError):

	def __init__(self, msg=None, cur=None):
		RuntimeError.__init__(self, msg)
		self.msg = msg # str
		self.cur = cur # state



def state_req(req, msg=None):
	"""
	Decorates an object method to throw StateError if its state is not listed
	in the given valid states.

	The object is assumed to have a "state" attribute; it is up to the caller
	to ensure that this holds.

	@param req: either the required state or an iterable of possible states
	@param msg: either an error message, or a callable or map that returns one
	"""
	msg_cb = callable_wrap(msg)
	def decorate(method):
		if hasattr(req, "__iter__"):
			def wrap(self, *args, **kwargs):
				if self.state not in req:
					raise StateError(msg_cb(self.state), self.state)
				return method(self, *args, **kwargs)
		else:
			def wrap(self, *args, **kwargs):
				if self.state != req:
					raise StateError(msg_cb(self.state), self.state)
				return method(self, *args, **kwargs)
		return wrap
	return decorate


def state_not(inv, msg=None):
	"""
	Decorates an object method to throw StateError if its state is listed in
	the given invalid states

	The object is assumed to have a "state" attribute; it is up to the caller
	to ensure that this holds.

	@param inv: either the invalid state or an iterable of invalid states
	@param msg: either an error message, or a callable or map that returns one
	"""
	msg_cb = callable_wrap(msg)
	def decorate(method):
		if hasattr(inv, "__iter__"):
			def wrap(self, *args, **kwargs):
				if self.state in inv:
					raise StateError(msg_cb(self.state), self.state)
				return method(self, *args, **kwargs)
		else:
			def wrap(self, *args, **kwargs):
				if self.state == inv:
					raise StateError(msg_cb(self.state), self.state)
				return method(self, *args, **kwargs)
		return wrap
	return decorate


def state_next(next, fail=None):
	"""
	Decorates an object method to transition into another state if it completes
	successfully, or optionally a fail state (which must not be None) if it
	throws an exception.

	The object is assumed to have a "state" attribute; it is up to the caller
	to ensure that this holds.
	"""
	def decorate(method):
		def wrap(self, *args, **kwargs):
			try:
				r = method(self, *args, **kwargs)
				self.state = next
				return r
			except:
				if fail is not None:
					self.state = fail
				raise
		return wrap
	return decorate

