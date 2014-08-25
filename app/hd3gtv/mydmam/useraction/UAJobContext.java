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
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Profile;

import java.lang.reflect.Type;
import java.util.ArrayList;

import models.UserProfile;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public final class UAJobContext implements Log2Dumpable {
	
	String functionality_name;
	UAConfigurator user_configuration;
	String creator_user_key;
	String basket_name;
	ArrayList<String> items;
	UARange range;
	UAFinisherConfiguration finisher;
	
	public static void createTask(UAFunctionality functionality, UserProfile userprofile, String basket_name, ArrayList<String> items, UARange range, UAFinisherConfiguration finisher, Profile profile)
			throws ConnectionException {
		// TODO add content of Useraction Creation
		
		if (items.isEmpty()) {
			throw new NullPointerException("Items can't to be empty");
		}
		
		UAJobContext context = new UAJobContext();
		context.functionality_name = functionality.getName();
		context.user_configuration = new UAConfigurator();
		context.creator_user_key = userprofile.key;
		context.basket_name = basket_name;
		context.items = items;
		context.range = range;
		context.finisher = finisher;
		
		StringBuffer name = new StringBuffer();
		name.append(functionality.getLongName());
		name.append(" for ");
		name.append(userprofile.longname);
		name.append(" (");
		name.append(items.size());
		name.append(" items)");
		
		Broker.publishTask(name.toString(), profile, context.toContext(), UAJobContext.class, false, 0, null, false);
	}
	
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
