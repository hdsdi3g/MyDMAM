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

import java.util.Arrays;
import java.util.LinkedHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import controllers.Check;
import ext.Bootstrap;
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
import hd3gtv.mydmam.auth.asyncjs.UserChGroup;
import hd3gtv.mydmam.auth.asyncjs.UserChPassword;
import hd3gtv.mydmam.auth.asyncjs.UserView;
import hd3gtv.mydmam.auth.asyncjs.UserViewList;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PrivilegeNG;

public class Auth extends AJSController {
	
	static {
		AJSController.registerTypeAdapter(UserView.class, new UserView.Serializer());
		AJSController.registerTypeAdapter(UserViewList.class, new UserViewList.Serializer());
		AJSController.registerTypeAdapter(GroupView.class, new GroupView.Serializer());
		AJSController.registerTypeAdapter(GroupViewList.class, new GroupViewList.Serializer());
		AJSController.registerTypeAdapter(RoleView.class, new RoleView.Serializer());
		AJSController.registerTypeAdapter(RoleViewList.class, new RoleViewList.Serializer());
		AJSController.registerTypeAdapter(NewUser.class, new NewUser.Deserializer());
		AJSController.registerTypeAdapter(UserChGroup.class, new UserChGroup.Deserializer());
		AJSController.registerTypeAdapter(GroupChRole.class, new GroupChRole.Deserializer());
		AJSController.registerTypeAdapter(RoleChPrivileges.class, new RoleChPrivileges.Deserializer());
	}
	
	@Check("authAdmin")
	public static UserView userCreate(NewUser newuser) throws Exception {
		newuser.login = newuser.login.toLowerCase().trim();
		newuser.domain = newuser.domain.toLowerCase().trim();
		return Bootstrap.getAuth().createUserIfNotExists(newuser).export(true, true);
	}
	
	@Check("authAdmin")
	public static UserView userGet(String key) throws Exception {
		UserNG user = Bootstrap.getAuth().getByUserKey(key);
		if (user == null) {
			return null;
		}
		return user.export(true, true);
	}
	
	@Check("authAdmin")
	public static UserViewList userList() throws Exception {
		UserViewList result = new UserViewList();
		result.users = new LinkedHashMap<String, UserView>();
		Bootstrap.getAuth().getAllUsers().forEach((k, v) -> {
			result.users.put(k, v.export(false, true));
		});
		return result;
	}
	
	@Check("authAdmin")
	public static UserViewList userDelete(String key) throws Exception {
		UserNG user = Bootstrap.getAuth().getByUserKey(key);
		if (user != null) {
			Bootstrap.getAuth().deleteAll(Arrays.asList(user));
		}
		return userList();
	}
	
	@Check("authAdmin")
	public static UserView userChangePassword(UserChPassword chpassword) throws Exception {
		return Bootstrap.getAuth().changeUserPassword(chpassword).export(true, true);
	}
	
	@Check("authAdmin")
	public static UserView userToogleLock(String key) throws Exception {
		return Bootstrap.getAuth().changeUserToogleLock(key).export(false, true);
	}
	
	@Check("authAdmin")
	public static UserView userChangeGroup(UserChGroup chgroup) throws Exception {
		return Bootstrap.getAuth().changeUserGroups(chgroup).export(true, true);
	}
	
	@Check("authAdmin")
	public static GroupView groupCreate(String new_group_name) throws Exception {
		return Bootstrap.getAuth().createGroup(new_group_name).export();
	}
	
	@Check("authAdmin")
	public static GroupViewList groupList() throws Exception {
		GroupViewList result = new GroupViewList();
		result.groups = new LinkedHashMap<String, GroupView>();
		Bootstrap.getAuth().getAllGroups().forEach((k, v) -> {
			result.groups.put(k, v.export());
		});
		return result;
	}
	
	@Check("authAdmin")
	public static GroupViewList groupDelete(String key) throws Exception {
		GroupNG group = Bootstrap.getAuth().getByGroupKey(key);
		if (group != null) {
			Bootstrap.getAuth().deleteAll(Arrays.asList(group));
		}
		return groupList();
	}
	
	@Check("authAdmin")
	public static GroupView groupChangeRoles(GroupChRole ch_group) throws Exception {
		return Bootstrap.getAuth().changeGroupRoles(ch_group).export();
	}
	
	@Check("authAdmin")
	public static RoleView roleCreate(String new_role_name) throws Exception {
		return Bootstrap.getAuth().createRole(new_role_name).export();
	}
	
	@Check("authAdmin")
	public static RoleViewList roleList() throws Exception {
		RoleViewList result = new RoleViewList();
		result.roles = new LinkedHashMap<String, RoleView>();
		Bootstrap.getAuth().getAllRoles().forEach((k, v) -> {
			result.roles.put(k, v.export());
		});
		return result;
	}
	
	@Check("authAdmin")
	public static RoleViewList roleDelete(String key) throws Exception {
		RoleNG role = Bootstrap.getAuth().getByRoleKey(key);
		if (role != null) {
			Bootstrap.getAuth().deleteAll(Arrays.asList(role));
		}
		return roleList();
	}
	
	@Check("authAdmin")
	public static RoleView roleChangePrivilege(RoleChPrivileges new_privileges) throws Exception {
		return Bootstrap.getAuth().changeRolePrivileges(new_privileges).export();
	}
	
	@Check("authAdmin")
	public static JsonObject getAllPrivilegesList() throws Exception {
		return PrivilegeNG.dumpAllPrivileges();
	}
	
	public static JsonObject getPreferencies() throws Exception {
		return AJSController.getUserProfile().getPreferencies();
	}
	
	public static UserView changePassword(String new_clear_text_passwd) throws Exception {
		UserChPassword chpassword = new UserChPassword();
		chpassword.user_key = AJSController.getUserProfile().getKey();
		chpassword.password = new_clear_text_passwd;
		return Bootstrap.getAuth().changeUserPassword(chpassword).export(true, false);
	}
	
	public static void sendTestMail() throws Exception {
		AJSController.getUserProfile().sendTestMail();
	}
	
	public static UserView changeUserMail(String new_mail_addr) throws Exception {
		return Bootstrap.getAuth().changeUserMail(AJSController.getUserProfile().getKey(), new_mail_addr).export(true, false);
	}
	
	public static JsonObject getActivities() throws Exception {
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getActivities(), UserNG.al_useractivity_typeOfT).getAsJsonObject();
	}
	
	public static JsonObject basketsList() throws Exception {
		// TODO basketsList
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), UserNG.linmap_string_basket_typeOfT).getAsJsonObject();
	}
	
	public static JsonObject basketPush(BasketUpdate update) throws Exception {
		// TODO basketPush
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), UserNG.linmap_string_basket_typeOfT).getAsJsonObject();
	}
	
	public static JsonObject basketDelete(String basket_key) throws Exception {
		// TODO basketDelete
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), UserNG.linmap_string_basket_typeOfT).getAsJsonObject();
	}
	
	public static JsonObject basketRename(BasketRename rename) throws Exception {
		// TODO basketRename
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getBaskets(), UserNG.linmap_string_basket_typeOfT).getAsJsonObject();
	}
	
	public static JsonArray notificationsList() throws Exception {
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getNotifications(), UserNG.al_usernotification_typeOfT).getAsJsonArray();
	}
	
	public static JsonArray notificationCheck(String notification_key) throws Exception {
		// TODO notificationCheck
		return Bootstrap.getAuth().getGson().toJsonTree(AJSController.getUserProfile().getNotifications(), UserNG.al_usernotification_typeOfT).getAsJsonArray();
	}
	
}
