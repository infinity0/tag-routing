#!/usr/bin/python
# Released under GPLv2 or later. See http://www.gnu.org/ for details.
"""
Generates StoreGenerator.java
"""

import sys, math
from random import choice, random, sample

TAG = 0
DOC = 1000
TGR = 2000
IDX = 3000
PTB = 8000


def type_match(ty, id):
	return (type(id) == str) if ty == TAG else (type(id) == int and 0 <= id - ty < 1000)


def print_err(s):
	sys.stderr.write("%s\n" % s)


def sort_by_val(map, reverse=False):
	return sorted(map.iteritems(), cmp = lambda x, y: 0 if x[1] == y[1] else 1 if x[1] > y[1] else -1, reverse=reverse)


def breadth(g, s, n):

	def next(g, done, queue):
		k = queue.pop(0)
		if k in done: return
		queue.extend(g[k].keys())
		done.append(k)

	nodes = []
	queue = [s]

	for i in xrange(0, n):
		next(g, nodes, queue)

	return nodes


def biased_list(map):
	list = []
	for k, v in map.iteritems():
		for i in xrange(0, len(v)):
			list.append(k)
	return list


def make_tag_doc_maps(levels, n_doc):

	item_tag = {} # map of tags to documents
	item_doc = {} # map of documents to tags

	# generate tag list and biased levels list to choice() later
	levels_biased = []
	for i, lv in enumerate(levels):
		for t in lv:
			item_tag[t] = []
		for j in xrange(0, i+1):
			levels_biased.append(lv)

	# generate document-tag mappings
	for x in xrange(8, 8+n_doc):
		d = DOC + x
		n = int(40/float(x))+1 # power-law distribution
		tt = set()
		while len(tt) < n:
			lv = choice(levels_biased)
			tt.add(choice(lv))
		item_doc[d] = list(tt)
		for t in tt:
			item_tag[t].append(d)

	return item_tag, item_doc, len(item_tag), len(item_doc)


def pair_probs(item, num):
	"""
	@param item: { key: [item] }
	@param num: number of different possible items
	@return: { (key,key): [item] }, { key: prob }, { (key,key): prob }
	"""
	itx2 = {} # map of tagpairs to documents
	for (k, ss) in item.iteritems():
		for (k2, ss2) in item.iteritems():
			if k == k2: continue
			if (k, k2) not in itx2:
				itx2[(k, k2)] = list(set(ss).intersection(set(ss2)))

	prob = dict([(k, len(dd) / float(num)) for k, dd in item.iteritems()])
	prx2 = dict([(k, len(dd) / float(num)) for k, dd in itx2.iteritems()])

	return itx2, prob, prx2


def net_links(prob, prx2):
	"""
	@param prob: { key: prob }
	@param prx2: { (key,key): prob }
	@return: { key: { out: prob } }
	"""
	net = {}
	for (k, p) in prob.iteritems():
		out = {}
		for (k2, p2) in prob.iteritems():
			if k == k2: continue
			pp = prx2[(k, k2)]
			if pp > p*p2: # better than independent
				out[k2] = pp / p2
		net[k] = out

	return net


def make_tgr(n_tgr, item_tag, net_tgr, n_doc):

	obj_tgr = {} # map of ids to tgraphs

	# generate entries
	rel_tgr = dict([(t, set()) for t in item_tag.iterkeys()]) # map of tags to tgraphs related to them
	seed_tgr = {}
	for x in xrange(8, 8+n_tgr):
		id = TGR + x
		n = int(80/float(x))+1
		seed = choice(net_tgr.keys())
		seed_tgr[id] = seed

		# create tgraph from cluster of global
		tt = set(breadth(net_tgr, seed, n))
		gg = {}
		for t in tt:
			out = net_tgr[t]
			gg[t] = dict([(k, out[k]) for k in tt.intersection(out)])
		obj_tgr[id] = gg

		# this tgraph can be pointed to via the following tags
		for t in breadth(net_tgr, seed, n/3):
			rel_tgr[t].add(id)

	# generate tgraph probabilities
	item_tgr = {} # map of ids to documents with tags mentioned in the tgraph
	prob_tgr = {} # map of ids to tgraph probabilites
	for (id, gg) in obj_tgr.iteritems():
		dd = set()
		for t in gg.iterkeys():
			dd.update(item_tag[t])
		item_tgr[id] = dd
		prob_tgr[id] = len(dd) / float(n_doc)

	# add links to other tgraphs
	for (id, gg) in obj_tgr.iteritems():
		x = id - TGR
		n = int(80/float(x))+1
		seed = seed_tgr[id]
		t_out = breadth(net_tgr, seed, n)[n/3:]

		for i in xrange(0, n):
			t = choice(t_out)
			while len(rel_tgr[t]) == 0:
				t = choice(rel_tgr.keys())

			if t not in gg: gg[t] = {}
			gg2 = choice(list(rel_tgr[t]))
			gg[t][gg2] = len(item_tag[t]) / float(len(item_tgr[gg2]))
			gg[gg2] = {}

	return obj_tgr, prob_tgr


