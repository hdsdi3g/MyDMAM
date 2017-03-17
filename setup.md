# MyDMAM Setup instructions
TODO

Setup instructions can be found in the [MyDMAM site](http://mydmam.org/setup/). **Take a look before continuing.** You should download [prepared packs](http://mydmam.org/downloads/).

TODO
Don't forget to read best setup practices before start MyDMAM in production. > site

TODO security (FW / auth / pws / access control) > site

## Build MyDMAM

More informations in _build.md_.

 * Install Apache ant
 * Install a Java JDK 8
 * Install git
 * Git clone from GitHub: https://github.com/hdsdi3g/MyDMAM.git
 * ant build
 * ant pack-databases

## Configure Databases

 * Unpack mydmam-databases archive **tar xvfz mydmam-databases**... or with 7zip.
 * You can move mydmam-databases to a better directory like /opt/mydmam-databases or c:\mydmam-databases
 * Go to unpacked mydmam-databases directory.
 * Switch to root rights.
 * # bash bootstrap.bash



edit conf
try to start
up services
check up

		TODO DOC:
		== Cassandra
		 - edit log4j-server.properties (log4j.appender.R.File) to /var/log/nosqldb/ and conf/cassandra.yaml
		 - Windows, get instructions for create service in bin/cassandra.bat (prunsrv.exe will be founded a the root directory)
		 - Windows, set JAVA_HOME to jre-windows/jre1.x.0_xxx
		log in /var/log/nosqldb/system.log
		  INFO [Thread-2] 0000-00-00 00:00:00,000 ThriftServer.java (line 110) Listening for thrift clients...
		== ES
		 - edit /config/elasticsearch.yml
		 - Windows, set JAVA_HOME to jre-windows/jre1.x.0_xxx
		 - Windows, use /bin/elasticsearch-service-x64.exe for create service
		 log in /var/log/nosqldb/escluster.log
		 install Head + http://<addr>:9200/_plugin/head/

 * You can delete unused jre (example: remove jre-macos and jre-windows directories on linux server)
 * You can delete unused database main directory (example: remove apache-cassandra directory on elasticsearch only setup).


## Configure MyDMAM

bash bootstrap / windows

cd startup
bash linux-bootstrap.bash ou macos-bootstrap.bash 
conf
cli
`ln -s $BASEPATH/startup/mydmam-cli.bash /bin/mydmam`
startup with cli
set service
start service

## Configure external tools
Not mantatory

ffmpeg + IM + bmx

## Remove C* ES
systemctl stop elasticsearch
systemctl stop cassandra
systemctl disable /usr/lib/systemd/cassandra.service
systemctl disable /usr/lib/systemd/elasticsearch.service
deluser nosqldb
rm -rf /var/log/nosqldb
rm -rf /var/lib/nosqldb
rm -rf /tmp/nosqldb
rm /usr/lib/systemd/elasticsearch.service
rm /usr/lib/systemd/cassandra.service
rm -rf /opt/mydmam-databases

## Remove MyDMAM
systemctl stop mydmam
systemctl disable /usr/lib/systemd/mydmam.service
deluser mydmam
rm -rf  /var/log/mydmam/
rm -rf /opt/mydmam
