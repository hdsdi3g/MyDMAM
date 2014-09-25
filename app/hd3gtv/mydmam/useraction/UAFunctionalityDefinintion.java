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

public final class UAFunctionalityDefinintion implements Log2Dumpable {
	
	public String section;
	public String vendor;
	public String classname;
	public String longname;
	public String description;
	public String instance;
	public String messagebasename;
	public List<Profile> profiles;
	public UACapabilityDefinition capability;
	public UAConfigurator configurator;
	
	private static final ProfileSerializer profileserializer;
	private static final Serializer serializer;
	
	public static void mergueInList(List<UAFunctionalityDefinintion> ressource, UAFunctionalityDefinintion definition) {
		if (ressource == null) {
			return;
		}
		if (definition == null) {
			return;
		}
		
		UAFunctionalityDefinintion current;
		for (int pos = 0; pos < ressource.size(); pos++) {
			current = ressource.get(pos);
			if (definition.classname.equalsIgnoreCase(current.classname) == false) {
				continue;
			}
			
			/**
			 * Mergue...
			 */
			if (current.profiles == null) {
				current.profiles = new ArrayList<Profile>();
			}
			if (definition.profiles != null) {
				current.profiles.addAll(definition.profiles);
			}
			if (current.capability != null) {
				current.capability.mergue(definition.capability);
			} else {
				current.capability = new UACapabilityDefinition();
				current.capability.storageindexeswhitelist = new ArrayList<String>();
			}
			return;
		}
		
		ressource.add(definition);
	}
	
	static {
		profileserializer = new Profile.ProfileSerializer();
		serializer = new Serializer();
	}
	
	static UAFunctionalityDefinintion fromFunctionality(UAFunctionality functionality) {
		UAFunctionalityDefinintion def = new UAFunctionalityDefinintion();
		def.section = functionality.getSection();
		def.vendor = functionality.getVendor();
		def.longname = functionality.getLongName();
		def.description = functionality.getDescription();
		def.instance = functionality.getInstanceReference().toString();
		def.classname = functionality.getClass().getName();
		def.profiles = functionality.getUserActionProfiles();
		def.configurator = functionality.createEmptyConfiguration();
		def.messagebasename = functionality.getMessageBaseName();
		if (def.messagebasename == null) {
			def.messagebasename = functionality.getClass().getName();
		}
		
		UACapability capability = functionality.getCapabilityForInstance();
		if (capability != null) {
			def.capability = capability.getDefinition();
		} else {
			def.capability = new UACapabilityDefinition();
			def.capability.storageindexeswhitelist = new ArrayList<String>();
		}
		return def;
	}
	
	public static Gson getGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		builder.registerTypeAdapter(UAFunctionalityDefinintion.class, serializer);
		builder.registerTypeAdapter(UACapabilityDefinition.class, new UACapabilityDefinition.Serializer());
		builder.registerTypeAdapter(UAConfigurator.class, new UAConfigurator.JsonUtils());
		return builder.create();
	}
	
	private static class Serializer implements JsonSerializer<UAFunctionalityDefinintion>, JsonDeserializer<UAFunctionalityDefinintion> {
		Gson gson;
		
		Type profiles_typeOfT = new TypeToken<ArrayList<Profile>>() {
		}.getType();
		
		private Serializer() {
			GsonBuilder builder = new GsonBuilder();
			builder.serializeNulls();
			builder.registerTypeAdapter(Profile.class, profileserializer);
			builder.registerTypeAdapter(UACapabilityDefinition.class, new UACapabilityDefinition.Serializer());
			
			gson = builder.create();
		}
		
		public JsonElement serialize(UAFunctionalityDefinintion src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jo = (JsonObject) gson.toJsonTree(src);
			jo.add("profiles", gson.toJsonTree(src.profiles, profiles_typeOfT));
			return jo;
		}
		
		public UAFunctionalityDefinintion deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			if ((json instanceof JsonObject) == false) {
				return null;
			}
			JsonObject jo = (JsonObject) json;
			UAFunctionalityDefinintion result = gson.fromJson(json, UAFunctionalityDefinintion.class);
			result.profiles = gson.fromJson(jo.get("profiles").getAsJsonArray(), profiles_typeOfT);
			return result;
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("section", section);
		dump.add("vendor", vendor);
		dump.add("classname", classname);
		dump.add("longname", longname);
		dump.add("description", description);
		dump.add("instance", instance);
		dump.add("messagebasename", messagebasename);
		dump.add("profiles", profiles);
		dump.addAll(capability);
		dump.addAll(configurator);
		return dump;
	}
}