def make_idx(n_idx, item_tag, net_idx, item_doc):

	obj_idx = {} # map of ids to indexes

	# generate entries
	rel_idx = dict([(t, set()) for t in item_tag.iterkeys()]) # map of tags to indexes related to them
	prob_idx = {} # map of ids to {tag:prob}
	seed_idx = {}
	freq_idx = {}
	for x in xrange(64, 64+n_idx):
		id = IDX + x
		n = int(4096/float(x))+1
		seed = choice(net_idx.keys())
		seed_idx[id] = seed

		# n nearest indexes
		list_doc = breadth(net_idx, seed, n)
		freq_tag = {} # map of tag: number of docs with this tag
		hh = {}
		for d in list_doc:
			n_t = len(item_doc[d])
			for t in item_doc[d]:
				if t not in freq_tag: freq_tag[t] = 0
				freq_tag[t] += 1

				if t not in hh: hh[t] = {}
				hh[t][d] = math.sqrt(1/float(n_t))
		obj_idx[id] = hh
		freq_idx[id] = sort_by_val(freq_tag, True)

		# this index can be pointed to via the most related tags
		for (t, f) in freq_idx[id][:len(freq_tag)/2]:
			rel_idx[t].add(id)

		# generate index probabilities
		prob_idx[id] = dict([(k, float(v) / len(list_doc)) for k, v in freq_idx[id]])

	# add links to other indexes
	for (id, hh) in obj_idx.iteritems():
		x = id - IDX
		n = int(1024/float(x))+1
		seed = seed_idx[id]

		# add links to least related tags
		t_out = dict(freq_idx[id][len(freq_idx[id])/2:]).keys()
		for i in xrange(0, n):
			t = choice(t_out)
			while len(rel_idx[t]) == 0:
				t = choice(rel_idx.keys())

			if t not in hh: hh[t] = {}
			hh2 = choice(list(rel_idx[t]))
			hh[t][hh2] = prob_idx[hh2][t]

	return obj_idx


def make_ptb(n_ptb, obj_tgr, obj_idx):

	obj_ptb = {}
	ll_h = biased_list(obj_idx)
	ll_g = biased_list(obj_tgr)

	for x in xrange(16, 16+n_ptb):
		id = PTB + x
		n_h = int(1024/float(x))+1
		n_g = int(256/float(x))+1

		ptb = {}
		for k in sample(ll_h, n_h):
			ptb[k] = random()/2+0.5
		for k in sample(ll_g, n_g):
			ptb[k] = random()/2+0.5
		obj_ptb[id] = ptb

	obj_frn = {}
	ll_f = biased_list(obj_ptb)

	for id in obj_ptb:
		x = id - PTB
		n_f = int(128/float(x))+1

		frn = {}
		for k in sample(ll_f, n_f):
			frn[k] = random()/2+0.5
		obj_frn[id] = frn

	return obj_ptb, obj_frn


def main(prefix, jclass):

	item_tag, item_doc, n_tag, n_doc = make_tag_doc_maps([
		["aaaa"],
		["aabf", "aabg", "aabh", "aabj", "aabk"],
		["aacp", "aacq", "aacr", "aacs", "aact", "aacu", "aacv", "aacw", "aacx", "aacy"]
	], 256)

	itx2_tag, prob_tag, prx2_tag = pair_probs(item_tag, n_doc)
	itx2_doc, prob_doc, prx2_doc = pair_probs(item_doc, n_tag)

	net_tgr = net_links(prob_tag, prx2_tag)
	net_idx = net_links(prob_doc, prx2_doc)

	obj_tgr, prob_tgr = make_tgr(16, item_tag, net_tgr, n_doc)
	obj_idx = make_idx(256, item_tag, net_idx, item_doc)
	obj_ptb, obj_frn = make_ptb(32, obj_tgr, obj_idx)

	plain(prefix, obj_idx, obj_tgr, obj_ptb, obj_frn, prob_tag, prob_tgr, item_doc, item_tag),
	print >>open('%s.java' % jclass, 'w'), javagen(obj_idx, obj_tgr, obj_ptb, obj_frn, prob_tag, prob_tgr, item_doc, item_tag),


