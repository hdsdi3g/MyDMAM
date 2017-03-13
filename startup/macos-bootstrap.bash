#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
# 
# Tested and functionnal with macOS 10.10 / Yosemite
# But it can works with more recent macOS version.
#
# If you change the classpath and/or the JRE version/directory, you must redeclare the macOS service file.

set -e

# Where I am ?
CURRENT_SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
cd $CURRENT_SCRIPT_DIR;
BASEPATH=$(realpath $CURRENT_SCRIPT_DIR/..);

# Load boostrap configuration
. setup.bash
# Load toolkit
. _utils.bash

resolve_jre_path $JAVA_OSX;
set_classpath;
create_cli;
set_logs;

SERVICE_LABEL="hd3gtv.mydmam.service";
SERVICE_FILE=$CURRENT_SCRIPT_DIR/hd3gtv.mydmam.service.plist;
# Create service declaration file

cat <<- EOF > $SERVICE_FILE
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>Label</key>
        <string>$SERVICE_LABEL</string>
        <key>ProgramArguments</key>
        <array>
            <string>$JAVA</string>
            <string>-noverify</string>
            <string>-server</string>
            <string>-Dfile.encoding=UTF-8</string>
            <string>-Dservice.config.path=$BASEPATH/conf/app.d</string>
            <string>-classpath</string>
            <string>$CLASSPATH</string>
            <string>hd3gtv.mydmam.MainClass</string>
        </array>
        <key>StandardOutPath</key>
        <string>$LOG_DIR/service.log</string>
        <key>StandardErrorPath</key>
        <string>$LOG_DIR/service.log</string>
        <key>WorkingDirectory</key>
        <string>$BASEPATH</string>
        <key>RunAtLoad</key>
        <true/>
    </dict>
    </plist>
EOF

create_service_tools_filenames;

SETUP_SERVICE_FILE="$HOME/Library/LaunchAgents/"$(basename $SERVICE_FILE);

# http://www.launchd.info/

cat <<- EOF > $SERVICE_ENABLE_FILE
    #/bin/bash
    # MyDMAM Service script
    set -e
    cp -f $SERVICE_FILE $SETUP_SERVICE_FILE
    echo "MyDMAM Service installed in $SETUP_SERVICE_FILE"
    launchctl load -w $SETUP_SERVICE_FILE
    echo "Use $SERVICE_START_FILE or launchctl start $SERVICE_LABEL start MyDMAM Service"
EOF
chmod +x $SERVICE_ENABLE_FILE

cat <<- EOF > $SERVICE_DISABLE_FILE
    #/bin/bash
    # MyDMAM Service script
    $SERVICE_STOP_FILE
    set -e
    launchctl unload -w $SETUP_SERVICE_FILE
    rm -f $SETUP_SERVICE_FILE
    echo "MyDMAM Service removed from $SETUP_SERVICE_FILE"
EOF
chmod +x $SERVICE_DISABLE_FILE

cat <<- EOF > $SERVICE_START_FILE
    #/bin/bash
    # MyDMAM Service script
    set -e
    if [ ! -f "$SETUP_SERVICE_FILE" ]; then
        $SERVICE_ENABLE_FILE
    fi
    launchctl start $SERVICE_LABEL
    $SERVICE_STATUS_FILE
EOF
chmod +x $SERVICE_START_FILE

cat <<- EOF > $SERVICE_STOP_FILE
    #/bin/bash
    # MyDMAM Service script
    set -e
    if [ ! -f "$SETUP_SERVICE_FILE" ]; then
         echo "MyDMAM service script ($SETUP_SERVICE_FILE) is not installed."
         exit 0;
    fi
    
    launchctl stop $SERVICE_LABEL
    $SERVICE_STATUS_FILE
EOF
chmod +x $SERVICE_STOP_FILE

cat <<- EOF > $SERVICE_STATUS_FILE
    #/bin/bash
    # MyDMAM Service script
    set -e
    if [ ! -f "$SETUP_SERVICE_FILE" ]; then
        echo "MyDMAM service script ($SETUP_SERVICE_FILE) is not installed."
        exit 0;
    fi
     
    launchctl list | grep $SERVICE_LABEL
EOF
chmod +x $SERVICE_STATUS_FILE

ends_setup;
