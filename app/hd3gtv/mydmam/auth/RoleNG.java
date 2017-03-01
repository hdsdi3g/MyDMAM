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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.asyncjs.RoleView;
import hd3gtv.mydmam.gson.GsonKit;

public class RoleNG implements AuthEntry {
	
	private String key;
	private String role_name;
	private HashSet<String> privileges;
	
	private transient AuthTurret turret;
	
	/**
	 * This cols names will always be imported from db.
	 */
	static final HashSet<String> COLS_NAMES_LIMITED_TO_DB_IMPORT = new HashSet<String>(Arrays.asList("role_name", "privileges"));
	
	public void save(ColumnListMutation<String> mutator) {
		Loggers.Auth.trace("Save Role " + key);
		mutator.putColumnIfNotNull("role_name", role_name);
		if (privileges != null) {
			mutator.putColumnIfNotNull("privileges", MyDMAM.gson_kit.getGson().toJson(privileges, GsonKit.type_HashSet_String));
		}
	}
	
	RoleNG loadFromDb(ColumnList<String> cols) {
		if (cols.isEmpty()) {
			return this;
		}
		role_name = cols.getStringValue("role_name", null);
		
		if (cols.getColumnByName("privileges") != null) {
			privileges = MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("privileges").getStringValue(), GsonKit.type_HashSet_String);
		} else {
			privileges = null;
		}
		
		return this;
	}
	
	RoleNG(AuthTurret turret, String key, boolean load_from_db) {
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
	 * Create simple role
	 */
	RoleNG(AuthTurret turret, String role_name) {
		this.turret = turret;
		if (turret == null) {
			throw new NullPointerException("\"turret\" can't to be null");
		}
		if (role_name == null) {
			throw new NullPointerException("\"role_name\" can't to be null");
		}
		this.role_name = role_name;
		this.key = AuthTurret.makeKey("role", role_name);
	}
	
	public String getName() {
		return role_name;
	}
	
	RoleNG update(Set<String> privileges) {
		if (privileges == null) {
			throw new NullPointerException("\"privileges\" can't to be null");
		}
		this.privileges = new HashSet<String>(privileges);
		return this;
	}
	
	public HashSet<String> getPrivileges() {
		if (privileges == null) {
			privileges = new HashSet<String>(1);
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("getPrivileges from db " + key);
			}
			ColumnList<String> cols;
			try {
				cols = turret.prepareQuery().getKey(key).withColumnSlice("privileges").execute().getResult();
				if (cols.getColumnNames().contains("privileges")) {
					if (cols.getColumnByName("privileges").hasValue()) {
						privileges = MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("privileges").getStringValue(), GsonKit.type_HashSet_String);
					}
				}
			} catch (ConnectionException e) {
				turret.onConnectionException(e);
			}
		}
		return privileges;
	}
	
	public String getKey() {
		return key;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(key);
		sb.append(", ");
		sb.append(role_name);
		return sb.toString();
	}
	
	public RoleView export() {
		RoleView result = new RoleView();
		result.role_name = role_name;
		result.key = key;
		result.privileges = new ArrayList<String>();
		
		getPrivileges().forEach(privilege -> {
			result.privileges.add(privilege);
		});
		
		return result;
	}
	
	public void delete(ColumnListMutation<String> mutator) {
		turret.getAllGroups().forEach((group_key, group) -> {
			group.getGroupRoles().remove(this);
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("Remove role " + key + " from Group " + group_key);
			}
		});
		mutator.delete();
	}
	
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if ((obj instanceof RoleNG) == false) {
			return false;
		}
		return key.equals(((RoleNG) obj).key);
	}
	
	public int hashCode() {
		return key.hashCode();
	}
	
}
