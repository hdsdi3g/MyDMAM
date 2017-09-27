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
 * Copyright (C) hdsdi3g for hd3g.tv 25 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.FileData.Entry;
import junit.framework.TestCase;

public class FileDataTest extends TestCase {
	
	private FileData file_data;
	private File file;
	
	public FileDataTest() throws Exception {
		file = File.createTempFile("mydmam-test-data", ".bin", new File(System.getProperty("user.home")));
		file.delete();
		file.deleteOnExit();
		file_data = new FileData(file);
		
		if (file.getFreeSpace() < 10_000_000l) {
			throw new IOException("No more space for " + file);
		}
	}
	
	protected void tearDown() throws Exception {
		file_data.clear();
	}
	
	public void testWriteReadSimple() throws IOException {
		Random rnd = ThreadLocalRandom.current();
		ItemKey key = new ItemKey("test-" + rnd.nextInt(100));
		
		byte[] data = new byte[rnd.nextInt(10000) + 1];
		rnd.nextBytes(data);
		long pos = file_data.write(key, data);
		assertTrue("Invalid write pos", pos > 0);
		Entry entry = file_data.read(pos, key);
		checkValidity(key, data, entry.key, entry.value);
	}
	
	private static void checkValidity(ItemKey key_source, byte[] data_source, ItemKey key_dest, byte[] data_dest) {
		assertTrue("Invalid key", Arrays.equals(key_source.key, key_dest.key));
		assertTrue("Invalid datas", Arrays.equals(data_source, data_dest));
	}
	
	public void testWriteReadOverwriteSimple() throws IOException {
		ItemKey key = new ItemKey("test");
		byte[] data = "TestData".getBytes(MyDMAM.UTF8);
		long pos = file_data.write(key, data);
		assertTrue("Invalid write pos", pos > 0);
		Entry entry = file_data.read(pos, key);
		checkValidity(key, data, entry.key, entry.value);
		file_data.markDelete(pos, key);
		
		IOException expected = null;
		try {
			file_data.read(pos, key);
		} catch (IOException e) {
			expected = e;
		}
		assertNotNull("Can't mark deleted", expected);
		
		ItemKey key2 = new ItemKey("test2");
		byte[] data2 = "Test2".getBytes(MyDMAM.UTF8);
		long new_pos = file_data.write(key2, data2);
		assertEquals("Can't re-map old datas", new_pos, pos);
		Entry entry2 = file_data.read(pos, key2);
		assertTrue("Can't overwrite new datas", Arrays.equals(data2, entry2.value));
		
		ItemKey key3 = new ItemKey("test2");
		byte[] data3 = "Test3WithMoreDatas".getBytes(MyDMAM.UTF8);
		long new_pos3 = file_data.write(key2, data3);
		assertFalse("Try to overwrite old valid datas", new_pos3 == pos);
		
		Entry entry3 = file_data.read(new_pos3, key3);
		assertTrue("Can't read new datas", Arrays.equals(data3, entry3.value));
		
		entry2 = file_data.read(pos, key2);
		assertTrue("Trouble with overwrite", Arrays.equals(data2, entry2.value));
	}
	
	public void testWriteReadMultipleParallel() throws IOException {
		final Random rnd = ThreadLocalRandom.current();
		
		class Item {
			ItemKey key;
			byte[] data;
			long data_pos;
			
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
		all_items.parallelStream().forEach(item -> {
			try {
				item.data_pos = file_data.write(item.key, item.data);
				assertTrue("Invalid write pos", item.data_pos > 0);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Read datas
		 */
		all_items.parallelStream().forEach(item -> {
			try {
				Entry entry = file_data.read(item.data_pos, item.key);
				checkValidity(item.key, item.data, entry.key, entry.value);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
	}
	
	public void testWriteReadOverwriteMultiple() throws IOException {
		final Random rnd = ThreadLocalRandom.current();
		
		class Item {
			ItemKey key;
			byte[] data;
			long data_pos;
			
			Item(ItemKey key, byte[] data) {
				this.key = key;
				this.data = data;
			}
		}
		
		/**
		 * Create datas
		 */
		List<Item> all_items = IntStream.range(0, 1000).mapToObj(i -> {
			ItemKey key = new ItemKey("Test" + i);
			byte[] data = new byte[rnd.nextInt(1000) + 1];
			rnd.nextBytes(data);
			return new Item(key, data);
		}).collect(Collectors.toList());
		
		/**
		 * Write datas, 1st pass
		 */
		all_items.stream().forEach(item -> {
			try {
				item.data_pos = file_data.write(item.key, item.data);
				assertTrue("Invalid write pos", item.data_pos > 0);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		/**
		 * Mark to delete all
		 */
		all_items.parallelStream().forEach(item -> {
			try {
				file_data.markDelete(item.data_pos, item.key);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		long before_file_size = this.file.length();
		
		/**
		 * (Over)write datas, 2nd pass
		 */
		all_items.stream().forEach(item -> {
			try {
				item.data_pos = file_data.write(item.key, Arrays.copyOf(item.data, item.data.length - item.data.length / 2));
				assertTrue("Invalid write pos", item.data_pos > 0);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals("Can't re-map old datas", before_file_size, file.length());
	}
	
}
