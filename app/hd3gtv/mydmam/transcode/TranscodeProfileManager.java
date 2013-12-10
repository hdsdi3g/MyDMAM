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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/

package hd3gtv.mydmam.transcode;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.taskqueue.Profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TranscodeProfileManager {
	
	private static ArrayList<TranscodeProfile> profiles;
	
	public static boolean isEnabled() {
		return Configuration.global.isElementExists("transcodingprofiles");
	}
	
	public static ArrayList<TranscodeProfile> getProfiles() {
		if (profiles == null) {
			try {
				refreshProfileslist();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return profiles;
	}
	
	public static TranscodeProfile getProfile(Profile profile) {
		if (profiles == null) {
			try {
				refreshProfileslist();
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		for (int pos = 0; pos < profiles.size(); pos++) {
			if (profiles.get(pos).sameProfile(profile)) {
				return profiles.get(pos);
			}
		}
		return null;
	}
	
	public static void refreshProfileslist() throws Exception {
		profiles = null;
		try {
			if (isEnabled() == false) {
				return;
			}
			
			HashMap<String, ConfigurationItem> tp_list = Configuration.global.getElement("transcodingprofiles");
			
			if (tp_list.isEmpty()) {
				Log2.log.error("Can't found \"profile\" element in transcoding block in XML configuration", null);
			}
			
			profiles = new ArrayList<TranscodeProfile>(tp_list.size());
			
			String profile_type;
			String profile_name;
			String param;
			for (Map.Entry<String, ConfigurationItem> entry : tp_list.entrySet()) {
				profile_type = Configuration.getValue(tp_list, entry.getKey(), "type", null);
				if (profile_type == null) {
					throw new NullPointerException("Attribute \"type\" in \"profile\" element for transcoding can't to be null");
				} else if (profile_type.equals("")) {
					throw new NullPointerException("Attribute \"type\" in \"profile\" element for transcoding can't to be empty");
				}
				profile_name = entry.getKey();
				
				TranscodeProfile profile = new TranscodeProfile(profile_type, profile_name);
				String[] params = Configuration.getValue(tp_list, entry.getKey(), "command", null).trim().split(" ");
				for (int pos_par = 0; pos_par < params.length; pos_par++) {
					param = params[pos_par].trim();
					if (param.length() == 0) {
						continue;
					}
					if (param.startsWith(TranscodeProfile.TAG_STARTVAR) & param.endsWith(TranscodeProfile.TAG_ENDVAR)) {
						param = param.substring(TranscodeProfile.TAG_STARTVAR.length(), param.length() - TranscodeProfile.TAG_ENDVAR.length());
						param = Configuration.getValue(tp_list, entry.getKey(), param, null);
						if (param == null) {
							throw new NullPointerException("Can't found " + params[pos_par] + " param variable");
						}
						param = param.trim();
					}
					profile.getParam().add(param);
				}
				profile.testValidityProfile();
				profiles.add(profile);
			}
			
			Log2Dump dump = new Log2Dump();
			for (int pos = 0; pos < profiles.size(); pos++) {
				dump.add("profile", profiles.get(pos));
			}
			
			Log2.log.info("Set transcoding configuration", dump);
		} catch (Exception e) {
			Log2.log.error("Can't load transcoding configuration", e);
			throw e;
		}
	}
}
