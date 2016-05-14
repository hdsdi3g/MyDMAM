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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;

public class GroupNG {
	
	private String key;
	private String group_name;
	private ArrayList<RoleNG> group_roles;
	
	private transient ArrayList<UserNG> users_group;
	private transient AuthTurret turret;
	
	private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	// TODO group key must start with "group:"
	
	/**
	 * This cols names will always be imported from db.
	 */
	static final HashSet<String> COLS_NAMES_LIMITED_TO_DB_IMPORT = new HashSet<String>(Arrays.asList("group_name", "group_roles"));
	
	GroupNG save(ColumnListMutation<String> mutator) {
		mutator.putColumnIfNotNull("group_name", group_name);
		if (group_roles != null) {
			ArrayList<String> roles_keys = new ArrayList<String>(group_roles.size() + 1);
			group_roles.forEach(role -> {
				roles_keys.add(role.getKey());
			});
			mutator.putColumnIfNotNull("group_roles", turret.getGson().toJson(roles_keys, al_String_typeOfT));
		}
		return this;
	}
	
	GroupNG loadFromDb(ColumnList<String> cols) {
		if (cols.isEmpty()) {
			return this;
		}
		group_name = cols.getStringValue("group_name", null);
		
		if (cols.getColumnByName("group_roles") != null) {
			ArrayList<String> roles_keys = turret.getGson().fromJson(cols.getColumnByName("group_roles").getStringValue(), al_String_typeOfT);
			group_roles = new ArrayList<RoleNG>(roles_keys.size() + 1);
			roles_keys.forEach(role_key -> {
				group_roles.add(turret.getByRoleKey(role_key));
			});
		} else {
			group_roles = null;
		}
		return this;
	}
	
	GroupNG(AuthTurret turret, String key, boolean load_from_db) {
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
				loadFromDb(turret.prepareQuery().getKey(key).execute().getResult());
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
	}
	
	/**
	 * Create simple group
	 */
	GroupNG(String group_name) {
		this.group_name = group_name;
		if (group_name == null) {
			throw new NullPointerException("\"group_name\" can't to be null");
		}
		key = "group:" + group_name;
	}
	
	public String getGroupName() {
		return group_name;
	}
	
	GroupNG update(ArrayList<RoleNG> group_roles) {
		if (group_roles == null) {
			throw new NullPointerException("\"group_roles\" can't to be null");
		}
		this.group_roles = group_roles;
		return this;
	}
	
	public ArrayList<RoleNG> getGroupRoles() {
		synchronized (group_roles) {
			if (group_roles == null) {
				group_roles = new ArrayList<RoleNG>(1);
				ColumnList<String> cols;
				try {
					cols = turret.prepareQuery().getKey(key).withColumnSlice("group_roles").execute().getResult();
					if (cols.getColumnByName("group_roles").hasValue()) {
						ArrayList<String> roles_keys = turret.getGson().fromJson(cols.getColumnByName("group_roles").getStringValue(), al_String_typeOfT);
						roles_keys.forEach(role_key -> {
							group_roles.add(turret.getByRoleKey(role_key));
						});
					}
				} catch (ConnectionException e) {
					turret.onConnectionException(e);
				}
			}
		}
		return group_roles;
	}
	
	public String getKey() {
		return key;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(", ");
		sb.append(group_name);
		return sb.toString();
	}
	
	public JsonObject exportForAdmin() {
		JsonObject jo = new JsonObject();
		jo.addProperty("group_name", group_name);
		
		JsonArray ja_roles = new JsonArray();
		getGroupRoles().forEach(role -> {
			ja_roles.add(new JsonPrimitive(role.getKey()));
		});
		jo.add("group_roles", ja_roles);
		
		return jo;
	}
	
	void delete(ColumnListMutation<String> mutator) {
		turret.getAllUsers().forEach((user_key, user) -> {
			user.getUserGroups().remove(this);
		});
		mutator.delete();
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return super.hashCode();
	}
}
