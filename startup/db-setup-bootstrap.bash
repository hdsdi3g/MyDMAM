#!/bin/bash
# This file is part of MyDMAM under the LGPL
# Copyright (C) hdsdi3g for hd3g.tv 2017
#
# Databases setup bootstrap
# Tested and functionnal with Debian 8 / Systemd.
# But it can works with others GNU/Linux distribution that support Systemd
# You also can adapt this setup for others NIX OS.

set -e

# Where I am ?
BASEPATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )";

# Load boostrap configuration
. setup.bash
# Load OS/Posix type
UNAME=$(uname);

# Only root can create users/groups
if [[ "$UNAME" == 'Linux' ]]; then
	if [ "$(id -u)" != "0" ]; then
	   echo "This script must be run as root" 1>&2
	   exit 1
	fi
fi

# Get Java JRE
if [[ "$UNAME" == 'Darwin' ]]; then
	JAVA=$(realpath "$JAVA_OSX");
elif [[ "$UNAME" == 'Linux' ]]; then
	JAVA=$(realpath "$JAVA_LINUX");
fi
echo "Test Java JRE: $JAVA"
"$JAVA/bin/java" -version

# Set working directories
if [[ "$UNAME" == 'Linux' ]]; then
	LOG_DIR="/var/log/nosqldb";
	TEMP_DIR="/tmp/nosqldb";
	DATA_DIR="/var/lib/nosqldb";
elif [[ "$UNAME" == 'Darwin' ]]; then
	LOG_DIR="$HOME/nosqldb/log";
	TEMP_DIR="$HOME/nosqldb/tmp";
	DATA_DIR="$HOME/nosqldb/data";
fi

mkdir -p "$LOG_DIR"
mkdir -p "$TEMP_DIR"
mkdir -p "$DATA_DIR"

if [[ "$UNAME" == 'Linux' ]]; then
	echo "Create user and group nosqldb in /opt/nosqldb"
    adduser --system --home "$DATA_DIR" --no-create-home --group nosqldb nosqldb
    chown nosqldb:nosqldb -R "$LOG_DIR"
    chown nosqldb:nosqldb -R "$TEMP_DIR"
    chown nosqldb:nosqldb -R "$DATA_DIR"
fi

# Prepare service declaration files
if [[ "$UNAME" == 'Linux' ]]; then
	CASSANDRA_SERVICE_FILE=$BASEPATH/cassandra.service

	cat <<- EOF > $CASSANDRA_SERVICE_FILE
		# MyDMAM Cassandra configuration file, do not edit. Created by $0
		[Unit]
		Description=Cassandra
		Documentation=http://mydmam.org/
		Wants=network-online.target
		After=network-online.target auditd.service

		[Service]
		Environment=JAVA_HOME=$JAVA
		Type=forking
		PIDFile=$BASEPATH/cassandra.pid
		User=nosqldb
		Group=nosqldb
		ExecStart=$BASEPATH/$CASSANDRA_HOME_NAME/bin/cassandra -p $BASEPATH/cassandra.pid
		StandardOutput=journal
		StandardError=journal
		LimitNOFILE=infinity
		SuccessExitStatus=143
		TimeoutStopSec=30

		[Install]
		WantedBy=multi-user.target
		Alias=cassandra.service
	EOF

	ES_SERVICE_FILE=$BASEPATH/elasticsearch.service

	cat <<- EOF > $ES_SERVICE_FILE
		# MyDMAM Elasticsearch configuration file, do not edit. Created by $0
		[Unit]
		Description=Elasticsearch
		Documentation=http://mydmam.org/
		Wants=network-online.target
		After=network-online.target auditd.service

		[Service]
		Environment=JAVA_HOME=$JAVA
		Environment=ES_HOME=$BASEPATH/$ELASTICSEARCH_HOME_NAME
		Environment=CONF_DIR=$BASEPATH/$ELASTICSEARCH_HOME_NAME/config
		Environment=CONF_FILE=$BASEPATH/$ELASTICSEARCH_HOME_NAME/config/elasticsearch.yml
		Environment=DATA_DIR=$DATA_DIR
		Environment=LOG_DIR=$LOG_DIR
		Environment=PID_DIR=$DATA_DIR
		#EnvironmentFile=-/etc/default/elasticsearch
		User=nosqldb
		Group=nosqldb
		ExecStart=$BASEPATH/$ELASTICSEARCH_HOME_NAME/bin/elasticsearch -Des.pidfile=$DATA_DIR/elasticsearch.pid -Des.default.path.home=$BASEPATH/$ELASTICSEARCH_HOME_NAME -Des.default.path.logs=$LOG_DIR -Des.default.path.data=$DATA_DIR -Des.default.config=$CONF_FILE -Des.default.path.conf=$BASEPATH/$ELASTICSEARCH_HOME_NAME/config/elasticsearch.yml
		StandardOutput=null
		StandardError=journal
		SuccessExitStatus=143
		LimitNOFILE=65535
		#LimitMEMLOCK=infinity
		# Shutdown delay in seconds, before process is tried to be killed with KILL (if configured)
		TimeoutStopSec=30

		[Install]
		WantedBy=multi-user.target
		Alias=elasticsearch.service
	EOF

