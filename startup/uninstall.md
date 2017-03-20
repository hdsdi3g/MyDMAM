# Uninstall

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
