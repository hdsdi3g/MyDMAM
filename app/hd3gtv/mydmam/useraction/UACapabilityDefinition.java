/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

public final class UACapabilityDefinition implements Log2Dumpable {
	
	public boolean fileprocessing_enabled;
	public boolean directoryprocessing_enabled;
	public boolean rootstorageindexprocessing_enabled;
	public List<String> storageindexeswhitelist;
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("fileprocessing_enabled", fileprocessing_enabled);
		dump.add("directoryprocessing_enabled", directoryprocessing_enabled);
		dump.add("rootstorageindexprocessing_enabled", rootstorageindexprocessing_enabled);
		dump.add("storageindexeswhitelist", storageindexeswhitelist);
		return dump;
	}
	
	void mergue(UACapabilityDefinition definition) {
		if (storageindexeswhitelist == null) {
			storageindexeswhitelist = new ArrayList<String>();
		}
		
		if (definition == null) {
			return;
		}
		
		if (directoryprocessing_enabled == false) {
			directoryprocessing_enabled = definition.directoryprocessing_enabled;
		}
		if (fileprocessing_enabled == false) {
			fileprocessing_enabled = definition.fileprocessing_enabled;
		}
		if (rootstorageindexprocessing_enabled == false) {
			rootstorageindexprocessing_enabled = definition.rootstorageindexprocessing_enabled;
		}
		
		if (definition.storageindexeswhitelist != null) {
			storageindexeswhitelist.addAll(definition.storageindexeswhitelist);
		}
	}
	
	public static class Serializer implements JsonSerializer<UACapabilityDefinition>, JsonDeserializer<UACapabilityDefinition> {
		Gson gson;
		
		public static final Type capability_storageindexeswhitelist_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		public Serializer() {
			GsonBuilder builder = new GsonBuilder();
			builder.serializeNulls();
			gson = builder.create();
		}
		
		public JsonElement serialize(UACapabilityDefinition src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jo = (JsonObject) gson.toJsonTree(src);
			jo.add("storageindexeswhitelist", gson.toJsonTree(src.storageindexeswhitelist, capability_storageindexeswhitelist_typeOfT));
			return jo;
		}
		
		public UACapabilityDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if ((json instanceof JsonObject) == false) {
				return null;
			}
			JsonObject jo = (JsonObject) json;
			UACapabilityDefinition result = gson.fromJson(json, UACapabilityDefinition.class);
			result.storageindexeswhitelist = gson.fromJson(jo.get("storageindexeswhitelist").getAsJsonArray(), capability_storageindexeswhitelist_typeOfT);
			return result;
		}
	}
	
}
