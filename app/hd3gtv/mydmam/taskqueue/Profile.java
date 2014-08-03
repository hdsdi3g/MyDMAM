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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.simple.JSONObject;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.query.IndexQuery;

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
}
