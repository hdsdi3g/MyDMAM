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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.lang.reflect.Type;
import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public final class UAJobContext implements Log2Dumpable {
	
	String functionality_name;
	JsonObject user_configuration;
	String creator_user_key;
	String creator_user_group_name;
	ArrayList<String> items;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("functionality_name", functionality_name);
		dump.add("creator_user_key", creator_user_key);
		dump.add("creator_user_group_name", creator_user_group_name);
		dump.add("items", items);
		dump.add("user_configuration", user_configuration);
		return dump;
	}
	
	UAJobContext() {
	}
	
	static Gson makeGson() {
		return new GsonBuilder().serializeNulls().create();
	}
	
	static UAJobContext importFromJob(JSONObject context) {
		if (context == null) {
			return null;
		}
		Gson gson = makeGson();
		UAJobContext result = gson.fromJson(context.toJSONString(), UAJobContext.class);
		
		if (context.containsKey("items")) {
			Type typeOfT = new TypeToken<ArrayList<String>>() {
			}.getType();
			result.items = gson.fromJson(((JSONArray) context.get("items")).toJSONString(), typeOfT);
		} else {
			result.items = null;
		}
		
		return result;
	}
	
	JSONObject toContext() {
		JSONParser jp = new JSONParser();
		try {
			return (JSONObject) jp.parse(makeGson().toJson(this));
		} catch (ParseException e) {
			return new JSONObject();
		}
	}
	
}
