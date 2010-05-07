#!/usr/bin/python
# Released under GPLv2 or later. See http://www.gnu.org/ for details.

import sys, logging, os
from time import time, ctime
logging.basicConfig(format="%(asctime)s.%(msecs)03d | %(levelno)02d | %(message)s", datefmt="%s")

from tags.eval import Evaluation
from tags.eval.util import dict_load, signal_dump

NAME = "eval.py"
VERSION = 0.01


def first_nonwhite(line):
	i = 0
	for c in line:
		if c != ' ':
			break
		i += 1
	return i


def fmt_pydoc(sss):
	lines = sss.split("\n")

	while not lines[0]:
		lines.pop(0)

	if len(lines) > 0:
		indent = first_nonwhite(lines[0].replace('\t', '    '))

		for (i, line) in enumerate(lines):
			sline = line.replace('\t', '    ')
			iii = first_nonwhite(sline)
			lines[i] = sline[iii:] if iii < indent else sline[indent:]

	return "\n".join(lines)


def main(round, *args, **kwargs):

	signal_dump()
	LOG = logging.getLogger("")
	LOG.setLevel(kwargs.pop("v"))

	with Evaluation(**kwargs) as scr:

		f = getattr(scr, "round_%s" % round)

		if len(args) > 0 and args[0].lower() == "help":
			rinfo = Evaluation.rounds[round]
			print >>sys.stderr, fmt_pydoc(f.__doc__)
			print >>sys.stderr, "These rounds must already have been executed: %s" % ", ".join(rinfo.dep)
			print >>sys.stderr, "These files will be written to: %s" % ", ".join(os.path.join(kwargs["base"], ext) for ext in rinfo.out)
			return 0

		else:
			t = time()
			LOG.info("%s at %s" % (Evaluation.rounds[round].desc, ctime()))
			ret = f(*args)
			LOG.info('Round "%s" completed in %.4fs' % (round, time()-t))
			return ret


if __name__ == "__main__":

	from optparse import OptionParser
	config = OptionParser(
	  usage = "Usage: %prog [OPTIONS] [ROUND] [ARGS|help]",
	  description = "Crawl for and generate a data sample, and evaluate query results against it. ROUND is one of: %s." % ", ".join(Evaluation.roundlist),
	  version = VERSION,
	)

	exstr = " (See ./flickr.key.example for an example)" if os.path.exists("./flickr.key.example") else ""

	config.add_option("-d", "--base", type="string", metavar="DIR",
	  help = "Base output directory")
	config.add_option("-k", "--key", type="string", metavar="FILE", default="flickr.key",
	  help = 'File with Flickr API authentication details. Each line must read "key: value", '
	         'and the file must define keys "api_key" and "secret", and optionally "token".%s' % exstr)
	config.add_option("-i", "--interact", action="store_true", dest="interact",
	  help = "Go into interactive mode after performing a round, to examine the objects created")
	config.add_option("-c", "--cache", type="int", metavar="SIZE", default=0,
	  help = "Cache size for database objects (only sometimes used, eg. pgdb, phdb in round 'generate')")
	config.add_option("-v", type="int", metavar="LEVEL", default=100,
	  help = 'Verbosity level (1-50; 1 most verbose, 20 standard)')
	config.add_option("-p", "--pretty", action="store_true", dest="pretty",
	  help = "Pretty print (only for some outputs)")

	(opts, args) = config.parse_args()

	kwargs = opts.__dict__
	with open(opts.key) as fp:
		try:
			keys = dict_load(fp)
			for k in ["api_key", "secret"]:
				if k in keys:
					kwargs[k] = keys[k]
				else:
					raise ValueError('key "%s" not found' % k)
			for k in ["token"]:
				if k in keys:
					kwargs[k] = keys[k]
		except ValueError, e:
			print >>sys.stderr, "bad keyfile format in %s: %r" % (opts.key, e)
			sys.exit(1)

	del opts.key

	if len(args) < 1:
		config.print_help()
		sys.exit(2)
	else:
		sys.exit(main(*args, **kwargs))
