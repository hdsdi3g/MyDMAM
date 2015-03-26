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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import play.data.validation.Required;
import play.db.jpa.GenericModel;

@Entity
public class ACLGroup extends GenericModel {
	
	public static final String ADMIN_NAME = "administrators";
	public static final String NEWUSERS_NAME = "new_users";
	
	@Id
	@Required
	public String name;
	
	@OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
	public List<ACLUser> users;
	
	@ManyToOne()
	@Required
	public ACLRole role;
	
	// TODO add Security Policy @Entity, with SubnetUtils
	
	public ACLGroup(ACLRole role, String name) {
		this.role = role;
		this.name = name;
	}
	
	public ACLGroup addACLUser(String sourcename, String login, String fullname) {
		ACLUser newuser = new ACLUser(this, sourcename, login, fullname).save();
		this.users.add(newuser);
		this.save();
		return this;
	}
	
}
