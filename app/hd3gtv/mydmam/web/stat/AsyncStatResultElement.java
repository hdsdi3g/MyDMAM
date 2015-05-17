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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.web.stat;

import hd3gtv.mydmam.web.AsyncJSSerializer;
import hd3gtv.tools.GsonIgnore;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

public class AsyncStatResultElement extends AsyncStatResultSubElement {
	
	/**
	 * Bounded by from and size query
	 * pathelementkey > StatElement
	 */
	@GsonIgnore
	Map<String, AsyncStatResultSubElement> items;
	
	/**
	 * Total not bounded
	 */
	Long items_total;
	
	/**
	 * Bounded values
	 */
	Integer items_page_from;
	
	/**
	 * Bounded values
	 */
	Integer items_page_size;
	
	Boolean search_return_nothing;
	
	static class Serializer implements AsyncJSSerializer<AsyncStatResultElement> {
		public JsonElement serialize(AsyncStatResultElement src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = Stat.gson_simple.toJsonTree(src).getAsJsonObject();
			if (src.reference != null) {
				result.add("reference", src.reference.toGson());
			}
			result.add("items", Stat.gson.toJsonTree(src.items));
			return result;
		}
		
		public Class<AsyncStatResultElement> getEnclosingClass() {
			return AsyncStatResultElement.class;
		}
	}
	
	boolean isEmpty() {
		if (super.isEmpty() == false) {
			return false;
		}
		if (items == null) {
			return true;
		}
		return items.isEmpty();
	}
	
}
