#!/bin/sh

PYTHON="python2.6"

case $1 in
-p | --profile )
	PYTHON="/usr/lib/$PYTHON/cProfile.py -o profile.log -s time"
	shift
	;;
-d | --debug )
	PYTHON="/usr/lib/$PYTHON/pdb.py"
	shift
	;;
-h | --help )
	cat <<-EOF
	Usage: $0 [OPTION] [SCRAPER ARGS]
	Wrapper script; sets THREAD_DUMP_FILE="dump.log" and executes scrape.py using
	either the normal python interpreter, or one of the ones below:

	Options:
	  -p, --profile        use the python profiler
	  -d, --debug          use the python debugger

	----
	EOF
esac

THREAD_DUMP_FILE="dump.log" exec $PYTHON src/scrape.py "$@"

