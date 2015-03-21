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

import hd3gtv.mydmam.pathindexing.WebCacheInvalidation;
import hd3gtv.tools.GsonIgnore;

import com.google.gson.JsonObject;

final class RequestResponseCacheExpirableItem<E> {
	
	private static WebCacheInvalidation web_cache_invalidation;
	
	static {
		web_cache_invalidation = new WebCacheInvalidation();
	}
	
	@GsonIgnore
	private E item;
	
	private long indexed_date;
	private String storage_name;
	
	RequestResponseCacheExpirableItem(E item, String storage_name) {
		this.item = item;
		this.indexed_date = System.currentTimeMillis();
		this.storage_name = storage_name;
	}
	
	@SuppressWarnings("unused")
	private RequestResponseCacheExpirableItem() {
		/**
		 * Used by Gson
		 */
	}
	
	E getItem() {
		return item;
	}
	
	void setItem(E item) {
		this.item = item;
	}
	
	boolean hasExpired() {
		return (indexed_date < web_cache_invalidation.getLastInvalidationDate(storage_name));
	}
	
	final JsonObject getCacheStatus() {
		return Stat.gson_simple.toJsonTree(this).getAsJsonObject();
	}
	
}
