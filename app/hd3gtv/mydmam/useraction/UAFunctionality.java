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

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.taskqueue.Profile;

import java.util.HashMap;

import com.eaio.uuid.UUID;
import com.google.gson.JsonObject;

public abstract class UAFunctionality {
	
	public abstract UAProcess createProcess();
	
	/**
	 * @return like filesystem, transcoding, metadata/pvw, content check, download/delivery, user metadata...
	 */
	public abstract String getSection();
	
	public abstract String getVendor();
	
	public abstract Class<? extends UAFunctionality> getReferenceClass();
	
	public final String getName() {
		return getReferenceClass().getSimpleName().toLowerCase();
	}
	
	public abstract String getLongName();
	
	public abstract String getDescription();
	
	private volatile UUID instance_reference;
	
	/**
	 * @return an autogenerate UUID
	 */
	public final UUID getInstanceReference() {
		if (instance_reference == null) {
			instance_reference = new UUID();
		}
		return instance_reference;
	}
	
	/**
	 * Utility. Internal and local configuration from YAML.
	 * @return useraction->class.getSimpleName().toLowerCase() content from configuration, never null
	 */
	public final HashMap<String, ConfigurationItem> getConfigurationFromReferenceClass() {
		String classname = getName();
		if (Configuration.global.isElementKeyExists("useraction", classname) == false) {
			return new HashMap<String, ConfigurationItem>(1);
		}
		HashMap<String, ConfigurationItem> main_element = Configuration.global.getElement("useraction");
		return Configuration.getElement(main_element, classname);
	}
	
	/**
	 * @return External and global configuration for website (one configuration by worker type)
	 */
	public abstract CrudOrmEngine<? extends CrudOrmModel> getGlobalConfiguration();
	
	/**
	 * @return for create new Useraction in website
	 */
	public abstract CrudOrmModel createUserConfiguration();
	
	/**
	 * @return can be null for desactivate one click create Useraction
	 */
	public abstract CrudOrmModel createOneClickDefaultUserConfiguration();
	
	public abstract Class<? extends UACapability> getCapabilityClass();
	
	private volatile UACapability capability;
	
	public final UACapability getCapabilityForInstance() throws ReflectiveOperationException, SecurityException {
		if (capability == null) {
			capability = getCapabilityClass().getConstructor().newInstance();
			capability = capability.getFromConfigurations(getConfigurationFromReferenceClass(), getGlobalConfiguration().getInternalElement());
		}
		return capability;
	}
	
	private volatile Profile profile;
	
	public final Profile getProfile() {
		if (profile == null) {
			profile = new Profile("useraction", getName());
		}
		return profile;
	}
	
	public final JsonObject toJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("section", getSection());
		jo.addProperty("vendor", getVendor());
		jo.addProperty("reference", getName());
		jo.addProperty("longname", getLongName());
		jo.addProperty("description", getDescription());
		jo.addProperty("instance", getInstanceReference().toString());
		jo.addProperty("profilecategory", getProfile().getCategory());
		jo.addProperty("profilename", getProfile().getName());
		jo.add("capability", capability.toJson());
		/*
		HashMap<String, ConfigurationItem> internal_configuration = getConfigurationFromReferenceClass();
		public final HashMap<String, ConfigurationItem> getConfigurationFromReferenceClass() {
		public abstract CrudOrmEngine<? extends CrudOrmModel> getGlobalConfigurationFromModel();
		*/
		return jo;
	}
}
