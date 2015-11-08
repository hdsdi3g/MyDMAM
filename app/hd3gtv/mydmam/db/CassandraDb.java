/*
 * This file is part of MyDMAM.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2014
 * 
*/
package hd3gtv.mydmam.db;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.netflix.astyanax.AstyanaxContext;
import com.netflix.astyanax.AstyanaxContext.Builder;
import com.netflix.astyanax.Cluster;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.NodeDiscoveryType;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.impl.ConnectionPoolConfigurationImpl;
import com.netflix.astyanax.connectionpool.impl.CountingConnectionPoolMonitor;
import com.netflix.astyanax.ddl.ColumnDefinition;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.partitioner.Murmur3Partitioner;
import com.netflix.astyanax.recipes.reader.AllRowsReader;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.mydmam.Loggers;

public class CassandraDb {
	
	static Cluster cluster;
	private static List<ConfigurationClusterItem> clusterservers;
	private static Builder builder;
	static String default_keyspacename;
	
	public static String getDefaultKeyspacename() {
		return default_keyspacename;
	}
	
	private static int initcount = 0;
	
	static {
		resetinit();
	}
	
	private static void resetinit() {
		initcount++;
		
		/**
		 * Start of xml hooks configuration
		 */
		try {
			if (Configuration.global.isElementExists("cassandra") == false) {
				throw new Exception("Can't found Cassandra configuration");
			}
			
			String clustername = Configuration.global.getValue("cassandra", "clustername", null);
			clusterservers = Configuration.global.getClusterConfiguration("cassandra", "rcp_cluster", "127.0.0.1", 9160);
			default_keyspacename = Configuration.global.getValue("cassandra", "keyspace", null);
			
			if (Loggers.Cassandra.isInfoEnabled()) {
				StringBuilder sb = new StringBuilder();
				sb.append("Cassandra client configuration, keyspace: ");
				sb.append(default_keyspacename);
				sb.append(", clustername: ");
				sb.append(clustername);
				for (ConfigurationClusterItem item : clusterservers) {
					sb.append(", ");
					sb.append(item.address);
					sb.append(":");
					sb.append(item.port);
				}
				Loggers.Cassandra.info(sb.toString().trim());
			}
			
			ConnectionPoolConfigurationImpl connexionpool = new ConnectionPoolConfigurationImpl("mydmam-" + clustername);
			connexionpool.setPort(9160);
			
			StringBuffer sb = new StringBuffer();
			for (ConfigurationClusterItem item : clusterservers) {
				sb.append(item.address);
				sb.append(":");
				sb.append(item.port);
				sb.append(",");
			}
			connexionpool.setSeeds(sb.toString().substring(0, sb.toString().length() - 1));
			
			// connexionpool.setTimeoutWindow(connexionpool.getTimeoutWindow() * 2);
			// connexionpool.setSocketTimeout(connexionpool.getSocketTimeout() * 2);
			// connexionpool.setMaxTimeoutCount(connexionpool.getMaxTimeoutCount() * 2);
			
			AstyanaxConfigurationImpl configurationimpl = new AstyanaxConfigurationImpl();
			configurationimpl.setDiscoveryType(NodeDiscoveryType.NONE);
			configurationimpl.setTargetCassandraVersion("1.2");
			configurationimpl.setRetryPolicy(new BoundedExponentialBackoffLog(default_keyspacename, 100, 30000, 20));
			
			builder = new AstyanaxContext.Builder().forCluster(clustername);
			builder.withAstyanaxConfiguration(configurationimpl);
			builder.withConnectionPoolConfiguration(connexionpool);
			builder.withConnectionPoolMonitor(new CountingConnectionPoolMonitor());
			AstyanaxContext<Cluster> contextcluster = builder.buildCluster(ThriftFamilyFactory.getInstance());
			contextcluster.start();
			cluster = contextcluster.getClient();
			
		} catch (Exception e) {
			Loggers.Cassandra.error("Can't load Cassandra client configuration", e);
		}
	}
	
