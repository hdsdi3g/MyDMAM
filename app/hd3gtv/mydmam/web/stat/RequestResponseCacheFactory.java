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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public abstract class RequestResponseCacheFactory<E> {
	
	private static JsonParser json_parser;
	static {
		json_parser = new JsonParser();
	}
	
	/**
	 * @return never null
	 */
	abstract HashMap<String, RequestResponseCacheExpirableItem<E>> makeValues(List<String> cache_reference_tags) throws Exception;
	
	/**
	 * @return never null
	 */
	final String serializeThis(RequestResponseCacheExpirableItem<E> item) throws Exception {
		JsonObject result = item.getCacheStatus();
		result.add("item", toJson(item.getItem()));
		return Stat.gson_simple.toJson(result);
	}
	
	private Type RequestResponseExpirableItem_typeOfT = new TypeToken<RequestResponseCacheExpirableItem<E>>() {
	}.getType();
	
	/**
	 * @return never null
	 */
	final RequestResponseCacheExpirableItem<E> deserializeThis(String raw_json_value) throws Exception {
		JsonObject jo_ei = json_parser.parse(raw_json_value).getAsJsonObject();
		RequestResponseCacheExpirableItem<E> result = Stat.gson_simple.fromJson(jo_ei, RequestResponseExpirableItem_typeOfT);
		result.setItem(fromJson(jo_ei.get("item").getAsJsonObject()));
		return result;
	}
	
	/**
	 * @return never null
	 */
	protected abstract JsonElement toJson(E item) throws Exception;
	
	/**
	 * @return never null
	 */
	protected abstract E fromJson(JsonElement value) throws Exception;
	
}
