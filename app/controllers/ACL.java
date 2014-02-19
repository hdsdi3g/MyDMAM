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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/

package controllers;

import hd3gtv.mydmam.web.Privileges;

import java.util.List;

import models.ACLGroup;
import models.ACLRole;
import models.ACLUser;
import play.data.validation.Required;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class ACL extends Controller {
	
	@Check("showAcls")
	public static void showgroups() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.groups"));
		List<ACLGroup> groups = ACLGroup.findAll();
		render(title, groups);
	}
	
	@Check("editAcls")
	public static void addgroup() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.groups"));
		String name = null;
		String role = null;
		List<ACLRole> roles = ACLRole.findAll();
		
		render("ACL/formgroup.html", title, name, role, roles);
	}
	
	@Check("editAcls")
	public static void editgroup(String name) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.groups"));
		
		String role = null;
		if (name != null) {
			ACLGroup group = ACLGroup.findById(name);
			if (group != null) {
				role = group.role.name;
			}
		}
		List<ACLRole> roles = ACLRole.findAll();
		render("ACL/formgroup.html", title, name, role, roles);
	}
	
	@Check("editAcls")
	public static void updategroup(@Required String name, @Required String role) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		
		if (validation.hasErrors()) {
			List<ACLRole> roles = ACLRole.findAll();
			name = null;
			role = null;
			render("ACL/formgroup.html", title, name, role, roles);
			return;
		}
		
		ACLRole realrole = ACLRole.findById(role);
		if (realrole == null) {
			List<ACLRole> roles = ACLRole.findAll();
			name = null;
			role = null;
			render("ACL/formgroup.html", title, name, role, roles);
			return;
		}
		
		ACLGroup group = ACLGroup.findById(name);
		if (group != null) {
			group.role = realrole;
			group.save();
		} else {
			group = new ACLGroup(realrole, name);
			group.save();
		}
		
		redirect("ACL.showgroups");
	}
	
	@Check("editAcls")
	public static void deletegroup(@Required String name) {
		if (validation.hasErrors()) {
			redirect("ACL.showgroups");
			return;
		}
		
		ACLGroup group = ACLGroup.findById(name);
		if (group != null) {
			group.delete();
		}
		
		redirect("ACL.showgroups");
	}
	
	@Check("showAcls")
	public static void showusers() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.users"));
		List<ACLUser> users = ACLUser.findAll();
		render(title, users);
	}
	
	@Check("editAcls")
	public static void adduser() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.users"));
		
		String login = null;
		String group = null;
		List<ACLGroup> groups = ACLGroup.findAll();
		
		render("ACL/formuser.html", title, login, group, groups);
	}
	
	@Check("editAcls")
	public static void edituser(String login) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.groups"));
		
		String group = null;
		if (login != null) {
			ACLUser user = ACLUser.findById(login);
			if (user != null) {
				group = user.group.name;
			}
		}
		List<ACLGroup> groups = ACLGroup.findAll();
		render("ACL/formuser.html", title, login, group, groups);
	}
	
	@Check("editAcls")
	public static void updateuser(@Required String login, @Required String group) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		
		if (validation.hasErrors()) {
			List<ACLGroup> groups = ACLGroup.findAll();
			login = null;
			group = null;
			render("ACL/formuser.html", title, login, group, groups);
			return;
		}
		
		ACLGroup realgroup = ACLGroup.findById(group);
		if (realgroup == null) {
			List<ACLGroup> groups = ACLGroup.findAll();
			login = null;
			group = null;
			render("ACL/formuser.html", title, login, group, groups);
			return;
		}
		
		ACLUser user = ACLUser.findById(login);
		if (user != null) {
			user.group = realgroup;
			user.save();
		} else {
			realgroup.addACLUser("Manualy add", login, login);
		}
		
		redirect("ACL.showusers");
	}
	
	@Check("editAcls")
	public static void deleteuser(@Required String login) {
		if (validation.hasErrors()) {
			redirect("ACL.showusers");
			return;
		}
		
		ACLUser user = ACLUser.findById(login);
		if (user != null) {
			user.delete();
		}
		
		redirect("ACL.showusers");
	}
	
	@Check("showAcls")
	public static void showroles() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.roles"));
		List<ACLRole> roles = ACLRole.findAll();
		render(title, roles);
	}
	
	@Check("editAcls")
	public static void addrole() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.roles"));
		
		String name = null;
		List<ACLGroup> selectedgroups = null;
		List<ACLGroup> groups = ACLGroup.findAll();
		List<String> selectedprivileges = null;
		List<String> privileges = Privileges.getPrivileges();
		
		render("ACL/formrole.html", title, name, selectedgroups, groups, selectedprivileges, privileges);
	}
	
	@Check("editAcls")
	public static void editrole(String name) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.roles"));
		
		List<ACLGroup> selectedgroups = null;
		List<String> selectedprivileges = null;
		
		if (name != null) {
			ACLRole role = ACLRole.findById(name);
			if (role != null) {
				selectedgroups = role.groups;
				selectedprivileges = role.getPrivileges();
			}
		}
		List<ACLGroup> groups = ACLGroup.findAll();
		List<String> privileges = Privileges.getPrivileges();
		
		render("ACL/formrole.html", title, name, selectedgroups, groups, selectedprivileges, privileges);
		
	}
	
	@Check("editAcls")
	public static void deleterole(@Required String name) {
		if (validation.hasErrors()) {
			redirect("ACL.showroles");
			return;
		}
		
		ACLRole role = ACLRole.findById(name);
		if (role == null) {
			redirect("ACL.showroles");
			return;
		}
		if (role.groups != null) {
			if (role.groups.size() > 0) {
				redirect("ACL.showroles");
				return;
			}
		}
		role.delete();
		redirect("ACL.showroles");
	}
	
	@Check("editAcls")
	public static void updaterole(@Required String name) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		
		if (validation.hasErrors()) {
			List<ACLGroup> selectedgroups = null;
			List<ACLGroup> groups = ACLGroup.findAll();
			List<String> selectedprivileges = null;
			List<String> privileges = Privileges.getPrivileges();
			render("ACL/formrole.html", title, name, selectedgroups, groups, selectedprivileges, privileges);
			return;
		}
		
		String[] selectedprivilegenames = params.getAll("selectedprivileges");
		
		ACLRole role = ACLRole.findById(name);
		if (role == null) {
			role = new ACLRole(name);
			role = role.save();
		}
		
		role.privileges = null;
		if (selectedprivilegenames != null) {
			if (selectedprivilegenames.length > 0) {
				role.privileges = Privileges.getJSONPrivileges(selectedprivilegenames).toJSONString();
			}
		}
		
		role.save();
		
		redirect("ACL.showroles");
	}
}
