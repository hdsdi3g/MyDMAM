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

import java.util.List;

import models.ACLGroup;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class ACL extends Controller {
	
	// new ACLGroup("groupe2").save();
	// ACLGroup group = ACLGroup.findById(0l);
	// new ACLUser(group, "TestStupide1", "moi", "Moi !").save();
	// new ACLUser(group, "TestStupide2", "remoi", "Re Moi !").save();
	// ====User bob = User.find("byEmail", "bob@gmail.com").first();
	// new ACLUser(group, "TestStupide3", "remoi3", "Re Moi 3 !").save();
	// System.out.println(ACLUser.count());
	
	public static void showgroups() {
		/*ACLGroup group = ACLGroup.findById(0l);
		ACLUser aclu = new ACLUser(group, "TestStupide2", "moi2", "Moi2 !");
		aclu.save();
		group.users.add(aclu);
		group.save();*/
		
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.groups"));
		List<ACLGroup> groups = ACLGroup.all().fetch();
		render(title, groups);
	}
	
	public static void showusers() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.users"));
		render(title);
	}
	
	public static void showroles() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.roles"));
		render(title);
	}
	
	public static void showprivileges() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("acl.pagename.privileges"));
		render(title);
	}
}
