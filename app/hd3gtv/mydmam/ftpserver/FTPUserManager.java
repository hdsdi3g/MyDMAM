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

import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;

public class FTPUserManager implements UserManager {
	
	private String domain;
	
	/**
	 * @param domain can be empty
	 */
	public FTPUserManager(String domain) {
		this.domain = domain;
		if (domain == null) {
			throw new NullPointerException("\"domain\" can't to be null");
		}
	}
	
	public User getUserByName(String username) throws FtpException {
		try {
			return FTPUser.getUserByName(username, domain);
		} catch (ConnectionException e) {
			throw new FtpException("Can't access to db", e);
		}
	}
	
	public User authenticate(Authentication authentication) throws AuthenticationFailedException {
		if ((authentication instanceof UsernamePasswordAuthentication) == false) {
			throw new AuthenticationFailedException("Can't manage " + authentication.getClass().getSimpleName() + " auth class.");
		}
		UsernamePasswordAuthentication auth = (UsernamePasswordAuthentication) authentication;
		
		try {
			FTPUser user = FTPUser.getUserByName(auth.getUsername(), domain);
			if (user == null) {
				return null;
			}
			if (user.validPassword(auth)) {
				return user.updateLastLogin();
			}
		} catch (ConnectionException e) {
			Loggers.FTPserver.error("Can't access to db", e);
		}
		return null;
	}
	
	public String getAdminName() throws FtpException {
		throw new FtpException("No admin here");
	}
	
	public String[] getAllUserNames() throws FtpException {
		// return (String[]) users.keySet().toArray();
		throw new FtpException("Not implemented");
	}
	
	public void delete(String username) throws FtpException {
		// users.remove(username);
		throw new FtpException("Not implemented");
	}
	
	public void save(User user) throws FtpException {
		// users.put(user.getName(), user);
		throw new FtpException("Not implemented");
	}
	
	public boolean doesExist(String username) throws FtpException {
		// return users.containsKey(username);
		throw new FtpException("Not implemented");
	}
	
	public boolean isAdmin(String username) throws FtpException {
		return false;
	}
	
}
