#!/bin/bash
# Generate documents from source code

SRC="src-doc"
BLD="build-doc"
DOC="doc"

mkdir -p "$DOC"
mkdir -p "$BLD"

cd "$SRC"

for i in *.tex; do
	if [ "$i" -ot "../$DOC/${i%.tex}.pdf" ]; then continue; fi
	sed -e 's/\(\s\)"/\1``/g' "$i" > "../$BLD/$i" && pdflatex -output-directory "../$BLD" "$i"
	cp "../$BLD/${i%.tex}.pdf" "../$DOC/${i%.tex}.pdf"
done

for i in *.md.txt; do
	# markdown -x toc -x footnotes "$i" > "../$DOC/${i%.md.txt}.html"
	pandoc -s --toc -c common.css "$i" > "../$DOC/${i%.md.txt}.html"
done

cd ..

rm -rf "$BLD"
