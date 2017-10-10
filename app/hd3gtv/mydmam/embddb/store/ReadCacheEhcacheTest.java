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

import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.google.common.math.IntMath;

import hd3gtv.mydmam.Loggers;
import junit.framework.TestCase;

public class ReadCacheEhcacheTest extends TestCase {
	
	public void testAll() throws Exception {
		Logger log = Logger.getLogger(ReadCacheEhcache.class);
		log.setLevel(Level.DEBUG);
		
		ReadCacheEhcache cache = ReadCacheEhcache.getCache();
		
		final long max_bytes_local_heap = cache.getMaxBytesLocalHeap();
		
		final int size = IntStream.range(1, Integer.MAX_VALUE).map(i -> {
			return IntMath.pow(10, i);
		}).filter(i -> {
			return LongStream.range(1, i).map(j -> j).sum() > max_bytes_local_heap;
		}).findFirst().getAsInt();
		
		log.info("Put " + size + " items in cache");
		
		long total_size = IntStream.range(1, size)/*.parallel()*/.mapToLong(i -> {
			byte[] bytes = new byte[i];
			cache.put(new Item(String.valueOf(i), bytes));
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
		// cache.put(new Item(String.valueOf(size - 1), new byte[size - 1]));
		assertTrue("Can't found the last pushed item", cache.has(last_item_key));
		Item last_pushed_item = cache.get(last_item_key);
		assertNotNull("Can't found the last pushed item", last_pushed_item);
		assertEquals("Invalid data size for the last pushed item", size - 1, last_pushed_item.getPayload().length);
		
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
		
		/*
		TODO test re-put with ttl (in parallel)
		TODO test clear
		public boolean has(ItemKey key);
		public void remove(ItemKey key);
		public void purgeAll();
		 * */
		
		// objc[75364]: Class JavaLaunchHelper is implemented in both /Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home/bin/java (0x10ff274c0) and /Library/Java/JavaVirtualMachines/jdk1.8.0_111.jdk/Contents/Home/jre/lib/libinstrument.dylib (0x12f7fb4e0). One of the two will be used. Which one is undefined.
	}
}
