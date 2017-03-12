#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
# 
# Tested and functionnal with macOS 10.10 / Yosemite
# But it can works with more recent macOS version.
#
# If you change the classpath and/or the JRE version/directory, you must redeclare the macOS service file.
# TODO comments

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

SERVICE_FILE=$CURRENT_SCRIPT_DIR/hd3gtv.mydmam.service.plist;

cat <<- EOF > $SERVICE_FILE
    <?xml version="1.0" encoding="UTF-8"?>
    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
    <plist version="1.0">
    <dict>
        <key>Label</key>
        <string>hd3gtv.mydmam.service</string>
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

# http://www.launchd.info/
# ~/Library/LaunchAgents
# launchctl load ~/Library/LaunchAgents/com.example.app.plist
# launchctl unload ~/Library/LaunchAgents/com.example.app.plist
# launchctl start com.example.app
# launchctl stop com.example.app

cat <<- EOF > $SERVICE_ENABLE_FILE
    #/bin/bash
    # MyDMAM Service script
    set -e
    # cp -f $CURRENT_SCRIPT_DIR/mydmam.service /usr/lib/systemd/mydmam.service
    # echo "MyDMAM Service installed in /usr/lib/systemd/mydmam.service"
    # systemctl enable /usr/lib/systemd/mydmam.service
    # systemctl daemon-reload
    # echo "Use $SERVICE_START_FILE or systemctl start mydmam for start MyDMAM Service"
EOF
chmod +x $SERVICE_ENABLE_FILE

cat <<- EOF > $SERVICE_DISABLE_FILE
    #/bin/bash
    # MyDMAM Service script
    # $SERVICE_STOP_FILE
    # set -e
    # systemctl disable /usr/lib/systemd/mydmam.service
    # systemctl daemon-reload
    # rm -f /usr/lib/systemd/mydmam.service
    # echo "MyDMAM Service removed from /usr/lib/systemd/mydmam.service"
EOF
chmod +x $SERVICE_DISABLE_FILE

cat <<- EOF > $SERVICE_START_FILE
    #/bin/bash
    # MyDMAM Service script
    # set -e
    # if [ ! -f "/usr/lib/systemd/mydmam.service" ]; then
    #     $SERVICE_ENABLE_FILE
    # fi
    # 
    # systemctl start mydmam
    # $SERVICE_STATUS_FILE
EOF
chmod +x $SERVICE_START_FILE

cat <<- EOF > $SERVICE_STOP_FILE
    #/bin/bash
    # MyDMAM Service script
    # set -e
    # if [ ! -f "/usr/lib/systemd/mydmam.service" ]; then
    #     echo "MyDMAM service script (/usr/lib/systemd/mydmam.service) is not installed."
    #     exit 0;
    # fi
    # 
    # systemctl stop mydmam
    # $SERVICE_STATUS_FILE
EOF
chmod +x $SERVICE_STOP_FILE

cat <<- EOF > $SERVICE_STATUS_FILE
    #/bin/bash
    # MyDMAM Service script
    # set -e
    # if [ ! -f "/usr/lib/systemd/mydmam.service" ]; then
    #     echo "MyDMAM service script (/usr/lib/systemd/mydmam.service) is not installed."
    #     exit 0;
    # fi
    # 
    # systemctl status mydmam
EOF
chmod +x $SERVICE_STATUS_FILE

ends_setup;
