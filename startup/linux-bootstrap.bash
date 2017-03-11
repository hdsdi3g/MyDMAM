#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
# 
# Tested and functionnal with Debian 8 / Systemd.
# But it can works with others GNU/Linux distribution that support Systemd
# You also can adapt this setup for others NIX OS.

# This script will do some tasks, like checks, relative paths to absolute path, and script creation.
# You don't needs to install some tools.
# Topics:
	# Where I am ?
	# Load boostrap configuration
	# Resolve JRE relative path to absolute path
	# Test Java (JRE)
	# Create env file (actually classpath and Java)
	# Create Service file
	# Create Service register/unregister and actions
	# Create CLI script
	# Prepare log configuration
	# Let user to start something

set -e

# Where I am ?
CURRENT_SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
cd $CURRENT_SCRIPT_DIR;
BASEPATH=$(realpath $CURRENT_SCRIPT_DIR/..);

# Load boostrap configuration
. setup.bash

# Resolve JRE relative path to absolute path
JAVA=$(realpath $JAVA);

# Test Java (JRE)
if [ ! -f "$JAVA" ]; then
	echo "Can't found file $JAVA."
	echo "Please check you setup."
fi
if [ ! -x "$JAVA" ]; then
	chmod +x $JAVA
fi

echo "Try to start JVM: $JAVA"
$JAVA -version

# Create env file (actually classpath and Java)

ENV_FILE=$CURRENT_SCRIPT_DIR/env.bash;

CLASSPATH=$BASEPATH/conf;
for file in $BASEPATH/lib/*.jar
do
        CLASSPATH=$CLASSPATH:$file;
done

cat <<- EOF > $ENV_FILE
	# MyDMAM Configuration file, do not edit. Created by $0
	JAVA=$JAVA
	CLASSPATH=$CLASSPATH
EOF

# Create Service file

SERVICE_FILE=$CURRENT_SCRIPT_DIR/mydmam.service;

cat <<- EOF > $SERVICE_FILE
	# MyDMAM Service file, do not edit. Created by $0
	[Unit]
	Description=MyDMAM Service
	After=network.target auditd.service

	[Service]
	EnvironmentFile=$ENV_FILE
	ExecStart=\$\{JAVA\} -noverify -server -Dfile.encoding=UTF-8 -Dservice.config.path=$BASEPATH/conf/app.d -classpath \$\{CLASSPATH\} hd3gtv.mydmam.MainClass

	[Install]
	WantedBy=multi-user.target
	Alias=mydmam.service
EOF

# Create Service register/unregister and actions

cp -f $CURRENT_SCRIPT_DIR/mydmam.service /usr/lib/systemd/mydmam.service
echo "MyDMAM Service installed in /usr/lib/systemd/mydmam.service"

SERVICE_ENABLE_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-enable.bash
SERVICE_DISABLE_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-disable.bash
SERVICE_START_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-start.bash
SERVICE_STOP_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-stop.bash
SERVICE_STATUS_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-status.bash

cat <<- EOF > $SERVICE_ENABLE_FILE
	#/bin/bash
	# MyDMAM Service script
	set -e
	cp -f $CURRENT_SCRIPT_DIR/mydmam.service /usr/lib/systemd/mydmam.service
	echo "MyDMAM Service installed in /usr/lib/systemd/mydmam.service"
	systemctl enable /usr/lib/systemd/mydmam.service
	systemctl daemon-reload
	echo "Use $SERVICE_START_FILE or systemctl start mydmam for start MyDMAM Service"
EOF
chmod +x $SERVICE_ENABLE_FILE

cat <<- EOF > $SERVICE_DISABLE_FILE
	#/bin/bash
	# MyDMAM Service script
	$SERVICE_STOP_FILE
	set -e
	systemctl disable /usr/lib/systemd/mydmam.service
	systemctl daemon-reload
	rm -f /usr/lib/systemd/mydmam.service
	echo "MyDMAM Service removed from /usr/lib/systemd/mydmam.service"
EOF
chmod +x $SERVICE_DISABLE_FILE

cat <<- EOF > $SERVICE_START_FILE
	#/bin/bash
	# MyDMAM Service script
	set -e
	if [ ! -f "/usr/lib/systemd/mydmam.service" ]; then
		$SERVICE_ENABLE_FILE
	fi

	systemctl start mydmam
	$SERVICE_STATUS_FILE
EOF
chmod +x $SERVICE_START_FILE

cat <<- EOF > $SERVICE_STOP_FILE
	#/bin/bash
	# MyDMAM Service script
	set -e
	if [ ! -f "/usr/lib/systemd/mydmam.service" ]; then
		echo "MyDMAM service script (/usr/lib/systemd/mydmam.service) is not installed."
		exit 0;
	fi

	systemctl stop mydmam
	$SERVICE_STATUS_FILE
EOF
chmod +x $SERVICE_STOP_FILE

cat <<- EOF > $SERVICE_STATUS_FILE
	#/bin/bash
	# MyDMAM Service script
	set -e
	if [ ! -f "/usr/lib/systemd/mydmam.service" ]; then
		echo "MyDMAM service script (/usr/lib/systemd/mydmam.service) is not installed."
		exit 0;
	fi

	systemctl status mydmam
EOF
chmod +x $SERVICE_STATUS_FILE

# Create CLI script

CLI_FILE=$CURRENT_SCRIPT_DIR/mydmam-cli.bash

cat <<- EOF > $CLI_FILE
	#/bin/bash
	# MyDMAM CLI script
	set -e
	. $ENV_FILE
	\$JAVA -noverify -Dfile.encoding=UTF-8 -Dfile.encoding=UTF-8 -Dservice.config.path=$BASEPATH/conf/app.d -classpath \$CLASSPATH hd3gtv.mydmam.cli.MainClass $@
EOF

chmod +x $CLI_FILE

# Prepare log configuration

rm -f $BASEPATH/conf/log4j.xml
cp $BASEPATH/conf/log4j-prod-linux.xml $BASEPATH/conf/log4j.xml

LOG_FILE=$(cat $BASEPATH/conf/log4j.xml | grep param | grep log | grep File | cut -d "\"" -f 4);

LOG_DIR=$(dirname $LOG_FILE);

set +e
{ # try
	if [ ! -d "$LOG_DIR" ]; then
		mkdir -p $LOG_DIR
	fi
	touch $LOG_FILE
	echo "Set log configuration to $LOG_FILE"
	set -e
} || { # catch
	set -e
	LOG_DIR=$BASEPATH/logs
	echo "Can't prepare log directory $LOG_DIR"
	rm -f $BASEPATH/conf/log4j.xml
	cp $BASEPATH/conf/log4j-prod.xml $BASEPATH/conf/log4j.xml
	echo "MyDMAM log is set to local directory $LOG_DIR"
	echo "For change it, edit $BASEPATH/conf/log4j.xml"
}

# Let user to start something

echo "=== COMPLETED ==="
echo "Please change/check MyDMAM configuration files in $BASEPATH/conf and $BASEPATH/conf/app.d";
echo "You can use CLI tool $CLI_FILE and/or use mydmam-service-* tool in $CURRENT_SCRIPT_DIR for operate on service."
echo "By default this script don't enable MyDMAM service"
echo "After service startup, check MyDMAM status with tail -f $LOG_DIR/mydmam.log"
