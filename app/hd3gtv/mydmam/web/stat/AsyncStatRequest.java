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
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;

import hd3gtv.tools.GsonIgnore;

public class AsyncStatRequest {
	
	@GsonIgnore
	List<String> pathelementskeys;
	@GsonIgnore
	List<String> scopes_element;
	@GsonIgnore
	List<String> scopes_subelements;
	
	String search;
	int page_from;
	int page_size;
	List<SortDirListing> sort;
	
	static Type type_List_String = new TypeToken<ArrayList<String>>() {
	}.getType();
	static Type type_List_Sort = new TypeToken<ArrayList<SortDirListing>>() {
	}.getType();
	
	public static class Deserializer implements JsonDeserializer<AsyncStatRequest> {
		
		public AsyncStatRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
			AsyncStatRequest result = PathElementStat.gson_simple.fromJson(json, AsyncStatRequest.class);
			result.pathelementskeys = PathElementStat.gson_simple.fromJson(json.getAsJsonObject().get("pathelementskeys"), type_List_String);
			result.scopes_element = PathElementStat.gson_simple.fromJson(json.getAsJsonObject().get("scopes_element"), type_List_String);
			result.scopes_subelements = PathElementStat.gson_simple.fromJson(json.getAsJsonObject().get("scopes_subelements"), type_List_String);
			result.sort = PathElementStat.gson_simple.fromJson(json.getAsJsonObject().get("sort"), type_List_Sort);
			return result;
		}
		
	}
	
}
