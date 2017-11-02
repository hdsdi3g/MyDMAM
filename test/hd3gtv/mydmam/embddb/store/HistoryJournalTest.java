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
import java.nio.channels.ClosedChannelException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

public class HistoryJournalTest extends TestCase {
	
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
		Thread.sleep(100l + bulk_ttl * 2l);
		
		journal.defragment();
		assertEquals(0, journal.getAllSince(0).count());
		long shrinked_size = journal.getFileSize();
		assertTrue(shrinked_size < previous_size);
		
		/**
		 * 2nd test
		 * - should expire
		 * - should not expire (long ttl)
		 */
		write(journal, 0, size, bulk_ttl, 0);
		write(journal, 0, size, TimeUnit.HOURS.toMillis(1), 0);
		
		Thread.sleep(100l + bulk_ttl * 2l);
		journal.defragment();
		
		assertTrue(shrinked_size < journal.getFileSize());
		assertEquals(previous_size, journal.getFileSize());
		
		/**
		 * 3rd test
		 * ++ should expire
		 */
		long before_3rd = journal.getFileSize();
		write(journal, 0, size, bulk_ttl, 0);
		// long after_3rd = journal.getFileSize();
		
		Thread.sleep(100l + bulk_ttl * 2l);
		journal.defragment();
		assertEquals(before_3rd, journal.getFileSize());
		
		journal.close();
	}
	
	public void testOpenCloseReopen() throws IOException, InterruptedException, ExecutionException {
		File file = makeCleanFile("mydmam-test-history-openclosereopen");
		
		final HistoryJournal journal = new HistoryJournal(file, TimeUnit.HOURS.toMillis(1), 200);
		
		int size = 100;
		
		/**
		 * Write bulk
		 */
		Map<ItemKey, Item> all_pushed_items = write(journal, 0, size, TimeUnit.SECONDS.toMillis(10), 0);
		assertEquals(size, all_pushed_items.size());
		
		/**
		 * Read bulk
		 */
		int all_count = (int) journal.getAllSince(0).peek(entry -> {
			assertTrue(all_pushed_items.containsKey(entry.key));
			Item item = all_pushed_items.get(entry.key);
			assertTrue(Arrays.equals(item.getDigest(), entry.data_digest));
			assertEquals(item.getPayload().length, entry.data_size);
			assertEquals(item.getDeleteDate(), entry.delete_date);
			assertEquals(item.getUpdated(), entry.update_date);
		}).count();
		
		assertEquals(size, all_count);
		journal.close();
		
		/**
		 * Test if close really close
		 */
		ClosedChannelException real_error = null;
		try {
			journal.clear();
		} catch (ClosedChannelException e) {
			real_error = e;
		}
		assertNotNull(real_error);
		
		final HistoryJournal journal2 = new HistoryJournal(file, TimeUnit.HOURS.toMillis(1), 200);
		
		assertEquals(all_pushed_items.size(), journal2.getAllSince(0).peek(entry -> {
			assertTrue(all_pushed_items.containsKey(entry.key));
			Item item = all_pushed_items.get(entry.key);
			assertTrue(Arrays.equals(item.getDigest(), entry.data_digest));
			assertEquals(item.getPayload().length, entry.data_size);
			assertEquals(item.getDeleteDate(), entry.delete_date);
			assertEquals(item.getUpdated(), entry.update_date);
		}).count());
		
		write(journal2, 0, size, TimeUnit.SECONDS.toMillis(10), 0);
		journal2.close();
		
		final HistoryJournal journal3 = new HistoryJournal(file, TimeUnit.HOURS.toMillis(1), 200);
		
		assertEquals(all_pushed_items.size() * 2, journal3.getAllSince(0).count());
		journal3.close();
	}
	
	public void testConcurentAccess() throws IOException, InterruptedException, ExecutionException {
		File file = makeCleanFile("mydmam-test-history-testconcurrent");
		
		final HistoryJournal journal = new HistoryJournal(file, TimeUnit.HOURS.toMillis(1), 200);
		
		int size = 100;
		IntStream.range(0, size).parallel().forEach(i -> {
			write(journal, i, i + 1, TimeUnit.SECONDS.toMillis(10), 0);
		});
		
		assertEquals(size, journal.getAllSince(0).count());
		
		Thread.sleep(10);
		final long max_time = System.currentTimeMillis();
		Thread.sleep(10);
		
		int size2 = IntStream.range(0, size / 2).parallel().map(i -> {
			try {
				if (i % 2 == 0) {
					assertEquals(size, journal.getAllSince(0).filter(item -> {
						return item.update_date < max_time;
					}).count());
					return 0;
				} else {
					return write(journal, i, i + 1, TimeUnit.SECONDS.toMillis(10), 0).size();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).sum();
		
		assertEquals(size2, size / 4);
		assertEquals(size + size2, journal.getAllSince(0).count());
		journal.close();
	}
	
}
