#!/bin/sh
exec ant -e run "-Dtest.skip=true" "-Drun.args=$*"
