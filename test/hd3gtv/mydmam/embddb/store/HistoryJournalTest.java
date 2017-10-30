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
 * Copyright (C) hdsdi3g for hd3g.tv 27 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.store.HistoryJournal.HistoryEntry;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class HistoryJournalTest extends TestCase {
	
	private static Logger log = Logger.getLogger(HistoryJournalTest.class);
	
	public void testAll() throws IOException {
		File file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-history1");
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
		FileUtils.forceMkdir(file);
		
		ThreadPoolExecutorFactory write_pool = new ThreadPoolExecutorFactory("Write journal", Thread.NORM_PRIORITY).setSimplePoolSize();
		
		final HistoryJournal journal = new HistoryJournal(write_pool, file, TimeUnit.HOURS.toMillis(1), 1_000_000);
		
		log.info("Start parallel write");
		/**
		 * Parallel write
		 */
		long start_time = System.currentTimeMillis();
		int size = 1_000_000;
		long add_journal_nano_time = IntStream.range(0, size).parallel().mapToLong(i -> {
			try {
				Item item = new Item(null, String.valueOf(i), String.valueOf(i).getBytes());
				item.setTTL(TimeUnit.MINUTES.toMillis(10));
				
				long start_sub = System.nanoTime();
				journal.write(item);
				return System.nanoTime() - start_sub;
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).sum();
		write_pool.awaitTerminationAndShutdown(1, TimeUnit.MINUTES);
		
		long end_time = System.currentTimeMillis();
		
		log.info("End parallel write (" + (double) (end_time - start_time) / 1000d + " sec), add journal time: " + ((double) add_journal_nano_time / 1_000_000_000d) + " sec");
		
		int entry_count = journal.getEntryCount(true);
		assertEquals(size, entry_count);
		
		log.info("Start read");
		int all_count = (int) journal.getAllSince(start_time - 1).peek(entry -> {
			assertTrue(entry.delete_date > start_time);
			assertTrue(entry.delete_date > end_time);
			assertTrue(entry.delete_date < end_time + TimeUnit.MINUTES.toMillis(11));
			assertTrue(entry.update_date >= start_time);
			assertTrue(entry.update_date <= end_time);
		}).count();
		assertEquals(size, all_count);
		
		log.info("End read");
		
		long all_from_now = journal.getAllSince(System.currentTimeMillis()).count();
		assertEquals(0, all_from_now);
		
		log.info("Close 1");
		journal.close();
		
		/**
		 * Re-open
		 */
		HistoryJournal journal2 = new HistoryJournal(write_pool, file, TimeUnit.HOURS.toMillis(1), 1_000_000);
		entry_count = journal2.getEntryCount(true);
		assertEquals(size, entry_count);
		
		log.info("Start read (2)");
		all_count = (int) journal2.getAllSince(0).count();
		assertEquals(size, all_count);
		
		all_from_now = journal2.getAllSince(System.currentTimeMillis()).count();
		assertEquals(0, all_from_now);
		
		log.info("End read (2)");
		journal2.purge();
		journal2.close();
		
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
		}
	}
	
	public void testParallelIO() throws IOException, InterruptedException {
		File file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-history2");
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
		FileUtils.forceMkdir(file);
		
		ThreadPoolExecutorFactory write_pool = new ThreadPoolExecutorFactory("Write journal", Thread.NORM_PRIORITY).setSimplePoolSize();
		
		final HistoryJournal journal = new HistoryJournal(write_pool, file, TimeUnit.SECONDS.toMillis(10), 100_000);
		
		long estimated_process_time = 3000000;
		int size = 100; // 100_000 XXX
		
		IntConsumer saveIntToObj = i -> {
			try {
				journal.write(new Item(null, String.valueOf(i), String.valueOf(i).getBytes()).setTTL(estimated_process_time));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		};
		
		/**
		 * 1st writes
		 */
		long start_push_time = System.currentTimeMillis();
		IntStream.range(0, size).parallel().forEach(saveIntToObj);
		long end_push_time = System.currentTimeMillis();
		
		int entry_count = journal.getEntryCount(true);
		assertTrue(entry_count >= size);
		
		/**
		 * 2nd writes in background
		 */
		write_pool.submit(() -> {
			IntStream.range(size, size * 10).parallel().forEach(saveIntToObj);
		});
		
		Thread.sleep(1);
		/**
		 * Reads during new writes
		 */
		List<HistoryEntry> l_entries = journal.getAllSince(start_push_time).collect(Collectors.toList());
		assertTrue(l_entries.size() >= size);
		
		Map<ItemKey, HistoryEntry> map_entries = l_entries.stream().filter(entry -> {
			return entry.update_date <= end_push_time;
		}).collect(Collectors.toMap(entry -> {
			return entry.key;
		}, entry -> {
			return entry;
		}));
		
		assertEquals(size, map_entries.size());
		
		/**
		 * Checks first reads
		 */
		IntStream.range(0, size).forEach(i -> {
			Item item_ref = new Item(null, String.valueOf(i), String.valueOf(i).getBytes());
			assertTrue("Can't found " + i, map_entries.containsKey(item_ref.getKey()));
			HistoryEntry h_entry = map_entries.get(item_ref.getKey());
			assertEquals(item_ref.getPayload().length, h_entry.data_size);
			item_ref.checkDigest(h_entry.data_digest);
		});
		
		/**
		 * Check since date for reading
		 */
		journal.getAllSince(end_push_time).forEach(h_e -> {
			assertFalse("Item: " + h_e.source_item.getId(), map_entries.containsKey(h_e.key));
		});
		
		/**
		 * Test TTL
		 */
		long wait_time = System.currentTimeMillis() - (end_push_time + estimated_process_time);
		assertTrue("Too long processing...", wait_time < 0);
		
		log.info("Wait " + wait_time + " ms...");
		Thread.sleep(wait_time);
		
		log.info("Old entry count: " + journal.getEntryCount(false)); // TODO test
		
		assertEquals(0, journal.getAllSince(start_push_time).count());
		
		IntStream.range(0, 10).parallel().forEach(saveIntToObj);
		assertEquals(10, journal.getAllSince(start_push_time).count());
		
		log.info("Old entry count: " + journal.getEntryCount(true)); // TODO test
		
		journal.defragment();
		
		log.info("Old entry count: " + journal.getEntryCount(true)); // TODO test
		assertEquals(10, journal.getAllSince(start_push_time).count());
		
		journal.close();
		journal.purge();
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
		}
	}
	
}
