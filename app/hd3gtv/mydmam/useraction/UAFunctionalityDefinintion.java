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

import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Profile.ProfileSerializer;

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

public class UAFunctionalityDefinintion {
	
	public String section;
	public String vendor;
	public String reference;
	public String classname;
	public String longname;
	public String description;
	public String instance;
	public List<Profile> profiles;
	
	public boolean capability_fileprocessing_enabled;
	public boolean capability_directoryprocessing_enabled;
	public boolean capability_rootstorageindexprocessing_enabled;
	public boolean capability_musthavelocalstorageindexbridge;
	public List<String> capability_storageindexeswhitelist;
	
	private static final ProfileSerializer profileserializer;
	private static final Serializer serializer;
	
	static {
		profileserializer = new Profile.ProfileSerializer();
		serializer = new Serializer();
	}
	
	static UAFunctionalityDefinintion fromFunctionality(UAFunctionality functionality) {
		UAFunctionalityDefinintion def = new UAFunctionalityDefinintion();
		def.section = functionality.getSection();
		def.vendor = functionality.getVendor();
		def.reference = functionality.getName();
		def.longname = functionality.getLongName();
		def.description = functionality.getDescription();
		def.instance = functionality.getInstanceReference().toString();
		def.classname = functionality.getClass().getName();
		def.profiles = functionality.getUserActionProfiles();
		
		UACapability capability = functionality.getCapabilityForInstance();
		if (capability != null) {
			def.capability_fileprocessing_enabled = capability.enableFileProcessing();
			def.capability_directoryprocessing_enabled = capability.enableDirectoryProcessing();
			def.capability_rootstorageindexprocessing_enabled = capability.enableRootStorageindexProcessing();
			def.capability_musthavelocalstorageindexbridge = capability.mustHaveLocalStorageindexBridge();
			def.capability_storageindexeswhitelist = capability.getStorageindexesWhiteList();
		} else {
			def.capability_storageindexeswhitelist = new ArrayList<String>();
		}
		return def;
	}
	
	public static Gson getGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		builder.registerTypeAdapter(UAFunctionalityDefinintion.class, serializer);
		return builder.create();
	}
	
	private static class Serializer implements JsonSerializer<UAFunctionalityDefinintion>, JsonDeserializer<UAFunctionalityDefinintion> {
		Gson gson;
		
		Type profiles_typeOfT = new TypeToken<ArrayList<Profile>>() {
		}.getType();
		Type capability_storageindexeswhitelist_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		private Serializer() {
			GsonBuilder builder = new GsonBuilder();
			builder.serializeNulls();
			builder.registerTypeAdapter(Profile.class, profileserializer);
			gson = builder.create();
		}
		
		public JsonElement serialize(UAFunctionalityDefinintion src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jo = (JsonObject) gson.toJsonTree(src);
			jo.add("profiles", gson.toJsonTree(src.profiles, profiles_typeOfT));
			jo.add("capability_storageindexeswhitelist", gson.toJsonTree(src.capability_storageindexeswhitelist, capability_storageindexeswhitelist_typeOfT));
			return jo;
		}
		
		public UAFunctionalityDefinintion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if ((json instanceof JsonObject) == false) {
				return null;
			}
			JsonObject jo = (JsonObject) json;
			UAFunctionalityDefinintion result = gson.fromJson(json, UAFunctionalityDefinintion.class);
			result.profiles = gson.fromJson(jo.get("profiles").getAsJsonArray(), profiles_typeOfT);
			result.capability_storageindexeswhitelist = gson.fromJson(jo.get("capability_storageindexeswhitelist").getAsJsonArray(), capability_storageindexeswhitelist_typeOfT);
			return result;
		}
	}
	
}
