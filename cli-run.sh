#/bin/sh
# MyDMAM CLI script

set -e

. /etc/default/mydmam

/usr/bin/java -Dfile.encoding=UTF-8 ${CONFIG_PATH} -classpath ${CLASSPATH} hd3gtv.mydmam.cli.MainClass
