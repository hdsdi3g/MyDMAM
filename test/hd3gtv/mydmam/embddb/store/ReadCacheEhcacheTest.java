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
 * Copyright (C) hdsdi3g for hd3g.tv 10 oct. 2017
 * 
*/
package hd3gtv.mydmam.embddb.store;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.math.IntMath;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import junit.framework.TestCase;

public class ReadCacheEhcacheTest extends TestCase {
	
	public void testAll() throws Exception {
		Logger log = Logger.getLogger(ReadCacheEhcache.class);
		log.setLevel(Level.DEBUG);
		
		ReadCacheEhcache cache = ReadCacheEhcache.getCache();
		
		final long max_bytes_local_heap = cache.getMaxBytesLocalHeap();
		
		/**
		 * Compute best number of items to be sure to overload the cache size.
		 * Triangular numbers for 10^n
		 */
		final int size = IntStream.range(1, Integer.MAX_VALUE).map(i -> {
			return IntMath.pow(10, i);
		}).filter(i -> {
			return LongStream.range(1, i).map(j -> j).sum() > max_bytes_local_heap;
		}).findFirst().getAsInt();
		
		log.info("Put " + size + " items in cache");
		
		/**
		 * Big data push (only 0)
		 */
		long total_size = IntStream.range(1, size).mapToLong(i -> {
			byte[] bytes = new byte[i];
			cache.put(new Item(null, String.valueOf(i), bytes));
			return (long) bytes.length;
		}).sum();
		
		log.info("Total size pushed: " + Loggers.numberFormat(total_size) + " bytes");
		assertTrue("Invalid real pushed data size", cache.getMaxBytesLocalHeap() < total_size);
		assertNull("Found a never pushed item", cache.get(new ItemKey(String.valueOf(0))));
		assertNull("Found a \"should-be in garbage\" item", cache.get(new ItemKey(String.valueOf(1))));
		
		/**
		 * Check the last item
		 */
		ItemKey last_item_key = new ItemKey(String.valueOf(size - 1));
		assertTrue("Can't found the last pushed item", cache.has(last_item_key));
		Item last_pushed_item = cache.get(last_item_key);
		assertNotNull("Can't found the last pushed item", last_pushed_item);
		assertEquals("Invalid data size for the last pushed item", size - 1, last_pushed_item.getPayload().length);
		
		/**
		 * Check efficiency and total size for stored items
		 */
		long total_readed_size = IntStream.range(1, size).parallel().mapToLong(i -> {
			ItemKey current_key = new ItemKey(String.valueOf(i));
			Item current_item = cache.get(current_key);
			if (current_item == null) {
				return 0l;
			}
			assertEquals("Invalid data size for the item #" + i, i, current_item.getPayload().length);
			return (long) i;
		}).sum();
		
		log.info("Total size readed (keeped in memory): " + Loggers.numberFormat(total_readed_size) + " bytes");
		assertTrue(total_readed_size <= max_bytes_local_heap);
		
		double efficiency = 100d * (double) total_readed_size / (double) max_bytes_local_heap;
		assertTrue("Bad cache efficiency (" + efficiency + "%)", efficiency > 98d);
		
		/**
		 * Test TTL and natural cache expiration
		 */
		long wait_time = 100l;
		byte[] new_payload = "AAA".getBytes(MyDMAM.UTF8);
		last_pushed_item.setPayload(new_payload);
		last_pushed_item.setTTL(wait_time);
		cache.put(last_pushed_item);
		assertTrue("Can't found the last pushed item", cache.has(last_item_key));
		last_pushed_item = cache.get(last_item_key);
		assertNotNull("Can't found the last pushed item", last_pushed_item);
		assertEquals("Invalid payload", new_payload, last_pushed_item.getPayload());
		Thread.sleep(wait_time);
		assertNull("The last pushed item has not expired", cache.get(last_item_key));
		
		/**
		 * Test full purge
		 */
		ItemKey last_previous_item_key = new ItemKey(String.valueOf(size - 2));
		assertTrue("Can't found the last pushed item", cache.has(last_previous_item_key));
		cache.clear();
		assertFalse("Don't purge all", cache.has(last_previous_item_key));
		
		/**
		 * Test parallel random I/O
		 */
		final int secure_size = Math.round((float) size / 2.5f);
		final AtomicInteger action_count = new AtomicInteger(0);
		final ThreadPoolExecutorFactory pool = new ThreadPoolExecutorFactory("test-cache", Thread.NORM_PRIORITY);
		final String NEW_PATH = "newpath";
		final byte[] NEW_PAYLOAD = "newpayload".getBytes(MyDMAM.UTF8);
		
		IntStream.range(1, secure_size).forEach(i -> {
			pool.execute(() -> {
				byte[] bytes = new byte[i];
				final String _id = String.valueOf(i);
				Item new_item = new Item(null, _id, bytes);
				cache.put(new_item);
				ItemKey key = new_item.getKey();
				
				pool.execute(() -> {
					Item updated = cache.get(key);
					assertNotNull(updated);
					updated.setPath(NEW_PATH).setPayload(NEW_PAYLOAD);
					
					pool.execute(() -> {
						cache.put(updated);
						pool.execute(() -> {
							Item get_updated = cache.get(key);
							assertNotNull(get_updated);
							assertEquals(NEW_PATH, get_updated.getPath());
							assertEquals(NEW_PAYLOAD, get_updated.getPayload());
							cache.remove(key);
							pool.execute(() -> {
								assertFalse(cache.has(key));
								action_count.incrementAndGet();
							});
						});
					});
				});
			});
		});
		
		while (pool.getTotalActive() > 0 | pool.getQueueSize() > 0) {
			Thread.sleep(1);
		}
		assertEquals(secure_size, action_count.get() + 1);
	}
}
