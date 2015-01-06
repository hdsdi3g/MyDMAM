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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.mail.notification.Notification;
import hd3gtv.mydmam.mail.notification.NotifyReason;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import models.UserProfile;

import org.elasticsearch.action.index.IndexRequest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public final class UACreationRequest {
	
	public static final String ES_TYPE = "log";
	private static final long LOG_LIFETIME = 3600 * 24 * 365 * 2; // 2 years
	private static final String NOTIFICATION_REFERENCE = "useraction";
	
	static {
		try {
			Elasticsearch.enableTTL(Notification.ES_INDEX, ES_TYPE);
		} catch (Exception e) {
			Log2.log.error("Can't enable TTL in ES", e);
		}
	}
	
	private ArrayList<String> items;
	private String basket_name;
	private String comment;
	private ArrayList<NotifyReason> notification_reasons;
	private UAFinisherConfiguration finisher;
	private UACreationRange range;
	private ArrayList<JsonObject> configured_functionalities;
	private ArrayList<String> dependent_storages;
	private long created_at;
	
	private UserProfile userprofile;
	private ArrayList<String> user_restricted_privileges;
	private ArrayList<String> created_jobs_keys;
	
	public static class Serializer implements JsonSerializer<UACreationRequest>, JsonDeserializer<UACreationRequest> {
		
		Gson gson_simple = new Gson();
		private static Type typeOfArrayString = new TypeToken<ArrayList<String>>() {
		}.getType();
		private static Type typeOfArrayNotifyReason = new TypeToken<ArrayList<NotifyReason>>() {
		}.getType();
		
		public JsonElement serialize(UACreationRequest src, Type typeOfSrc, JsonSerializationContext context) {
			/**
			 * Only for log to ES.
			 */
			JsonObject result = gson_simple.toJsonTree(src).getAsJsonObject();
			
			JsonArray ja_configured_functionalities = new JsonArray();
			for (int pos = 0; pos < src.configured_functionalities.size(); pos++) {
				ja_configured_functionalities.add(src.configured_functionalities.get(pos));
			}
			result.add("configured_functionalities", ja_configured_functionalities);
			result.remove("user_restricted_privileges");
			
			result.remove("userprofile");
			result.addProperty("userprofile_longname", src.userprofile.longname);
			result.addProperty("userprofile_language", src.userprofile.language);
			result.addProperty("userprofile_key", src.userprofile.key);
			result.addProperty("userprofile_email", src.userprofile.email);
			
			return result;
		}
		
		public UACreationRequest deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			System.out.println(json.toString());// XXX
			
			JsonObject request = json.getAsJsonObject();
			UACreationRequest result = gson_simple.fromJson(json, UACreationRequest.class);
			result.items = UAManager.getGson().fromJson(request.get("items"), typeOfArrayString);
			if (result.items.isEmpty()) {
				throw new JsonParseException("No items");
			}
			
			result.notification_reasons = UAManager.getGson().fromJson(request.get("notification_reasons"), typeOfArrayNotifyReason);
			result.finisher = UAManager.getGson().fromJson(request.get("finisher"), UAFinisherConfiguration.class);
			
			result.configured_functionalities = new ArrayList<JsonObject>();
			JsonArray ja_configured_functionalities = request.get("configured_functionalities").getAsJsonArray();
			for (int pos = 0; pos < ja_configured_functionalities.size(); pos++) {
				result.configured_functionalities.add(ja_configured_functionalities.get(pos).getAsJsonObject());
			}
			
			result.created_at = System.currentTimeMillis();
			return result;
		}
		
	}
	
	public UACreationRequest setUserprofile(UserProfile userprofile) {
		this.userprofile = userprofile;
		return this;
	}
	
	public UACreationRequest setUserRestrictedPrivileges(ArrayList<String> user_restricted_privileges) {
		this.user_restricted_privileges = user_restricted_privileges;
		return this;
	}
	
	private void addUALogEntry() {
		String json_data = UAManager.getGson().toJson(this);
		
		long now = System.currentTimeMillis();
		
		StringBuffer sb = new StringBuffer();
		sb.append(now);
		sb.append(":");
		sb.append(userprofile.key);
		
		IndexRequest ir = new IndexRequest(Notification.ES_INDEX, ES_TYPE, sb.toString());
		ir.source(json_data);
		ir.ttl(LOG_LIFETIME);
		Elasticsearch.getClient().index(ir);
		
		Log2Dump dump = new Log2Dump();
		dump.add("id", sb.toString());
		dump.add("raw_json", json_data);
		Log2.log.debug("Create UserAction", dump);
	}
	
	public void createJobs() throws Exception {
		ArrayList<SourcePathIndexerElement> basket_content = new ArrayList<SourcePathIndexerElement>(items.size());
		Explorer explorer = new Explorer();
		LinkedHashMap<String, SourcePathIndexerElement> elements = explorer.getelementByIdkeys(items);
		basket_content.addAll(elements.values());
		items.clear();
		items.addAll(elements.keySet());
		
		dependent_storages = new ArrayList<String>();
		for (int pos = 0; pos < basket_content.size(); pos++) {
			if (dependent_storages.contains(basket_content.get(pos).storagename) == false) {
				dependent_storages.add(basket_content.get(pos).storagename);
			}
		}
		
		created_jobs_keys = new ArrayList<String>();
		
		// TODO
		System.out.println(UAManager.getGson().toJson(this));// XXX
		
	}
}
