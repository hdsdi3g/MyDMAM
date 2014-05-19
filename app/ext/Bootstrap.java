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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package ext;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthenticationBackend;
import hd3gtv.mydmam.web.JsCompile;
import hd3gtv.mydmam.web.Privileges;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import models.ACLGroup;
import models.ACLRole;
import models.ACLUser;
import play.i18n.Messages;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class Bootstrap extends Job {
	
	public void doJob() {
		/**
		 * Compare Messages entries between languages
		 */
		String first_locales_lang = null;
		Properties first_locales_messages = null;
		Set<String> first_locales_messages_string;
		
		String actual_locales_lang = null;
		Set<String> actual_messages_string;
		Log2Dump dump;
		boolean has_missing = false;
		
		for (Map.Entry<String, Properties> entry_messages_locale : Messages.locales.entrySet()) {
			if (first_locales_lang == null) {
				first_locales_lang = entry_messages_locale.getKey();
				first_locales_messages = entry_messages_locale.getValue();
				continue;
			}
			first_locales_messages_string = first_locales_messages.stringPropertyNames();
			actual_messages_string = entry_messages_locale.getValue().stringPropertyNames();
			actual_locales_lang = entry_messages_locale.getKey();
			
			dump = new Log2Dump();
			has_missing = false;
			for (String string : actual_messages_string) {
				if (first_locales_messages_string.contains(string) == false) {
					dump.add("missing", string);
					has_missing = true;
				}
			}
			if (has_missing) {
				Log2.log.error("Missing Messages strings in messages." + first_locales_lang + " lang (declared in messages." + actual_locales_lang + ")", null, dump);
			}
			
			dump = new Log2Dump();
			has_missing = false;
			for (String string : first_locales_messages_string) {
				if (actual_messages_string.contains(string) == false) {
					dump.add("missing", string);
					has_missing = true;
				}
			}
			if (has_missing) {
				Log2.log.error("Missing Messages strings in messages." + actual_locales_lang + " lang (declared in messages." + first_locales_lang + ")", null, dump);
			}
		}
		
		/**
		 * Inject configuration Messages to Play Messages
		 */
		for (Map.Entry<String, Properties> entry : Messages.locales.entrySet()) {
			entry.getValue().putAll(MyDMAM.getconfiguredMessages());
		}
		
		/**
		 * Peuplate DB ACLs : admin role
		 */
		ACLRole role_admim = ACLRole.findById(ACLRole.ADMIN_NAME);
		if (role_admim == null) {
			role_admim = new ACLRole(ACLRole.ADMIN_NAME);
			role_admim.privileges = Privileges.getJSONAllPrivileges().toJSONString();
			role_admim.save();
		} else {
			List<String> privileges = role_admim.getPrivilegesList();
			if (privileges.size() != Privileges.getAllPrivileges().size()) {
				role_admim.privileges = Privileges.getJSONAllPrivileges().toJSONString();
				role_admim.save();
			}
		}
		
		/**
		 * Peuplate DB ACLs : guest role
		 */
		ACLRole role_guest = ACLRole.findById(ACLRole.GUEST_NAME);
		if (role_guest == null) {
			role_guest = new ACLRole(ACLRole.GUEST_NAME);
			role_guest.save();
		}
		
		/**
		 * Peuplate DB ACLs : admin group
		 */
		ACLGroup group_admin = ACLGroup.findById(ACLGroup.ADMIN_NAME);
		if (group_admin == null) {
			group_admin = new ACLGroup(role_admim, ACLGroup.ADMIN_NAME);
			group_admin.save();
		} else {
			if (group_admin.role != role_admim) {
				group_admin.role = role_admim;
				group_admin.save();
			}
		}
		
		/**
		 * Peuplate DB ACLs : newusers group
		 */
		ACLGroup group_newusers = ACLGroup.findById(ACLGroup.NEWUSERS_NAME);
		if (group_newusers == null) {
			group_newusers = new ACLGroup(role_guest, ACLGroup.NEWUSERS_NAME);
			group_newusers.save();
		}
		
		/**
		 * Peuplate DB ACLs : admin user
		 */
		ACLUser user_admin = ACLUser.findById(ACLUser.ADMIN_NAME);
		if (user_admin == null) {
			user_admin = new ACLUser(group_admin, "Internal Play", ACLUser.ADMIN_NAME, "Administrator");
			user_admin.save();
		} else {
			if (user_admin.group != group_admin) {
				user_admin.group = group_admin;
				user_admin.save();
			}
		}
		
		try {
			AuthenticationBackend.checkFirstPlayBoot();
		} catch (Exception e) {
			Log2.log.error("Invalid authentication backend configuration", e);
		}
		
		JsCompile.purgeBinDirectory();
		JsCompile.prepareFiles();
	}
}
