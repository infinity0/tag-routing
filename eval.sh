#!/bin/sh

PYTHON="python2.6"

case $1 in
-p | --profile )
	PYTHON="/usr/lib/$PYTHON/cProfile.py -o profile.log -s time"
	echo "wrapper: using $PYTHON"
	shift
	;;
-d | --debug )
	PYTHON="/usr/lib/$PYTHON/pdb.py"
	echo "wrapper: using $PYTHON"
	shift
	;;
-- )
	shift
	;;
-h | --help )
	cat <<-EOF
	Usage: $0 [OPTION] [EVALUATE ARGS]
	Wrapper script; sets THREAD_DUMP_FILE="dump.log" and executes eval.py using
	either the normal python interpreter, or one of the ones below:

	Options:
	  -p, --profile        use the python profiler
	  -d, --debug          use the python debugger
	  --                   use the standard python interpreter

	----
	EOF
	;;
esac

THREAD_DUMP_FILE="dump.log" exec $PYTHON src/eval.py "$@"

