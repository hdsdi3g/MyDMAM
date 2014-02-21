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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import play.data.validation.Required;
import play.db.jpa.GenericModel;

@Entity
public class ACLUser extends GenericModel {
	
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
	
	public ACLUser(ACLGroup group, String sourcename, String login, String fullname) {
		this.group = group;
		this.sourcename = sourcename;
		this.login = login;
		this.fullname = fullname;
	}
	
	// TODO create date
	// TODO last login date
	// TODO last login source IP
	// TODO last edit date
	
}
