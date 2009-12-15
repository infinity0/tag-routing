#!/bin/bash
# Generate documents from source code

SRC="src-doc"
BLD="build-doc"
DOC="doc"

mkdir -p "$DOC"
mkdir -p "$BLD"

cd "$SRC"

for i in *.tex; do
	OUT="../$DOC/${i%.tex}.pdf"
	if [ "$i" -ot "$OUT" ]; then continue; fi

	sed -e 's/\(\s\)"/\1``/g' "$i" > "../$BLD/$i" && pdflatex -output-directory "../$BLD" "$i"
	cp "../$BLD/${i%.tex}.pdf" "$OUT"
done

for i in *.md.txt; do
	OUT="../$DOC/${i%.md.txt}.html"
	if [ "$i" -ot "$OUT" ]; then continue; fi

	cat "$i" | \
	sed 's/\\dom/\\mathtt{\\mathrm{dom}}\\,/g' | \
	sed 's/\\img/\\mathtt{\\mathrm{img}}\\,/g' | \
	sed 's/\\src/\\mathtt{\\mathrm{src}}\\,/g' | \
	sed 's/\\dst/\\mathtt{\\mathrm{dst}}\\,/g' | \
	sed 's/\\rft/\\mathtt{\\mathrm{rft}}\\,/g' | \
	pandoc -s --toc --latexmathml=LaTeXMathML.js --smart -c common.css > "$OUT"
done

cd ..

rm -rf "$BLD"
