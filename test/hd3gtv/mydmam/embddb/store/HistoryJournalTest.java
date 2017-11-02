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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.mydmam.embddb.store.HistoryJournal.HistoryEntry;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class HistoryJournalTest extends TestCase {
	
	private static Logger log = Logger.getLogger(HistoryJournalTest.class);
	
	static ThreadPoolExecutorFactory write_pool;
	static {
		write_pool = new ThreadPoolExecutorFactory("Write journal", Thread.NORM_PRIORITY).setSimplePoolSize();
	}
	
	static Map<ItemKey, Item> write(HistoryJournal journal, int from, int to, long ttl, long force_sleep) {
		return IntStream.range(from, to).mapToObj(i -> {
			try {
				return journal.writeSync(new Item(null, String.valueOf(i), String.valueOf(i).getBytes()).setTTL(ttl));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).peek(item -> {
			if (force_sleep > 0) {
				try {
					Thread.sleep(force_sleep);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}).collect(Collectors.toMap(item -> {
			return item.getKey();
		}, item -> {
			return item;
		}));
	}
	
	private static File makeCleanFile(String fname) throws IOException {
		File file = new File(System.getProperty("user.home") + File.separator + "mydmam-test");
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
		FileUtils.forceMkdir(file);
		return new File(file.getAbsolutePath() + File.separator + fname + ".journal");
	}
	
	/*
	 * ======================================================================================
	 * TESTS ZONE
	 * ======================================================================================
	 * */
	
	public void testPushPull() throws IOException, InterruptedException, ExecutionException {
		File file = makeCleanFile("mydmam-test-history-pushpull");
		
		final HistoryJournal journal = new HistoryJournal(file, TimeUnit.HOURS.toMillis(1), 200);
		
		int size = 100;
		
		/**
		 * Write bulk
		 */
		long start_time = System.currentTimeMillis();
		Map<ItemKey, Item> all_pushed_items = write(journal, 0, size, TimeUnit.SECONDS.toMillis(10), 1);
		long end_time = System.currentTimeMillis();
		
		assertEquals(size, all_pushed_items.size());
		assertTrue(end_time - start_time >= (long) size);
		
		/**
		 * Read bulk
		 */
		int all_count = (int) journal.getAllSince(start_time - 1).peek(entry -> {
			assertTrue(entry.delete_date > start_time);
			assertTrue(entry.delete_date > end_time);
			assertTrue(entry.delete_date < end_time + TimeUnit.SECONDS.toMillis(10));
			assertTrue(entry.update_date >= start_time);
			assertTrue(entry.update_date <= end_time);
		}).peek(entry -> {
			assertTrue(all_pushed_items.containsKey(entry.key));
			Item item = all_pushed_items.get(entry.key);
			assertTrue(Arrays.equals(item.getDigest(), entry.data_digest));
			assertEquals(item.getPayload().length, entry.data_size);
			assertEquals(item.getDeleteDate(), entry.delete_date);
			assertEquals(item.getUpdated(), entry.update_date);
		}).count();
		
		assertEquals(size, all_count);
		journal.close();
	}
	
	public void testLimitDate() throws IOException, InterruptedException, ExecutionException {
		File file = makeCleanFile("mydmam-test-history-limitdate");
		
		final HistoryJournal journal = new HistoryJournal(file, TimeUnit.HOURS.toMillis(1), 200);
		
		int size = 10;
		/**
		 * Write "old" datas
		 */
		Map<ItemKey, Item> all_old_pushed_items = write(journal, size - 20, size - 1, TimeUnit.SECONDS.toMillis(10), 10);
		Thread.sleep(1);
		
		/**
		 * Write "new" datas
		 */
		long start_valid_time = System.currentTimeMillis();
		Map<ItemKey, Item> all_new_pushed_items = write(journal, size + 1, size + 10, TimeUnit.SECONDS.toMillis(10), 0);
		
		/**
		 * Read bulk
		 */
		int all_count = (int) journal.getAllSince(start_valid_time).peek(entry -> {
			assertFalse(all_old_pushed_items.containsKey(entry.key));
			assertTrue(all_new_pushed_items.containsKey(entry.key));
		}).count();
		
		assertEquals(all_new_pushed_items.size(), all_count);
		journal.close();
	}
	
	public void testTTL() throws IOException, InterruptedException, ExecutionException {
		File file = makeCleanFile("mydmam-test-history-ttl");
		
		long bulk_ttl = 100l;
		final HistoryJournal journal = new HistoryJournal(file, bulk_ttl * 2, 200);
		
		int size = 10;
		/**
		 * Write bulk
		 */
		write(journal, 0, size, bulk_ttl, 0);
		
		/**
		 * Make ttl bulk set to delete
		 */
		Thread.sleep(bulk_ttl);
		
		/**
		 * Datas must be still here
		 */
		assertEquals(size, journal.getAllSince(0).count());
		
		/**
		 * Write new bulk
		 */
		write(journal, 0, size, bulk_ttl, 0);
		
		/**
		 * Make fisrt bulk set to expire
		 */
		Thread.sleep(100);
		
		/**
		 * Only second bulk must be here
		 */
		assertEquals(size, journal.getAllSince(0).collect(Collectors.toList()).stream().distinct().count());
		
		journal.close();
	}
	
	public void testDefrag() throws IOException, InterruptedException, ExecutionException {
		File file = makeCleanFile("mydmam-test-history-defrag");
		
		long bulk_ttl = 100l;
		int size = 10;
		
		final HistoryJournal journal = new HistoryJournal(file, bulk_ttl * 2, 5);
		
		/**
		 * Write bulk, wait to expire, check if really empty.
		 */
		write(journal, 0, size, bulk_ttl, 0);
		
		assertEquals(size, journal.getAllSince(0).count());
		
		long previous_size = journal.getFileSize();
		
		/**
		 * Nothing should change
		 */
		journal.defragment();
		assertEquals(size, journal.getAllSince(0).count());
		assertEquals(previous_size, journal.getFileSize());
		
		/**
		 * Wait to expire
		 */
		Thread.sleep(10 + bulk_ttl * 2);
		
		journal.defragment();
		assertEquals(0, journal.getAllSince(0).count());// XXX error
		long shrinked_size = journal.getFileSize();
		assertTrue(shrinked_size < previous_size);
		
		/**
		 * 2nd test
		 * - should expire
		 * - should not expire (long ttl)
		 */
		write(journal, 0, size, bulk_ttl, 0);
		write(journal, 0, size, TimeUnit.HOURS.toMillis(1), 0);
		
		Thread.sleep(1 + bulk_ttl * 2);
		journal.defragment();
		
		assertTrue(shrinked_size > journal.getFileSize());
		assertEquals(previous_size, journal.getFileSize());
		
		journal.close();
	}
	
	// TODO test create + push + close + open + read
	// TODO test concurent push/pull
	
	/*	long all_from_now = journal.getAllSince(System.currentTimeMillis()).count();
		assertEquals(0, all_from_now);
		
		log.info("Close 1");
		journal.close();
		
		 * Re-open
		HistoryJournal journal2 = new HistoryJournal(write_pool, file, TimeUnit.HOURS.toMillis(1), 1_000_000);
		
		log.info("Start read (2)");
		all_count = (int) journal2.getAllSince(0).count();
		assertEquals(size, all_count);
		
		all_from_now = journal2.getAllSince(System.currentTimeMillis()).count();
		assertEquals(0, all_from_now);
		
		log.info("End read (2)");
		journal2.close();
		
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
		}
	}*/
	
	@Deprecated
	public void AtestParallelIO() throws IOException, InterruptedException, ExecutionException {
		File file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-history2");
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
		FileUtils.forceMkdir(file);
		
		ThreadPoolExecutorFactory write_pool = new ThreadPoolExecutorFactory("Write journal", Thread.NORM_PRIORITY).setSimplePoolSize();
		
		int size = 10_000;
		long estimated_process_time = 1_000;
		
		final HistoryJournal journal = new HistoryJournal(file, estimated_process_time, size / 2);
		int total_pushed = 0;
		
		/**
		 * 1st writes
		 */
		long start_push_time = System.currentTimeMillis();
		
		Map<ItemKey, String> rainbow_key = IntStream.range(0, 100).mapToObj(i -> {
			Item item = new Item(null, String.valueOf(i), String.valueOf(i).getBytes()).setTTL(estimated_process_time);
			try {
				journal.writeSync(item);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return item;
		}).collect(Collectors.toMap(item -> {
			return item.getKey();
		}, item -> {
			return item.getId();
		}));
		long end_push_time = System.currentTimeMillis();
		
		total_pushed += size;
		
		/*
		 * 2nd writes in background
		 */
		/*List<Item>> bgk_writes = journal.write(IntStream.range(size, size * 10).mapToObj(i -> {
			return new Item(null, String.valueOf(i), String.valueOf(i).getBytes()).setTTL(estimated_process_time);
		}).peek(item -> {
			rainbow_key.put(item.getKey(), item.getId());
		}));
		total_pushed += size * 10;*/
		
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
		
		Thread.sleep(1000);
		
		/**
		 * Check since date for reading
		 */
		journal.getAllSince(end_push_time).forEach(h_e -> {
			if (map_entries.containsKey(h_e.key)) {
				if (rainbow_key.containsKey(h_e.key)) {
					System.out.println(rainbow_key.get(h_e.key) + " " + (System.currentTimeMillis() - h_e.update_date) + " " + (System.currentTimeMillis() - h_e.delete_date));
				} else {
					System.out.println((System.currentTimeMillis() - h_e.update_date) + " " + (System.currentTimeMillis() - h_e.delete_date));
				}
			}
			assertFalse("Item: " + h_e.key, map_entries.containsKey(h_e.key));
		});
		
		/**
		 * Test TTL
		 */
		long wait_time = (end_push_time + 3 * estimated_process_time) - System.currentTimeMillis();
		assertTrue("Too long processing...", wait_time > 0);
		
		log.info("Wait " + wait_time + " ms...");
		Thread.sleep(wait_time);
		
		assertEquals(0, journal.getAllSince(start_push_time).count());
		
		int size_2nd_push = 10;
		IntStream.range(0, size_2nd_push).forEach(i -> {
			try {
				journal.writeSync(new Item(null, String.valueOf(i), String.valueOf(i).getBytes()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals(size_2nd_push, journal.getAllSince(start_push_time).count());
		
		journal.defragment();
		
		assertEquals(size_2nd_push, journal.getAllSince(start_push_time).count());
		
		IntStream.range(0, size_2nd_push).forEach(i -> {
			try {
				journal.writeSync(new Item(null, String.valueOf(i), String.valueOf(i).getBytes()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		journal.defragment();
		
		journal.close();
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
		}
	}
	
}
