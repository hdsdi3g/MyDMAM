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
 * Copyright (C) hdsdi3g for hd3g.tv 11 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.MyDMAM;
import junit.framework.TestCase;

public class StoreTest extends TestCase {
	
	private final ItemFactory<UUID> factory = new ItemFactory<UUID>() {
		
		public Item toItem(UUID element) {
			return new Item(MyDMAM.gson_kit.getGsonSimple().toJson(element).getBytes());
		};
		
		public UUID getFromItem(Item item) {
			return MyDMAM.gson_kit.getGsonSimple().fromJson(new String(item.getPayload()), UUID.class);
		}
		
		public Class<UUID> getType() {
			return UUID.class;
		}
		
	};
	
	public void test() throws Exception {
		
		File backend_basedir = new File(System.getProperty("user.home") + File.separator + "mydmam-debug");
		if (backend_basedir.exists()) {
			FileUtils.forceDelete(backend_basedir);
		}
		FileBackend backend = new FileBackend(backend_basedir, UUID.fromString("00000000-0000-0000-0000-000000000000"));
		
		Store<UUID> store = new Store<>("test", factory, backend, ReadCacheEhcache.getCache(), 1_000_000, 10_000, 10000);
		
		UUID newer = UUID.randomUUID();
		store.put(String.valueOf(0), newer, 1, TimeUnit.DAYS).get();
		UUID actual = store.get(String.valueOf(0)).get();
		assertEquals(newer, actual);
		
		/**
		 * Check Runtime errors catch
		 */
		ExecutionException asset_null_error = null;
		try {
			store.put(null, newer, 1, TimeUnit.DAYS).get();
		} catch (ExecutionException e) {
			asset_null_error = e;
		}
		assertNotNull(asset_null_error);
		assertEquals(NullPointerException.class, asset_null_error.getCause().getCause().getClass());
		
		// TODO tests:
		// store.doDurableWrite();
		// store.exists(_id);
		// store.get(_id);
		// store.getAll();
		// store.getByPath(path);
		// Parallel store.put(element, ttl, unit);
		// store.removeAllByPath(path);
		// store.removeById(_id);
		// store.truncate();
		// open + close + open
	}
	
}
