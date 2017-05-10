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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.dareport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.auth.GroupNG;
import hd3gtv.mydmam.auth.RoleNG;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.auth.asyncjs.GroupChRole;
import hd3gtv.mydmam.auth.asyncjs.RoleChPrivileges;
import hd3gtv.mydmam.auth.asyncjs.UserAdminUpdate;
import hd3gtv.mydmam.web.PlayBootstrap;
import play.data.validation.Validation;

public class AJS_DAR_AccountNew {
	
	String userkey;
	String jobkey;
	
	public void create() throws Exception {
		PlayBootstrap.validate(Validation.required("userkey", userkey), Validation.required("job", jobkey));
		
		if (DARDB.get().getJobs().containsKey(jobkey) == false) {
			throw new Exception("Can't found job " + jobkey + " in configuration");
		}
		
		DARAccount account = new DARAccount();
		account.created_at = System.currentTimeMillis();
		account.jobkey = jobkey;
		account.userkey = userkey;
		
		AuthTurret turret = MyDMAM.getPlayBootstrapper().getAuth();
		UserNG user = turret.getByUserKey(userkey);
		if (user == null) {
			throw new NullPointerException("Can't found user " + userkey + " in current user list");
		}
		
		if (user.getUser_groups_roles_privileges().stream().anyMatch(privilege -> {
			return privilege.equalsIgnoreCase("userDAReport");
		}) == false) {
			/**
			 * Check Special role
			 */
			RoleNG dar_role = turret.getAllRoles().values().stream().filter(role -> {
				return role.getName().equals("Daily activity report author");
			}).findFirst().orElseGet(() -> {
				try {
					return turret.createRole("Daily activity report author");
				} catch (ConnectionException e) {
					Loggers.DAReport.error("Can't connect to Cassandra", e);
				}
				return null;
			});
			
			if (dar_role == null) {
				throw new Exception("Can't get new Role");
			}
			
			if (dar_role.getPrivileges().stream().noneMatch(privilege -> {
				return privilege.equalsIgnoreCase("userDAReport");
			})) {
				RoleChPrivileges rchp = new RoleChPrivileges();
				rchp.role_key = dar_role.getKey();
				rchp.privileges = new ArrayList<>();
				rchp.privileges.addAll(dar_role.getPrivileges());
				rchp.privileges.add("userDAReport");
				/*dar_role =*/ turret.changeRolePrivileges(rchp);// TODO changeRole don't works ?!
			}
			
			/**
			 * Check special group
			 */
			GroupNG dar_group = turret.getAllGroups().values().stream().filter(group -> {
				return group.getName().equals("Daily activity report authors");
			}).findFirst().orElseGet(() -> {
				try {
					return turret.createGroup("Daily activity report authors");
				} catch (ConnectionException e) {
					Loggers.DAReport.error("Can't connect to Cassandra", e);
				}
				return null;
			});
			
			if (dar_group == null) {
				throw new Exception("Can't get new Group");
			}
			
			if (dar_group.getGroupRoles().stream().noneMatch(role -> {
				return role.equals(dar_role);
			})) {
				GroupChRole gcr = new GroupChRole();
				gcr.group_key = dar_group.getKey();
				gcr.group_roles = new ArrayList<>(Arrays.asList(dar_role.getKey()));
				turret.changeGroupRoles(gcr);
			}
			
			/**
			 * Add user to this special group
			 */
			UserAdminUpdate uau = new UserAdminUpdate();
			uau.user_key = user.getKey();
			uau.user_groups = new ArrayList<>(user.getUserGroups().stream().map(group -> {
				return group.getKey();
			}).collect(Collectors.toList()));
			uau.user_groups.add(dar_group.getKey());
			
			turret.changeAdminUserPasswordGroups(uau);// TODO changeAdminUserPasswordGroups don't works ?!
		}
		
		Loggers.DAReport.info("Declare User Job: " + user.getFullname() + " will be a " + DARDB.get().getJobs().get(jobkey).name);
		
		account.save();
	}
	
}
