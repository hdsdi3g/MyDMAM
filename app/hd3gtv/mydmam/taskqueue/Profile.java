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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.taskqueue;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.MyDMAM;

import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.simple.JSONObject;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.IndexQuery;

@Deprecated
public class Profile implements Log2Dumpable {
	
	String name;
	
	String category;
	
	public Profile(String category, String name) {
		this.category = category;
		if (category == null) {
			throw new NullPointerException("\"category\" can't to be null");
		}
		this.name = name;
		if (name == null) {
			throw new NullPointerException("\"name\" can't to be null");
		}
		this.category = this.category.toLowerCase();
		this.name = this.name.toLowerCase();
	}
	
	Profile() {
	}
	
	public final String getCategory() {
		return category;
	}
	
	public final String getName() {
		return name;
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ((obj instanceof Profile) == false) {
			return false;
		}
		Profile profile = (Profile) obj;
		if (profile.name == null | profile.category == null) {
			return false;
		}
		if (name == null | category == null) {
			return false;
		}
		return (name.equalsIgnoreCase(profile.name) & category.equalsIgnoreCase(profile.category));
	}
	
	final void pushToDatabase(MutationBatch mutator, String taskkey, int ttl) {
		mutator.withRow(Broker.CF_TASKQUEUE, taskkey).putColumnIfNotNull("profile_name", name.toLowerCase(), ttl);
		mutator.withRow(Broker.CF_TASKQUEUE, taskkey).putColumnIfNotNull("profile_category", category.toLowerCase(), ttl);
	}
	
	final void pullFromDatabase(ColumnList<String> columns) {
		name = columns.getStringValue("profile_name", "");
		category = columns.getStringValue("profile_category", "");
	}
	
	/**
	 * For worker managed profiles
	 */
	@SuppressWarnings("unchecked")
	final JSONObject toJson() {
		JSONObject jo = new JSONObject();
		jo.put("category", category);
		jo.put("name", name);
		return jo;
	}
	
	@SuppressWarnings("unchecked")
	static void pullJSONFromDatabase(JSONObject jo, ColumnList<String> columns) {
		jo.put("profile_name", columns.getStringValue("profile_name", ""));
		jo.put("profile_category", columns.getStringValue("profile_category", ""));
	}
	
	static void selectProfileByCategory(IndexQuery<String, String> index_query, String category) {
		index_query.addExpression().whereColumn("profile_category").equals().value(category.toLowerCase());
	}
	
	String computeKey() {
		byte[] message = (category + ":" + name).toString().getBytes();
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(message);
			return "profile_" + MyDMAM.byteToString(md.digest()).substring(0, 16);
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException("NoSuchAlgorithmException !");
		}
	}
	
	public Log2Dump getLog2Dump() {
		return new Log2Dump("profile", category + ":" + name);
	}
	
	public static class ProfileSerializer implements JsonSerializer<Profile>, JsonDeserializer<Profile> {
		public JsonElement serialize(Profile src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jo = new JsonObject();
			jo.addProperty("name", src.name);
			jo.addProperty("category", src.category);
			return jo;
		}
		
		public Profile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if (json instanceof JsonObject) {
				JsonObject jo = (JsonObject) json;
				if (jo.has("category") & jo.has("name")) {
					return new Profile(jo.get("category").getAsString(), jo.get("name").getAsString());
				}
			}
			return null;
		}
	}
	
}
