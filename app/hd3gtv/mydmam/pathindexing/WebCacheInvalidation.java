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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.pathindexing;

import java.util.Arrays;
import java.util.List;

import com.google.common.cache.CacheLoader;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;

public class WebCacheInvalidation {
	
	public static final int CACHING_CASSANDRA_TTL_SEC = 60 * 60;
	public static final String CACHING_CLIENT_TTL = "60mn";
	
	private static final ColumnFamily<String, String> CF_CACHEINVALIDATION = new ColumnFamily<String, String>("cacheInvalidation", StringSerializer.get(), StringSerializer.get());
	
	private static Keyspace keyspace;
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_CACHEINVALIDATION.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_CACHEINVALIDATION.getName(), false);
			}
		} catch (Exception e) {
			Loggers.Pathindex.error("Can't init database CFs", e);
			System.exit(1);
		}
	}
	
	public static void addInvalidation(String... storages_name) {
		if (storages_name == null) {
			return;
		}
		addInvalidation(Arrays.asList(storages_name));
	}
	
	public static void addInvalidation(List<String> storages_name) {
		if (storages_name == null) {
			return;
		}
		if (storages_name.isEmpty()) {
			return;
		}
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			for (int pos = 0; pos < storages_name.size(); pos++) {
				if (storages_name.get(pos) == null) {
					continue;
				}
				mutator.withRow(CF_CACHEINVALIDATION, storages_name.get(pos)).putColumn("last_refresh", System.currentTimeMillis(), CACHING_CASSANDRA_TTL_SEC);
			}
			mutator.execute();
			Loggers.Pathindex.debug("Add invalidation(s), storages_name: " + storages_name);
		} catch (Exception e) {
			Loggers.Pathindex.error("Can't add invalidation, storages_name: " + storages_name, e);
		}
	}
	
	/**
	 * @return unix time, or 0 (== has not expired) if no invalidation
	 */
	private static long getCurrentInvalidationDate(String storagename) {
		if (storagename == null) {
			return 0;
		}
		
		try {
			ColumnList<String> col = keyspace.prepareQuery(CF_CACHEINVALIDATION).getKey(storagename).withColumnSlice("last_refresh").execute().getResult();
			
			if (col == null) {
				return 0;
			}
			if (col.isEmpty()) {
				return 0;
			}
			return col.getLongValue("last_refresh", 0l);
		} catch (Exception e) {
			Loggers.Pathindex.error("Can't add invalidation", e);
		}
		
		return 0;
	}
	
	private InternalCacheLoader cache_loader;
	
	private class InternalCacheLoader extends CacheLoader<String, Long> {
		public Long load(String storagename) throws Exception {
			return getCurrentInvalidationDate(storagename);
		}
	}
	
	public WebCacheInvalidation() {
		cache_loader = new InternalCacheLoader();
	}
	
	public long getLastInvalidationDate(String storagename) {
		try {
			Long result;
			result = cache_loader.load(storagename);
			return result;
		} catch (Exception e) {
			Loggers.Pathindex.error("Can't get last value, storagename: " + storagename, e);
			return 0;
		}
	}
}
