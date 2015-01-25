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

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.GsonIgnore;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class StatElement {
	
	public static final String SCOPE_DIRLIST = "dirlist";
	public static final String SCOPE_PATHINFO = "pathinfo";
	public static final String SCOPE_MTD_SUMMARY = "mtdsummary";
	public static final String SCOPE_COUNT_ITEMS = "countitems";
	public static final String SCOPE_ONLYDIRECTORIES = "onlydirs";
	
	StatElement() {
	}
	
	/**
	 * Referer to "this" element
	 */
	@GsonIgnore
	SourcePathIndexerElement reference;
	Map<String, Object> mtdsummary;
	
	/**
	 * Bounded by from and size query
	 * pathelementkey > StatElement
	 */
	@GsonIgnore
	Map<String, StatElement> items;
	
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
	
	static class Serializer implements JsonSerializer<StatElement> {
		Gson gson_simple;
		Gson gson;
		
		public JsonElement serialize(StatElement src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = gson_simple.toJsonTree(src).getAsJsonObject();
			if (src.reference != null) {
				result.add("reference", src.reference.toGson());
			}
			result.add("items", gson.toJsonTree(src.items));
			return result;
		}
	}
	
}
