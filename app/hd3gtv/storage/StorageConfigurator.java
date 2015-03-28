/*
 * This file is part of hd3g.tv' Java Storage Abstraction
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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.storage;

@Deprecated
class StorageConfigurator {
	
	StorageType storagetype;
	String name;
	String username;
	String host;
	int port = 0;
	String password;
	String path;
	boolean passive = true;
	boolean readonly = false;
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(storagetype.toString().toLowerCase());
		if ((storagetype == StorageType.ftp) | (storagetype == StorageType.ftpnexio)) {
			if (passive) {
				sb.append("_pasv");
			}
		}
		sb.append("://");
		
		if (username != null) {
			sb.append(username);
			if (password != null) {
				sb.append(":");
				sb.append(password);
			}
			sb.append("@");
		}
		
		if (host != null) {
			sb.append(host);
		}
		
		if ((storagetype == StorageType.ftp) | (storagetype == StorageType.ftpnexio)) {
			if (port > 0) {
				sb.append(":");
				sb.append(port);
			}
		}
		sb.append(path);
		
		sb.append(" ");
		sb.append(name);
		
		if (readonly) {
			sb.append(" [READ ONLY]");
		}
		
		return sb.toString();
	}
}
