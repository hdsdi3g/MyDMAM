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

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.Password;
import hd3gtv.tools.CopyMove;

public class FTPUser implements User {
	
	private transient static Password password;
	
	private String user_id;
	private String user_name;
	private byte[] obscured_password;
	private String group_name;
	private String domain;
	private boolean enabled;
	private File home_directory;
	
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
	
	public static FTPUser create(String user_name, String clear_password, String group_name, String domain, File home_directory) throws IOException {
		FTPUser user = new FTPUser();
		
		user.user_name = user_name;
		if (user_name == null) {
			throw new NullPointerException("\"user_name\" can't to be null");
		}
		if (user_name.isEmpty()) {
			throw new NullPointerException("\"user_name\" can't to be empty");
		}
		
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
		user.user_id = domain + "#" + user_name;
		
		user.home_directory = home_directory;
		if (home_directory == null) {
			throw new NullPointerException("\"home_directory\" can't to be null");
		}
		
		CopyMove.checkExistsCanRead(home_directory);
		CopyMove.checkIsDirectory(home_directory);
		CopyMove.checkIsWritable(home_directory);
		
		return user;
	}
	
	public boolean validPassword(UsernamePasswordAuthentication auth) {
		return password.checkPassword(auth.getPassword(), this.obscured_password);
	}
	
	public static FTPUser getUserByName(String user_name, String domain) {
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
		return home_directory.getAbsolutePath();
	}
	
	String getUserId() {
		return user_id;
	}
	
}
