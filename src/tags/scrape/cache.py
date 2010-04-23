# Released under GPLv2 or later. See http://www.gnu.org/ for details.

from collections import MutableMapping

DEFAULT_CAP = 16

class _Node(object):

	__slots__ = ("key", "val", "prev", "next")

	def __init__(self, key, val):
		object.__init__(self)
		self.key = key
		self.val = val
		self.prev = None
		self.next = None

class LRUDict(MutableMapping):

	__slots__ = ("__dict", "__head", "__tail", "capacity")

	def __init__(self, items=(), capacity=DEFAULT_CAP):
		# Check arguments
		if capacity <= 0:
			raise ValueError, size
		self.__dict = {}
		self.__head = _Node(None, None)
		self.__tail = _Node(None, None)
		self.__head.next = self.__tail
		self.__tail.prev = self.__head
		self.capacity = capacity
		for k, v in items:
			self[k] = v

	def __remove_node(self, node):
		node.prev.next = node.next
		node.next.prev = node.prev

	def __insert_head(self, node):
		nextelement = self.__head.next
		# connect with __head.next
		node.next = nextelement
		nextelement.prev = node
		# connect with __head
		node.prev = self.__head
		self.__head.next = node

	def __repr__(self):
		return "LRUDict(%s, %r)" % (str(self.items()), self.capacity)

	def __str__(self):
		return repr(dict(self))

	def __len__(self):
		return len(self.__dict)

	def __iter__(self):
		return iter(self.__dict)

	def __contains__(self, key):
		return key in self.__dict

	def __getitem__(self, key):
		node = self.__dict[key]
		self.__remove_node(node)
		self.__insert_head(node)
		return node.val

	def __setitem__(self, key, val):
		if key in self.__dict:
			node = self.__dict[key]
			node.val = val
			self.__remove_node(node)
			self.__insert_head(node)
		else:
			node = _Node(key, val)
			self.__insert_head(node)
			self.__dict[key] = node
			if len(self.__dict) > self.capacity:
				tail = self.__tail.prev
				self.__remove_node(tail)
				self.handle_clear(tail, True)

	def __delitem__(self, key):
		node = self.__dict[key]
		self.__remove_node(node)
		self.handle_clear(node, False)

	def handle_clear(self, node, drop=True):
		del self.__dict[node.key]
		node.key = None
		node.val = None
		node.prev = None
		node.next = None


class StatLRUDict(LRUDict):

	__slots__ = ("c_hit", "c_miss", "c_drop")

	def __init__(self, items=(), capacity=DEFAULT_CAP):
		LRUDict.__init__(self, items, capacity)
		self.c_hit = 0
		self.c_miss = 0
		self.c_drop = 0

	def __getitem__(self, key):
		try:
			val = LRUDict.__getitem__(self, key)
			self.c_hit += 1
			return val
		except KeyError:
			self.c_miss += 1
			raise
		finally:
			#print self.report_stats()
			pass

	def handle_clear(self, key, val):
		LRUDict.handle_clear(self, key, val)
		self.c_drop += 1
		#print self.report_stats()

	def report_stats(self):
		return {"hit": self.c_hit, "miss": self.c_miss, "drop": self.c_drop}



from shelve import Shelf
def shelve_attach_cache(db, capacity, cachetype=LRUDict):
	if not isinstance(db, Shelf):
		raise TypeError()

	if db.writeback:
		raise ValueError()

	db.writeback = True
	db.cache = cachetype(capacity=capacity)
	osync = db.sync
	def sync(self):
		self.writeback=False
		osync()
	db.sync = sync.__get__(db.__class__, db)