elif [[ "$UNAME" == 'Darwin' ]]; then
	CASSANDRA_SERVICE_LABEL="org.apache.cassandra";
	CASSANDRA_SERVICE_FILE="$BASEPATH/$CASSANDRA_SERVICE_LABEL.plist";

	cat <<- EOF > $CASSANDRA_SERVICE_FILE
	    <?xml version="1.0" encoding="UTF-8"?>
	    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
	    <plist version="1.0">
	    <dict>
	        <key>Label</key>
	        <string>$CASSANDRA_SERVICE_LABEL</string>
	        <key>ProgramArguments</key>
	        <array>
	            <string>$BASEPATH/$CASSANDRA_HOME_NAME/bin/cassandra</string>
	        </array>
	        <key>StandardOutPath</key>
	        <string>$LOG_DIR/service-cassandra.log</string>
	        <key>StandardErrorPath</key>
	        <string>$LOG_DIR/service-cassandra.log</string>
	        <key>WorkingDirectory</key>
	        <string>$BASEPATH</string>
	        <key>RunAtLoad</key>
	        <true/>
	    </dict>
	    </plist>
	EOF

	ES_SERVICE_LABEL="org.elasticsearch"
	ES_SERVICE_FILE"=$BASEPATH/$ES_SERVICE_LABEL.plist";
	cat <<- EOF > $ES_SERVICE_FILE
	    <?xml version="1.0" encoding="UTF-8"?>
	    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
	    <plist version="1.0">
	    <dict>
	        <key>Label</key>
	        <string>$ES_SERVICE_LABEL</string>
	        <key>ProgramArguments</key>
	        <array>
	            <string>$BASEPATH/$ELASTICSEARCH_HOME_NAME/bin/elasticsearch</string>
	            <string>-Des.default.path.home=$BASEPATH/$ELASTICSEARCH_HOME_NAME</string>
	            <string>-Des.default.path.logs=$LOG_DIR</string>
	            <string>-Des.default.path.data=$DATA_DIR</string>
	            <string>-Des.default.config=$CONF_FILE</string>
	            <string>-Des.default.path.conf=$BASEPATH/$ELASTICSEARCH_HOME_NAME/config/elasticsearch.yml</string>
	        </array>
	        <key>StandardOutPath</key>
	        <string>$LOG_DIR/service-elasticsearch.log</string>
	        <key>StandardErrorPath</key>
	        <string>$LOG_DIR/service-elasticsearch.log</string>
	        <key>WorkingDirectory</key>
	        <string>$BASEPATH</string>
	        <key>RunAtLoad</key>
	        <true/>
	    </dict>
	    </plist>
	EOF
fi

# Get CPU Count
if [[ "$UNAME" == 'Linux' ]]; then
	if [ -f /proc/cpuinfo ]; then
		CPU_COUNT=$(grep -c ^processor /proc/cpuinfo);
	else
		CPU_COUNT=1;
	fi
elif [[ "$UNAME" == 'Darwin' ]]; then
	CPU_COUNT=$(hw.ncpu | cut -d " " -f 2);
fi

# Default values = CPU NUM * 16
CASSANDRA_CONCURENT=$((16 * "$CPU_COUNT"));

# Backup actual configuration
CASSANDRA_CONF_FILE=$BASEPATH/$CASSANDRA_HOME_NAME/conf/cassandra.yaml
ELASTICSEARCH_CONF_FILE=$BASEPATH/$ELASTICSEARCH_HOME_NAME/config/elasticsearch.yaml

mv "$CASSANDRA_CONF_FILE" "$CASSANDRA_CONF_FILE.original"
mv "$ELASTICSEARCH_CONF_FILE" "$ELASTICSEARCH_CONF_FILE.original"

