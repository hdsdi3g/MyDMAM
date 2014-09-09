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
import com.google.gson.reflect.TypeToken;

public final class UAJobContext implements Log2Dumpable {
	
	String functionality_name;
	UAConfigurator user_configuration;
	String creator_user_key;
	String basket_name;
	ArrayList<String> items;
	UARange range;
	UAFinisherConfiguration finisher;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("functionality_name", functionality_name);
		dump.add("creator_user_key", creator_user_key);
		dump.add("basket_name", basket_name);
		dump.add("range", range);
		dump.add("items", items);
		dump.addAll(finisher);
		dump.addAll(user_configuration);
		return dump;
	}
	
	UAJobContext() {
	}
	
	static Gson makeGson() {
		GsonBuilder gsonbuilder = new GsonBuilder();
		gsonbuilder.registerTypeAdapter(UAConfigurator.class, new UAConfigurator.JsonUtils());
		gsonbuilder.serializeNulls();
		return gsonbuilder.create();
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