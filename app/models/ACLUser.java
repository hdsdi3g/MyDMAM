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
*/package models;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import hd3gtv.mydmam.auth.DbAccountExtractor;
import hd3gtv.mydmam.auth.SelfExtractor;
import play.data.validation.Required;
import play.db.jpa.GenericModel;
import play.db.jpa.JPABase;

@Entity
public class ACLUser extends GenericModel implements SelfExtractor {
	
	public static final String ADMIN_NAME = "admin";
	
	@Id
	@Required
	public String login;
	
	@Required
	public String sourcename;
	
	@Required
	public String fullname;
	
	@Required
	@ManyToOne()
	public ACLGroup group;
	
	@Required
	public Date createdate;
	
	public Date lastlogindate;
	
	public String lastloginipsource;
	
	@Required
	public Date lasteditdate;
	
	/**
	 * User can't login.
	 */
	@Required
	public Boolean locked_account;
	
	public ACLUser(ACLGroup group, String sourcename, String login, String fullname) {
		this.group = group;
		this.sourcename = sourcename;
		this.login = login;
		this.fullname = fullname;
		createdate = new Date();
		lasteditdate = new Date();
		locked_account = false;
	}
	
	public <T extends JPABase> T save() {
		DbAccountExtractor.extractor.save();
		return super.save();
	}
	
	public Element exportToXML(Document document) {
		Element root = document.createElement("acluser");
		root.setAttribute("login", login);
		root.setAttribute("sourcename", sourcename);
		root.setAttribute("fullname", fullname);
		root.setAttribute("group", group.name);
		root.setAttribute("lastloginipsource", lastloginipsource);
		root.setAttribute("createdate", String.valueOf(createdate.getTime()));
		root.setAttribute("lastlogindate", String.valueOf(lastlogindate.getTime()));
		root.setAttribute("lasteditdate", String.valueOf(lasteditdate.getTime()));
		root.setAttribute("locked_account", String.valueOf(locked_account));
		return root;
	}
	
}
