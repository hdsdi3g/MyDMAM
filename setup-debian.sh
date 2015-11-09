#!/bin/sh
#
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2015
# 
# Tested and functionnal with Debian 8 / Systemd.
# You can adapt this setup for others OS.

set -e

if [ $# -eq 0 ]; then
	echo "Usage: $0 setup|update";
	exit 1;
fi

ROOTSETUP="/opt"; 								# DIRECTORY WHERE GO APP AND PLAY
BASEPATH="$ROOTSETUP/mydmam"; 					# DIRECTORY WHERE IS MYDMAM
PLAYVERSION="1.3.2"; 							# SETUP THIS PLAY VERSION
PLAYPATH="$ROOTSETUP/play-$PLAYVERSION"; 		# DIRECTORY WHERE PLAY
PLAY_JAR="play-$PLAYVERSION.jar"; 				# PLAY JAR FILE
MYDMAM_GIT_BRANCH="master";						# THE GIT BRANCH TO SETUP
MYDMAM_LIB="http://mydmam.org/dwl/lib.tar.gz"	# DOWNLOAD ALL JARS FROM THIS
STARTUP_SCRIPTS="$ROOTSETUP/mydmam-statup"; 	# DIRECTORY WHERE ARE MYDMAM STARTUP SCRIPTS

# If you change this, don't forget to update EnvironmentFile var for all *.service
# and update this Systemd files version
DEST_DEFAULT="/etc/default/mydmam";

cd $ROOTSETUP

command -v java > /dev/null 2>&1 || {
	echo "MyDMAM require a valid Java 8 setup. Aborting." >&2;
	exit 1;
}
command -v git > /dev/null 2>&1 || {
	echo "MyDMAM require git. Installing it...";
	apt-get install -y git;
}
command -v curl > /dev/null 2>&1 || {
	echo "MyDMAM require curl. Installing it...";
	apt-get install -y curl;
}
command -v unzip > /dev/null 2>&1 || {
	echo "MyDMAM require unzip. Installing it...";
	apt-get install -y unzip;
}
command -v tar > /dev/null 2>&1 || {
	echo "MyDMAM require tar. Installing it...";
	apt-get install -y tar;
}
command -v gzip > /dev/null 2>&1 || {
	echo "MyDMAM require gzip. Installing it...";
	apt-get install -y gzip;
}
setup_play () {
	cd $ROOTSETUP
	curl https://github.com/playframework/play1/releases/download/$PLAYVERSION/play-$PLAYVERSION.zip > play-$PLAYVERSION.zip
	unzip -o play-$PLAYVERSION.zip
	rm play-$PLAYVERSION.zip
	rm -fq 
	if [ ! -d "$PLAYPATH" ]
	then
		echo "Can't found extracted Play setup ($PLAYPATH). Aborting." >&2;
		exit 1;
	fi
	chmod +x $PLAYPATH/play
	ln -sf $PLAYPATH/play /bin/play
}

command -v play > /dev/null 2>&1 || {
	echo "MyDMAM require Play! Framework v$PLAYVERSION. Installing it...";
	setup_play;
}

if [ ! -d "$BASEPATH" ]
then
	cd $BASEPATH
	echo "Deploy MyDMAM sources";
	git clone https://github.com/hdsdi3g/MyDMAM.git $BASEPATH
	git checkout $MYDMAM_GIT_BRANCH
	
	echo "Deploy MyDMAM dependencies";
	curl $MYDMAM_LIB | tar xzf -
	
	echo "Prepare default configuration files";
	cp -r conf/app.d-examples conf/app.d
	cp conf/application.conf.examples conf/application.conf
	cp conf/dependencies.yml.examples conf/dependencies.yml
	cp conf/log4j-prod-linux.xml conf/log4j.xml
	mkdir /var/log/mydmam
	play secret --silent $BASEPATH
	
	git clone https://github.com/hdsdi3g/MyDMAM.git $STARTUP_SCRIPTS
	cd $STARTUP_SCRIPTS
	git checkout startup
fi

if [ $1 = "update"  ]
then
	echo "Update Play! Framework v$PLAYVERSION...";
	setup_play;
	
	echo "Update MyDMAM...";
	cd $BASEPATH
	git checkout $MYDMAM_GIT_BRANCH
	git pull origin $MYDMAM_GIT_BRANCH
	rm -f lib/*
	curl $MYDMAM_LIB | tar xzf -
	
	cd $STARTUP_SCRIPTS
	git pull origin startup
fi

play clean --silent $BASEPATH
play precompile --silent $BASEPATH

CLASSPATH=$BASEPATH/precompiled/java;
CLASSPATH=$CLASSPATH:$BASEPATH/conf;
CLASSPATH="$CLASSPATH:$PLAYPATH/framework/$PLAY_JAR";

for file in $PLAYPATH/framework/lib/*.jar
do
        CLASSPATH=$CLASSPATH:$file;
done

for file in $BASEPATH/lib/*.jar
do
        CLASSPATH=$CLASSPATH:$file;
done

echo "# MyDMAM Configuration file, do not edit. Created by $0" > $DEST_DEFAULT
echo "BASEPATH=$BASEPATH" >> $DEST_DEFAULT
echo "CONFIG_PATH=\"-Dservice.config.path=$BASEPATH/conf/app.d\"" >> $DEST_DEFAULT
echo "CLASSPATH=$CLASSPATH" >> $DEST_DEFAULT
echo "PLAYPATH=$PLAYPATH" >> $DEST_DEFAULT

systemctl daemon-reload

echo "=== COMPLETED ==="
echo "Please change/check MyDMAM configuration files in $BASEPATH/conf and $BASEPATH/conf/app.d";
echo "";
echo "For setup/upgrade Systemd files, you can copy from $STARTUP_SCRIPTS"
cd $STARTUP_SCRIPTS
ls *.service
echo "To /usr/lib/systemd"
echo "And call systemctl daemon-reload."
echo "";
echo "Else, you can call systemctl, like:";
echo "systemctl start|stop|status|enable mydmam-server|mydmam-probe"
echo "";
echo "For manual tests, you can start from $STARTUP_SCRIPTS";
ls *-run.sh
