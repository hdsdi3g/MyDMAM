#!/bin/sh

wget --no-check-certificate https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.5.2.deb

mv elasticsearch-1.5.2.deb elasticsearch.deb

# Add dependances with oracle JRE
# Add GUI for setup
# Add set autostart