def jmethod_def(s):
	return "public static void sctl_gen_%s(FileStoreControl<Long, String, Long, Probability, Probability, Probability, Probability> sctl)" % s

def jmethod_chain(s, n):
	return "		sctl_gen_%s%s(sctl);\n	}\n\n	%s {" % (s, n, jmethod_def("%s%s" % (s, n)))

def jcode_tgr(id, tgr, prob_tag, prob_tgr):
	return "		sctl.map_tgr_node.put(%sL, uDG(bHS().%sbuild(), bHM().%sbuild()));\
\n		sctl.map_tgr.put(%sL, Maps.<String, U2Map<String, Long, Probability>>buildHashMap().%sbuild());\n" % (
		id,
		"".join(["_(\"%s\", p(%s))." % (k, prob_tag[k]) if type_match(TAG, k) else "" for k in tgr.iterkeys()]),
		"".join(["_(%sL, p(%s))." % (k, prob_tgr[k]) if type_match(TGR, k) else "" for k in tgr.iterkeys()]),
		id,
		"" .join(["_(\"%s\", uDG(bHS().%sbuild(), bHM().%sbuild()))." % (t,
			"".join(["_(\"%s\", p(%s))." % (k, v) if type_match(TAG, k) else "" for k, v in out.iteritems()]),
			"".join(["_(%sL, p(%s))." % (k, v) if type_match(TGR, k) else "" for k, v in out.iteritems()]))
			for t, out in tgr.iteritems()]
		)
	)

def jcode_idx(id, idx):
	return "		sctl.map_idx.put(%sL, Maps.<String, U2Map<Long, Long, Probability>>buildHashMap().%sbuild());\n" % (id,
		"" .join(["_(\"%s\", uDH(bHM().%sbuild(), bHM().%sbuild()))." % (t,
			"".join(["_(%sL, p(%s))." % (k, v) if type_match(DOC, k) else "" for k, v in out.iteritems()]),
			"".join(["_(%sL, p(%s))." % (k, v) if type_match(IDX, k) else "" for k, v in out.iteritems()])
		) for t, out in idx.iteritems()]))

def jcode_ptb(id, ptb):
	return "		sctl.map_ptb.put(%sL, new PTable<Long, Probability>(bHM().%sbuild(), bHM().%sbuild()));\n" % (id,
		"".join(["_(%sL, p(%s))." % (k, v) if type_match(TGR, k) else "" for k, v in ptb.iteritems()]),
		"".join(["_(%sL, p(%s))." % (k, v) if type_match(IDX, k) else "" for k, v in ptb.iteritems()]))

def jcode_frn(id, frn):
	return "		sctl.map_frn.put(%sL, bHM().%sbuild());\n" % (id,
		"".join(["_(%sL, p(%s))." % (k, v) for k, v in frn.iteritems()]))

def jcode_doc(id, tag):
	return "		sctl.map_doc.put(%sL, bHS().%sbuild().keySet());\n" % (id,
		"".join(["_(\"%s\", p(0))." % t for t in tag]))

def jcode_tag(id, doc):
	return "		sctl.map_tag.put(\"%s\", bHM().%sbuild().keySet());\n" % (id,
		"".join(["_(%sL, p(0))." % d for d in doc]))


