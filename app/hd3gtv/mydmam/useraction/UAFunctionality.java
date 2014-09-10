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
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.taskqueue.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.eaio.uuid.UUID;

public abstract class UAFunctionality {
	
	public abstract UAJobProcess createProcess();
	
	/**
	 * @return like filesystem, transcoding, metadata/pvw, content check, download/delivery, user metadata...
	 */
	public abstract String getSection();
	
	public abstract String getVendor();
	
	public final String getName() {
		return getClass().getSimpleName().toLowerCase();
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
	public final LinkedHashMap<String, ?> getConfigurationFromReferenceClass() {
		String classname = getName();
		if (Configuration.global.isElementKeyExists("useraction", classname) == false) {
			return new LinkedHashMap<String, Object>(1);
		}
		HashMap<String, ConfigurationItem> main_element = Configuration.global.getElement("useraction");
		return main_element.get(classname).content;
	}
	
	/**
	 * For display create form in website.
	 * @return can be null (if no form or one click UA).
	 */
	public abstract UAConfigurator createEmptyConfiguration();
	
	/**
	 * For display create form in website.
	 */
	public abstract boolean hasOneClickDefault();
	
	public abstract UAFinisherConfiguration getFinisherForOneClick();
	
	public abstract UARange getRangeForOneClick();
	
	/**
	 * For execute an UA.
	 */
	public abstract UAConfigurator createOneClickDefaultUserConfiguration();
	
	public abstract UACapability createCapability(LinkedHashMap<String, ?> internal_configuration);
	
	private volatile UACapability capability;
	
	public final UACapability getCapabilityForInstance() {
		if (capability == null) {
			capability = createCapability(getConfigurationFromReferenceClass());
			if (capability == null) {
				throw new NullPointerException("No capability for " + getClass().getName());
			}
		}
		return capability;
	}
	
	private volatile List<Profile> user_action_profiles;
	
	public final List<Profile> getUserActionProfiles() {
		if (user_action_profiles == null) {
			user_action_profiles = new ArrayList<Profile>();
			List<String> whitelist = getCapabilityForInstance().getStorageindexesWhiteList();
			List<String> bridgedstorages = Explorer.getBridgedStoragesName();
			if (whitelist != null) {
				if (whitelist.isEmpty() == false) {
					for (int pos = 0; pos < whitelist.size(); pos++) {
						if (bridgedstorages.contains(whitelist.get(pos)) == false) {
							continue;
						}
						user_action_profiles.add(new Profile("useraction", getName() + "=" + whitelist.get(pos)));
					}
				}
			} else {
				/**
				 * No whitelist
				 */
				for (int pos = 0; pos < bridgedstorages.size(); pos++) {
					user_action_profiles.add(new Profile("useraction", getName() + "=" + bridgedstorages.get(pos)));
				}
			}
		}
		return user_action_profiles;
	}
	
	private volatile List<Profile> finisher_profiles;
	
	public final List<Profile> getFinisherProfiles() {
		if (finisher_profiles == null) {
			finisher_profiles = new ArrayList<Profile>();
			List<String> whitelist = getCapabilityForInstance().getStorageindexesWhiteList();
			List<String> bridgedstorages = Explorer.getBridgedStoragesName();
			if (whitelist != null) {
				if (whitelist.isEmpty() == false) {
					for (int pos = 0; pos < whitelist.size(); pos++) {
						if (bridgedstorages.contains(whitelist.get(pos)) == false) {
							continue;
						}
						finisher_profiles.add(new Profile("useraction-finisher", whitelist.get(pos)));
					}
					return finisher_profiles;
				}
			}
			/**
			 * No whitelist
			 */
			for (int pos = 0; pos < bridgedstorages.size(); pos++) {
				finisher_profiles.add(new Profile("useraction-finisher", bridgedstorages.get(pos)));
			}
		}
		return finisher_profiles;
	}
	
	public UAFunctionalityDefinintion getDefinition() {
		return UAFunctionalityDefinintion.fromFunctionality(this);
	}
	
}
