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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.auth;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Properties;

import javax.mail.internet.InternetAddress;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.ColumnList;

import hd3gtv.mydmam.mail.EndUserBaseMail;
import play.i18n.Lang;

public class UserNG {
	
	private String key;
	
	private String login;
	private String fullname;
	private String domain;
	private String language;
	private String email_addr;
	private byte[] protected_password;
	
	private long lasteditdate;
	private long lastlogindate;
	private String lastloginipsource;
	private boolean locked_account;
	
	private ArrayList<GroupNG> user_groups;
	private JsonObject preferencies;
	private Properties properties;
	private LinkedHashMap<String, BasketNG> baskets;
	private ArrayList<UserActivity> activities;
	private ArrayList<UserNotificationNG> notifications;
	
	private transient ArrayList<RoleNG> user_groups_roles;
	private transient HashSet<String> user_groups_roles_privileges;
	private transient AuthTurret turret;
	
	private static Type al_group_typeOfT = new TypeToken<ArrayList<GroupNG>>() {
	}.getType();
	private static Type linmap_string_basket_typeOfT = new TypeToken<LinkedHashMap<String, BasketNG>>() {
	}.getType();
	private static Type al_useractivity_typeOfT = new TypeToken<ArrayList<UserActivity>>() {
	}.getType();
	private static Type al_usernotification_typeOfT = new TypeToken<ArrayList<UserNotificationNG>>() {
	}.getType();
	
	// TODO Gson (de) serializers
	// TODO import db
	// TODO CRUD
	
	UserNG save(ColumnListMutation<String> mutator) {
		mutator.putColumnIfNotNull("login", login);
		mutator.putColumnIfNotNull("fullname", fullname);
		mutator.putColumnIfNotNull("domain", domain);
		mutator.putColumnIfNotNull("language", language);
		mutator.putColumnIfNotNull("email_addr", email_addr);
		mutator.putColumnIfNotNull("protected_password", protected_password);
		mutator.putColumnIfNotNull("lasteditdate", lasteditdate);
		mutator.putColumnIfNotNull("lastlogindate", lastlogindate);
		mutator.putColumnIfNotNull("lastloginipsource", lastloginipsource);
		mutator.putColumnIfNotNull("locked_account", locked_account);
		
		if (user_groups != null) {
			mutator.putColumnIfNotNull("user_groups", turret.getGson().toJson(user_groups, al_group_typeOfT));
		}
		if (properties != null) {
			mutator.putColumnIfNotNull("properties", turret.getGson().toJson(properties));
		}
		if (baskets != null) {
			mutator.putColumnIfNotNull("baskets", turret.getGson().toJson(baskets, linmap_string_basket_typeOfT));
		}
		if (activities != null) {
			mutator.putColumnIfNotNull("activities", turret.getGson().toJson(activities, al_useractivity_typeOfT));
		}
		if (notifications != null) {
			mutator.putColumnIfNotNull("notifications", turret.getGson().toJson(notifications, al_usernotification_typeOfT));
		}
		if (preferencies != null) {
			mutator.putColumnIfNotNull("preferencies", preferencies.toString());
		}
		
		return this;
	}
	
	UserNG loadFromDb(String key, ColumnList<String> cols) {
		// TODO
		return this;
	}
	
	UserNG(AuthTurret turret, String key, boolean load_from_db) {
		this.turret = turret;
		if (turret == null) {
			throw new NullPointerException("\"turret\" can't to be null");
		}
		
		// TODO Auto-generated constructor stub
	}
	
	public JsonObject exportForAdmin() {
		// TODO
		// TODO lazy load preferencies
		return null;
	}
	
	public JsonObject exportForWebUser() {
		// TODO
		// TODO lazy load preferencies
		return null;
	}
	
	void doUpdateOperations() {
		// TODO like update lasteditdate
	}
	
	void doLoginOperations(String loginipsource, String language, String email_addr) {
		this.lastloginipsource = loginipsource;
		this.language = language;
		this.email_addr = email_addr;
		this.lastlogindate = System.currentTimeMillis();
	}
	
	public String getFullname() {
		return fullname;
	}
	
	public String getEmailAddr() {
		return email_addr;
	}
	
	public boolean isLockedAccount() {
		return locked_account;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public ArrayList<GroupNG> getUserGroups() {
		// TODO lazy load
		return user_groups;
	}
	
	public String getKey() {
		return key;
	}
	
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
	
	public Properties getProperties() {
		// TODO lazy load
		return properties;
	}
	
	public LinkedHashMap<String, BasketNG> getBaskets() {
		// TODO lazy load
		return baskets;
	}
	
	public ArrayList<UserActivity> getActivity() {
		// TODO lazy load
		return activities;
	}
	
	public ArrayList<UserNotificationNG> getNotifications() {
		// TODO lazy load
		return notifications;
	}
	
	public ArrayList<RoleNG> getUser_groups_roles() {
		// TODO lazy load
		return user_groups_roles;
	}
	
	public HashSet<String> getUser_groups_roles_privileges() {
		// TODO lazy load
		return user_groups_roles_privileges;
	}
	
	public void sendTestMail() throws Exception {
		// TODO create button for send a mail
		InternetAddress email_addr = new InternetAddress(this.email_addr);
		
		EndUserBaseMail mail;
		if (language == null) {
			mail = new EndUserBaseMail(Locale.getDefault(), email_addr, "usertestmail");
		} else {
			mail = new EndUserBaseMail(Lang.getLocale(language), email_addr, "usertestmail");
		}
		
		mail.send();
	}
	
}
