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

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UAFunctionalityDefinintion {
	
	public String section;
	public String vendor;
	public String reference;
	public String classname;
	public String longname;
	public String description;
	public String instance;
	public List<Profile> profiles;
	
	// public JsonObject capability; NOPE a JsonObject
	
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
		// def.capability = functionality.getCapabilityForInstance(); TODO ADD Capability
		/*
		jo.addProperty("enablefileprocessing", enableFileProcessing());
		jo.addProperty("enabledirectoryprocessing", enableDirectoryProcessing());
		jo.addProperty("enablerootstorageindexprocessing", enableRootStorageindexProcessing());
		jo.addProperty("musthavelocalstorageindexbridge", mustHaveLocalStorageindexBridge());
		
		List<String> storages_wl = getStorageindexesWhiteList();
		JsonArray storages_wl_json = new JsonArray();
		for (int pos = 0; pos < storages_wl.size(); pos++) {
			storages_wl_json.add(new JsonPrimitive(storages_wl.get(pos)));
		}
		jo.add("storagewhitelist", storages_wl_json);
		return jo;
		*/
		return def;
	}
	
	public static Gson getGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		// builder.registerTypeHierarchyAdapter(baseType, typeAdapter)
		// TODO add specific registers
		return builder.create();
	}
	
	/*
	public final JsonObject toJson() {
		jo.add("capability", capability.toJson());
		//HashMap<String, ConfigurationItem> internal_configuration = getConfigurationFromReferenceClass();
		//public final HashMap<String, ConfigurationItem> getConfigurationFromReferenceClass() {
		//public abstract CrudOrmEngine<? extends CrudOrmModel> getGlobalConfigurationFromModel();
		return jo;
	}
	 * */
	
}
