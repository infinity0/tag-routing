#!/bin/sh
CLASSPATH="dist/tags.jar:/usr/share/java/commons-cli.jar"
for i in lib/*.jar; do CLASSPATH="$CLASSPATH:$i"; done
export CLASSPATH
exec java -Xincgc $JOPT tags.Tags "$@"
