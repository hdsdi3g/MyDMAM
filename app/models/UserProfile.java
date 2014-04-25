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
import hd3gtv.mydmam.mail.EndUserBaseMail;

import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import javax.mail.internet.InternetAddress;

import play.Play;
import play.data.validation.Email;
import play.data.validation.Required;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Router;

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
		InternetAddress email_addr = new InternetAddress(email);
		EndUserBaseMail mail;
		HashMap<Object, Object> variables = new HashMap<Object, Object>();
		// TODO EN messages
		try {
			if (Play.initialized) {
				mail = EndUserBaseMail.create(Lang.getLocale(), email_addr, "usertestmail");
				Properties messages = Messages.all(play.i18n.Lang.get());
				
				String real_message = messages.getProperty("crud.field.userprofile.sendTestMail.by", "");
				variables.put("me_has_sent_this_message", String.format(real_message, longname));
				variables.put("sitename", messages.getProperty("site.name", "[MyDMAM]"));
				variables.put("sitefooter", messages.getProperty("site.name", "[MyDMAM]") + " :: " + Router.getFullUrl("Application.index"));
			} else {
				throw new Exception();
			}
		} catch (Exception e) {
			/**
			 * Outside Play scope
			 */
			mail = EndUserBaseMail.create(Locale.getDefault(), email_addr, "usertestmail");
			variables.put("sitename", "[MyDMAM]");
			variables.put("sitefooter", "");
		}
		mail.send(variables);
	}
	
	protected String getCF_Name() {
		return "userprofiles";
	}
	
	/**
	 * If user don't login, delete its configuration.
	 * @return 2 years.
	 */
	protected int getTTL() {
		return 3600 * 24 * 365 * 2;
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
