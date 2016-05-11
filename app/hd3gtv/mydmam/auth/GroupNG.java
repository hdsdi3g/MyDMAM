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

public class GroupNG {
	
	private String key;
	private String group_name;
	private ArrayList<RoleNG> group_roles;
	
	private transient HashSet<String> group_roles_privileges;
	private transient ArrayList<UserNG> users_group;
	private transient AuthTurret turret;
	
	private static Type al_role_typeOfT = new TypeToken<ArrayList<RoleNG>>() {
	}.getType();
	
	GroupNG save(ColumnListMutation<String> mutator) {
		mutator.putColumnIfNotNull("group_name", group_name);
		if (group_roles != null) {
			mutator.putColumnIfNotNull("group_roles", turret.getGson().toJson(group_roles, al_role_typeOfT));
		}
		return this;
	}
	
	GroupNG(AuthTurret turret, String key, boolean load_from_db) {
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
}
