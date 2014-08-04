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
package hd3gtv.mydmam.metadata.container;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

/**
 * Store all Metadatas references for a StorageIndex element, and (de)serialize from/to json.
 */
public class Container {
	
	public static JsonObject getJsonObject(JsonElement json, boolean can_null) throws JsonParseException {
		if (json.isJsonNull()) {
			if (can_null) {
				return null;
			} else {
				throw new JsonParseException("Json element is null");
			}
		}
		if (json.isJsonObject() == false) {
			throw new JsonParseException("Json element is not an object: " + json.toString());
		}
		return (JsonObject) json.getAsJsonObject();
	}
	
	private static final Map<String, EntryBase> declared_entries_type;
	private static final GsonBuilder gson_builder;
	
	static {
		declared_entries_type = new LinkedHashMap<String, EntryBase>();
		gson_builder = new GsonBuilder();
		gson_builder.setPrettyPrinting();// TODO remove this after tests
		gson_builder.serializeNulls();
		
		// declareEntryType(new EntryBaseSummary());
	}
	
	public synchronized static void declareEntryType(EntryBase entry_serialiser) throws NullPointerException {
		if (entry_serialiser == null) {
			throw new NullPointerException("\"serialiser\" can't to be null");
		}
		declared_entries_type.put(entry_serialiser.getType(), entry_serialiser);
		entry_serialiser.getEntrySerialiser();
	}
	
	public static GsonBuilder getGsonBuilder() {
		return gson_builder;
	}
	
	public Container() {
		entries = new ArrayList<EntryBaseSummary>();
	}
	
	List<EntryBaseSummary> entries;// TODO reset to EntryBase
	
	public void addEntry(EntryBaseSummary entry) {
		declareEntryType(entry);
		entries.add(entry);
	}
	
	/**
	 * @deprecated
	 */
	public String toGSONString() {
		return gson_builder.create().toJson(entries); // Non sense.
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * @deprecated
	 */
	void load(String content) {
		// entries = gson_builder.create().fromJson(content, List.class);
		
		Type typeOfT = new TypeToken<List<EntryBaseSummary>>() {
		}.getType();
		
		entries = gson_builder.create().fromJson(content, typeOfT);
	}
	
	// TODO export to ES (all entries -> Index)
	// TODO import from ES (all hits -> entries)
	
	// TODO "search" by Entry Type
}
