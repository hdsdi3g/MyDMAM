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
 * Copyright (C) hdsdi3g for hd3g.tv 26 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Test;

import hd3gtv.mydmam.embddb.store.FileData.Entry;
import junit.framework.TestCase;

public class FileHashTableTest extends TestCase {
	
	private File data_file;
	private File index_file;
	private FileHashTable hash_table;
	
	public FileHashTableTest() throws Exception {
		data_file = File.createTempFile("mydmam-test-data", ".bin", new File(System.getProperty("user.home")));
		data_file.delete();
		data_file.deleteOnExit();
		index_file = File.createTempFile("mydmam-test-file", ".bin", new File(System.getProperty("user.home")));
		index_file.delete();
		index_file.deleteOnExit();
		
		hash_table = new FileHashTable(index_file, data_file, 16);
		
		if (data_file.getFreeSpace() < 10_000_000l) {
			throw new IOException("No more space for " + data_file);
		}
	}
	
	@After
	public void onAfterEachTest() throws Exception {
		hash_table.clear();
	}
	
	@Test
	public void testWriteReadRemoveSimple() throws IOException {
		Random rnd = new Random(0);
		ItemKey key = new ItemKey("test-" + rnd.nextInt(100));
		byte[] data = new byte[rnd.nextInt(10000) + 1];
		rnd.nextBytes(data);
		
		hash_table.put(key, data);
		assertEquals("Invalid number of items", 1, hash_table.size());
		assertTrue("Can't found item", hash_table.has(key));
		assertFalse("Not empty", hash_table.isEmpty());
		
		long file_size = index_file.length();
		
		Entry entry = hash_table.getEntry(key);
		assertNotNull("Can't found " + key, entry);
		assertTrue("Not same key", key.equals(entry.key));
		assertTrue("Not same datas", Arrays.equals(data, entry.value));
		assertEquals("Not the same Item", key, hash_table.forEachKeys().findFirst().get());
		assertTrue("Not the same Item content", Arrays.equals(data, hash_table.forEachKeyValue().findFirst().get().value));
		
		hash_table.remove(key);
		assertNull("Item is not deleted " + key, hash_table.getEntry(key));
		assertFalse("Item is not deleted " + key, hash_table.has(key));
		assertEquals("Invalid number of items", 0, hash_table.size());
		assertTrue("Not empty", hash_table.isEmpty());
		
		/**
		 * Overwrite
		 */
		data = new byte[rnd.nextInt(10000) + 1];
		rnd.nextBytes(data);
		hash_table.put(key, data);
		
		assertEquals("Invalid number of items", 1, hash_table.size());
		assertTrue("Can't found item", hash_table.has(key));
		assertFalse("Not empty", hash_table.isEmpty());
		
		entry = hash_table.getEntry(key);
		assertNotNull("Can't found " + key, entry);
		assertTrue("Not same key", key.equals(entry.key));
		assertTrue("Not same datas", Arrays.equals(data, entry.value));
		assertEquals("Not the same Item", key, hash_table.forEachKeys().findFirst().get());
		assertTrue("Not the same Item content", Arrays.equals(data, hash_table.forEachKeyValue().findFirst().get().value));
		
		assertEquals("Hash file has not recycled its space", file_size, index_file.length());
	}
	
	@Test
	public void testWriteReadRemoveMultiple() throws IOException {
		final Random rnd = new Random(0);
		
		class Item {
			ItemKey key;
			byte[] data;
			
			Item(ItemKey key, byte[] data) {
				this.key = key;
				this.data = data;
			}
		}
		
		/**
		 * Create datas
		 */
		List<Item> all_items = IntStream.range(0, 1000).mapToObj(i -> {
			ItemKey key = new ItemKey("Test-" + i);
			byte[] data = new byte[rnd.nextInt(1000) + 1];
			rnd.nextBytes(data);
			return new Item(key, data);
		}).collect(Collectors.toList());
		
		/**
		 * Write datas
		 */
		all_items.stream().forEach(item -> {
			try {
				hash_table.put(item.key, item.data);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals("Invalid number of items", all_items.size(), hash_table.size());
		long file_size = index_file.length();
		
		/**
		 * Check has
		 */
		Collections.shuffle(all_items, rnd);
		
		all_items.parallelStream().forEach(item -> {
			try {
				assertTrue("Can't found item " + item.key, hash_table.has(item.key));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Check get
		 */
		Collections.shuffle(all_items, rnd);
		all_items.parallelStream().forEach(item -> {
			try {
				Entry entry = hash_table.getEntry(item.key);
				assertTrue("Can't get item " + item.key, Arrays.equals(entry.value, item.data));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		Collections.shuffle(all_items, rnd);
		
		int max = all_items.size() / 2;
		
		/**
		 * Do remove the half
		 */
		all_items.stream().limit(max).forEach(item -> {
			try {
				hash_table.remove(item.key);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Check deleted items
		 */
		all_items.stream().limit(max).forEach(item -> {
			try {
				assertFalse("Removed item still exists", hash_table.has(item.key));
				assertNull("Removed item still exists", hash_table.getEntry(item.key));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		all_items.parallelStream().skip(max).forEach(item -> {
			try {
				assertTrue("Can't found item " + item.key, hash_table.has(item.key));
				Entry entry = hash_table.getEntry(item.key);
				assertTrue("Can't get item " + item.key, Arrays.equals(entry.value, item.data));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals("Invalid number of items", max, hash_table.size());
		
		assertEquals("Hash file has not recycled its space", file_size, index_file.length());
		
		/*all_items.stream().limit(max).forEach(item -> {
			try {
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});*/
		
	}
	
	// TODO test hash_table.forEachKeys() and hash_table.forEachKeyValue();
}
