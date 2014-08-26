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
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Profile;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
	
	public static void createTask(UAFunctionality functionality, UAConfigurator user_configuration, UserProfile userprofile, String basket_name, ArrayList<SourcePathIndexerElement> items_spie,
			UARange range, UAFinisherConfiguration finisher) throws ConnectionException {
		if (items_spie.isEmpty()) {
			throw new NullPointerException("Items can't to be empty");
		}
		
		HashMap<Profile, UAJobContext> contexts_by_profiles = new LinkedHashMap<Profile, UAJobContext>();// TODO
		
		UAJobContext context = new UAJobContext();
		context.functionality_name = functionality.getName();
		context.user_configuration = user_configuration;
		context.creator_user_key = userprofile.key;
		context.basket_name = basket_name;
		
		context.items = new ArrayList<String>();
		for (int pos = 0; pos < items_spie.size(); pos++) {
			context.items.add(items_spie.get(pos).prepare_key());
		}
		context.range = range;
		context.finisher = finisher;
		
		StringBuffer name = new StringBuffer();
		name.append(functionality.getLongName());
		name.append(" for ");
		name.append(userprofile.longname);
		name.append(" (");
		name.append(context.items.size());
		name.append(" items)");
		
		Profile profile = new Profile("useraction", functionality.getName() + "=" + "<storage>");
		
		Broker.publishTask(name.toString(), profile, context.toContext(), UAJobContext.class, false, 0, null, false);
		// TODO range ?
		// TODO notification ?
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
