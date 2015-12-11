#!/bin/sh

set -e

cd /root

if [ -f mydmam.deb ]; then
	rm -f mydmam.deb
fi

mkdir -p mydmam/DEBIAN

cat > "mydmam/DEBIAN/control" <<EOF
Package: mydmam
Version: 0.14
Section: main
Priority: optional
Architecture: amd64
Depends: git, wget, tar, gzip, oracle-java-jre
Suggests: ffmpeg, imagemagick
Maintainer: contact<AT>hd3g.tv
Description: A Digital Media Asset Management
 MyDMAM is another way of looking a Digital Media Asset Management, it able to let you choose your way to manage your medias, your metadatas, your storages organisation and IDs.
EOF

cat > "mydmam/DEBIAN/copyright" <<EOF
Format: http://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: MyDMAM Project
Upstream-Contact: contact<AT>hd3g.tv
Source: https://github.com/hdsdi3g/MyDMAM

Files: *
Copyright: 2012 hdsdi3g for hd3g.tv
License: LGPL-3+
EOF

NOW=$(date -R)
cat >> "mydmam/DEBIAN/changelog" <<EOF
mydmam (0.14.deploydebug.1) unstable; urgency=low

 * Test deploy Debian package

-- contact<AT>hd3g.tv $NOW

EOF

mkdir -p mydmam/var/log/mydmam
mkdir -p mydmam/usr/bin
mkdir -p mydmam/opt
mkdir -p mydmam/etc/default
mkdir -p mydmam/usr/lib/systemd

git clone https://github.com/hdsdi3g/MyDMAM.git mydmam-git
cd mydmam-git
git checkout dvl
wget http://mydmam.org/dwl/lib.tar.gz
tar xvfz lib.tar.gz
rm lib.tar.gz
cd ..
mv mydmam-git mydmam/opt/mydmam

git clone https://github.com/hdsdi3g/MyDMAM.git startup-git
cd startup-git/
git checkout startup
cd ..
mv startup-git mydmam/opt/mydmam-scripts

cp mydmam/opt/mydmam-scripts/probe.service mydmam/usr/lib/systemd/mydmam-probe.service
cp mydmam/opt/mydmam-scripts/server.service mydmam/usr/lib/systemd/mydmam-server.service

chmod +x mydmam/opt/mydmam-scripts/cli-run.sh
chmod +x mydmam/opt/mydmam-scripts/server-run.sh
chmod +x mydmam/opt/mydmam-scripts/probe-run.sh

cat > "mydmam/DEBIAN/preinst" <<EOF
#!/bin/bash
# STOP tous les services qui tournent
exit 0
EOF

cat > "mydmam/DEBIAN/prerm" <<EOF
#!/bin/bash
# STOP tous les services qui tournent
exit 0
EOF

cat > "mydmam/DEBIAN/postinst" <<EOF
#!/bin/bash
set -e
echo "Configuring mon premier paquet..."

ln -s /opt/mydmam-scripts/cli-run.sh /usr/bin/mydmam-cli
ln -s /opt/mydmam-scripts/probe-run.sh /usr/bin/mydmam-probe
ln -s /opt/mydmam-scripts/server-run.sh /usr/bin/mydmam-server

# A faire si ils n'existent pas deja
#cp -r conf/app.d-examples conf/app.d
#cp conf/application.conf.examples conf/application.conf
#cp conf/dependencies.yml.examples conf/dependencies.yml
#cp conf/log4j-prod-linux.xml conf/log4j.xml
# PLAY SECURE, DEFAULT...
#DEBHELPER#
exit 0
EOF

# conffiles ??

cat > "mydmam/DEBIAN/postrm" <<EOF
#!/bin/bash
rm /usr/bin/mydmam-cli
rm /usr/bin/mydmam-probe
rm /usr/bin/mydmam-server
EOF

chmod 0755 mydmam/DEBIAN/*
dpkg-deb --build mydmam
rm -rf mydmam

