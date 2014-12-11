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
package hd3gtv.mydmam.manager;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.MyDMAM;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Full configuration for a Job
 */
public interface JobContext {
	
	public JsonObject contextToJson();
	
	public void contextFromJson(JsonObject json_object);
	
	/**
	 * @return can be null or empty
	 */
	public List<String> getNeededIndexedStoragesNames();
	
	final static class Serializer implements JsonSerializer<JobContext>, JsonDeserializer<JobContext> {
		
		public JobContext deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				JsonObject json = jejson.getAsJsonObject();
				String context_class = json.get("classname").getAsString();
				JobContext result = AppManager.instanceClassForName(context_class, JobContext.class);
				result.contextFromJson(json.getAsJsonObject("content"));
				return result;
			} catch (Exception e) {
				throw new JsonParseException("Invalid context class", e);
			}
		}
		
		public JsonElement serialize(JobContext src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			result.addProperty("classname", src.getClass().getName());
			result.add("content", src.contextToJson());
			result.add("neededstorages", AppManager.getGson().toJsonTree(src.getNeededIndexedStoragesNames()));
			return result;
		}
	}
	
	final static class SerializerList implements JsonSerializer<ArrayList<JobContext>>, JsonDeserializer<ArrayList<JobContext>> {
		
		public ArrayList<JobContext> deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonArray ja = jejson.getAsJsonArray();
			ArrayList<JobContext> result = new ArrayList<JobContext>();
			for (int pos = 0; pos < ja.size(); pos++) {
				result.add(AppManager.getGson().fromJson(ja.get(pos), JobContext.class));
			}
			return result;
		}
		
		public JsonElement serialize(ArrayList<JobContext> src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray ja = new JsonArray();
			if (src == null) {
				return ja;
			}
			for (int pos = 0; pos < src.size(); pos++) {
				ja.add(AppManager.getGson().toJsonTree(src.get(pos), JobContext.class));
			}
			return ja;
		}
	}
	
	final static class Utility {
		static String prepareContextKeyForTrigger(JobContext context) {
			if (context == null) {
				throw new NullPointerException("\"context\" can't to be null");
			}
			StringBuffer sb = new StringBuffer();
			sb.append(context.getClass().getName());
			
			List<String> storagesneeded = context.getNeededIndexedStoragesNames();
			if (storagesneeded != null) {
				/**
				 * Copy storagesneeded for not alter actual order in Context.
				 */
				ArrayList<String> storagesneeded_sorted = new ArrayList<String>();
				storagesneeded_sorted.addAll(storagesneeded);
				Collections.sort(storagesneeded_sorted);
				for (int pos = 0; pos < storagesneeded_sorted.size(); pos++) {
					sb.append("/storage:");
					sb.append(storagesneeded_sorted.get(pos));
				}
			}
			
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(sb.toString().getBytes());
				return "jobcontext:" + MyDMAM.byteToString(md.digest());
			} catch (Exception e) {
				Log2.log.error("Can't compute digest", e);
				return sb.toString();
			}
		}
	}
}
