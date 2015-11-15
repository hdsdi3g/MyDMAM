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

import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;

public class FTPUser implements User {
	
	public String getName() {
		return "test";
	}
	
	public String getPassword() {
		return "pws";
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
		return 600;
	}
	
	public boolean getEnabled() {
		return true;
	}
	
	public String getHomeDirectory() {
		return "/tmp";// TODO
	}
	
}
