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

import com.google.gson.JsonObject;

import controllers.Check;
import hd3gtv.mydmam.auth.asyncjs.BasketList;
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
import hd3gtv.mydmam.auth.asyncjs.UserNotificationsList;
import hd3gtv.mydmam.auth.asyncjs.UserView;
import hd3gtv.mydmam.auth.asyncjs.UserViewList;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.PrivilegeNG;

public class Auth extends AJSController {
	
	@Check("authAdmin")
	public static UserView userCreate(NewUser user) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static UserView userGet(String key) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static UserViewList userList() throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static void userDelete(String key) throws Exception {
		// TODO
	}
	
	@Check("authAdmin")
	public static UserView userChangePassword(UserChPassword chpassword) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static UserView userToogleLock(String key) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static UserView userChangeGroup(UserChGroup chgroup) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static GroupView groupCreate(String new_group_name) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static GroupViewList groupList() throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static void groupDelete(String key) throws Exception {
	}
	
	@Check("authAdmin")
	public static GroupView groupChangeRoles(GroupChRole ch_group) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static RoleView roleCreate(String new_role_name) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static RoleViewList roleList() throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static void roleDelete(String keg) throws Exception {
		// TODO
	}
	
	@Check("authAdmin")
	public static RoleView roleChangePrivilege(RoleChPrivileges new_privileges) throws Exception {
		return null;// TODO
	}
	
	@Check("authAdmin")
	public static JsonObject getAllPrivilegesList() throws Exception {
		return PrivilegeNG.dumpAllPrivileges();
	}
	
	public static JsonObject getPreferencies() throws Exception {
		return new JsonObject();// TODO
	}
	
	public static UserView changePassword(UserChPassword new_passwd) throws Exception {
		return null;// TODO
	}
	
	public static UserView sendTestMail() throws Exception {
		return null;// TODO
	}
	
	public static UserView changeUserMail() throws Exception {
		return null;// TODO
	}
	
	public static JsonObject getActivities() throws Exception {
		return null;// TODO
	}
	
	public static BasketList basketsList() throws Exception {
		return null;// TODO
	}
	
	public static BasketList basketPush(BasketUpdate update) throws Exception {
		return null;// TODO
	}
	
	public static BasketList basketDelete(String basket_key) throws Exception {
		return null;// TODO
	}
	
	public static BasketList basketRename(BasketRename rename) throws Exception {
		return null;// TODO
	}
	
	public static UserNotificationsList notificationsList() throws Exception {
		return null;// TODO
	}
	
	public static UserNotificationsList notificationCheck(String notification_key) throws Exception {
		return null;// TODO
	}
}
