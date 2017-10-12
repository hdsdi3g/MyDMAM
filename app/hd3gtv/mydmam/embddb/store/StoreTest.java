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
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.MyDMAM;
import junit.framework.TestCase;

public class StoreTest extends TestCase {
	
	public void testAll() throws Exception {
		
		ItemFactory<UUID> factory = new ItemFactory<UUID>() {
			
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
		
		File backend_basedir = new File(System.getProperty("user.home") + File.separator + "mydmam-debug");
		if (backend_basedir.exists()) {
			FileUtils.forceDelete(backend_basedir);
		}
		FileBackend backend = new FileBackend(backend_basedir, UUID.fromString("00000000-0000-0000-0000-000000000000"));
		
		Store<UUID> store = new Store<>("test", factory, backend, ReadCacheEhcache.getCache(), 1_000_000, 10_000, 10000);
		
		UUID newer = UUID.randomUUID();
		store.put(String.valueOf(0), newer, 1, TimeUnit.DAYS).thenRun(() -> {
			store.get(String.valueOf(0)).thenAccept(actual -> {
				assertNotSame(newer, actual);
			});
		}).get();// XXX do blocking...
		System.out.println("+");
		
		// TODO tests:
		// check NPE with completable
		
		// store.doDurableWrite();
		// store.exists(_id, onDone, onError);
		// store.get(_id, onDone, onNotFound, onError);
		// store.getAll(onDone, onError);
		// store.getByPath(path, onDone, onError);
		// store.put(element, ttl, unit, onDone, onError);
		// store.removeAllByPath(path, onDone, onError);
		// store.removeById(_id, onDone, onNotFound, onError);
		// store.truncate(onDone, onError);
		// open + close + open
	}
	
}
