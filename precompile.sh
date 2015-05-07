#!/bin/sh

set -e

BASEPATH="/opt/mydmam"; # DIRECTORY WHERE IS APP
PLAYPATH="/opt/play1-1.3.1";  # DIRECTORY WHERE PLAY
PLAY_JAR="play-1.3.1.jar";

# If you change this, don't forget to update EnvironmentFile var for all *.service
# and update this Systemd files version
DEST_DEFAULT="/etc/default/mydmam";

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

cd $BASEPATH

$PLAYPATH/play clean --silent $BASEPATH
$PLAYPATH/play precompile --silent $BASEPATH

echo "# MyDMAM Configuration file, do not edit. Created by $0" > $DEST_DEFAULT
echo "BASEPATH=$BASEPATH" >> $DEST_DEFAULT
echo "CONFIG_PATH=\"-Dservice.config.path=$BASEPATH/conf/app.d\"" >> $DEST_DEFAULT
echo "CLASSPATH=$CLASSPATH" >> $DEST_DEFAULT
echo "PLAYPATH=$PLAYPATH" >> $DEST_DEFAULT

systemctl daemon-reload

chmod +x $PLAYPATH/play
ln -sf $PLAYPATH/play /bin/play
