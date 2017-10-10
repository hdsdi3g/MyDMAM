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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import org.apache.log4j.Logger;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

public final class ReadCacheEhcache implements ReadCache {
	
	private static Logger log = Logger.getLogger(ReadCacheEhcache.class);
	
	private static ReadCacheEhcache current_instance;
	
	public static ReadCacheEhcache getCache() {
		if (current_instance == null) {
			current_instance = new ReadCacheEhcache();
		}
		return current_instance;
	}
	
	private Cache lh_cache;
	
	private ReadCacheEhcache() {
		CacheConfiguration conf = new CacheConfiguration(getClass().getSimpleName(), 0);
		// conf.memoryStoreEvictionPolicy(memoryStoreEvictionPolicy);
		// conf.eternal(false);
		// conf.timeToLiveSeconds(timeToLiveSeconds);
		// conf.timeToIdleSeconds(timeToIdleSeconds);
		// conf.persistence(persistenceConfiguration)
		// conf.diskPersistent(diskPersistent);
		// conf.overflowToDisk(overflowToDisk);
		// conf.diskExpiryThreadIntervalSeconds(diskExpiryThreadIntervalSeconds);
		// conf.maxElementsOnDisk(maxElementsOnDisk);
		// conf.diskSpoolBufferSizeMB(diskSpoolBufferSizeMB);
		// conf.clearOnFlush(true);
		
		MemoryMXBean mmvb = ManagementFactory.getMemoryMXBean();
		long max_heap_mem = mmvb.getHeapMemoryUsage().getMax();
		long used_heap_mem = mmvb.getHeapMemoryUsage().getUsed();
		long avaliable_heap_mem = max_heap_mem - used_heap_mem;
		long max_heap = Configuration.global.getValue("embddb", "ehcache_maxbyteslocalheap", avaliable_heap_mem / 4l);
		if (max_heap > avaliable_heap_mem) {
			throw new IndexOutOfBoundsException("Can't map a so large amount of memory. Wanted: " + max_heap + ", free: " + avaliable_heap_mem);
		}
		if (log.isDebugEnabled()) {
			log.debug("Actual max heap: " + Loggers.numberFormat(max_heap_mem) + " bytes, actual used heap: " + Loggers.numberFormat(used_heap_mem) + " bytes, set setMaxBytesLocalHeap to " + Loggers.numberFormat(max_heap) + " bytes");
		}
		
		conf.setMaxBytesLocalHeap(max_heap);
		
		lh_cache = new Cache(conf);
		CacheManager cacheManager = CacheManager.create();
		cacheManager.addCache(lh_cache);
		lh_cache.bootstrap();
		// cache = cacheManager.getCache(cacheName);
	}
	
	long getMaxBytesLocalHeap() {
		return lh_cache.getCacheConfiguration().getMaxBytesLocalHeap();
	}
	
	public void put(Item item) {
		// TODO read + set cache ttl (ttl, not expiration)
		lh_cache.put(new Element(item.getKey(), item));
	}
	
	public Item get(ItemKey key) {
		Element element = lh_cache.get(key);
		if (element == null) {
			return null;
		}
		return (Item) element.getObjectValue();
	}
	
	public boolean has(ItemKey key) {
		return lh_cache.isKeyInCache(key);
	}
	
	public void remove(ItemKey key) {
		lh_cache.remove(key);
	}
	
	public void purgeAll() {
		lh_cache.removeAll();
	}
	
}
