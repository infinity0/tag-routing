#!/bin/bash
# Generate documents from source code

SRC="doc"
BLD="build/doc"
DOC="site"
INC="inc"
RES="res"

case $1 in
-f | --force )
	force="yes"
	shift
	;;
esac


mkdir -p "$DOC"
cp -falT "$SRC/$INC" "$DOC/$INC"
cp -falT "$SRC/$RES" "$DOC/$RES"
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
	if [ -z "$force" -a "$i" -ot "../$OUT" ]; then continue; fi

	# first regexp is a hackaround a markdown deficiency
	# others are self-defined math operators
	sed \
	  -e 's/^  \(\w\)/    \1/g' \
	  -e 's/\\pred/\\mathtt{\\mathrm{pred}}\\,/g' \
	  -e 's/\\succ/\\mathtt{\\mathrm{succ}}\\,/g' \
	  -e 's/\\nbr/\\mathtt{\\mathrm{nbr}}\\,/g' \
	  -e 's/\\dom/\\mathtt{\\mathrm{dom}}\\,/g' \
	  -e 's/\\img/\\mathtt{\\mathrm{img}}\\,/g' \
	  -e 's/\\src/\\mathtt{\\mathrm{src}}\\,/g' \
	  -e 's/\\dst/\\mathtt{\\mathrm{dst}}\\,/g' \
	  -e 's/\\rft/\\mathtt{\\mathrm{rft}}\\,/g' \
	  -e 's/\\row/\\mathtt{\\mathrm{row}}\\,/g' \
	  -e 's/\\col/\\mathtt{\\mathrm{col}}\\,/g' \
	  -e 's/\\com/\\mathtt{\\mathrm{com}}\\,/g' \
	"$i" | pandoc -s --toc --smart -c "$INC/common.css" --latexmathml="$INC/LaTeXMathML.js" \
	  -H "../$SRC/header.html" -B "../$SRC/body_header.html" -A "../$SRC/body_footer.html" > "../$OUT"
	echo "built $OUT"
done

cd ..
rm -rf "$BLD"