# Create Cassandra configuration file
cat <<- EOF > $CASSANDRA_CONF_FILE
	# MyDMAM autogenerated file
	# YOU MUST SET THE SAME VALUE LIKE IN MyDMAM CONF cassandra.yml (mycluster is the default)
	cluster_name: 'mycluster'

	# YOU MUST SET THE SEED LIST, COMMA SEPARATED: ALL CASSANDRA NODES
	seed_provider:
	    - class_name: org.apache.cassandra.locator.SimpleSeedProvider
	      parameters:
	          - seeds: "127.0.0.1"

	# YOU MUST SET THIS HOST ADDR (THIS CASSANDRA SERVER), or 0.0.0.0
	listen_address: 127.0.0.1
	rpc_address: 127.0.0.1

	data_file_directories:
	    - $DATA_DIR/data
	commitlog_directory: $DATA_DIR/commitlog
	saved_caches_directory: $DATA_DIR/saved_caches
	concurrent_reads: $CASSANDRA_CONCURENT
	concurrent_writes: $CASSANDRA_CONCURENT
	endpoint_snitch: GossipingPropertyFileSnitch
	internode_compression: none
	initial_token:
	hinted_handoff_enabled: true
	hinted_handoff_throttle_in_kb: 1024
	max_hints_delivery_threads: 2
	batchlog_replay_throttle_in_kb: 1024
	authenticator: AllowAllAuthenticator
	authorizer: AllowAllAuthorizer
	permissions_validity_in_ms: 2000
	partitioner: org.apache.cassandra.dht.Murmur3Partitioner
	disk_failure_policy: stop
	key_cache_size_in_mb:
	key_cache_save_period: 14400
	row_cache_size_in_mb: 0
	row_cache_save_period: 0
	row_cache_provider: SerializingCacheProvider
	commitlog_sync: periodic
	commitlog_sync_period_in_ms: 10000
	commitlog_segment_size_in_mb: 32
	flush_largest_memtables_at: 0.75
	reduce_cache_sizes_at: 0.85
	reduce_cache_capacity_to: 0.6
	memtable_flush_queue_size: 4
	trickle_fsync: false
	trickle_fsync_interval_in_kb: 10240
	storage_port: 7000
	ssl_storage_port: 7001
	start_native_transport: true
	native_transport_port: 9042
	start_rpc: true
	rpc_port: 9160
	rpc_keepalive: true
	rpc_server_type: sync
	thrift_framed_transport_size_in_mb: 15
	incremental_backups: false
	snapshot_before_compaction: false
	auto_snapshot: true
	tombstone_debug_threshold: 10000
	column_index_size_in_kb: 64
	in_memory_compaction_limit_in_mb: 64
	multithreaded_compaction: false
	compaction_throughput_mb_per_sec: 16
	compaction_preheat_key_cache: true
	read_request_timeout_in_ms: 10000
	range_request_timeout_in_ms: 10000
	write_request_timeout_in_ms: 10000
	truncate_request_timeout_in_ms: 60000
	request_timeout_in_ms: 10000
	cross_node_timeout: false
	dynamic_snitch_update_interval_in_ms: 100
	dynamic_snitch_reset_interval_in_ms: 600000
	dynamic_snitch_badness_threshold: 0.1
	request_scheduler: org.apache.cassandra.scheduler.NoScheduler
	index_interval: 128
	server_encryption_options:
	    internode_encryption: none
	    keystore: conf/.keystore
	    keystore_password: cassandra
	    truststore: conf/.truststore
	    truststore_password: cassandra
	client_encryption_options:
	    enabled: false
	    keystore: conf/.keystore
	    keystore_password: cassandra
	inter_dc_tcp_nodelay: true
EOF

# Create Elasticsearch configuration file
cat <<- EOF > $ELASTICSEARCH_CONF_FILE
	# MyDMAM autogenerated file
	# YOU MUST SET THE SAME VALUE LIKE IN MyDMAM CONF elasticsearch.yml (escluster is the default)
	cluster.name: escluster

	# You should set this host addr (this Elasticsearch server)
	network.host: 127.0.0.1

	# You can set a seed list with all Elasticsearch nodes.
	# Or let the "multicast discover" doing this for you.
	# discovery.zen.ping.multicast.enabled: false
	# discovery.zen.ping.unicast.hosts: ["host1", "host2:port"]

	# You should set a node name if you don't like Star Trek characters names
	# node.name: "esnode"

	path.conf: $BASEPATH/$ELASTICSEARCH_HOME_NAME/config
	path.data: $DATA_DIR
	path.work: $TEMP_DIR
	path.logs: $LOG_DIR
	</echo>
EOF

