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

import java.util.HashMap;

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

public class FTPUserManager implements UserManager {
	
	HashMap<String, User> users;
	
	public FTPUserManager() {
		users = new HashMap<String, User>(1);
		users.put("test", new FTPUser());
	}
	
	public User getUserByName(String username) throws FtpException {
		return users.get(username);
	}
	
	public String[] getAllUserNames() throws FtpException {
		return (String[]) users.keySet().toArray();
	}
	
	public void delete(String username) throws FtpException {
		users.remove(username);
	}
	
	public void save(User user) throws FtpException {
		users.put(user.getName(), user);
	}
	
	public boolean doesExist(String username) throws FtpException {
		return users.containsKey(username);
	}
	
	public User authenticate(Authentication authentication) throws AuthenticationFailedException {
		if ((authentication instanceof UsernamePasswordAuthentication) == false) {
			throw new AuthenticationFailedException("Can't manage " + authentication.getClass().getSimpleName() + " auth class.");
		}
		UsernamePasswordAuthentication auth = (UsernamePasswordAuthentication) authentication;
		// System.out.println("IP: " + auth.getUserMetadata().getInetAddress()); "/127.0.0.1"
		// System.out.println(auth.getUsername() + ":" + auth.getPassword());
		return users.get("test");
	}
	
	public String getAdminName() throws FtpException {
		throw new FtpException("No admin here");
	}
	
	public boolean isAdmin(String username) throws FtpException {
		return false;
	}
	
}
