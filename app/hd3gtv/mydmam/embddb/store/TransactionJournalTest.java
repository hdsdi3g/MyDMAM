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
 * Copyright (C) hdsdi3g for hd3g.tv 27 sept. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class TransactionJournalTest extends TestCase {
	
	private final UUID uuid;
	private final File file;
	
	public TransactionJournalTest() {
		uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
		file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-transactionjournal-" + uuid);
	}
	
	protected void tearDown() throws IOException {
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
	}
	
	public void testCreateJournal() throws IOException {
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
		
		FileUtils.forceMkdir(file);
		TransactionJournal journal_write = new TransactionJournal(file, uuid);
		
		long before_size = journal_write.getFileSize();
		
		int size = 10000;
		ConcurrentHashMap<ItemKey, Integer> hash_map = new ConcurrentHashMap<>(size);
		
		IntStream.range(0, size).parallel().forEach(i -> {
			ThreadLocalRandom random = ThreadLocalRandom.current();
			byte[] bytes = new byte[random.nextInt(1, 1000)];
			random.nextBytes(bytes);
			try {
				Item item = new Item(String.valueOf(i), bytes);
				journal_write.write(item.getKey(), item, System.currentTimeMillis() + 1_000_000l, null);
				hash_map.put(item.getKey(), bytes.length);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		journal_write.channelSync();
		assertTrue("Empty journal", journal_write.getFileSize() > before_size);
		journal_write.close();
		
		TransactionJournal.allJournalsByDate(file).forEach(journal_read -> {
			try {
				assertTrue("Empty journal: " + journal_read.getFileSize(), journal_read.getFileSize() > before_size);
				
				AtomicLong last_date = new AtomicLong(0);
				AtomicInteger item_count = new AtomicInteger(0);
				
				journal_read.readAll(false).forEach(entry -> {
					item_count.incrementAndGet();
					
					assertTrue("Invalid date: in #" + item_count.get() + " " + last_date.get() + "<<<" + entry.date, last_date.get() <= entry.date);
					last_date.set(entry.date);
					assertTrue("Unknow key in #" + item_count.get() + ": " + entry.key, hash_map.containsKey(entry.key));
					assertEquals("Invalid size in #" + item_count.get(), (int) hash_map.get(entry.key), entry.data_export_source.getByteBufferWriteSize());
				});
				
				assertEquals("Invalid item count", hash_map.size(), item_count.get());
				journal_read.purge();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		List<File> actual_dir_content = Arrays.asList(file.listFiles());
		assertEquals("Invalid delete journal", 0, actual_dir_content.size());
	}
	
}
