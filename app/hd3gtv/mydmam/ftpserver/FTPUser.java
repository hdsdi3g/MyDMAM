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
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.Password;

public class FTPUser implements User {
	
	private transient static Password password;
	
	private String user_id;
	private String user_name;
	private byte[] obscured_password;
	private String group_name;
	private String domain;
	private boolean enabled;
	private long create_date;
	private long update_date;
	private long last_path_index_refreshed;
	
	/**
	 * populateGroup will init it.
	 */
	private transient FTPGroup group;
	
	static {
		try {
			password = new Password(Configuration.global.getValue("ftpserver", "master_password_key", ""));
		} catch (Exception e) {
			Loggers.FTPserver.fatal("Can't load password API for FTP Server, check configuration", e);
			System.exit(1);
		}
	}
	
	private FTPUser() {
	}
	
	/**
	 * @param domain can be empty, but not null.
	 */
	public static FTPUser create(String user_name, String clear_password, String group_name, String domain) throws IOException {
		FTPUser user = new FTPUser();
		
		user.user_name = user_name;
		if (user_name == null) {
			throw new NullPointerException("\"user_name\" can't to be null");
		}
		if (user_name.isEmpty()) {
			throw new NullPointerException("\"user_name\" can't to be empty");
		}
		// TODO check bad chars in user_name
		
		user.obscured_password = password.getHashedPassword(clear_password);
		if (clear_password == null) {
			throw new NullPointerException("\"clear_password\" can't to be null");
		}
		if (clear_password.isEmpty()) {
			throw new NullPointerException("\"clear_password\" can't to be empty");
		}
		
		user.domain = domain;
		if (domain == null) {
			throw new NullPointerException("\"domain\" can't to be null");
		}
		
		user.group_name = group_name;
		if (group_name == null) {
			throw new NullPointerException("\"group_name\" can't to be null");
		}
		if (group_name.isEmpty()) {
			throw new NullPointerException("\"group_name\" can't to be empty");
		}
		
		user.enabled = true;
		user.user_id = makeUserId(user_name, domain);
		
		user.create_date = System.currentTimeMillis();
		user.update_date = user.create_date;
		
		return user;
	}
	
	public boolean validPassword(UsernamePasswordAuthentication auth) {
		return password.checkPassword(auth.getPassword(), this.obscured_password);
	}
	
	private void populateGroup() {
		if (group == null) {
			group = FTPGroup.getFromName(group_name);
			if (group == null) {
				throw new NullPointerException("Can't found declared group \"" + group_name + "\".");
			}
		}
	}
	
	private static String makeUserId(String user_name, String domain) {
		return "ftpuser:" + domain + "#" + user_name;
	}
	
	public static FTPUser getUserByName(String user_name, String domain) {
		return getUserId(makeUserId(user_name, domain));
	}
	
	public static FTPUser getUserId(String user_id) {
		// TODO get User, check if user is disabled, populateGroup(); check if group is disabled , group.getUserHomeDirectory(user)
		return null; // TODO
	}
	
	public String getName() {
		return user_name;
	}
	
	/**
	 * @return null
	 */
	public String getPassword() {
		return null;
	}
	
	public List<Authority> getAuthorities() {
		return null;
	}
	
	public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
		return null;
	}
	
	public AuthorizationRequest authorize(AuthorizationRequest request) {
		// ConcurrentLoginRequest clr = (ConcurrentLoginRequest) request;
		// System.err.println(request.getClass());
		return request;
	}
	
	public int getMaxIdleTime() {
		return Configuration.global.getValue("ftpserver", "maxidletime", 300);
	}
	
	public boolean getEnabled() {
		return enabled;
	}
	
	public String getHomeDirectory() {
		populateGroup();
		try {
			return group.getUserHomeDirectory(this).getAbsolutePath();
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't get home directory for user " + user_id + " (" + user_name + ")", e);
		}
		return null;
	}
	
	String getUserId() {
		return user_id;
	}
	
	/**
	 * @return never null, maybe empty
	 */
	String getDomain() {
		return domain;
	}
	
	FTPGroup getGroup() {
		populateGroup();
		return group;
	}
	
	long getLastPathIndexRefreshed() {
		return last_path_index_refreshed;
	}
	
	void setLastPathIndexRefreshed() {
		last_path_index_refreshed = System.currentTimeMillis();
	}
	
}
