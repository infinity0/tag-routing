#!/bin/sh
# Run as root to install dependencies for this package

# core
aptitude install ant git-core junit libcommons-cli-java

# eval
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 80C21FC1
aptitude update
aptitude install python2.6 python-igraph

# doc
aptitude install texlive-latex-base pandoc

# TODO
# python-flickrapi
# python-futures
# python-simplejson
