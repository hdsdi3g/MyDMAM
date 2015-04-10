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
package hd3gtv.mydmam.web.search;

import hd3gtv.mydmam.web.AsyncJSManager;
import hd3gtv.mydmam.web.AsyncJSResponseObject;
import hd3gtv.tools.GsonIgnoreStrategy;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public final class AsyncSearchResults implements AsyncJSResponseObject {
	
	/**
	 * Also serialize AsyncSearchResult
	 */
	public static final Serializer serializer = new Serializer();
	
	/**
	 * Only used here for the toJsonString
	 */
	private static final Gson internal_gson;
	private static final Type type_Resultlist = new TypeToken<ArrayList<AsyncSearchResult>>() {
	}.getType();
	
	static {
		GsonBuilder builder = new GsonBuilder();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		builder.serializeNulls();
		builder.registerTypeAdapter(AsyncSearchResults.class, serializer);
		internal_gson = builder.create();
	}
	
	private List<AsyncSearchResult> results;
	
	private String q;
	
	private int from;
	
	public static class Serializer implements JsonSerializer<AsyncSearchResults> {
		public JsonElement serialize(AsyncSearchResults src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			result.addProperty("q", src.q);
			result.addProperty("from", src.from);
			result.add("results", AsyncJSManager.global.getGsonSimple().toJsonTree(src.results, type_Resultlist));
			return result;
		}
	}
	
	public AsyncSearchResults(String q, int from) {
		if (q == null) {
			q = "";
		}
		this.q = q;
		if (from > 0) {
			this.from = from;
		} else {
			this.from = 0;
		}
		
		// TODO Do search
		results = new ArrayList<AsyncSearchResult>();
		HashMap<String, Object> content = new HashMap<String, Object>();
		content.put("val1", "value1");
		content.put("val2", 2);
		content.put("val3", Lists.newArrayList("Sub value 3 A", "Sub value 3 B", "Sub value 3 C"));
		results.add(new AsyncSearchResult("type1", "k1", content, 0.5f));
	}
	
	public String toJsonString() {
		return internal_gson.toJson(this);
	}
	
	public String getQ() {
		return q;
	}
	
	public boolean hasResults() {
		return results.isEmpty() == false;
	}
	
}
