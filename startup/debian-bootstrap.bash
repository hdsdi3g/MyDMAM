#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
# 
# Tested and functionnal with Debian 8 / Systemd.
# But it can works with others GNU/Linux distribution that support SystemD
# You also can adapt this setup for others NIX OS.

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

//TODO move service file to build XML

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

SERVICE_ENABLE_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-enable.sh
SERVICE_DISABLE_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-disable.sh
SERVICE_START_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-start.sh
SERVICE_STOP_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-stop.sh
SERVICE_STATUS_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-status.sh

cat <<- EOF > $SERVICE_ENABLE_FILE
	#/bin/sh
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
	#/bin/sh
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
	#/bin/sh
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
	#/bin/sh
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
	#/bin/sh
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

CLI_FILE=$CURRENT_SCRIPT_DIR/mydmam-cli.sh

cat <<- EOF > $CLI_FILE
	#/bin/sh
	# MyDMAM CLI script
	set -e
	. $ENV_FILE
	\$JAVA -noverify -Dfile.encoding=UTF-8 -Dfile.encoding=UTF-8 -Dservice.config.path=$BASEPATH/conf/app.d -classpath \$CLASSPATH hd3gtv.mydmam.cli.MainClass $@
EOF

chmod +x $CLI_FILE

//TODO ....



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

Check with

- `systemctl status mydmam-server` and
- `tail -f /var/log/mydmam/mydmam.log`
- Your mails

And set CLI:

- `ln -s (this directory)/cli-run.sh /bin/mydmam-cli`
- `chmod +x (this directory)/cli-run.sh`

## Update

See [MyDMAM changelogs](http://mydmam.org/category/changelogs) before updating...

- use Git with mydmam app
- change Play directory
- update-alternative for new Java JRE
- change all Jars in `mydmam/lib` directory
- upgrade ES and/or Cassandra version

Activate this with a simple `./precompile.sh`
Edit it before run if you want to change Play version.
Restart with

- `systemctl daemon-reload`
- `systemctl stop mydmam-{server|probe}`
- `systemctl start mydmam-{server|probe}`
- `systemctl status mydmam-{server|probe}`

## Tips
### Setup Java 

- unpack Oracle’s version somewhere, like in `/opt`
- `update-alternatives --install /usr/bin/java java (extracted directory)/bin/java 2000`

If Debian install some others JRE, check with
`update-alternatives --display java`
if your JRE has the best priority (we set 2000 here).

Don’t forget to setup the *Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files*, free downloadable from [Oracle website](http://www.oracle.com/technetwork/java/javase/downloads/index.html), else the startup script will throw a fatal exception. See the message for known the exact location to extract the files, often here: `(extracted JRE directory)/jre/lib/security/`.

### Update Java

- `update-alternatives --remove java $(realpath /etc/alternatives/java)`
- `update-alternatives --install /usr/bin/java java (extracted directory for new JRE)/bin/java 2000`

Check with

- `update-alternatives --display java`
- `java -version`
- Unpack *Java Cryptography Extension (JCE*)