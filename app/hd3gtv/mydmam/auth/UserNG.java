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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.internet.InternetAddress;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.asyncjs.UserView;
import hd3gtv.mydmam.mail.EndUserBaseMail;
import play.i18n.Lang;

public class UserNG implements AuthEntry {
	
	private String key;
	
	private String login;
	private String fullname;
	private String domain;
	private String language;
	private String email_addr;
	private byte[] protected_password;
	
	private long createdate;
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
	
	public static final Type linmap_string_basket_typeOfT = new TypeToken<LinkedHashMap<String, BasketNG>>() {
	}.getType();
	public static final Type al_useractivity_typeOfT = new TypeToken<ArrayList<UserActivity>>() {
	}.getType();
	public static final Type al_usernotification_typeOfT = new TypeToken<ArrayList<UserNotificationNG>>() {
	}.getType();
	public static final Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	/**
	 * This cols names will always be imported from db.
	 */
	static final HashSet<String> COLS_NAMES_LIMITED_TO_DB_IMPORT = new HashSet<String>(
			Arrays.asList("login", "fullname", "domain", "language", "email_addr", "protected_password", "lasteditdate", "lastlogindate", "lastloginipsource", "locked_account", "user_groups"));
			
	public void save(ColumnListMutation<String> mutator) {
		Loggers.Auth.trace("Save User " + key);
		
		mutator.putColumnIfNotNull("login", login);
		mutator.putColumnIfNotNull("fullname", fullname);
		mutator.putColumnIfNotNull("domain", domain);
		mutator.putColumnIfNotNull("language", language);
		mutator.putColumnIfNotNull("email_addr", email_addr);
		mutator.putColumnIfNotNull("protected_password", protected_password);
		mutator.putColumnIfNotNull("lasteditdate", lasteditdate);
		mutator.putColumnIfNotNull("createdate", createdate);
		mutator.putColumnIfNotNull("lastlogindate", lastlogindate);
		mutator.putColumnIfNotNull("lastloginipsource", lastloginipsource);
		mutator.putColumnIfNotNull("locked_account", locked_account);
		
		if (user_groups != null) {
			ArrayList<String> group_keys = new ArrayList<String>(user_groups.size() + 1);
			user_groups.forEach(group -> {
				group_keys.add(group.getKey());
			});
			mutator.putColumnIfNotNull("user_groups", turret.getGson().toJson(group_keys, al_String_typeOfT));
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
	}
	
	UserNG loadFromDb(ColumnList<String> cols) {
		if (cols.isEmpty()) {
			return this;
		}
		login = cols.getStringValue("login", null);
		fullname = cols.getStringValue("fullname", null);
		domain = cols.getStringValue("domain", null);
		language = cols.getStringValue("language", null);
		email_addr = cols.getStringValue("email_addr", null);
		protected_password = cols.getByteArrayValue("protected_password", null);
		lasteditdate = cols.getLongValue("lasteditdate", 0l);
		createdate = cols.getLongValue("createdate", 0l);
		lastlogindate = cols.getLongValue("lastlogindate", 0l);
		lastloginipsource = cols.getStringValue("lastloginipsource", null);
		locked_account = cols.getBooleanValue("locked_account", false);
		
		Column<String> col = null;
		col = cols.getColumnByName("user_groups");
		if (col != null) {
			ArrayList<String> group_keys = turret.getGson().fromJson(col.getStringValue(), al_String_typeOfT);
			user_groups = new ArrayList<GroupNG>(group_keys.size() + 1);
			group_keys.forEach(group_key -> {
				user_groups.add(turret.getByGroupKey(group_key));
			});
		} else {
			user_groups = new ArrayList<>();
		}
		
		col = cols.getColumnByName("properties");
		if (col != null) {
			properties = turret.getGson().fromJson(col.getStringValue(), Properties.class);
		} else {
			properties = null;
		}
		
		col = cols.getColumnByName("baskets");
		if (col != null) {
			baskets = turret.getGson().fromJson(col.getStringValue(), linmap_string_basket_typeOfT);
		} else {
			baskets = null;
		}
		col = cols.getColumnByName("activities");
		if (col != null) {
			activities = turret.getGson().fromJson(col.getStringValue(), al_useractivity_typeOfT);
		} else {
			activities = null;
		}
		
		col = cols.getColumnByName("notifications");
		if (col != null) {
			notifications = turret.getGson().fromJson(col.getStringValue(), al_usernotification_typeOfT);
		} else {
			notifications = null;
		}
		
		col = cols.getColumnByName("preferencies");
		if (col != null) {
			preferencies = turret.parser.parse(col.getStringValue()).getAsJsonObject();
		} else {
			preferencies = null;
		}
		
		if (Loggers.Auth.isTraceEnabled()) {
			Loggers.Auth.trace("Load User from db: " + toString());
		}
		return this;
	}
	
	UserNG(AuthTurret turret, String key, boolean load_from_db) {
		this.turret = turret;
		if (turret == null) {
			throw new NullPointerException("\"turret\" can't to be null");
		}
		this.key = key;
		if (key == null) {
			throw new NullPointerException("\"key\" can't to be null");
		}
		
		if (load_from_db) {
			try {
				loadFromDb(turret.prepareQuery().getKey(key).withColumnSlice(COLS_NAMES_LIMITED_TO_DB_IMPORT).execute().getResult());
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
	}
	
	public static String computeUserKey(String login, String domain) {
		return "user:" + login + "%" + domain;
	}
	
	/**
	 * New simple user
	 */
	UserNG(AuthTurret turret, String login, String domain) {
		this.turret = turret;
		if (turret == null) {
			throw new NullPointerException("\"turret\" can't to be null");
		}
		this.login = login;
		if (login == null) {
			throw new NullPointerException("\"login\" can't to be null");
		}
		this.domain = domain;
		if (domain == null) {
			throw new NullPointerException("\"domain\" can't to be null");
		}
		key = computeUserKey(login, domain);
		createdate = System.currentTimeMillis();
	}
	
	UserNG update(String fullname, String language, String email_addr, boolean locked_account) {
		if (fullname == null) {
			throw new NullPointerException("\"fullname\" can't to be null");
		}
		this.fullname = fullname;
		if (language == null) {
			throw new NullPointerException("\"language\" can't to be null");
		}
		this.language = language;
		if (email_addr == null) {
			throw new NullPointerException("\"email_addr\" can't to be null");
		}
		this.email_addr = email_addr;
		this.locked_account = locked_account;
		lasteditdate = System.currentTimeMillis();
		
		if (Loggers.Auth.isDebugEnabled()) {
			Loggers.Auth.debug("Update User: " + toString());
		}
		return this;
	}
	
	UserNG updateLastEditTime() {
		lasteditdate = System.currentTimeMillis();
		return this;
	}
	
	UserNG chpassword(String clear_text_password) {
		if (clear_text_password == null) {
			throw new NullPointerException("\"clear_text_password\" can't to be null");
		}
		
		protected_password = turret.getPassword().getHashedPassword(clear_text_password);
		
		if (Loggers.Auth.isDebugEnabled()) {
			Loggers.Auth.debug("Ch User password: " + toString());
		}
		return this;
	}
	
	UserNG setEmailAddr(String email_addr) {
		this.email_addr = email_addr;
		return this;
	}
	
	boolean checkValidPassword(String clear_text_password_candidate) {
		if (protected_password == null) {
			Loggers.Auth.debug("Password is not set for " + key);
			return false;
		}
		if (protected_password.length == 0) {
			Loggers.Auth.debug("Password has no datas for " + key);
			return false;
		}
		
		Loggers.Auth.debug("Do check user password: " + key);
		return turret.getPassword().checkPassword(clear_text_password_candidate, protected_password);
	}
	
	public ArrayList<GroupNG> getUserGroups() {
		if (user_groups == null) {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getUserGroups from db " + key);
			}
			user_groups = new ArrayList<GroupNG>(1);
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("user_groups").execute().getResult();
				if (cols.getColumnNames().contains("user_groups")) {
					if (cols.getColumnByName("user_groups").hasValue()) {
						ArrayList<String> group_keys = turret.getGson().fromJson(cols.getColumnByName("user_groups").getStringValue(), al_String_typeOfT);
						group_keys.forEach(group_key -> {
							user_groups.add(turret.getByGroupKey(group_key));
						});
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return user_groups;
	}
	
	public UserNG setUserGroups(List<GroupNG> user_groups) {
		if (user_groups == null) {
			throw new NullPointerException("\"user_groups\" can't to be null");
		}
		
		this.user_groups = new ArrayList<GroupNG>(user_groups);
		return this;
	}
	
	public String getKey() {
		return key;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(", ");
		sb.append(login);
		sb.append("@");
		sb.append(domain);
		sb.append(" (");
		sb.append(fullname);
		sb.append("/");
		sb.append(language);
		sb.append(")");
		return sb.toString();
	}
	
	public Properties getProperties() {
		if (properties == null) {
			properties = new Properties();
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getProperties from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("properties").execute().getResult();
				if (cols.getColumnNames().contains("properties")) {
					if (cols.getColumnByName("properties").hasValue()) {
						properties = turret.getGson().fromJson(cols.getColumnByName("properties").getStringValue(), Properties.class);
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		
		return properties;
	}
	
	public LinkedHashMap<String, BasketNG> getBaskets() {
		if (baskets == null) {
			baskets = new LinkedHashMap<String, BasketNG>(1);
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getBaskets from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("baskets").execute().getResult();
				if (cols.getColumnNames().contains("baskets")) {
					if (cols.getColumnByName("baskets").hasValue()) {
						baskets = turret.getGson().fromJson(cols.getColumnByName("baskets").getStringValue(), linmap_string_basket_typeOfT);
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return baskets;
	}
	
	public ArrayList<UserActivity> getActivities() {
		if (activities == null) {
			activities = new ArrayList<UserActivity>();
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getActivities from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("activities").execute().getResult();
				if (cols.getColumnNames().contains("activities")) {
					if (cols.getColumnByName("activities").hasValue()) {
						activities = turret.getGson().fromJson(cols.getColumnByName("activities").getStringValue(), al_useractivity_typeOfT);
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return activities;
	}
	
	public JsonObject getPreferencies() {
		if (preferencies == null) {
			preferencies = new JsonObject();
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getPreferencies from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("preferencies").execute().getResult();
				if (cols.getColumnNames().contains("preferencies")) {
					if (cols.getColumnByName("preferencies").hasValue()) {
						preferencies = turret.parser.parse(cols.getColumnByName("preferencies").getStringValue()).getAsJsonObject();
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return preferencies;
	}
	
	public ArrayList<UserNotificationNG> getNotifications() {
		if (notifications == null) {
			notifications = new ArrayList<UserNotificationNG>();
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getNotifications from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("notifications").execute().getResult();
				if (cols.getColumnNames().contains("notifications")) {
					if (cols.getColumnByName("notifications").hasValue()) {
						notifications = turret.getGson().fromJson(cols.getColumnByName("notifications").getStringValue(), al_usernotification_typeOfT);
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return notifications;
	}
	
	public ArrayList<RoleNG> getUser_groups_roles() {
		if (user_groups_roles == null) {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getUser_groups_roles from db " + key);
			}
			user_groups_roles = turret.getRolesByGroupList(getUserGroups());
		}
		return user_groups_roles;
	}
	
	public HashSet<String> getUser_groups_roles_privileges() {
		if (user_groups_roles_privileges == null) {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getUser_groups_roles_privileges from db " + key);
			}
			user_groups_roles_privileges = turret.getPrivilegesByGroupList(getUserGroups());
		}
		return user_groups_roles_privileges;
	}
	
	public void sendTestMail() throws Exception {
		if (Loggers.Auth.isDebugEnabled()) {
			Loggers.Auth.debug("Send test mail " + toString());
		}
		
		InternetAddress email_addr = new InternetAddress(this.email_addr);
		
		EndUserBaseMail mail;
		if (language == null) {
			mail = new EndUserBaseMail(Locale.getDefault(), email_addr, "usertestmail");
		} else {
			mail = new EndUserBaseMail(Lang.getLocale(language), email_addr, "usertestmail");
		}
		
		mail.send();
	}
	
	public UserView export(boolean complete, boolean admin_view) {
		UserView result = new UserView();
		if (admin_view) {
			result.key = this.key;
			result.login = this.login;
			result.domain = this.domain;
			result.createdate = this.createdate;
			result.lasteditdate = this.lasteditdate;
			result.lastlogindate = this.lastlogindate;
			result.lastloginipsource = this.lastloginipsource;
			result.locked_account = this.locked_account;
			result.user_groups = new ArrayList<String>();
			getUserGroups().forEach(group -> {
				result.user_groups.add(group.getKey());
			});
		}
		
		result.fullname = this.fullname;
		result.language = this.language;
		result.email_addr = this.email_addr;
		
		if (complete) {
			result.preferencies = getPreferencies();
			result.properties = turret.getGson().toJsonTree(getProperties()).getAsJsonObject();
			result.baskets = turret.getGson().toJsonTree(getBaskets(), linmap_string_basket_typeOfT).getAsJsonObject();
			result.activities = turret.getGson().toJsonTree(getActivities(), al_useractivity_typeOfT).getAsJsonArray();
			result.notifications = turret.getGson().toJsonTree(getNotifications(), al_usernotification_typeOfT).getAsJsonArray();
		} else {
			if (preferencies != null) {
				result.preferencies = preferencies;
			}
			if (properties != null) {
				result.properties = turret.getGson().toJsonTree(properties).getAsJsonObject();
			}
			if (baskets != null) {
				result.baskets = turret.getGson().toJsonTree(baskets, linmap_string_basket_typeOfT).getAsJsonObject();
			}
			if (activities != null) {
				result.activities = turret.getGson().toJsonTree(activities, al_useractivity_typeOfT).getAsJsonArray();
			}
			if (notifications != null) {
				result.notifications = turret.getGson().toJsonTree(notifications, al_usernotification_typeOfT).getAsJsonArray();
			}
		}
		
		return result;
	}
	
	void doLoginOperations(String loginipsource, String language) {
		this.lastloginipsource = loginipsource;
		this.language = language;
		this.lastlogindate = System.currentTimeMillis();
		if (Loggers.Auth.isTraceEnabled()) {
			Loggers.Auth.trace("Do login operations " + key);
		}
	}
	
	void postCreate(String fullname, String email_addr) {
		this.fullname = fullname;
		this.email_addr = email_addr;
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
	
	void setLocked_account(boolean locked_account) {
		this.locked_account = locked_account;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public void delete(ColumnListMutation<String> mutator) {
		if (Loggers.Auth.isTraceEnabled()) {
			Loggers.Auth.trace("Delete user " + key);
		}
		mutator.delete();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ((obj instanceof UserNG) == false) {
			return false;
		}
		return key.equals(((UserNG) obj).key);
	}
	
	public int hashCode() {
		return key.hashCode();
	}
}