echo "=== COMPLETED ==="
echo "By default this script don't enable Cassandra or Elasticsearch services"
echo "Please check Cassandra and/or Elasticsearch configuration files before start:";
echo " - $CASSANDRA_CONF_FILE";
echo " - $ELASTICSEARCH_CONF_FILE";
echo "If you needs to use java or Cassandra ou Elasticsearch CLI tools on this shell,";
echo "you should add in you .bashrc/.profile: JAVA_HOME=$JAVA";
echo "";
echo "Run Cassandra and Elasticsearch in the same server only for testing pupose.";
echo "Run two server for each database type is the prerequisite for a good production setup.";
echo "";
echo "=== Service instructions ===";
if [[ "$UNAME" == 'Linux' ]]; then
	echo "Declare service:";
	echo "   cp $CASSANDRA_SERVICE_FILE /usr/lib/systemd/.service";
	echo "   cp $ES_SERVICE_FILE /usr/lib/systemd/elasticsearch.service";
	echo "Update services status:";
	echo "   systemctl daemon-reload";
	echo "Activate on-boot service";
	echo "   systemctl enable /usr/lib/systemd/cassandra.service";
	echo "   systemctl enable /usr/lib/systemd/elasticsearch.service";
	echo "Desactivate on-boot service";
	echo "   systemctl disable /usr/lib/systemd/cassandra.service";
	echo "   systemctl disable /usr/lib/systemd/elasticsearch.service";
	echo "Start/stop";
	echo "   systemctl start cassandra";
	echo "   systemctl start elasticsearch";
	echo "   systemctl stop cassandra";
	echo "   systemctl stop elasticsearch";
	echo "Status";
	echo "   systemctl status cassandra";
	echo "   systemctl status elasticsearch";
elif [[ "$UNAME" == 'Darwin' ]]; then
	SETUP_SERVICE_BASE_DIR_CASSANDRA="$HOME/Library/LaunchAgents/"$(basename "$CASSANDRA_SERVICE_FILE");
	SETUP_SERVICE_BASE_DIR_ES="$HOME/Library/LaunchAgents/"$(basename "$ES_SERVICE_FILE");

	echo "Declare service:";
	echo "   cp $CASSANDRA_SERVICE_FILE $SETUP_SERVICE_BASE_DIR_CASSANDRA";
	echo "   cp $ES_SERVICE_FILE $SETUP_SERVICE_BASE_DIR_ES";
	echo "Activate on-boot service";
	echo "   launchctl load -w $CASSANDRA_SERVICE_FILE";
	echo "   launchctl load -w $ES_SERVICE_FILE";
	echo "Desactivate on-boot service";
	echo "   launchctl unload -w $CASSANDRA_SERVICE_FILE";
	echo "   launchctl unload -w $ES_SERVICE_FILE";
	echo "Start/stop";
	echo "   launchctl start $CASSANDRA_SERVICE_LABEL";
	echo "   launchctl stop $CASSANDRA_SERVICE_LABEL";
	echo "   launchctl start $ES_SERVICE_LABEL";
	echo "   launchctl stop $ES_SERVICE_LABEL";
	echo "Status";
	echo "   launchctl list | grep $CASSANDRA_SERVICE_LABEL";
	echo "   launchctl list | grep $ES_SERVICE_LABEL";
fi
echo "=== CLI Instructions ===";
echo "Before to start something: export JAVA_HOME=$JAVA";

CHOWNUSER="";
if [[ "$UNAME" == 'Linux' ]]; then
	CHOWNUSER="runuser -u nosqldb";
fi

echo "You can start Elasticsearch with:";
echo "  $CHOWNUSER $BASEPATH/$ELASTICSEARCH_HOME_NAME/bin/elasticsearch ";
echo "You can install head plugin for Elasticsearch:";
echo "  $CHOWNUSER $BASEPATH/$ELASTICSEARCH_HOME_NAME/bin/plugin --install head";
echo "You can start Cassandra with:";
echo "  $CHOWNUSER $BASEPATH/$CASSANDRA_HOME_NAME/bin/cassandra";
echo "You can start Cassandra CLI with:";
echo "  $CHOWNUSER $BASEPATH/$CASSANDRA_HOME_NAME/bin/cassandra-cli --host localhost";
echo "You can start Cassandra nodetool status tool with:";
echo "  $CHOWNUSER $BASEPATH/$CASSANDRA_HOME_NAME/bin/nodetool status";

echo "=== Configuration variables ===";
echo "CPU_COUNT=$CPU_COUNT, DATA_DIR=$DATA_DIR, TEMP_DIR=$TEMP_DIR, LOG_DIR=$LOG_DIR";