def javagen(obj_idx, obj_tgr, obj_ptb, obj_frn, prob_tag, prob_tgr, item_doc, item_tag):
	return '''// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.proto.*;
import tags.util.*;
import tags.util.Maps.U2Map;
import static tags.util.Probability.p;

import java.util.*;

/**
** This is a machine-generated class for populating {@link FileStoreControl}
** instances with pre-defined randomly-generated data.
*/
final public class StoreGenerator {

	private StoreGenerator() { }

	public static Maps.MapBuilder<Long, Probability> bHM() {
		return Maps.<Long, Probability>buildHashMap();
	}

	public static Maps.MapBuilder<String, Probability> bHS() {
		return Maps.<String, Probability>buildHashMap();
	}

	public static U2Map<Long, Long, Probability> uDH(Map<Long, Probability> m0, Map<Long, Probability> m1) {
		return Maps.uniteDisjoint(m0, m1);
	}

	public static U2Map<String, Long, Probability> uDG(Map<String, Probability> m0, Map<Long, Probability> m1) {
		return Maps.uniteDisjoint(m0, m1);
	}

	%s {
		sctl_gen_idx(sctl);
		sctl_gen_tgr(sctl);
		sctl_gen_ptb(sctl);
		sctl_gen_frn(sctl);
		sctl_gen_doc(sctl);
		sctl_gen_tag(sctl);
	}

	%s {
%s	}

	%s {
%s	}

	%s {
%s	}

	%s {
%s	}

	%s {
%s	}

	%s {
%s	}

}
''' % (
	jmethod_def("all"),
	jmethod_def("tgr"),
	"".join([jcode_tgr(id, tgr, prob_tag, prob_tgr) for (id, tgr) in obj_tgr.iteritems()]),
	jmethod_def("idx"),
	"".join([jcode_idx(id, idx) + jmethod_chain("idx", i/16) if i%16==0 and i/16>0 else jcode_idx(id, idx) for (i, (id, idx)) in enumerate(obj_idx.iteritems())]),
	jmethod_def("ptb"),
	"".join([jcode_ptb(id, ptb) for (id, ptb) in obj_ptb.iteritems()]),
	jmethod_def("frn"),
	"".join([jcode_frn(id, frn) for (id, frn) in obj_frn.iteritems()]),
	jmethod_def("doc"),
	"".join([jcode_doc(id, tag) for (id, tag) in item_doc.iteritems()]),
	jmethod_def("tag"),
	"".join([jcode_tag(id, doc) for (id, doc) in item_tag.iteritems()]),
	)


def plain(name, obj_idx, obj_tgr, obj_ptb, obj_frn, prob_tag, prob_tgr, item_doc, item_tag):
	print >>open('%s.tag.txt' % name, 'w'), "".join(["%s %s\n" % (id, tag) for (id, tag) in sorted(item_doc.iteritems())]),
	print >>open('%s.doc.txt' % name, 'w'), "".join(["%s %s\n" % (id, doc) for (id, doc) in sorted(item_tag.iteritems())]),
	print >>open('%s.tgr.txt' % name, 'w'), "".join([plain_tgr(id, tgr, prob_tag, prob_tgr) for (id, tgr) in sorted(obj_tgr.iteritems())]),
	print >>open('%s.idx.txt' % name, 'w'), "".join([plain_idx(id, idx) for (id, idx) in sorted(obj_idx.iteritems())]),
	print >>open('%s.ptb.txt' % name, 'w'), "".join([plain_ptb(id, ptb) for (id, ptb) in sorted(obj_ptb.iteritems())]),
	print >>open('%s.frn.txt' % name, 'w'), "".join([plain_frn(id, frn) for (id, frn) in sorted(obj_frn.iteritems())]),

def plain_tgr(id, tgr, prob_tag, prob_tgr, pre="    "):
	prob = prob_tag.copy()
	prob.update(prob_tgr)
	return "%s\n%s" % (id,
		"".join([pre + "%s:%.4f { %s}\n" % (k, prob[k], "".join(["%s:%.4f " % (k2, v) for k2, v in sorted(out.iteritems())]))
			for (k, out) in sorted(tgr.iteritems())]))

def plain_idx(id, idx, pre="    "):
	return "%s\n%s" % (id,
		"".join([pre + "%s { %s}\n" % (k, "".join(["%s:%.4f " % (k2, v) for k2, v in sorted(out.iteritems())]))
			for (k, out) in sorted(idx.iteritems())]))

def plain_ptb(id, ptb, pre="    "):
	return "%s\n%s" % (id,
		"".join([pre + "%s %.4f\n" % (k, v)
			for (k, v) in sorted(ptb.iteritems())]))

def plain_frn(id, frn, pre="    "):
	return plain_ptb(id, frn, pre)


if __name__ == "__main__":
	from optparse import OptionParser, OptionGroup, IndentedHelpFormatter
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS]",
	  description = "Generates random test data to a set of files all sharing a common prefix.",
	  formatter = IndentedHelpFormatter(max_help_position=25)
	)

	config.add_option("-p", "--prefix", type="string", metavar="PREFIX",
	  help = "Prefix for data files")
	config.add_option("-j", "--jclass", type="string", metavar="CLASS",
	  help = "Name and path of Java class to create")

	(opts, args) = config.parse_args()

	if not opts.prefix or not opts.jclass:
		config.print_help()
		sys.exit(2)

	while True:
		try:
			main(prefix=opts.prefix, jclass=opts.jclass)
			break
		except IndexError:
			sys.stderr.write("generated a disconnected tag-graph, will try again\n")
