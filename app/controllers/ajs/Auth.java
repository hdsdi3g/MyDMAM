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
package controllers.ajs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.Check;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.AuthEntry;
import hd3gtv.mydmam.auth.GroupNG;
import hd3gtv.mydmam.auth.RoleNG;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.auth.asyncjs.BasketRename;
import hd3gtv.mydmam.auth.asyncjs.BasketUpdate;
import hd3gtv.mydmam.auth.asyncjs.GroupChRole;
import hd3gtv.mydmam.auth.asyncjs.GroupView;
import hd3gtv.mydmam.auth.asyncjs.GroupViewList;
import hd3gtv.mydmam.auth.asyncjs.NewUser;
import hd3gtv.mydmam.auth.asyncjs.RoleChPrivileges;
import hd3gtv.mydmam.auth.asyncjs.RoleView;
import hd3gtv.mydmam.auth.asyncjs.RoleViewList;
import hd3gtv.mydmam.auth.asyncjs.UserAdminUpdate;
import hd3gtv.mydmam.auth.asyncjs.UserSearchResult;
import hd3gtv.mydmam.auth.asyncjs.UserView;
import hd3gtv.mydmam.auth.asyncjs.UserViewList;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PrivilegeNG;

public class Auth extends AJSController {
	
	@Check("authAdmin")
	public static UserView userCreate(NewUser newuser) throws Exception {
		newuser.login = newuser.login.toLowerCase().trim();
		newuser.domain = newuser.domain.toLowerCase().trim();
		return MyDMAM.getPlayBootstrapper().getAuth().createUserIfNotExists(newuser).export(true, true);
	}
	
	@Check("authAdmin")
	public static UserView userGet(String key) throws Exception {
		UserNG user = MyDMAM.getPlayBootstrapper().getAuth().getByUserKey(key);
		if (user == null) {
			return null;
		}
		return user.export(true, true);
	}
	
	@Check("authAdmin")
	public static UserViewList userList() throws Exception {
		UserViewList result = new UserViewList();
		result.users = new LinkedHashMap<String, UserView>();
		
		sortList(MyDMAM.getPlayBootstrapper().getAuth().getAllUsers().values()).forEach(v -> {
			result.users.put(v.getKey(), ((UserNG) v).export(false, true));
		});
		
		return result;
	}
	
	@Check("authAdmin")
	public static JsonArray domainList() throws Exception {
		return MyDMAM.gson_kit.getGsonSimple().toJsonTree(MyDMAM.getPlayBootstrapper().getAuth().getDeclaredDomainList()).getAsJsonArray();
	}
	
	@Check("authAdmin")
	public static UserViewList userDelete(String key) throws Exception {
		UserNG user = MyDMAM.getPlayBootstrapper().getAuth().getByUserKey(key);
		if (user != null) {
			MyDMAM.getPlayBootstrapper().getAuth().deleteAll(Arrays.asList(user));
		}
		return userList();
	}
	
