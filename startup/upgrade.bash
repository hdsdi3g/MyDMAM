#!/bin/sh
# MyDMAM Upgrade script
#
# This script extract the new version archive file,
# backup the previous version, move new version in place of the previous version
# and move the previous configuration files to the new version.
#
# Tested with Debian 8. It should works with all Linux and macOS versions.
#
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017

VERSION="1.0";
THIS_SCRIPT="$0";
set -e

echo "MyDMAM Upgrade script v$VERSION";

# Where I am ?
BASEPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

# Load OS/Posix type
UNAME=$(uname);

# Only root can create users/groups
if [[ "$UNAME" == 'Linux' ]]; then
	if [ "$(id -u)" != "0" ]; then
	   echo "This script must be run as root" 1>&2
	   exit 1
	fi
fi

function usage {
	echo "Usage:";
	echo "   $THIS_SCRIPT mydmam_redistributable_pack mydmam_path";
	echo "With:";
	echo "   mydmam_redistributable_pack";
	echo "       The path to the MyDMAM archive file (the new version downloaded from MyDMAM website).";
	echo "   mydmam_path";
	echo "       The path to the current MyDMAM installation directory to update.";
	echo " ";
	echo "Configuration files will be transfered to the new setup.";
	echo "The old setup will be backuped.";
	echo "Please stop MyDMAM execution before start this script.";
}

ARCHVIVE_FILE="$1";
MYDMAM_DIR="$2";

# TODO Check archive file
# TODO Check mydmam dir (var, presence, is really mydmam)
# TODO Extract, backup, move files, copy conf


# Show upgrade notes
function show_upgrade_notes {
	echo "";
	echo "Upgrade notes:"
	local IFS=$'\n'
	for line in $(cat "$MYDMAM_DIR/UPGRADE.txt")
	do
		if [[ $line == *"=== Previous versions ==="* ]]; then
		    break;
		fi
		if [[ $line == *"==="* ]]; then
		    continue;
		fi
		echo "  $line";
	done
	unset IFS
	echo "";
}
show_upgrade_notes;

if [[ "$UNAME" == 'Linux' ]]; then
	bash "$MYDMAM_DIR/startup/linux-bootstrap.bash"
else
	# TODO remetre !!
	echo "$MYDMAM_DIR/startup/macos-bootstrap.bash"
fi

echo "";
echo "Upgrade is done. You can restart MyDMAM service if you want."
