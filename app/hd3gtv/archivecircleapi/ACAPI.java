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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.archivecircleapi;

public class ACAPI {
	
	private String host;
	private String user;
	private String password;
	private int tcp_port = 8081;
	
	public ACAPI(String host, String user, String password) {
		this.host = host;
		if (host == null) {
			throw new NullPointerException("\"host\" can't to be null");
		}
		this.user = user;
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		this.password = password;
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
	}
	
	public void setTcp_port(int tcp_port) throws IndexOutOfBoundsException {
		if (tcp_port < 1 | tcp_port > 65535) {
			throw new IndexOutOfBoundsException("Invalid TCP port: " + tcp_port);
		}
		this.tcp_port = tcp_port;
	}
}
