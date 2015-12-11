#!/bin/sh

set -e

cd /root

if [ -f cassandra.deb ]; then
        rm -f cassandra.deb
fi

wget "http://dl.bintray.com/apache/cassandra/pool/main/c/cassandra/cassandra_1.2.19_all.deb"

dpkg-deb --raw-extract cassandra_1.2.19_all.deb cassandra

rm -f cassandra_1.2.19_all.deb

cat > "cassandra/DEBIAN/control" <<EOF
Package: cassandra
Version: 1.2.19
Architecture: all
Maintainer: Eric Evans <eevans@apache.org>
Installed-Size: 15034
Depends: oracle-java-jre, jsvc (>= 1.0), libcommons-daemon-java (>= 1.0), adduser, libjna-java, python (>= 2.5), python-support (>= 0.90.0)
Recommends: ntp | time-daemon
Section: misc
Priority: extra
Homepage: http://cassandra.apache.org
Description: distributed storage system for structured data
 Cassandra is a distributed (peer-to-peer) system for the management
 and storage of structured data.
EOF

chmod 0755 cassandra/DEBIAN/*
dpkg-deb --build cassandra
rm -rf cassandra

# Add GUI for setup
