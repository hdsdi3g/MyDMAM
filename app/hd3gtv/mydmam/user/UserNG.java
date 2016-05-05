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
package hd3gtv.mydmam.user;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.netflix.astyanax.MutationBatch;

public class UserNG {
	
	private String key;
	
	private String login;
	private String fullname;
	private String domain;
	private byte[] protected_password;
	
	private long lasteditdate;
	private long lastlogindate;
	private String lastloginipsource;
	private boolean locked_account;
	
	private ArrayList<GroupNG> user_groups;
	private JsonObject preferencies;
	private Properties properties;
	private LinkedHashMap<String, BasketNG> baskets;
	private ArrayList<UserActivity> activity;
	private ArrayList<UserNotificationNG> notifications;
	
	private ArrayList<RoleNG> user_groups_roles;
	private HashSet<String> user_groups_roles_privileges;
	
	UserNG(AuthTurret turret, String key, boolean load_from_db) {
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
	
	void save(MutationBatch mutator) {
		// TODO
	}
	
	void doUpdateOperations() {
		// TODO like update lasteditdate
	}
	
	void doLoginOperations(String loginipsource) {
		// TODO like update lastlogindate ...
	}
	
	public boolean isLockedAccount() {
		return locked_account;
	}
	
	public ArrayList<GroupNG> getUserGroups() {
		// TODO lazy load
		return user_groups;
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
		return activity;
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
	
	// TODO import/export db
	
	// TODO CRUD
	
}
