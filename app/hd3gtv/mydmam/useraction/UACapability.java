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

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.db.orm.CrudOrmModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonObject;

public abstract class UACapability {
	/**
	 * TODO Usergroups white/black list (declared in website by Admin Configuration)
	 * - Force to have a local storage bridge (hard-coded)
	 */
	
	public UACapability() {
	}
	
	public abstract UACapability getFromConfigurations(HashMap<String, ConfigurationItem> internal_configuration, CrudOrmModel external_configuration);
	
	public abstract boolean enableFileProcessing();
	
	public abstract boolean enableDirectoryProcessing();
	
	public abstract boolean enableRootStorageindexProcessing();
	
	public abstract boolean mustHaveLocalStorageindexBridge();
	
	public List<String> getStorageindexesWhiteList() {
		return new ArrayList<String>();
	}
	
	public List<String> getStorageindexesBlackList() {
		return new ArrayList<String>();
	}
	
	public final JsonObject toJson() {
		// TODO
		JsonObject jo = new JsonObject();
		/*jo.addProperty("section", getSection());
		jo.addProperty("vendor", getVendor());
		jo.addProperty("reference", getReferenceClass().getSimpleName().toLowerCase());
		jo.addProperty("longname", getLongName());
		jo.addProperty("description", getDescription());
		jo.addProperty("instance", getInstanceReference().toString());
		HashMap<String, ConfigurationItem> internal_configuration = getConfigurationFromReferenceClass();
		public final HashMap<String, ConfigurationItem> getConfigurationFromReferenceClass() {
		public abstract CrudOrmEngine<? extends CrudOrmModel> getGlobalConfigurationFromModel();
		public abstract UACapability getCapabilityForInstance(HashMap<String, ConfigurationItem> internal_configuration, CrudOrmModel external_configuration);
		*/
		return jo;
	}
	
}
