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

import com.google.common.reflect.TypeToken;
import com.netflix.astyanax.ColumnListMutation;

public class RoleNG {
	
	private String key;
	private String role_name;
	private HashSet<String> privileges;
	
	private transient ArrayList<GroupNG> role_groups;
	private transient ArrayList<UserNG> role_groups_users;
	private transient AuthTurret turret;
	
	private static Type hashset_privileges_typeOfT = new TypeToken<HashSet<String>>() {
	}.getType();
	
	RoleNG save(ColumnListMutation<String> mutator) {
		mutator.putColumnIfNotNull("role_name", role_name);
		if (privileges != null) {
			mutator.putColumnIfNotNull("group_roles", turret.getGson().toJson(privileges, hashset_privileges_typeOfT));
		}
		return this;
	}
	
	RoleNG(AuthTurret turret, String key, boolean load_from_db) {
		this.turret = turret;
		if (turret == null) {
			throw new NullPointerException("\"turret\" can't to be null");
		}
	}
	
	public String getKey() {
		return key;
	}
	
	// TODO import db
	
	// TODO CRUD
	// TODO Gson (de) serializers
	
	// @see Privileges.getAllSortedPrivileges()
}
