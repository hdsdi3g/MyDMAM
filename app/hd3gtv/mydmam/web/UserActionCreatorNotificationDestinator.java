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
package hd3gtv.mydmam.web;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.mail.notification.NotifyReason;

import java.io.IOException;

import models.UserProfile;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import ext.MydmamExtensions;

@Deprecated
public class UserActionCreatorNotificationDestinator implements Log2Dumpable {
	
	private CrudOrmEngine<UserProfile> user_profile_orm;
	
	UserActionCreatorNotificationDestinator() throws ConnectionException, IOException {
		user_profile_orm = new CrudOrmEngine<UserProfile>(new UserProfile());
	}
	
	String crypted_user_key;
	String reason;
	
	transient UserProfile userprofile;
	transient NotifyReason n_reason;
	
	void prepare() throws NullPointerException, ConnectionException {
		n_reason = NotifyReason.getFromString(reason);
		if (n_reason == null) {
			throw new NullPointerException("Invalid reason " + reason + ".");
		}
		if (userprofile != null) {
			return;
		}
		String user_key = MydmamExtensions.decrypt(crypted_user_key);
		userprofile = user_profile_orm.read(user_key);
		if (userprofile == null) {
			throw new NullPointerException("Can't found userprofile " + user_key + ".");
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("crypted_user_key", crypted_user_key);
		dump.add("user_key", MydmamExtensions.decrypt(crypted_user_key));
		dump.add("reason", reason);
		return dump;
	}
	
}
