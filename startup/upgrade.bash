#!/bin/sh
# MyDMAM Upgrade script
#
# This script extract the new version archive file,
# backup the previous version, move the new version in place of the previous version
# and move the previous configuration files to the new version.
#
# Tested with Debian 8 and macOS. It should works with all Linux.
# It needs only tar, gzip, bash and uname.
# 
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017

# Space separated
DIRS_TO_KEEP_AFTER_UPGRADE="conf/app.d conf/ssh modules";
FILES_TO_KEEP_AFTER_UPGRADE="conf/application.conf conf/dependencies.yml conf/log4j.xml";

VERSION="1.0";
THIS_SCRIPT="$0";

# Validated with: shellcheck -s bash -e SC2013 upgrade.bash
set -e

echo "MyDMAM Upgrade script v$VERSION";

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

if [ ! $# -eq 2 ]; then
    usage;
    echo "";
    echo "Error: invalid arguments count supplied" 1>&2
	exit 2;
fi

# Check archive file
if [ ! -f "$ARCHVIVE_FILE" ]; then
    usage;
    echo "";
	echo "Error: $ARCHVIVE_FILE don't exists or it's not a valid file." 1>&2
	exit 2;
fi

# Check if archive file is really... an archive file...
gzip -l "$ARCHVIVE_FILE" > /dev/null

# Check mydmam dir (presence and if it's really mydmam)
if [ ! -d "$MYDMAM_DIR" ]; then
    usage;
    echo "";
	echo "Error: $MYDMAM_DIR don't exists or it's not a directory." 1>&2
	exit 2;
fi
if [ ! -f "$MYDMAM_DIR/version" ]; then
    usage;
    echo "";
	echo "Error: can't found version file in the supplied MyDMAM directory." 1>&2
	exit 2;
fi

# Check if archive file and this script is not localized in old mydmam directory
CURRENT_SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";
ARCHVIVE_FILE_RPATH="$(dirname "$( realpath "$ARCHVIVE_FILE" )")";
MYDMAM_DIR_RPATH=$(realpath "$MYDMAM_DIR");

if [[ $ARCHVIVE_FILE_RPATH == "$MYDMAM_DIR_RPATH"* ]]; then
	echo "You can't use $ARCHVIVE_FILE inside the mydmam directory to upgrade... Copy it outside first." 1>&2
	exit 2;
fi
if [[ $CURRENT_SCRIPT_DIR == "$MYDMAM_DIR_RPATH"* ]]; then
	echo "You can't use $0 inside the mydmam directory to upgrade... Copy it outside first." 1>&2
	exit 2;
fi

ACTUAL_MYDMAM_VERSION=$(cat "$MYDMAM_DIR/version");

# Make temp directory
TMPDIR=$MYDMAM_DIR"-newversion";
if [ -d "$TMPDIR" ]; then
	rm -rf "$TMPDIR"
fi
mkdir -p "$TMPDIR";

# Extract new files to temp directory
tar xfz "$ARCHVIVE_FILE" -C "$TMPDIR"

NEW_MYDMAM_VERSION=$(cat "$TMPDIR/mydmam/version");

echo "Upgrade MyDMAM from $ACTUAL_MYDMAM_VERSION to $NEW_MYDMAM_VERSION";

# Prepare and backup actual version
OLD_BACKUP_DIR=$(dirname "$MYDMAM_DIR");
OLD_BACKUP_DIR="$OLD_BACKUP_DIR/.mydmam-backup";
mkdir -p "$OLD_BACKUP_DIR"
OLD_BACKUP_PREVIOUS_MYDMAM_DIR="$OLD_BACKUP_DIR/mydmam-$ACTUAL_MYDMAM_VERSION-"$(date +"%Y-%m-%d_%H-%M-%S");
echo "Backup actual MyDMAM directory in $OLD_BACKUP_PREVIOUS_MYDMAM_DIR";
mv "$MYDMAM_DIR" "$OLD_BACKUP_PREVIOUS_MYDMAM_DIR"

# Move new files files
mv "$TMPDIR/mydmam" "$MYDMAM_DIR"

echo "Copy actual configuration files/dirs to new the MyDMAM directory:";
# copy conf files and dirs
for dir in $DIRS_TO_KEEP_AFTER_UPGRADE
do
	ACTUAL_DIR="$OLD_BACKUP_PREVIOUS_MYDMAM_DIR/$dir";
	# Test if source dir exists
	if [ -d "$ACTUAL_DIR" ]; then
		NEW_DIR="$MYDMAM_DIR/$dir";
		if [ -d "$NEW_DIR" ]; then
			rm -rf "$NEW_DIR"
		fi
		echo "  $dir"
		cp -r "$ACTUAL_DIR" "$NEW_DIR"
	fi
done

for file in $FILES_TO_KEEP_AFTER_UPGRADE
do
	ACTUAL_FILE="$OLD_BACKUP_PREVIOUS_MYDMAM_DIR/$file";
	# Test if source file exists
	if [ -f "$ACTUAL_FILE" ]; then
		NEW_FILE="$MYDMAM_DIR/$file";
		if [ -f "$NEW_FILE" ]; then
			rm -f "$NEW_FILE"
		fi
		echo "  $file"
		cp "$ACTUAL_FILE" "$NEW_FILE"
	fi
done

rm -rf "$TMPDIR"

echo "Start the new bootstrap script...";
echo "";

if [[ "$UNAME" == 'Linux' ]]; then
	bash "$MYDMAM_DIR/startup/linux-bootstrap.bash"
else
	bash "$MYDMAM_DIR/startup/macos-bootstrap.bash"
fi

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

echo "Upgrade is done."
echo "Please adapt the configuration/setup based on this upgrade notes."
