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

package hd3gtv.mydmam.web;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.db.orm.annotations.TypeEmail;

public class UserProfile extends CrudOrmModel {
	
	@TypeEmail
	public String email;
	
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
	
}
