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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonObject;
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
	
	GroupNG save(ColumnListMutation<String> mutator) {
		mutator.putColumnIfNotNull("group_name", group_name);
		if (group_roles != null) {
			// mutator.putColumnIfNotNull("group_roles", turret.getGson().toJson(group_roles, al_role_typeOfT));
			// TODO set roles by names
		}
		return this;
	}
	
	GroupNG loadFromDb(ColumnList<String> cols) {
		if (cols.isEmpty()) {
			return this;
		}
		group_name = cols.getStringValue("group_name", null);
		
		if (cols.getColumnByName("group_roles") != null) {
			// group_roles = turret.getGson().fromJson(cols.getColumnByName("group_roles").getStringValue(), al_role_typeOfT);
			// TODO get roles by names
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
	
	public ArrayList<RoleNG> getGroupRoles() {
		synchronized (group_roles) {
			if (group_roles == null) {
				group_roles = new ArrayList<RoleNG>(1);
				ColumnList<String> cols;
				try {
					cols = turret.prepareQuery().getKey(key).withColumnSlice("group_roles").execute().getResult();
					if (cols.getColumnByName("group_roles").hasValue()) {
						// group_roles = turret.getGson().fromJson(cols.getColumnByName("group_roles").getStringValue(), al_role_typeOfT);
						// TODO get roles by names
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
		getGroupRoles().forEach(role -> {
			jo.add(role.getKey(), role.exportForAdmin());
		});
		return jo;
	}
	
	// TODO CRUD
}
