/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.cli;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.tools.ApplicationArgs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.TokenRange;
import com.netflix.astyanax.ddl.ColumnDefinition;
import com.netflix.astyanax.ddl.ColumnFamilyDefinition;
import com.netflix.astyanax.ddl.KeyspaceDefinition;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;

public class CliModuleOperateDatabase implements CliModule {
	
	public String getCliModuleName() {
		return "actiondb";
	}
	
	public String getCliModuleShortDescr() {
		return "Do actions to databases";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-cassandra")) {
			if (args.getParamExist("-keyspacesdef")) {
				List<KeyspaceDefinition> kd_def = CassandraDb.getAllKeyspaces();
				System.out.println("List all Keyspaces in cluster:");
				for (int pos = 0; pos < kd_def.size(); pos++) {
					System.out.println(" - " + kd_def.get(pos).getName());
					System.out.print("   strategy: " + kd_def.get(pos).getStrategyClass());
					System.out.println(" with " + kd_def.get(pos).getStrategyOptions());
				}
				return;
			}
			
			String deletekeyspace = args.getSimpleParamValue("-deletekeyspace");
			if (deletekeyspace != null) {
				CassandraDb.deleteKeyspace(deletekeyspace);
			}
			
			String keyspacename = args.getSimpleParamValue("-keyspace");
			
			Keyspace keyspace = null;
			if (keyspacename != null) {
				keyspace = CassandraDb.getkeyspace(keyspacename);
			} else {
				keyspace = CassandraDb.getkeyspace();
			}
			if (keyspace == null) {
				throw new NullPointerException("Unknown keyspace " + keyspacename);
			}
			
			String truncate = args.getSimpleParamValue("-truncate");
			if (truncate != null) {
				CassandraDb.truncateColumnFamilyString(keyspace, truncate);
				return;
			}
			
			String delete = args.getSimpleParamValue("-delete");
			if (delete != null) {
				CassandraDb.dropColumnFamilyString(keyspace, delete);
				return;
			}
			
			String describe = args.getSimpleParamValue("-describe");
			if (describe != null) {
				ColumnFamilyDefinition cfd = keyspace.describeKeyspace().getColumnFamily(describe);
				
				if (cfd == null) {
					System.err.println("Column family not found...");
					return;
				}
				
				List<ColumnDefinition> collist = cfd.getColumnDefinitionList();
				if (collist.size() > 0) {
					System.out.println("Column definition:");
					ColumnDefinition cd;
					for (int pos = 0; pos < collist.size(); pos++) {
						cd = collist.get(pos);
						System.out.print(" \"");
						System.out.print(cd.getName());
						System.out.print("\" validclass:");
						System.out.print(cd.getValidationClass());
						System.out.print(" idxname:");
						System.out.print(cd.getIndexName());
						System.out.print(" idxtype:");
						System.out.print(cd.getIndexType());
						System.out.println();
						
						if (cd.getOptions() != null) {
							for (Map.Entry<String, String> entry : cd.getOptions().entrySet()) {
								System.out.print(" Option: ");
								System.out.print(entry.getKey());
								System.out.print(" => ");
								System.out.print(entry.getValue());
								System.out.println();
							}
						}
					}
				}
				
				System.out.println();
				System.out.print(" getName: ");
				System.out.println(cfd.getName());
				System.out.print(" getId: ");
				System.out.println(cfd.getId());
				System.out.print(" getComment: ");
				System.out.println(cfd.getComment());
				System.out.print(" getCaching: ");
				System.out.println(cfd.getCaching());
				System.out.print(" getCompactionStrategy: ");
				System.out.println(cfd.getCompactionStrategy());
				System.out.print(" getComparatorType: ");
				System.out.println(cfd.getComparatorType());
				System.out.print(" getDefaultValidationClass: ");
				System.out.println(cfd.getDefaultValidationClass());
				System.out.print(" getCompactionStrategyOptions: ");
				System.out.println(cfd.getCompactionStrategyOptions());
				System.out.print(" getCompressionOptions: ");
				System.out.println(cfd.getCompressionOptions());
				System.out.print(" getKeyValidationClass: ");
				System.out.println(cfd.getKeyValidationClass());
				System.out.print(" getKeyCacheSavePeriodInSeconds: ");
				System.out.println(cfd.getKeyCacheSavePeriodInSeconds());
				System.out.print(" getKeyCacheSize: ");
				System.out.println(cfd.getKeyCacheSize());
				System.out.print(" getKeyValidationClass: ");
				System.out.println(cfd.getKeyValidationClass());
				System.out.print(" getGcGraceSeconds: ");
				System.out.println(cfd.getGcGraceSeconds());
				System.out.print(" getLocalReadRepairChance: ");
				System.out.println(cfd.getLocalReadRepairChance());
				System.out.print(" getBloomFilterFpChance: ");
				System.out.println(cfd.getBloomFilterFpChance());
				System.out.print(" getMergeShardsChance: ");
				System.out.println(cfd.getMergeShardsChance());
				System.out.print(" getReadRepairChance: ");
				System.out.println(cfd.getReadRepairChance());
				System.out.print(" getMinCompactionThreshold: ");
				System.out.println(cfd.getMinCompactionThreshold());
				System.out.print(" getMaxCompactionThreshold: ");
				System.out.println(cfd.getMaxCompactionThreshold());
				System.out.print(" getReplicateOnWrite: ");
				System.out.println(cfd.getReplicateOnWrite());
				System.out.print(" getRowCacheProvider: ");
				System.out.println(cfd.getRowCacheProvider());
				System.out.print(" getRowCacheSavePeriodInSeconds: ");
				System.out.println(cfd.getRowCacheSavePeriodInSeconds());
				System.out.print(" getRowCacheSize: ");
				System.out.println(cfd.getRowCacheSize());
				System.out.println();
				
				final HashMap<String, Integer> all_cols_list = new HashMap<String, Integer>();
				CassandraDb.allRowsReader(new ColumnFamily<String, String>(describe, StringSerializer.get(), StringSerializer.get()), new AllRowsFoundRow() {
					
					public void onFoundRow(Row<String, String> row) throws Exception {
						for (String columname : row.getColumns().getColumnNames()) {
							Integer i = all_cols_list.get(columname);
							if (i != null) {
								all_cols_list.put(columname, i + 1);
							} else {
								all_cols_list.put(columname, 1);
							}
						}
					}
				});
				
				if (all_cols_list.size() > 0) {
					System.out.println(" All column list: ");
					for (Map.Entry<String, Integer> entry : all_cols_list.entrySet()) {
						// entry.getKey() entry.getValue()
						System.out.print("  \"");
						System.out.print(entry.getKey());
						System.out.print("\" : ");
						System.out.print(entry.getValue());
						System.out.println(" keys");
					}
				} else {
					System.out.println("EMPTY COLUMN FAMILY");
				}
				
				System.out.println();
				return;
			}
			
			System.out.print("partitioner: ");
			System.out.println(keyspace.describePartitioner());
			
			List<ColumnFamilyDefinition> l_cfd = keyspace.describeKeyspace().getColumnFamilyList();
			if (l_cfd != null) {
				if (l_cfd.size() > 0) {
					System.out.println("ColumnFamilies:");
					ColumnFamilyDefinition cfd;
					for (int pos = 0; pos < l_cfd.size(); pos++) {
						cfd = l_cfd.get(pos);
						System.out.print(" ");
						System.out.println(cfd.getName());
					}
					System.out.println();
				}
			}
			
			List<TokenRange> l_tr = keyspace.describeRing();
			if (l_tr != null) {
				if (l_tr.size() > 0) {
					System.out.println("TokenRange:");
					for (int pos = 0; pos < l_tr.size(); pos++) {
						System.out.print(" ");
						System.out.println(l_tr.get(pos));
					}
					System.out.println();
				}
			}
			return;
		}
		if (args.getParamExist("-es")) {
			String index_to_delete = args.getSimpleParamValue("-delete");
			if (index_to_delete != null) {
				Elasticsearch.deleteIndexRequest(index_to_delete);
				return;
			}
			String ttltoset = args.getSimpleParamValue("-setttl");
			if (ttltoset != null) {
				if (ttltoset.indexOf("/") == -1) {
					showFullCliModuleHelp();
					throw new Exception("No type is set for set TTL");
				}
				String index_name = ttltoset.split("/")[0];
				String type = ttltoset.split("/")[1];
				
				Elasticsearch.enableTTL(index_name, type);
				return;
			}
		}
		if (args.getParamExist("-clean")) {
			Log2.log.info("Start clean operations");
			MetadataCenter.database_gc();
			return;
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage for Cassandra:");
		System.out.println(" " + getCliModuleName() + " -cassandra [-keyspace keyspacename] [-describe cf | -truncate cf | -delete cf]");
		System.out.println("  default : keyspace informations");
		System.out.println("  cf : columnfamily to use");
		System.out.println();
		System.out.println(" Keyspaces definition:");
		System.out.println(" " + getCliModuleName() + " -cassandra -keyspacesdef");
		System.out.println();
		System.out.println(" Delete Keyspace:");
		System.out.println(" " + getCliModuleName() + " -cassandra -deletekeyspace keyspacename");
		System.out.println();
		System.out.println("Usage for Elasticsearch:");
		System.out.println(" " + getCliModuleName() + " -es [-delete index | -setttl index/type]");
		System.out.println("  index: the index name to use");
		System.out.println("  type:  the type from the index to use");
		System.out.println();
		System.out.println("Operate usages:");
		System.out.println(" " + getCliModuleName() + " -clean");
		System.out.println("  Do clean operations");
	}
	
}