	public static Keyspace getkeyspace() throws ConnectionException {
		if (isKeyspaceExists(default_keyspacename) == false) {
			createKeyspace(default_keyspacename);
		}
		return cluster.getKeyspace(default_keyspacename);
	}
	
	public static Keyspace getkeyspace(String keyspacename) throws ConnectionException {
		if (isKeyspaceExists(keyspacename) == false) {
			createKeyspace(keyspacename);
		}
		return cluster.getKeyspace(keyspacename);
	}
	
	public static List<KeyspaceDefinition> getAllKeyspaces() throws ConnectionException {
		return cluster.describeKeyspaces();
	}
	
	public static void deleteKeyspace(String keyspacename) throws ConnectionException {
		cluster.dropKeyspace(keyspacename);
	}
	
	public static boolean isKeyspaceExists(String keyspacename) throws ConnectionException {
		try {
			cluster.getKeyspace(keyspacename).describeKeyspace();
		} catch (ConnectionException e) {
			if (e.getMessage().endsWith("InvalidRequestException(why:Keyspace '" + keyspacename + "' does not exist)")) {
				return false;
			}
			throw e;
		}
		return true;
	}
	
	public static void createKeyspace(String keyspacename) throws ConnectionException {
		AstyanaxContext<Keyspace> ctx = builder.forKeyspace(keyspacename).buildKeyspace(ThriftFamilyFactory.getInstance());
		ctx.start();
		Keyspace keyspace = ctx.getClient();
		
		keyspace.createKeyspace(ImmutableMap.<String, Object> builder()
				.put("strategy_options",
						ImmutableMap.<String, Object> builder().put("replication_factor", String.valueOf(Configuration.global.getValue("cassandra", "default_replication_factor", 1))).build())
				.put("strategy_class", "SimpleStrategy").build());
				
		cluster.getKeyspace(keyspacename).describeKeyspace();
		Loggers.Cassandra.info("Create Keyspace " + keyspacename);
	}
	
	public static MutationBatch prepareMutationBatch() throws ConnectionException {
		return getkeyspace().prepareMutationBatch();
	}
	
	public static MutationBatch prepareMutationBatch(String keyspacename) throws ConnectionException {
		return getkeyspace(keyspacename).prepareMutationBatch();
	}
	
	public static boolean isColumnFamilyExists(Keyspace keyspace, String cfname) throws ConnectionException {
		return (keyspace.describeKeyspace().getColumnFamily(cfname) != null);
	}
	
	/**
	 * @param has_long_grace_period Set true (3 day) if datas are not continually refreshed. False (600 sec) if CF will be always small (like a lock table).
	 */
	public static void createColumnFamilyString(String keyspacename, String cfname, boolean has_long_grace_period) throws ConnectionException {
		Keyspace keyspace = getkeyspace(keyspacename);
		Loggers.Cassandra.info("Create ColumnFamily " + cfname + " in " + keyspace.getKeyspaceName());
		ColumnFamily<String, String> cf = ColumnFamily.newColumnFamily(cfname, StringSerializer.get(), StringSerializer.get());
		keyspace.createColumnFamily(cf, null);
		
		try {
			for (int pos = 0; pos < 10000; pos++) {
				if (CassandraDb.isColumnFamilyExists(keyspace, cfname)) {
					break;
				}
				Thread.sleep(1);
			}
		} catch (InterruptedException e) {
		}
		
		keyspace = getkeyspace(keyspacename);
		Map<String, Object> metadatas = new HashMap<String, Object>();
		if (has_long_grace_period) {
			metadatas.put("gc_grace_seconds", 3 * 24 * 3600);
		} else {
			metadatas.put("gc_grace_seconds", 600);
		}
		keyspace.updateColumnFamily(cf, metadatas);
		
		Loggers.Cassandra.info("ColumnFamily " + cfname + " is created");
	}
	
