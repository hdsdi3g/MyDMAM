#!/bin/sh

set -e

cd /root

if [ -f play-framework.deb ]; then
        rm -f play-framework.deb
fi

mkdir -p play-framework/DEBIAN

cat > "play-framework/DEBIAN/control" <<EOF
Package: play-framework
Version: 1.3.0
Section: main
Priority: optional
Architecture: amd64
Depends: wget, unzip, python, oracle-java-jre
Maintainer: contact<AT>hd3g.tv
Description: Play framework v1
 See https://www.playframework.com/download
EOF

cat > "play-framework/DEBIAN/copyright" <<EOF
Format: http://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: Play Framework - MyDMAM Project
Upstream-Contact: contact<AT>hd3g.tv
Source: https://github.com/playframework/play1

Files: *
Copyright: 2010 Guillaume Bort (http://guillaume.bort.fr) zenexity (http://www.zenexity.fr) Play framework contributors (https://github.com/playframework/play/contributors)
License: Apache-2
 This package contains several Open sources libraries that are released under their own licence:
 .
 http://www.apache.org/licenses/LICENSE-2.0
    - Netty
    - Asyncweb
    - Cglib
    - Jakarta commons components
    - Eclipse compiler
    - Ehcache
    - Ezmorph
    - Groovy
    - GSon
    - Joda Time
    - JSR 107 Cache
    - Log4J
    - Snake YAML
    - Geronimo Servlets
    - Signpost
 .
 http://www.gnu.org/licenses/lgpl.html
    - Bytecodeparser
    - c3p0
    - Hibernate
 .
 http://www.antlr.org/license.html
    - antlr
 .
 http://www.dom4j.org/license.html
    - Dom4J
 .
 http://www.opensource.org/licenses/bsd-license.php
    - Jamon
    - Jaxen
    - JRegex
    - Simple Captcha
    - Imaging
 .
 http://www.opensource.org/licenses/mit-license.php
    - Spy Memcached
    - SLF4J
 .
 http://www.eclipse.org/legal/epl-v10.html
    - H2 Database
    - WikiText
    - Oval
 .
 http://www.opensource.org/licenses/cpl1.0.php
    - JUnit
 .
 http://www.mozilla.org/MPL/MPL-1.1.html
    - Javassist
 .
 http://www.gnu.org/licenses/gpl.html
    - MySQL connector
 .
 Public Domain
    - backport-util-concurrent
EOF

mkdir -p play-framework/opt

PLAYVERSION="1.3.0"
wget --no-check-certificate https://downloads.typesafe.com/play/$PLAYVERSION/play-$PLAYVERSION".zip"
unzip play-$PLAYVERSION".zip"
rm play-$PLAYVERSION".zip"
mv play1-$PLAYVERSION play-framework/opt/play1
# echo -n $PLAYVERSION > play-framework/opt/play1/framework/src/play/version

cat > "play-framework/DEBIAN/postinst" <<EOF
#!/bin/bash
set -e
chmod +x /opt/play1/play
ln -s /opt/play1/play /usr/bin/play
#/usr/bin/play version
#DEBHELPER#
exit 0
EOF

cat > "play-framework/DEBIAN/prerm" <<EOF
#!/bin/bash
rm /usr/bin/play
rm -rf /opt/play1
EOF

chmod 0755 play-framework/DEBIAN/*
dpkg-deb --build play-framework
rm -rf play-framework

