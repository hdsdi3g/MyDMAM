#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
#
# Toolkit for *-bootstrap.bash

set -e

# Resolve JRE relative path to absolute path
# Param $1  Relative JRE Path
# Set       $JAVA
resolve_jre_path () {
	JAVA=$(realpath "$1");

	# Test Java (JRE)
	if [ ! -f "$JAVA" ]; then
		echo "Can't found file $JAVA."
		echo "Please check you setup."
	fi
	if [ ! -x "$JAVA" ]; then
		chmod +x "$JAVA"
	fi

	echo "Try to start JVM: $JAVA"
	$JAVA -version
}

# Compute classpath
# Set     $CLASSPATH
set_classpath () {
	CLASSPATH=$BASEPATH/conf;
	for file in $BASEPATH/lib/*.jar
	do
			CLASSPATH=$CLASSPATH:$file;
	done
}

# Create CLI script
create_cli () {
	CLI_FILE=$CURRENT_SCRIPT_DIR/mydmam-cli.bash

	cat <<- EOF > $CLI_FILE
		#/bin/bash
		# MyDMAM CLI script
		set -e
		. $ENV_FILE
		\$JAVA -noverify -Dfile.encoding=UTF-8 -Dfile.encoding=UTF-8 -Dservice.config.path=$BASEPATH/conf/app.d -classpath \$CLASSPATH hd3gtv.mydmam.cli.MainClass $@
	EOF
	chmod +x "$CLI_FILE"
}

# Prepare log configuration
# Set       $LOG_FILE and $LOG_DIR
set_logs () {
	rm -f "$BASEPATH/conf/log4j.xml"
	cp "$BASEPATH/conf/log4j-prod-linux.xml" "$BASEPATH/conf/log4j.xml"

	LOG_FILE=$(cat "$BASEPATH/conf/log4j.xml" | grep param | grep log | grep File | cut -d "\"" -f 4);

	LOG_DIR=$(dirname "$LOG_FILE");

	set +e
	{ # try
		if [ ! -d "$LOG_DIR" ]; then
			mkdir -p "$LOG_DIR"
		fi
		touch "$LOG_FILE"
		echo "Set log configuration to $LOG_FILE"
		set -e
	} || { # catch
		set -e
		LOG_DIR=$BASEPATH/logs
		echo "Can't prepare log directory $LOG_DIR"
		rm -f "$BASEPATH/conf/log4j.xml"
		cp "$BASEPATH/conf/log4j-prod.xml" "$BASEPATH/conf/log4j.xml"
		echo "MyDMAM log is set to local directory $LOG_DIR"
		echo "For change it, edit $BASEPATH/conf/log4j.xml"
	}
}

create_service_tools_filenames () {
	SERVICE_ENABLE_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-enable.bash
	SERVICE_DISABLE_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-disable.bash
	SERVICE_START_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-start.bash
	SERVICE_STOP_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-stop.bash
	SERVICE_STATUS_FILE=$CURRENT_SCRIPT_DIR/mydmam-service-status.bash
}

# Let user to start something
ends_setup () {
	echo "=== COMPLETED ==="
	echo "Please change/check MyDMAM configuration files in $BASEPATH/conf and $BASEPATH/conf/app.d";
	echo "You can use CLI tool $CLI_FILE and/or use mydmam-service-* tool in $CURRENT_SCRIPT_DIR for operate on service."
	echo "By default this script don't enable MyDMAM service"
	echo "After service startup, check MyDMAM status with tail -f $LOG_DIR/mydmam.log"
}