	@Check("authAdmin")
	public static UserView userToogleLock(String key) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().changeUserToogleLock(key).export(false, true);
	}
	
	@Check("authAdmin")
	public static UserView userAdminUpdate(UserAdminUpdate ch) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().changeAdminUserPasswordGroups(ch).export(false, true);
	}
	
	@Check("authAdmin")
	public static GroupView groupCreate(String new_group_name) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().createGroup(new_group_name).export();
	}
	
	private static ArrayList<AuthEntry> sortList(Collection<?> source) {
		if (source.isEmpty()) {
			return new ArrayList<>(1);
		}
		ArrayList<AuthEntry> result = new ArrayList<AuthEntry>(source.size());
		
		source.forEach(v -> {
			result.add((AuthEntry) v);
		});
		
		Collections.sort(result);
		
		return result;
	}
	
	@Check("authAdmin")
	public static GroupViewList groupList() throws Exception {
		GroupViewList result = new GroupViewList();
		result.groups = new LinkedHashMap<String, GroupView>();
		
		sortList(MyDMAM.getPlayBootstrapper().getAuth().getAllGroups().values()).forEach(v -> {
			result.groups.put(v.getKey(), ((GroupNG) v).export());
		});
		
		return result;
	}
	
	@Check("authAdmin")
	public static GroupViewList groupDelete(String key) throws Exception {
		GroupNG group = MyDMAM.getPlayBootstrapper().getAuth().getByGroupKey(key);
		if (group != null) {
			MyDMAM.getPlayBootstrapper().getAuth().deleteAll(Arrays.asList(group));
		}
		return groupList();
	}
	
	@Check("authAdmin")
	public static GroupView groupChangeRoles(GroupChRole ch_group) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().changeGroupRoles(ch_group).export();
	}
	
	@Check("authAdmin")
	public static RoleView roleCreate(String new_role_name) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().createRole(new_role_name).export();
	}
	
	@Check("authAdmin")
	public static RoleViewList roleList() throws Exception {
		RoleViewList result = new RoleViewList();
		result.roles = new LinkedHashMap<String, RoleView>();
		
		sortList(MyDMAM.getPlayBootstrapper().getAuth().getAllRoles().values()).forEach(v -> {
			result.roles.put(v.getKey(), ((RoleNG) v).export());
		});
		
		return result;
	}
	
	@Check("authAdmin")
	public static RoleViewList roleDelete(String key) throws Exception {
		RoleNG role = MyDMAM.getPlayBootstrapper().getAuth().getByRoleKey(key);
		if (role != null) {
			MyDMAM.getPlayBootstrapper().getAuth().deleteAll(Arrays.asList(role));
		}
		return roleList();
	}
	
	@Check("authAdmin")
	public static RoleView roleChangePrivilege(RoleChPrivileges new_privileges) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().changeRolePrivileges(new_privileges).export();
	}
	
	@Check("authAdmin")
	public static JsonObject getAllPrivilegesList() throws Exception {
		return PrivilegeNG.dumpAllPrivileges();
	}
	
	public static JsonObject getPreferences() throws Exception {
		return AJSController.getUserProfile().getPreferences();
	}
	
	public static void setPreferences(JsonObject new_preferences) throws Exception {
		UserNG user = AJSController.getUserProfile();
		if (Loggers.Auth.isDebugEnabled()) {
			Loggers.Auth.debug("Push preferences for " + user.getFullname());
		}
		user.setPreferences(new_preferences);
	}
	
	/**
	 * @return null or error message...
	 */
	public static String changePassword(String new_clear_text_passwd) throws Exception {
		UserAdminUpdate upd = new UserAdminUpdate();
		upd.user_key = AJSController.getUserProfile().getKey();
		upd.new_password = new_clear_text_passwd;
		try {
			MyDMAM.getPlayBootstrapper().getAuth().changeAdminUserPasswordGroups(upd);
		} catch (SecurityException se) {
			Loggers.Auth.warn("User " + AJSController.getUserProfileLongName() + " try to change its password", se);
			return se.getMessage();
		}
		return null;
	}
	
	public static void sendTestMail() throws Exception {
		AJSController.getUserProfile().sendTestMail();
	}
	
	/**
	 * @return new filtered mail addr
	 */
	public static String changeUserMail(String new_mail_addr) throws Exception {
		return MyDMAM.getPlayBootstrapper().getAuth().changeUserMail(AJSController.getUserProfile().getKey(), new_mail_addr).getEmailAddr();
	}
	
	public static JsonObject getActivities() throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getActivities(), GsonKit.type_ArrayList_UserActivity).getAsJsonObject();
	}
	
	public static JsonObject basketsList() throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), GsonKit.type_LinkedHashMap_StringBasketNG).getAsJsonObject();
	}
	
	public static JsonObject basketPush(BasketUpdate update) throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), GsonKit.type_LinkedHashMap_StringBasketNG).getAsJsonObject();
	}
	
	public static JsonObject basketDelete(String basket_key) throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), GsonKit.type_LinkedHashMap_StringBasketNG).getAsJsonObject();
	}
	
	public static JsonObject basketRename(BasketRename rename) throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), GsonKit.type_LinkedHashMap_StringBasketNG).getAsJsonObject();
	}
	
	public static JsonArray notificationsList() throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getNotifications(), GsonKit.type_ArrayList_UserNotificationNG).getAsJsonArray();
	}
	
	public static JsonArray notificationCheck(String notification_key) throws Exception {
		return MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getUserProfile().getNotifications(), GsonKit.type_ArrayList_UserNotificationNG).getAsJsonArray();
	}
	
	@Check("adminDAReport")
	public static UserSearchResult searchuser(String q) throws Exception {
		UserSearchResult usr = new UserSearchResult();
		usr.search(q, MyDMAM.getPlayBootstrapper().getAuth());
		return usr;
	}
	
	@Check("adminDAReport")
	public static UserSearchResult resolveuserkeylist(JsonArray user_key_list) throws Exception {
		UserSearchResult usr = new UserSearchResult();
		usr.resolve(user_key_list, AJSController.getUserProfile().getDomain(), MyDMAM.getPlayBootstrapper().getAuth());
		return usr;
	}
}