	public static void truncateColumnFamilyString(Keyspace keyspace, String cfname) throws ConnectionException {
		keyspace.truncateColumnFamily(cfname);
	}
	
	public static void dropColumnFamilyString(Keyspace keyspace, String cfname) throws ConnectionException {
		keyspace.dropColumnFamily(cfname);
	}
	
	public static void declareIndexedColumn(Keyspace keyspace, ColumnFamily<?, ?> columnfamily, String columnname, String indexname, String validationclass) throws ConnectionException {
		String validationclassname = null;
		if (validationclass.indexOf(".") > -1) {
			validationclassname = validationclass.substring(validationclass.lastIndexOf(".") + 1);
		} else {
			validationclassname = validationclass;
		}
		
		/**
		 * Get the existing configuration
		 */
		ColumnFamilyDefinition cf_definition = keyspace.describeKeyspace().getColumnFamily(columnfamily.getName());
		
		List<ColumnDefinition> collist = cf_definition.getColumnDefinitionList();
		Map<String, Object> column_metadata = new HashMap<String, Object>();
		for (int pos = 0; pos < collist.size(); pos++) {
			Map<String, Object> def = new HashMap<String, Object>();
			if (collist.get(pos).getIndexName() != null) {
				if (collist.get(pos).getValidationClass() != null) {
					def.put("validation_class", collist.get(pos).getValidationClass());
				}
				def.put("index_name", collist.get(pos).getIndexName());
				def.put("index_type", collist.get(pos).getIndexType());
				column_metadata.put(collist.get(pos).getName(), def);
			}
		}
		/**
		 * Overwrite new configuration
		 */
		Map<String, Object> def = new HashMap<String, Object>();
		def.put("validation_class", validationclassname);
		def.put("index_name", indexname);
		def.put("index_type", "KEYS");
		column_metadata.put(columnname, def);
		
		/**
		 * Prepare to push
		 */
		Map<String, Object> metadatas = new HashMap<String, Object>();
		metadatas.put("gc_grace_seconds", cf_definition.getGcGraceSeconds());
		metadatas.put("default_validation_class", cf_definition.getDefaultValidationClass());
		metadatas.put("comparator_type", cf_definition.getComparatorType());
		if (column_metadata.size() > 0) {
			metadatas.put("column_metadata", column_metadata);
		}
		
		/**
		 * Push
		 */
		keyspace.updateColumnFamily(columnfamily, metadatas);
		
	}
	
	/**
	 * Test if database is ok, else create keyspace and CF as needed
	 */
	public static void autotest() throws Exception {
		if (initcount > 1) {
			/**
			 * If you have already initialized the class
			 */
			resetinit();
		}
		
		Keyspace keyspace = getkeyspace();
		try {
			keyspace.describeKeyspace();
		} catch (ConnectionException e) {
			if (e.getMessage().endsWith("InvalidRequestException(why:Keyspace '" + default_keyspacename + "' does not exist)")) {
				Loggers.Cassandra.info("Create keyspace " + default_keyspacename);
				createKeyspace(default_keyspacename);
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * @param columns not mandatory
	 */
	public static boolean allRowsReader(ColumnFamily<String, String> columnfamily, AllRowsFoundRow handler, String... columns) throws Exception {
		AllRowsHandlerBridge handler_bridge = new AllRowsHandlerBridge(handler);
		AllRowsReader.Builder<String, String> allrowsreaderbuilder = new AllRowsReader.Builder<String, String>(getkeyspace(), columnfamily);
		allrowsreaderbuilder.withPartitioner(Murmur3Partitioner.get());
		allrowsreaderbuilder.forEachRow(handler_bridge);
		if (columns != null) {
			if (columns.length > 0) {
				allrowsreaderbuilder.withColumnSlice(columns);
			}
		}
		allrowsreaderbuilder.withIncludeEmptyRows(false);
		return allrowsreaderbuilder.build().call();
	}
	
}
