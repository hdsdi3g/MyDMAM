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

public class WebCacheInvalidation {
	// TODO
	
	public static final long CACHING_TTL_SEC = 5 * 60;
	public static final String CACHING_TTL_STR = "5mn";
	
	/*Cache<Key, Graph> graphs = CacheBuilder.newBuilder()
	.concurrencyLevel(4)
	.maximumSize(10000)
	.expireAfterWrite(10, TimeUnit.MINUTES)
	.build(
	new CacheLoader<Key, Graph>() {
	  public Graph load(Key key) throws AnyException {
	    return createExpensiveGraph(key);
	  }
	});*/
	
	public static void addInvalidation(String storagename) {
		try {
			// TODO to cassandra
			
		} catch (Exception e) {
			Log2.log.error("Can't add invalidation", e);
		}
	}
	
	private static long getCurrentInvalidationDate(String storagename) {
		// TODO from cassandra, don't add to local cache
		return 0;
	}
	
	public static long getLastInvalidationDate(String storagename) {
		// TODO from local cache, or getCurrentInvalidationDate() + add to local cache
		return 0;
	}
	
}
