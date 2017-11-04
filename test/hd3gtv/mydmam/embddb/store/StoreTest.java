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

import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;
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
	private static final String THOUSAND_PATH = "/thousand";
	
	public StoreTest() throws IOException {
		File backend_basedir = new File(System.getProperty("user.home") + File.separator + "mydmam-debug");
		if (backend_basedir.exists()) {
			FileUtils.forceDelete(backend_basedir);
		}
		backend = new FileBackend(backend_basedir, key -> {
			ReadCacheEhcache.getCache().remove(key);
		});
	}
	
	public void testSimplePushRemove() throws Exception {
		ReadCacheEhcache.getCache().clear();
		
		Store<UUID> store1 = new Store<>(new ThreadPoolExecutorFactory("EMBDDBTest", Thread.MIN_PRIORITY), "test", factory, backend, ReadCacheEhcache.getCache(), 100_000, 10_000, 10_000);
		
		store1.clear();
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
		
		/**
		 * Re-open
		 */
		Store<UUID> store = new Store<>(new ThreadPoolExecutorFactory("EMBDDBTest", Thread.MIN_PRIORITY), "test", factory, backend, ReadCacheEhcache.getCache(), 100_000, 10_000, 10_000);
		
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
		Store<UUID> store = new Store<>(new ThreadPoolExecutorFactory("EMBDDBTest", Thread.MIN_PRIORITY), "test", factory, backend, ReadCacheEhcache.getCache(), 1_000_000, 10_000, 100_000);
		store.clear();
		
		int size = 100_000;
		/**
		 * Push a lot of UUIDs
		 * Set for each 1000' a special path
		 * And update for each 100'
		 */
		IntStream.range(0, size).parallel().mapToObj(i -> {
			if (i % 1000 == 0) {
				return store.put(String.valueOf(i), THOUSAND_PATH, UUID.randomUUID(), 1, TimeUnit.HOURS);
			} else {
				return store.put(String.valueOf(i), null, UUID.randomUUID(), 1, TimeUnit.HOURS);
			}
		}).map(cf -> {
			try {
				int pushed_id = Integer.valueOf(cf.get(5, TimeUnit.SECONDS));
				if (pushed_id % 1000 == 0) {
					return store.put(String.valueOf(pushed_id), THOUSAND_PATH, ZERO_UUID, 1, TimeUnit.HOURS);
				} else if (pushed_id % 100 == 0) {
					return store.put(String.valueOf(pushed_id), null, ZERO_UUID, 1, TimeUnit.HOURS);
				} else {
					return CompletableFuture.completedFuture(pushed_id);
				}
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		}).forEach(cf -> {
			try {
				cf.get(5, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		});
		
		ReadCacheEhcache.getCache().clear();
		
		Map<String, UUID> all_pushed = store.getAllIDs().get(5, TimeUnit.SECONDS);
		assertEquals(size, all_pushed.size());
		
		all_pushed.forEach((k, u) -> {
			int i = Integer.valueOf(k);
			assertTrue(i >= 0);
			assertTrue(i <= size);
			if (i % 100 == 0) {
				assertEquals(ZERO_UUID, u);
			} else {
				assertNotEquals(ZERO_UUID, u);
			}
		});
		
		ReadCacheEhcache.getCache().clear();
		
		IntStream.range(0, size).parallel().forEach(i -> {
			try {
				UUID u = store.get(String.valueOf(i)).get(5, TimeUnit.SECONDS);
				assertNotNull(u);
				Item item = store.getItem(String.valueOf(i)).get(5, TimeUnit.SECONDS);
				assertNotNull(item);
				
				if (i % 1000 == 0) {
					assertEquals(ZERO_UUID, u);
					assertEquals(item.getPath(), THOUSAND_PATH);
				} else if (i % 100 == 0) {
					assertEquals(ZERO_UUID, u);
					assertTrue(item.getPath().isEmpty());
				} else {
					assertTrue(item.getPath().isEmpty());
				}
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		});
		
		ReadCacheEhcache.getCache().clear();
		
		List<Item> all_thousand_items = store.getItemsByPath(THOUSAND_PATH).get(5, TimeUnit.SECONDS).collect(Collectors.toList());
		assertEquals(size / 1000, all_thousand_items.size());
		
		all_thousand_items.forEach(item -> {
			int i = Integer.valueOf(item.getId());
			assertEquals(0, i % 1000);
			assertEquals(THOUSAND_PATH, item.getPath());
			assertEquals(ZERO_UUID, factory.getFromItem(item));
		});
		
		ReadCacheEhcache.getCache().clear();
		
		int removed_count = store.removeAllByPath(THOUSAND_PATH).get(5, TimeUnit.SECONDS);
		assertEquals(all_thousand_items.size(), removed_count);
		all_pushed = store.getAllIDs().get(5, TimeUnit.SECONDS);
		assertEquals(size - removed_count, all_pushed.size());
		
		all_pushed.forEach((id, u) -> {
			int i = Integer.valueOf(id);
			assertTrue(i >= 0);
			assertTrue(i <= size);
			if (i % 100 == 0) {
				assertEquals(ZERO_UUID, u);
			} else {
				assertNotEquals(ZERO_UUID, u);
			}
		});
		
		store.clear();
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		store.close();
	}
	
	private static final long TTL_DURATION = 300l;
	private static final long GRACE_PERIOD_TTL = TTL_DURATION * 3l;
	
	public void testTTL() throws Exception {
		ReadCacheEhcache.getCache().clear();
		
		Store<UUID> store = new Store<>(new ThreadPoolExecutorFactory("EMBDDBTest", Thread.MIN_PRIORITY), "test", factory, backend, ReadCacheEhcache.getCache(), 100_000, GRACE_PERIOD_TTL, 10_000);
		store.clear();
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		final String ID = "ttltest";
		
		UUID newer = UUID.randomUUID();
		store.put(ID, newer, TTL_DURATION, TimeUnit.MILLISECONDS).get(5, TimeUnit.SECONDS);
		long start_time = System.currentTimeMillis();
		
		assertTrue(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		UUID actual = store.get(ID).get(5, TimeUnit.SECONDS);
		assertEquals(newer, actual);
		
		Thread.sleep(Math.max(TTL_DURATION - (System.currentTimeMillis() - start_time), 1l));
		
		/**
		 * Check after delete date
		 */
		assertTrue(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		assertNull(store.get(ID).get(5, TimeUnit.SECONDS));
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		/**
		 * Check delete after durable writes
		 */
		store.doDurableWrites();
		assertTrue(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		assertNull(store.get(ID).get(5, TimeUnit.SECONDS));
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		/**
		 * Check after purge cache
		 */
		store.doDurableWritesAndCleanUpFiles();
		ReadCacheEhcache.getCache().clear();
		
		assertTrue(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		assertNull(store.get(ID).get(5, TimeUnit.SECONDS));
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		/**
		 * Check after grace time
		 */
		Thread.sleep(Math.max((2l * GRACE_PERIOD_TTL) - (System.currentTimeMillis() - start_time), 1l));
		
		assertTrue(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		assertNull(store.get(ID).get(5, TimeUnit.SECONDS));
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		ReadCacheEhcache.getCache().clear();
		
		assertFalse(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		assertNull(store.get(ID).get(5, TimeUnit.SECONDS));
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		store.doDurableWritesAndCleanUpFiles();
		
		assertFalse(store.hasPresence(ID).get(5, TimeUnit.SECONDS));
		assertNull(store.get(ID).get(5, TimeUnit.SECONDS));
		assertEquals(0, store.getAllValues().get(5, TimeUnit.SECONDS).size());
		
		store.close();
	}
	
	private class RandomBinTest {
		byte[] content;
		
		RandomBinTest(byte[] content) {
			this.content = content;
		}
	}
	
	public void testXML() throws Exception {
		Store<RandomBinTest> store = new Store<>(new ThreadPoolExecutorFactory("EMBDDBTest", Thread.MIN_PRIORITY), "test", new ItemFactory<RandomBinTest>() {
			
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
		
		store.clear();
		
		byte[] garbadge = new byte[100_000];
		ThreadLocalRandom.current().nextBytes(garbadge);
		
		HashMap<String, RandomBinTest> data_test = new HashMap<>(4);
		data_test.put("test-xml-text", new RandomBinTest("TËST".getBytes()));
		data_test.put("test-xml-cdata", new RandomBinTest("<![CDATA[CULE]]>".getBytes()));
		data_test.put("test-xml-bin", new RandomBinTest(new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04 }));
		data_test.put("test-xml-big", new RandomBinTest(garbadge));
		
		data_test.forEach((k, v) -> {
			try {
				store.put(k, v, 1, TimeUnit.HOURS).get();
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		
		File document = store.xmlExport().get();
		File temp_file = File.createTempFile("mydmam-embddb-store-test", ".xml");
		FileUtils.forceDelete(temp_file);
		FileUtils.moveFile(document, temp_file);
		
		store.clear();
		store.doDurableWritesAndCleanUpFiles();
		ReadCacheEhcache.getCache().clear();
		assertNull(store.get("test-xml-text").get());
		
		store.xmlImport(temp_file).get();
		
		data_test.forEach((k, v) -> {
			try {
				RandomBinTest item = store.get(k).get();
				assertNotNull(item);
				assertTrue(Arrays.equals(v.content, item.content));
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException(e);
			}
		});
		
		store.close();
		FileUtils.forceDelete(temp_file);
	}
}
