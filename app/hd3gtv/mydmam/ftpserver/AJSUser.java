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

public class AJSUser {
	
	public String user_id;
	public String user_name;
	public String group_name;
	public String domain;
	public boolean enabled;
	public long create_date;
	public long update_date;
	public long last_login;
	
	static AJSUser fromFTPUser(FTPUser ftp_user) {
		AJSUser user = new AJSUser();
		user.user_id = ftp_user.getUserId();
		user.user_name = ftp_user.getName();
		user.group_name = ftp_user.getGroupName();
		user.domain = ftp_user.getDomain();
		user.enabled = ftp_user.getEnabled();
		user.create_date = ftp_user.getCreateDate();
		user.update_date = ftp_user.getUpdateDate();
		user.last_login = ftp_user.getLastLogin();
		return user;
	}
	
}
