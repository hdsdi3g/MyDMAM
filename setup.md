# MyDMAM Setup instructions
TODO

Setup instructions can be found in the [MyDMAM site](http://mydmam.org/setup/). **Take a look before continuing.** You should download [prepared packs](http://mydmam.org/downloads/).

Don't forget to read [best setup practices](http://mydmam.org/setup/performance-tips-for-production/) before start MyDMAM in production. Also read [security recommendations](http://mydmam.org/setup/security-recommendations/).

TODO add (FW + access control) > security

## Configure Databases

 * Unpack mydmam-databases archive **tar xvfz mydmam-databases**... or with 7zip.
 * You can move mydmam-databases to a better directory like /opt/mydmam-databases or c:\mydmam-databases
 * Go to unpacked mydmam-databases directory.
 * Switch to root rights.
 * `# bash bootstrap.bash`
 * Read script output
 * Edit Cassandra configuration (in conf/cassandra.yml).
 * Edit Cassandra log configuration (in conf/log4j-server.properties). Edit `log4j.appender.R.File` to `/var/log/nosqldb/cassandra.log` (for Linux, adapt it for macOS and Windows).
 * Edit Elasticsearch log configuration (in conf/log4j-
 - edit /config/elasticsearch.yml

### Windows services
Set JAVA_HOME to (this directory)/jre-windows/jre1.x.0_xxx in system variables.

Get instructions for create service in `bin/cassandra.bat` (prunsrv.exe will be founded a the root directory).

Use `/bin/elasticsearch-service-x64.exe` for create Elasticsearch service

### Linux services

If you needs, absolutely start Cassandra and Elasticsearch in the same server, remove this lines in `cassandra.service` file:

```
LimitNOFILE=100000
LimitMEMLOCK=infinity
LimitNPROC=32768
LimitAS=infinity
```

 * Try to start servers in command line, like the script tell you how to do.
 * You should install Head Plugin for each Elasticsearch node. Head can be access to http://<addr>:9200/_plugin/head/
 * Setup and start services again like the script tell you how to do.
 * After start, Cassandra should display in it's log a kind of `INFO [Thread-2] 0000-00-00 00:00:00,000 ThriftServer.java (line 110) Listening for thrift clients...`
 * You can found Elasticsearch in `/var/log/nosqldb/escluster.log` for Linux.
 * Why not check Cassandra run with a nodetool call ? 
 * You can delete unused jre (example: remove jre-macos and jre-windows directories on linux server)
 * You can delete unused database main directory (example: remove apache-cassandra directory on elasticsearch only setup).

Don't forget to check NTP status for all servers, with Debian it's not automatic:

```
echo "Servers=0.debian.pool.ntp.org 1.debian.pool.ntp.org 2.debian.pool.ntp.org 3.debian.pool.ntp.org" >> /etc/systemd/timesyncd.conf
systemctl start systemd-timesyncd.service
systemctl enable systemd-timesyncd.service
systemctl status systemd-timesyncd.service
```

## Configure MyDMAM

After unpack MyDMAM archive OR do an ant build:

* Windows: use goto in scripts/*.bat / *.exe
* macOS / Linux, `cd startup`
* `bash linux-bootstrap.bash` OR `macos-bootstrap.bash`
* If this instance not needs a HTTP Server (web interface), remove  `/conf/app.d/play.yml`
* Check configuration files in `/conf/app.d`, setup databases addresses, master password key, your mail...
* Test a startup with cli, like the script tell you to do.
* For Linux, you can setup cli in /bin directory for get in in path `ln -s (here)/startup/mydmam-cli.bash /bin/mydmam`
* Activate service (see .bash files) and start it.
* Keep an eye to logs. Log file are declared in `/conf/log4j.xml', and only this file.
* For web interface, Play Server needs some seconds to start, so wait for it.


## Configure external tools

Not mantatory.

Comp ffmpeg + IM + bmx + ntp

## Build and configure MyDMAM

In the case if you want build yourself a MyDMAM.

More informations in _build.md_.

 * Install Apache ant
 * Install a Java JDK 8
 * Install git
 * Git clone from GitHub: https://github.com/hdsdi3g/MyDMAM.git
 * ant build
 * ant pack-databases

## Uninstall

On macOS, no system change are made, just cut off services.
On Windows, no easy-setup ar made, just cut off services and remove JAVA_HOME.
On Linux, you can:

### Remove Cassandra and Elasticsearch

```
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
```

### Remove MyDMAM

```
systemctl stop mydmam
systemctl disable /usr/lib/systemd/mydmam.service
deluser mydmam
rm -rf  /var/log/mydmam/
rm -rf /opt/mydmam
```
