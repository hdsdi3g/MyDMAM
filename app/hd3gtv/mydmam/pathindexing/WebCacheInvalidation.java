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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.CassandraDb;

import java.util.Arrays;
import java.util.List;

import com.google.common.cache.CacheLoader;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.StringSerializer;

public class WebCacheInvalidation {
	
	public static final int CACHING_CASSANDRA_TTL_SEC = 60 * 60;
	public static final String CACHING_CLIENT_TTL = "60mn";
	
	// public static final long CACHING_LOCAL_CASSANDRA_INVALIDATION_TTL = 5;
	
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
			Log2.log.error("Can't init database CFs", e);
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
		Log2Dump dump = new Log2Dump();
		dump.add("storages_name", storages_name);
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			for (int pos = 0; pos < storages_name.size(); pos++) {
				if (storages_name.get(pos) == null) {
					continue;
				}
				mutator.withRow(CF_CACHEINVALIDATION, storages_name.get(pos)).putColumn("last_refresh", System.currentTimeMillis(), CACHING_CASSANDRA_TTL_SEC);
			}
			mutator.execute();
			
			Log2.log.debug("Add invalidation(s)", dump);
		} catch (Exception e) {
			Log2.log.error("Can't add invalidation", e, dump);
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
			Log2.log.error("Can't add invalidation", e);
		}
		
		return 0;
	}
	
	// private Cache<String, Long> internal_invalidation_cache;
	private InternalCacheLoader cache_loader;
	
	private class InternalCacheLoader extends CacheLoader<String, Long> {
		public Long load(String storagename) throws Exception {
			return getCurrentInvalidationDate(storagename);
		}
	}
	
	public WebCacheInvalidation() {
		/*CacheBuilder<Object, Object> graphs = CacheBuilder.newBuilder();
		graphs.expireAfterWrite(CACHING_LOCAL_CASSANDRA_INVALIDATION_TTL, TimeUnit.SECONDS);*/
		cache_loader = new InternalCacheLoader();
		// internal_invalidation_cache = graphs.build(cache_loader);
	}
	
	public long getLastInvalidationDate(String storagename) {
		/*if (storagename == null) {
			long min_value = System.currentTimeMillis();
			// Root storage (only dir list can be updated), search the min cached values.
			if (internal_invalidation_cache.size() > 0) {
				ConcurrentMap<String, Long> ddd = internal_invalidation_cache.asMap();
				for (Map.Entry<String, Long> entry : ddd.entrySet()) {
					if (min_value > entry.getValue()) {
						// Search the oldest refresh date cached (root storages are not so many added or removed).
						min_value = entry.getValue();
					}
				}
			}
			return min_value;
		}*/
		
		try {
			Long result /*= internal_invalidation_cache.getIfPresent(storagename)*/;
			// if (result == null) {
			result = cache_loader.load(storagename);
			// internal_invalidation_cache.put(storagename, result);
			// }
			return result;
		} catch (Exception e) {
			Log2.log.error("Can't get last value", e, new Log2Dump("storagename", storagename));
			return 0;
		}
	}
}
