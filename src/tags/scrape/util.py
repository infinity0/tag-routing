# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys
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

