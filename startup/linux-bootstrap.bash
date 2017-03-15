#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
# 
# Tested and functionnal with Debian 8 / Systemd.
# But it can works with others GNU/Linux distribution that support Systemd
# You also can adapt this setup for others NIX OS.

# This script will do some tasks, like checks, relative paths to absolute path, and script creation.
# You don't needs to install some tools.

set -e

if [ "$(id -u)" != "0" ]; then
   echo "This script must be run as root" 1>&2
   exit 1
fi

# Where I am ?
CURRENT_SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
cd "$CURRENT_SCRIPT_DIR";
BASEPATH=$(realpath "$CURRENT_SCRIPT_DIR/..");

# Load boostrap configuration
. setup.bash
# Load toolkit
. _utils.bash

resolve_jre_path "$JAVA_LINUX";
set_classpath;

# Create env file (actually classpath and Java)
ENV_FILE=$CURRENT_SCRIPT_DIR/env.bash;
cat <<- EOF > $ENV_FILE
	# MyDMAM Configuration file, do not edit. Created by $0
	JAVA=$JAVA
	CLASSPATH=$CLASSPATH
EOF

# Create Linux User
echo "Create user and group mydmam in /var/lib/mydmam"
adduser --system --home "$BASEPATH" --no-create-home --group mydmam mydmam
chown mydmam:mydmam -R "$BASEPATH"

# Create Service file
SERVICE_FILE=$CURRENT_SCRIPT_DIR/mydmam.service;

cat <<- EOF > $SERVICE_FILE
	# MyDMAM Service file, do not edit. Created by $0
	[Unit]
	Description=MyDMAM
	Documentation=http://mydmam.org/
	Wants=network-online.target
	After=network-online.target auditd.service

	[Service]
	EnvironmentFile=$ENV_FILE
	ExecStart=\$\{JAVA\} -noverify -server -Dfile.encoding=UTF-8 -Dservice.config.path=$BASEPATH/conf/app.d -classpath \$\{CLASSPATH\} hd3gtv.mydmam.MainClass
	SuccessExitStatus=143
	User=mydmam
	Group=mydmam
	StandardOutput=journal
	StandardError=journal
	SuccessExitStatus=143
	TimeoutStopSec=30

	[Install]
	WantedBy=multi-user.target
	Alias=mydmam.service
EOF

# Create Service register/unregister and actions
create_service_tools_filenames;

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
chmod +x "$SERVICE_ENABLE_FILE"

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
chmod +x "$SERVICE_DISABLE_FILE"

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
chmod +x "$SERVICE_START_FILE"

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
chmod +x "$SERVICE_STOP_FILE"

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
chmod +x "$SERVICE_STATUS_FILE"

create_cli;
set_logs;
ends_setup;

echo "You should start MyDMAM (service/cli) as mydmam user with"
echo "runuser -u mydmam $CLI_FILE";
