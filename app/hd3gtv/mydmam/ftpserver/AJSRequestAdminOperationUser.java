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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.ftpserver;

import java.io.IOException;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class AJSRequestAdminOperationUser {
	
	public String user_id;
	public String user_name;
	public String clear_password;
	public String group_name;
	public String domain;
	public Operation operation;
	
	public enum Operation {
		CREATE, DELETE, CH_PASSWORD, TOGGLE_ENABLE;
	}
	
	public FTPUser createFTPUser() throws IOException, ConnectionException {
		FTPUser user = FTPUser.create(user_name, clear_password, group_name, domain);
		return user.save();
	}
	
	public void delete() throws ConnectionException {
		FTPUser user = FTPUser.getUserId(user_id, false);
		if (user == null) {
			return;
		}
		user.removeUser();
	}
	
	public void chPassword() throws ConnectionException {
		FTPUser user = FTPUser.getUserId(user_id, false);
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		user.changePassword(clear_password);
		user.save();
	}
	
	public void toggleEnable() throws ConnectionException {
		FTPUser user = FTPUser.getUserId(user_id, false);
		if (user == null) {
			throw new NullPointerException("Can't found \"user\"");
		}
		user.setDisabled(user.getEnabled());
		user.save();
	}
	
}
