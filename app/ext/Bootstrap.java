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

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthenticationBackend;
import hd3gtv.mydmam.auth.DbAccountExtractor;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.web.JSSourceManager;
import hd3gtv.mydmam.web.Privileges;
import models.ACLGroup;
import models.ACLRole;
import models.ACLUser;
import play.i18n.Messages;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@SuppressWarnings("rawtypes")
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
		StringBuilder sb;
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
			
			sb = new StringBuilder();
			has_missing = false;
			for (String string : actual_messages_string) {
				if (first_locales_messages_string.contains(string) == false) {
					sb.append(" missing: " + string);
					has_missing = true;
				}
			}
			if (has_missing) {
				Loggers.Play.error("Missing Messages strings in messages." + first_locales_lang + " lang (declared in messages." + actual_locales_lang + ") " + sb.toString());
			}
			
			sb = new StringBuilder();
			has_missing = false;
			for (String string : first_locales_messages_string) {
				if (actual_messages_string.contains(string) == false) {
					sb.append(" missing: " + string);
					has_missing = true;
				}
			}
			if (has_missing) {
				Loggers.Play.error("Missing Messages strings in messages." + actual_locales_lang + " lang (declared in messages." + first_locales_lang + ") " + sb.toString());
			}
		}
		
		/**
		 * Inject configuration Messages to Play Messages
		 */
		for (Map.Entry<String, Properties> entry : Messages.locales.entrySet()) {
			entry.getValue().putAll(MyDMAM.getconfiguredMessages());
		}
		
		/**
		 * Peuplate DB ACLs : set locked_account if null
		 */
		List<ACLUser> user_list = ACLUser.all().fetch();
		for (int pos = 0; pos < user_list.size(); pos++) {
			if (user_list.get(pos).locked_account == null) {
				user_list.get(pos).locked_account = false;
				user_list.get(pos).save();
			}
		}
		
		/**
		 * Peuplate DB ACLs : admin role
		 */
		ACLRole role_admim = ACLRole.findById(ACLRole.ADMIN_NAME);
		if (role_admim == null) {
			role_admim = new ACLRole(ACLRole.ADMIN_NAME);
			role_admim.privileges = Privileges.getJSONArrayStringPrivileges();
			role_admim.functionalities = "{}";
			role_admim.save();
		} else {
			List<String> privileges = role_admim.getPrivilegesList();
			if (privileges.containsAll(Privileges.getAllSortedPrivileges()) == false) {
				role_admim.privileges = Privileges.getJSONArrayStringPrivileges();
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
			Loggers.Play.error("Invalid authentication backend configuration", e);
		}
		
		try {
			JSSourceManager.init();
		} catch (Exception e) {
			Loggers.Play_JSSource.error("Can't init", e);
		}
		
		try {
			CassandraDb.getkeyspace();
		} catch (ConnectionException e) {
			Loggers.Play.error("Can't access to keyspace", e);
		}
		
		DbAccountExtractor.extractor.save();
	}
}
