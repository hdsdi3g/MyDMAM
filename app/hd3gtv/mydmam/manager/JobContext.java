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

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;

/**
 * Full configuration for a Job
 */
public abstract class JobContext {
	
	/**
	 * @return can be null
	 */
	public abstract JsonObject contextToJson();
	
	/**
	 * @param json_object never null
	 */
	public abstract void contextFromJson(JsonObject json_object);
	
	/**
	 * @return can be null or empty
	 */
	public @GsonIgnore List<String> neededstorages;
	
	/**
	 * @return can be null or empty
	 */
	public @GsonIgnore List<String> hookednames;
	
	public final static class Serializer implements JsonSerializer<JobContext>, JsonDeserializer<JobContext> {
		
		public JobContext deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			try {
				JsonObject json = jejson.getAsJsonObject();
				String context_class = json.get("classname").getAsString();
				JobContext result = AppManager.instanceClassForName(context_class, JobContext.class);
				result.contextFromJson(json.getAsJsonObject("content"));
				result.neededstorages = MyDMAM.gson_kit.getGson().fromJson(json.get("neededstorages"), GsonKit.type_ArrayList_String);
				result.hookednames = MyDMAM.gson_kit.getGson().fromJson(json.get("hookednames"), GsonKit.type_ArrayList_String);
				return result;
			} catch (Exception e) {
				Loggers.Manager.error("Can't deserialize json source:\t" + jejson.toString(), e);
				throw new JsonParseException("Invalid context class: " + jejson.toString(), e);
			}
		}
		
		public JsonElement serialize(JobContext src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			result.addProperty("classname", src.getClass().getName());
			JsonObject context_content = src.contextToJson();
			if (context_content == null) {
				context_content = new JsonObject();
			}
			result.add("content", context_content);
			result.add("neededstorages", MyDMAM.gson_kit.getGson().toJsonTree(src.neededstorages));
			result.add("hookednames", MyDMAM.gson_kit.getGson().toJsonTree(src.hookednames));
			return result;
		}
	}
	
	public final static class SerializerList implements JsonSerializer<ArrayList<JobContext>>, JsonDeserializer<ArrayList<JobContext>> {
		
		public ArrayList<JobContext> deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonArray ja = jejson.getAsJsonArray();
			ArrayList<JobContext> result = new ArrayList<JobContext>();
			for (int pos = 0; pos < ja.size(); pos++) {
				result.add(MyDMAM.gson_kit.getGson().fromJson(ja.get(pos), JobContext.class));
			}
			return result;
		}
		
		public JsonElement serialize(ArrayList<JobContext> src, Type typeOfSrc, JsonSerializationContext context) {
			JsonArray ja = new JsonArray();
			if (src == null) {
				return ja;
			}
			for (int pos = 0; pos < src.size(); pos++) {
				ja.add(MyDMAM.gson_kit.getGson().toJsonTree(src.get(pos), JobContext.class));
			}
			return ja;
		}
	}
	
	public final static class Utility {
		static String prepareContextKeyForTrigger(JobContext context) {
			if (context == null) {
				throw new NullPointerException("\"context\" can't to be null");
			}
			StringBuffer sb = new StringBuffer();
			sb.append(context.getClass().getName());
			
			if (context.neededstorages != null) {
				/**
				 * Copy storagesneeded for not alter actual order in Context.
				 */
				ArrayList<String> storagesneeded_sorted = new ArrayList<String>();
				storagesneeded_sorted.addAll(context.neededstorages);
				Collections.sort(storagesneeded_sorted);
				for (int pos = 0; pos < storagesneeded_sorted.size(); pos++) {
					sb.append("/storage:");
					sb.append(storagesneeded_sorted.get(pos));
				}
			}
			
			if (context.hookednames != null) {
				/**
				 * Copy hookednames for not alter actual order in Context.
				 */
				ArrayList<String> hookednames_sorted = new ArrayList<String>();
				hookednames_sorted.addAll(context.hookednames);
				Collections.sort(hookednames_sorted);
				for (int pos = 0; pos < hookednames_sorted.size(); pos++) {
					sb.append("/hookname:");
					sb.append(hookednames_sorted.get(pos));
				}
			}
			
			try {
				MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(sb.toString().getBytes());
				return "jobcontext:" + MyDMAM.byteToString(md.digest());
			} catch (Exception e) {
				Loggers.Manager.error("Can't compute digest", e);
				return sb.toString();
			}
		}
		
	}
	
	public final String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("classname", getClass().getName());
		log.put("context", contextToJson());
		log.put("neededstorages", neededstorages);
		log.put("hookednames", hookednames);
		return log.toString();
	}
}
