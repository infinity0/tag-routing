% A framework for decentralised semantic routing
% Ximin Luo



## Build-depends

### Main

ant

python2.6

git

java libs:
:	commons-cli
:	junit (3.8)

### Docs

bliki-doclet
:	http://github.com/infinity0/bliki-doclet
:	Make sure the `.jar` exists at `lib/bliki-doclet.jar`

pandoc
:	http://johnmacfarlane.net/pandoc/
:	Debian: install `pandoc`

pdflatex
:	Debian: install `texlive-latex-base`

GNU coreutils
:	Debian: install `coreutils`

### Evaluation

JUNG
:	http://jung.sourceforge.net/
:	Make sure these jars are in lib/: collections-generic colt jung-algorithms
	jung-api jung-graph-impl jung-io wstx-asl

JSON.simple
:	http://code.google.com/p/json-simple/
:	Put json_simple-1.1.jar into lib/

igraph
:	http://igraph.sourceforge.net/
:	Debian: install `python-igraph` https://launchpad.net/~igraph/+archive/ppa

FlickrAPI
:	http://stuvel.eu/flickrapi/
:	Debian: install `python-flickrapi` (>= 1.4.2) or put flickrapi/ into src/

python-futures
:	http://code.google.com/p/pythonfutures/source/list
:	Get it from the SVN repo (r67) and put trunk/python2/futures into src/
	(resulting in src/futures)


## Generating data

1. Make sure you have a flickr API key.
2. Pick a directory to store scrape data
3. Then, run `src/scrape.py -d $BASEDIR` through each of the rounds in order.
4. Run `src/postgen -d $BASEDIR`
5. Test it out

### Format

2.4.2 Declaring GraphML-Attributes
:	http://graphml.graphdrawing.org/primer/graphml-primer.html#AttributesDefinition">

Node, Edge and Graph Attributes
:	http://www.graphviz.org/doc/info/attrs.html


## Searching

- TODO




