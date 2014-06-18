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

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

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
import com.netflix.astyanax.impl.AstyanaxConfigurationImpl;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.partitioner.Murmur3Partitioner;
import com.netflix.astyanax.recipes.reader.AllRowsReader;
import com.netflix.astyanax.serializers.StringSerializer;
import com.netflix.astyanax.thrift.ThriftFamilyFactory;

public class CassandraDb {
	
	static Cluster cluster;
	private static List<ConfigurationClusterItem> clusterservers;
	private static Builder builder;
	static String keyspacename;
	
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
			keyspacename = Configuration.global.getValue("cassandra", "keyspace", null);
			
			Log2Dump dump = new Log2Dump();
			dump.add("clustername", clustername);
			for (ConfigurationClusterItem item : clusterservers) {
				dump.addAll(item);
			}
			dump.add("keyspacename", keyspacename);
			Log2.log.info("Cassandra client configuration", dump);
			
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
			configurationimpl.setRetryPolicy(new BoundedExponentialBackoffLog(100, 30000, 20));
			
			builder = new AstyanaxContext.Builder().forCluster(clustername);
			builder.withAstyanaxConfiguration(configurationimpl);
			builder.withConnectionPoolConfiguration(connexionpool);
			builder.withConnectionPoolMonitor(new CountingConnectionPoolMonitor());
			AstyanaxContext<Cluster> contextcluster = builder.buildCluster(ThriftFamilyFactory.getInstance());
			contextcluster.start();
			cluster = contextcluster.getClient();
			
		} catch (Exception e) {
			Log2.log.error("Can't load Cassandra client configuration", e);
		}
	}
	
	public static Keyspace getkeyspace() throws ConnectionException {
		if (isKeyspaceExists(keyspacename) == false) {
			createKeyspace(keyspacename);
		}
		return cluster.getKeyspace(keyspacename);
	}
	
	public static Keyspace getkeyspace(String keyspacename) throws ConnectionException {
		if (isKeyspaceExists(keyspacename) == false) {
			createKeyspace(keyspacename);
		}
		return cluster.getKeyspace(keyspacename);
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
		keyspace.createKeyspace(ImmutableMap.<String, Object> builder().put("strategy_options", ImmutableMap.<String, Object> builder().put("replication_factor", "1").build())
				.put("strategy_class", "SimpleStrategy").build());
		cluster.getKeyspace(keyspacename).describeKeyspace();
		Log2.log.info("Create Keyspace", new Log2Dump("keyspacename", keyspacename));
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
	
	public static void createColumnFamilyString(Keyspace keyspace, String cfname) throws ConnectionException {
		ColumnFamily<String, String> cf = ColumnFamily.newColumnFamily(cfname, StringSerializer.get(), StringSerializer.get());
		keyspace.createColumnFamily(cf, null);
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
			if (e.getMessage().endsWith("InvalidRequestException(why:Keyspace '" + keyspacename + "' does not exist)")) {
				Log2.log.info("Create keyspace");
				createKeyspace(keyspacename);
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
