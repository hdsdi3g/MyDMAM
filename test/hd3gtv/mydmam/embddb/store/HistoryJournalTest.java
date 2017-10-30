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
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class HistoryJournalTest extends TestCase {
	
	private static Logger log = Logger.getLogger(HistoryJournalTest.class);
	private final File file;
	
	public HistoryJournalTest() throws IOException {
		file = new File(System.getProperty("user.home") + File.separator + "mydmam-test-history");
	}
	
	public void testAll() throws IOException {
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
			
			// TODO check entry.key
			// TODO check entry.data_digest
			// TODO check entry.data_size
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
	}
	
	// TODO test TTl + getAllSince + defragment
	// TODO test small size, but check every ones
	// TODO parallel I+O
	
}
