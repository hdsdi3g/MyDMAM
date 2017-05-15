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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.gson.JsonObject;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnList;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.asyncjs.UserView;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.gson.GsonKit;
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
	private JsonObject preferences;
	private Properties properties;
	private LinkedHashMap<String, BasketNG> baskets;
	private ArrayList<UserActivity> activities;
	private ArrayList<UserNotificationNG> notifications;
	
	private transient ArrayList<RoleNG> user_groups_roles;
	private transient HashSet<String> user_groups_roles_privileges;
	private transient AuthTurret turret;
	
	/**
	 * This cols names will always be imported from db.
	 */
	static final HashSet<String> COLS_NAMES_LIMITED_TO_DB_IMPORT = new HashSet<String>(Arrays.asList("login", "fullname", "domain", "language", "email_addr", "protected_password", "lasteditdate", "lastlogindate", "lastloginipsource", "locked_account", "user_groups"));
	
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
			mutator.putColumnIfNotNull("user_groups", MyDMAM.gson_kit.getGson().toJson(group_keys, GsonKit.type_ArrayList_String));
		}
		if (properties != null) {
			mutator.putColumnIfNotNull("properties", MyDMAM.gson_kit.getGson().toJson(properties));
		}
		if (baskets != null) {
			mutator.putColumnIfNotNull("baskets", MyDMAM.gson_kit.getGson().toJson(baskets, GsonKit.type_LinkedHashMap_StringBasketNG));
		}
		if (activities != null) {
			mutator.putColumnIfNotNull("activities", MyDMAM.gson_kit.getGson().toJson(activities, GsonKit.type_ArrayList_UserActivity));
		}
		if (notifications != null) {
			mutator.putColumnIfNotNull("notifications", MyDMAM.gson_kit.getGson().toJson(notifications, GsonKit.type_ArrayList_UserNotificationNG));
		}
		if (preferences != null) {
			mutator.putColumnIfNotNull("preferences", preferences.toString());
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
			ArrayList<String> group_keys = MyDMAM.gson_kit.getGson().fromJson(col.getStringValue(), GsonKit.type_ArrayList_String);
			user_groups = new ArrayList<GroupNG>(group_keys.size() + 1);
			ArrayList<String> old_group_keys = new ArrayList<>();
			
			checkAndSetGroups(group_keys, old_group_keys);
			if (old_group_keys.isEmpty() == false) {
				try {
					saveGroups(old_group_keys);
				} catch (ConnectionException e) {
					turret.onConnectionException(e);
				}
			}
		} else {
			user_groups = new ArrayList<>();
		}
		
		col = cols.getColumnByName("properties");
		if (col != null) {
			properties = MyDMAM.gson_kit.getGson().fromJson(col.getStringValue(), Properties.class);
		} else {
			properties = null;
		}
		
		col = cols.getColumnByName("baskets");
		if (col != null) {
			baskets = MyDMAM.gson_kit.getGson().fromJson(col.getStringValue(), GsonKit.type_LinkedHashMap_StringBasketNG);
		} else {
			baskets = null;
		}
		col = cols.getColumnByName("activities");
		if (col != null) {
			activities = MyDMAM.gson_kit.getGson().fromJson(col.getStringValue(), GsonKit.type_ArrayList_UserActivity);
		} else {
			activities = null;
		}
		
		col = cols.getColumnByName("notifications");
		if (col != null) {
			notifications = MyDMAM.gson_kit.getGson().fromJson(col.getStringValue(), GsonKit.type_ArrayList_UserNotificationNG);
		} else {
			notifications = null;
		}
		
		col = cols.getColumnByName("preferences");
		if (col != null) {
			preferences = turret.parser.parse(col.getStringValue()).getAsJsonObject();
		} else {
			preferences = null;
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
		return AuthTurret.makeKey("user", login + "%" + domain);
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
	
	/**
	 * == login
	 */
	public String getName() {
		return login;
	}
	
	public String getDomain() {
		return domain;
	}
	
	UserNG update(String fullname, String language, String email_addr, boolean locked_account) throws AddressException {
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
		setEmailAddr(email_addr);
		
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
	
	UserNG setEmailAddr(String email_addr) throws AddressException {
		if (email_addr == null) {
			this.email_addr = null;
			return this;
		}
		if (email_addr.equalsIgnoreCase(this.email_addr)) {
			return this;
		}
		this.email_addr = email_addr;
		new InternetAddress(email_addr, true);
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
				ArrayList<String> old_groups_keys = new ArrayList<String>(1);
				final ArrayList<String> groups_keys = new ArrayList<String>(1);
				
				cols = turret.prepareQuery().getKey(key).withColumnSlice("user_groups").execute().getResult();
				if (cols.getColumnNames().contains("user_groups")) {
					if (cols.getColumnByName("user_groups").hasValue()) {
						groups_keys.clear();
						groups_keys.addAll(MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("group_roles").getStringValue(), GsonKit.type_ArrayList_String));
						checkAndSetGroups(groups_keys, old_groups_keys);
					}
				}
				
				if (old_groups_keys.isEmpty() == false) {
					saveGroups(old_groups_keys);
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return user_groups;
	}
	
	private void checkAndSetGroups(ArrayList<String> group_keys, ArrayList<String> old_group_keys) {
		group_keys.forEach(group_key -> {
			GroupNG group = turret.getByGroupKey(group_key);
			if (group != null) {
				user_groups.add(group);
			} else {
				old_group_keys.add(group_key);
			}
		});
	}
	
	private void saveGroups(ArrayList<String> old_group_keys) throws ConnectionException {
		Loggers.Auth.info("Missing groups for user " + this + ", remove " + old_group_keys);
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		save(mutator.withRow(AuthTurret.CF_AUTH, getKey()));
		mutator.execute();
		turret.getCache().updateManuallyCache(this);
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
						properties = MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("properties").getStringValue(), Properties.class);
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
						baskets = MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("baskets").getStringValue(), GsonKit.type_LinkedHashMap_StringBasketNG);
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
						activities = MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("activities").getStringValue(), GsonKit.type_ArrayList_UserActivity);
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return activities;
	}
	
	public JsonObject getPreferences() {
		if (preferences == null) {
			preferences = new JsonObject();
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getPreferences from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("preferences").execute().getResult();
				if (cols.getColumnNames().contains("preferences")) {
					if (cols.getColumnByName("preferences").hasValue()) {
						preferences = turret.parser.parse(cols.getColumnByName("preferences").getStringValue()).getAsJsonObject();
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return preferences;
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
						notifications = MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("notifications").getStringValue(), GsonKit.type_ArrayList_UserNotificationNG);
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
		
		InternetAddress email_addr = getInternetAddress();
		if (email_addr == null) {
			return;
		}
		
		EndUserBaseMail mail;
		if (language == null) {
			mail = new EndUserBaseMail(Locale.getDefault(), "usertestmail", email_addr);
		} else {
			mail = new EndUserBaseMail(Lang.getLocale(language), "usertestmail", email_addr);
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
			result.preferences = getPreferences();
			StringWriter sw = new StringWriter();
			try {
				getProperties().store(sw, null);
			} catch (IOException e) {
				Loggers.Auth.error("Can't export Properties to String", e);
			}
			result.properties = sw.toString();
			
			result.baskets = MyDMAM.gson_kit.getGson().toJsonTree(getBaskets(), GsonKit.type_LinkedHashMap_StringBasketNG).getAsJsonObject();
			result.activities = MyDMAM.gson_kit.getGson().toJsonTree(getActivities(), GsonKit.type_ArrayList_UserActivity).getAsJsonArray();
			result.notifications = MyDMAM.gson_kit.getGson().toJsonTree(getNotifications(), GsonKit.type_ArrayList_UserNotificationNG).getAsJsonArray();
		} else {
			if (preferences != null) {
				result.preferences = preferences;
			}
			if (properties != null) {
				StringWriter sw = new StringWriter();
				try {
					getProperties().store(sw, null);
				} catch (IOException e) {
					Loggers.Auth.error("Can't export Properties to String", e);
				}
				result.properties = sw.toString();
			}
			if (baskets != null) {
				result.baskets = MyDMAM.gson_kit.getGson().toJsonTree(baskets, GsonKit.type_LinkedHashMap_StringBasketNG).getAsJsonObject();
			}
			if (activities != null) {
				result.activities = MyDMAM.gson_kit.getGson().toJsonTree(activities, GsonKit.type_ArrayList_UserActivity).getAsJsonArray();
			}
			if (notifications != null) {
				result.notifications = MyDMAM.gson_kit.getGson().toJsonTree(notifications, GsonKit.type_ArrayList_UserNotificationNG).getAsJsonArray();
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
	
	void postCreate(String fullname, String email_addr, String language) throws AddressException {
		this.fullname = fullname;
		setEmailAddr(email_addr);
		this.language = language;
	}
	
	/**
	 * @return key if fullname is not set
	 */
	public String getFullname() {
		return fullname;
	}
	
	public String getEmailAddr() {
		return email_addr;
	}
	
	/**
	 * @return can be null
	 */
	public InternetAddress getInternetAddress() {
		if (email_addr == null) {
			return null;
		}
		if (email_addr.isEmpty()) {
			return null;
		}
		try {
			InternetAddress ia = new InternetAddress(email_addr, true);
			if (fullname != null) {
				if (fullname.isEmpty() == false) {
					ia.setPersonal(fullname);
				}
			}
			return ia;
		} catch (UnsupportedEncodingException | AddressException e) {
			Loggers.Auth.error("Invalid email address for " + key + " (" + fullname + ")", e);
		}
		return null;
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
	
	public Locale getLocale() {
		return Lang.getLocaleOrDefault(language);
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
