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
	
	public void testAll() throws IOException, InterruptedException, ExecutionException {
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
		
		List<Item> all_pushed_items = IntStream.range(0, 10).parallel().mapToObj(chunk_factor -> {
			try {
				/*return journal.write(IntStream.range(chunk_factor * size / 10, (chunk_factor + 1) * size / 10).mapToObj(i -> {
					return new Item(null, String.valueOf(i), String.valueOf(i).getBytes()).setTTL(TimeUnit.MINUTES.toMillis(10));
				})).get();*/
				
				return IntStream.range(chunk_factor * size / 10, (chunk_factor + 1) * size / 10).mapToObj(i -> {
					Item item = new Item(null, String.valueOf(i), String.valueOf(i).getBytes()).setTTL(TimeUnit.MINUTES.toMillis(10));
					try {
						journal.writeSync(item);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					return item;
				}).collect(Collectors.toList());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}).flatMap(list -> {
			return list.stream();
		}).collect(Collectors.toList());
		
		assertEquals(size, all_pushed_items.size());
		
		long end_time = System.currentTimeMillis();
		
		log.info("End parallel write (" + (double) (end_time - start_time) / 1000d + " sec)");
		
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
		journal2.close();
		
		try {
			FileUtils.forceDelete(file);
		} catch (IOException e) {
		}
	}
	
	public void testParallelIO() throws IOException, InterruptedException, ExecutionException {
		File file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-history2");
		try {
			FileUtils.forceDelete(file);
		} catch (FileNotFoundException e) {
		}
		FileUtils.forceMkdir(file);
		
		ThreadPoolExecutorFactory write_pool = new ThreadPoolExecutorFactory("Write journal", Thread.NORM_PRIORITY).setSimplePoolSize();
		
		/**
		 * This values are tricky.
		 * Tested with *simple* SSD (300 MB/sec).
		 */
		int size = 10_000;
		long estimated_process_time = 1_000;
		
		final HistoryJournal journal = new HistoryJournal(write_pool, file, 900, size / 2);
		int total_pushed = 0;
		
		/**
		 * 1st writes
		 */
		long start_push_time = System.currentTimeMillis();
		
		Map<ItemKey, String> rainbow_key = IntStream.range(0, size).mapToObj(i -> {
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
		
		int entry_count = journal.getEntryCount(true);
		assertTrue(entry_count >= size);
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
		
		assertEquals(total_pushed - size, journal.getEntryCount(false));
		assertEquals(0, journal.getAllSince(start_push_time).count());
		
		int size_2nd_push = 10;
		IntStream.range(0, size_2nd_push).forEach(i -> {
			try {
				journal.writeSync(new Item(null, String.valueOf(i), String.valueOf(i).getBytes()));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		
		assertEquals(total_pushed - size + size_2nd_push, journal.getEntryCount(true));
		assertEquals(size_2nd_push, journal.getAllSince(start_push_time).count());
		
		journal.defragment();
		
		assertEquals(size_2nd_push, journal.getEntryCount(true));
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
