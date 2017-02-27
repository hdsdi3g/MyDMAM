#/bin/sh
# Only used for test service
set -e

. /etc/default/mydmam

cd $BASEPATH

/usr/bin/java -noverify -server -Dfile.encoding=UTF-8 ${CONFIG_PATH} -classpath ${CLASSPATH} hd3gtv.mydmam.probe.MainClass

