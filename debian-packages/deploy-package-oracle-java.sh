#!/bin/sh

set -e

cd /root

if [ -f oracle-java-jre.deb ]; then
        rm -f oracle-java-jre.deb
fi

mkdir -p oracle-java-jre/DEBIAN

cat > "oracle-java-jre/DEBIAN/control" <<EOF
Package: oracle-java-jre
Version: 8u65
Priority: optional
Architecture: amd64
Depends: wget, tar, gzip, unzip
Provides: java-browser-plugin, java-runtime, java-runtime-headless, java-virtual-machine, java2-runtime, java2-runtime-headless, java6-runtime, java6-runtime-headless, java7-runtime, java7-runtime-headless, java8-runtime, java8-runtime-headless
Section: non-free/devel
Maintainer: contact<AT>hd3g.tv
Description: Oracle Java JRE runtime with Java Cryptography Extension Policy files
 See http://www.oracle.com/technetwork/java/javase/downloads/index.html for more informations. You must accept the Oracle Binary Code License Agreement for Java SE to download this software, on http://www.oracle.com/technetwork/java/javase/terms/license/index.html
EOF

cat > "oracle-java-jre/DEBIAN/copyright" <<EOF
Format: http://www.debian.org/doc/packaging-manuals/copyright-format/1.0/
Upstream-Name: Oracle - MyDMAM Project
Upstream-Contact: contact<AT>hd3g.tv
Source: http://www.oracle.com/technetwork/java/javase/downloads/index.html

Files: *
Copyright: 1993-2015 Oracle and/or its affiliates. All rights reserved
License: ORACLE
 Copyright (C) 1993, 2015, Oracle and/or its affiliates.
 All rights reserved.
 .
 This software and related documentation are provided under a
 license agreement containing restrictions on use and
 disclosure and are protected by intellectual property laws.
 Except as expressly permitted in your license agreement or
 allowed by law, you may not use, copy, reproduce, translate,
 broadcast, modify, license, transmit, distribute, exhibit,
 perform, publish, or display any part, in any form, or by
 any means. Reverse engineering, disassembly, or
 decompilation of this software, unless required by law for
 interoperability, is prohibited.
 .
 The information contained herein is subject to change
 without notice and is not warranted to be error-free. If you
 find any errors, please report them to us in writing.
 .
 If this is software or related documentation that is
 delivered to the U.S. Government or anyone licensing it on
 behalf of the U.S. Government, the following notice is
 applicable:
 .
 U.S. GOVERNMENT END USERS: Oracle programs, including any
 operating system, integrated software, any programs
 installed on the hardware, and/or documentation, delivered
 to U.S. Government end users are "commercial computer
 software" pursuant to the applicable Federal Acquisition
 Regulation and agency-specific supplemental regulations. As
 such, use, duplication, disclosure, modification, and
 adaptation of the programs, including any operating system,
 integrated software, any programs installed on the hardware,
 and/or documentation, shall be subject to license terms and
 license restrictions applicable to the programs. No other
 rights are granted to the U.S. Government.
 .
 This software or hardware is developed for general use in a
 variety of information management applications. It is not
 developed or intended for use in any inherently dangerous
 applications, including applications that may create a risk
 of personal injury. If you use this software or hardware in
 dangerous applications, then you shall be responsible to
 take all appropriate fail-safe, backup, redundancy, and
 other measures to ensure its safe use. Oracle Corporation
 and its affiliates disclaim any liability for any damages
 caused by use of this software or hardware in dangerous
 applications.
 .
 Oracle and Java are registered trademarks of Oracle and/or
 its affiliates. Other names may be trademarks of their
 respective owners.
 .
 Intel and Intel Xeon are trademarks or registered trademarks
 of Intel Corporation. All SPARC trademarks are used under
 license and are trademarks or registered trademarks of SPARC
 International, Inc. AMD, Opteron, the AMD logo, and the AMD
 Opteron logo are trademarks or registered trademarks of
 Advanced Micro Devices. UNIX is a registered trademark of
 The Open Group.
 .
 This software or hardware and documentation may provide
 access to or information on content, products, and services
 from third parties. Oracle Corporation and its affiliates
 are not responsible for and expressly disclaim all
 warranties of any kind with respect to third-party content,
 products, and services. Oracle Corporation and its
 affiliates will not be responsible for any loss, costs, or
 damages incurred due to your access to or use of third-party
 content, products, or services.
EOF

mkdir -p oracle-java-jre/opt

JRE_URL="http://download.oracle.com/otn-pub/java/jdk/8u65-b17/jre-8u65-linux-x64.tar.gz"

wget --no-check-certificate --header "Cookie: oraclelicense=accept-securebackup-cookie" $JRE_URL

NEWJRE_TAR_FILE=$(echo $JRE_URL | rev | cut -d '/' -f 1 | rev)
tar -xzf $NEWJRE_TAR_FILE
NEWJRE_BASE_DIR=$(tar tzf ${NEWJRE_TAR_FILE} | head -n 1 | cut -d "/" -f 1);
mv $NEWJRE_BASE_DIR oracle-java-jre/opt/oracle-java-jre
rm $NEWJRE_TAR_FILE

chown -R root:root oracle-java-jre/opt/oracle-java-jre

wget --no-verbose --header "Cookie: oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip"
unzip jce_policy-8.zip 
mv UnlimitedJCEPolicyJDK8/* oracle-java-jre/opt/oracle-java-jre/lib/security/
rm jce_policy-8.zip
rm -rf UnlimitedJCEPolicyJDK8

cat > "oracle-java-jre/DEBIAN/postinst" <<EOF
#!/bin/bash
set -e
if [ -x /etc/alternatives/java ]; then 
	update-alternatives --remove java /opt/oracle-java-jre/bin/java
	rm -f /etc/alternatives/java
fi      
update-alternatives --install /usr/bin/java java /opt/oracle-java-jre/bin/java 2000
#DEBHELPER#
exit 0
EOF

cat > "oracle-java-jre/DEBIAN/prerm" <<EOF
#!/bin/bash
if [ -x /etc/alternatives/java ]; then
	update-alternatives --remove java /opt/oracle-java-jre/bin/java
	rm -f /etc/alternatives/java
fi
EOF

chmod 0755 oracle-java-jre/DEBIAN/*
dpkg-deb --build oracle-java-jre

rm -rf oracle-java-jre

