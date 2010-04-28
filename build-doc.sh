#!/bin/bash
# Generate documents from source code

SRC="doc"
BLD="build/doc"
DOC="site"
RES="res"

mkdir -p "$DOC"
cp -alT "$SRC/$RES" "$DOC/$RES"
mkdir -p "$BLD"

cd "$SRC"

for i in *.tex; do
	OUT="$DOC/${i%.tex}.pdf"
	if [ "$i" -ot "../$OUT" ]; then continue; fi

	sed -e 's/\(\s\)"/\1``/g' "$i" > "../$BLD/$i" && pdflatex -output-directory "../$BLD" "$i"
	cp "../$BLD/${i%.tex}.pdf" "../$OUT"
	echo "built $OUT"
done

for i in *.md.txt; do
	OUT="$DOC/${i%.md.txt}.html"
	if [ "$i" -ot "../$OUT" ]; then continue; fi

	sed \
	  -e 's/\\dom/\\mathtt{\\mathrm{dom}}\\,/g' \
	  -e 's/\\img/\\mathtt{\\mathrm{img}}\\,/g' \
	  -e 's/\\src/\\mathtt{\\mathrm{src}}\\,/g' \
	  -e 's/\\dst/\\mathtt{\\mathrm{dst}}\\,/g' \
	  -e 's/\\rft/\\mathtt{\\mathrm{rft}}\\,/g' \
	  -e 's/\\row/\\mathtt{\\mathrm{row}}\\,/g' \
	  -e 's/\\col/\\mathtt{\\mathrm{col}}\\,/g' \
	  -e 's/\\com/\\mathtt{\\mathrm{com}}\\,/g' \
	"$i" | pandoc -s --toc --smart -c "$RES/common.css" -H "../$SRC/header.html" --latexmathml="$RES/LaTeXMathML.js" > "../$OUT"
	echo "built $OUT"
done

cd ..
rm -rf "$BLD"
