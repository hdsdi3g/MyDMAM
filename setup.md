# MyDMAM Setup instructions
TODO

Setup instructions can be found in the [MyDMAM site](http://mydmam.org/setup/). **Take a look before continuing.**

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

go to dir
move dir
bash bootstrap.bash
edit conf
try to start
up services
check up

		TODO DOC:
		== Cassandra
		 - edit log4j-server.properties (log4j.appender.R.File) and conf/cassandra.yaml
		 - Windows, get instructions for create service in bin/cassandra.bat (prunsrv.exe will be founded a the root directory)
		 - Windows, set JAVA_HOME to jre-windows/jre1.x.0_xxx
		== ES
		 - edit /config/elasticsearch.yml
		 - Windows, set JAVA_HOME to jre-windows/jre1.x.0_xxx
		 - Windows, use /bin/elasticsearch-service-x64.exe for create service

## Configure MyDMAM

bash bootstrap / windows
conf
cli
`ln -s $BASEPATH/startup/mydmam-cli.bash /bin/mydmam`
startup with cli
set service
start service

## Configure external tools
Not mantatory

ffmpeg + IM + bmx
