/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/

package models;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.annotations.AuthorisedForAdminController;
import hd3gtv.mydmam.db.orm.annotations.PublishedMethod;
import hd3gtv.mydmam.db.orm.annotations.ReadOnly;
import hd3gtv.mydmam.db.orm.annotations.TypeEmail;
import play.data.validation.Email;
import play.data.validation.Required;

@AuthorisedForAdminController
public class UserProfile extends CrudOrmModel {
	
	@ReadOnly
	public String longname;
	
	@Required
	@Email
	@TypeEmail
	public String email;
	
	@PublishedMethod
	public void sendTestMail() throws Exception {
		System.out.println("ok");// XXX
	}
	
	protected String getCF_Name() {
		return "userprofiles";
	}
	
	protected Class<? extends CrudOrmModel> getClassInstance() {
		return UserProfile.class;
	}
	
	public static String prepareKey(String username) {
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null");
		}
		String user_scope_isolation = Configuration.global.getValue("auth", "user_scope_isolation", "noset");
		StringBuffer sb = new StringBuffer();
		sb.append(user_scope_isolation);
		sb.append("%");
		sb.append(username);
		return sb.toString();
	}
	
	/**
	 * Create it if don't exists.
	 */
	public static CrudOrmEngine<UserProfile> getORMEngine(String key) throws Exception {
		UserProfile userprofile = new UserProfile();
		CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(userprofile);
		if (engine.exists(key)) {
			userprofile = engine.read(key);
		} else {
			userprofile = engine.create();
			userprofile.key = key;
			engine.saveInternalElement();
		}
		return engine;
	}
	
}
