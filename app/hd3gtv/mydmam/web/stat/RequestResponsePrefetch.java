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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.web.stat;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.util.List;
import java.util.concurrent.Callable;

class RequestResponsePrefetch<T> implements Callable<Boolean> {
	
	private RequestResponseCacheFactory<T> factory;
	private List<String> cache_reference_tags;
	
	RequestResponsePrefetch(List<String> cache_reference_tags, RequestResponseCacheFactory<T> factory) {
		this.cache_reference_tags = cache_reference_tags;
		this.factory = factory;
	}
	
	public Boolean call() throws Exception {
		if (RequestResponseCache.DISPLAY_VERBOSE_LOG) {
			Log2Dump dump = new Log2Dump();
			dump.add("cache_reference_tags", cache_reference_tags);
			dump.add("factory", factory.getClass());
			Log2.log.debug("Prefetch items", dump);
		}
		RequestResponseCache.getItems(cache_reference_tags, factory);
		return true;
	}
}