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
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

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
	
	private final FileBackend backend;
	
	private static final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	
	public StoreTest() throws IOException {
		File backend_basedir = new File(System.getProperty("user.home") + File.separator + "mydmam-debug");
		if (backend_basedir.exists()) {
			FileUtils.forceDelete(backend_basedir);
		}
		backend = new FileBackend(backend_basedir, ZERO_UUID, key -> {
			ReadCacheEhcache.getCache().remove(key);
		});
	}
	
	public void testSimplePushRemove() throws Exception {
		ReadCacheEhcache.getCache().clear();
		Store<UUID> store1 = new Store<>("test", factory, backend, ReadCacheEhcache.getCache(), 100_000, 10_000, 10_000);
		
		store1.truncate();
		assertEquals(0, store1.getAllValues().get().size());
		
		final String ID = "volatile";
		
		UUID newer = UUID.randomUUID();
		store1.put(ID, newer, 1, TimeUnit.DAYS).get();
		assertTrue(store1.hasPresence(ID).get());
		UUID actual = store1.get(ID).get();
		assertEquals(newer, actual);
		
		/**
		 * Check Runtime errors catch
		 */
		ExecutionException asset_null_error = null;
		try {
			store1.put(null, newer, 1, TimeUnit.DAYS).get();
		} catch (ExecutionException e) {
			asset_null_error = e;
		}
		assertNotNull(asset_null_error);
		assertEquals(NullPointerException.class, asset_null_error.getCause().getCause().getClass());
		
		store1.doDurableWrites();
		
		/**
		 * Check if it can re-read after durable writes
		 */
		assertTrue(store1.hasPresence(ID).get());
		actual = store1.get(ID).get();
		assertEquals(newer, actual);
		
		/**
		 * Overwrite value
		 */
		newer = UUID.randomUUID();
		store1.put(ID, newer, 1, TimeUnit.DAYS).get();
		assertTrue(store1.hasPresence(ID).get());
		actual = store1.get(ID).get();
		assertEquals(newer, actual);
		
		store1.close();
		// ReadCacheEhcache.getCache().clear(); XXX is ok ?
		
		/**
		 * Re-open
		 */
		Store<UUID> store = new Store<>("test", factory, backend, ReadCacheEhcache.getCache(), 100_000, 10_000, 10_000);
		
		/**
		 * Re-read
		 */
		assertTrue(store.hasPresence(ID).get());
		actual = store.get(ID).get();
		assertEquals(newer, actual);
		
		/**
		 * Delete
		 */
		store.removeById(ID).get();
		assertTrue(store.hasPresence(ID).get());
		assertNull(store.get(ID).get());
		assertEquals(0, store.getAllValues().get().size());
		
		store.doDurableWrites();
		
		/**
		 * Check delete after durable writes
		 */
		assertTrue(store.hasPresence(ID).get());
		assertNull(store.get(ID).get());
		assertEquals(0, store.getAllValues().get().size());
		
		store.close();
	}
	
	public void testParallelPushRemove() throws Exception {
		Store<UUID> store = new Store<>("test", factory, backend, ReadCacheEhcache.getCache(), 100_000, 10_000, 10_000);
		
		int size = 100_000;
		/**
		 * Push a lot of UUIDs
		 * Set for each 1000' a special path
		 * And update for each 100'
		 */
		final String HUNDRED_PATH = "/Hundred";
		IntStream.range(0, size).parallel().mapToObj(i -> {
			if (i % 1000 == 0) {
				return store.put(String.valueOf(i), HUNDRED_PATH, UUID.randomUUID(), 1, TimeUnit.HOURS);
			} else {
				return store.put(String.valueOf(i), null, UUID.randomUUID(), 1, TimeUnit.HOURS);
			}
		}).map(cf -> {
			try {
				int pushed_id = Integer.valueOf(cf.get());
				if (pushed_id % 100 == 0) {
					if (pushed_id % 1000 == 0) {
						return store.put(String.valueOf(pushed_id), HUNDRED_PATH, ZERO_UUID, 1, TimeUnit.HOURS);
					} else {
						return store.put(String.valueOf(pushed_id), null, ZERO_UUID, 1, TimeUnit.HOURS);
					}
				} else {
					return CompletableFuture.completedFuture(pushed_id);
				}
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		}).forEach(cf -> {
			try {
				cf.get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals(size, store.getAllValues().get().size());
		
		ReadCacheEhcache.getCache().clear();
		Map<ItemKey, UUID> all_pushed = store.getAll().get();
		assertEquals(size, all_pushed.size());
		
		ReadCacheEhcache.getCache().clear();
		
		// TODO check all_pushed
		
		ReadCacheEhcache.getCache().clear();
		// TODO check parallel get
		/*IntStream.range(0, size).parallel().mapToObj(i -> {
			store.get(_id)
		});*/
		
		// TODO tests:
		// store.getByPath(path);
		// test TTL
		// store.removeAllByPath(path);
		
		store.truncate();
		assertEquals(0, store.getAllValues().get().size());
		
		store.close();
	}
	
	private class RandomBinTest {
		byte[] content;
		
		RandomBinTest(byte[] content) {
			this.content = content;
		}
	}
	
	public void testXML() throws Exception {
		Store<RandomBinTest> store = new Store<>("test", new ItemFactory<RandomBinTest>() {
			
			public Item toItem(RandomBinTest element) {
				return new Item(element.content);
			}
			
			public RandomBinTest getFromItem(Item item) {
				return new RandomBinTest(item.getPayload());
			}
			
			public Class<RandomBinTest> getType() {
				return RandomBinTest.class;
			}
			
		}, backend, ReadCacheEhcache.getCache(), 1000, 10, 10);
		
		store.truncate();
		store.put("test-xml-text", new RandomBinTest("TËST".getBytes()), 1, TimeUnit.HOURS).get();
		store.put("test-xml-bin", new RandomBinTest(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04 }), 1, TimeUnit.HOURS).get();
		
		byte[] garbadge = new byte[100_000];
		ThreadLocalRandom.current().nextBytes(garbadge);
		store.put("test-xml-big", new RandomBinTest(garbadge), 1, TimeUnit.HOURS).get();
		
		store.xmlExport().get();
		store.truncate();
		store.close();
	}
}
