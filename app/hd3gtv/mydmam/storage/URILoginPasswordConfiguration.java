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
package hd3gtv.mydmam.storage;

import java.util.ArrayList;
import java.util.LinkedHashMap;

final class URILoginPasswordConfiguration {
	
	String type;
	String login;
	String password;
	String host;
	int port;
	String relative_path;
	
	URILoginPasswordConfiguration(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) {
		int pos_colon = configuration_path.indexOf(":");
		type = configuration_path.substring(0, pos_colon);
		
		String raw_path = configuration_path.substring(pos_colon + 1);
		if (raw_path.startsWith("//")) {
			raw_path = raw_path.substring(2);
		} else if (raw_path.startsWith("/")) {
			raw_path = raw_path.substring(1);
		}
		
		int pos_slash = raw_path.indexOf("/");
		relative_path = raw_path.substring(pos_slash);
		
		raw_path = raw_path.substring(0, pos_slash);
		pos_colon = raw_path.indexOf(":");
		
		int pos_at = raw_path.lastIndexOf("@");
		if ((pos_colon > -1) & (pos_at > pos_colon)) {
			login = raw_path.substring(0, pos_colon);
			raw_path = raw_path.substring(pos_colon + 1);
			pos_colon = raw_path.lastIndexOf(":");
			pos_at = raw_path.lastIndexOf("@");
		}
		
		port = 0;
		if (pos_at < pos_colon) {
			port = Integer.parseInt(raw_path.substring(pos_colon + 1));
			raw_path = raw_path.substring(0, pos_colon);
		}
		
		host = raw_path.substring(pos_at + 1);
		
		if (pos_at > -1) {
			if (login != null) {
				password = raw_path.substring(0, pos_at);
			} else {
				login = raw_path.substring(0, pos_at);
			}
		}
		
		if (optional_storage_def.containsKey("password")) {
			password = (String) optional_storage_def.get("password");
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(type);
		sb.append("://");
		if (login != null) {
			sb.append(login);
			if (password != null) {
				sb.append(":");
				sb.append(password);
			}
			sb.append("@");
		}
		sb.append(host);
		if (port > 0) {
			sb.append(":");
			sb.append(port);
		}
		sb.append(relative_path);
		return sb.toString();
	}
	
	static void doTests() {
		ArrayList<String> raw_path_example = new ArrayList<String>();
		raw_path_example.add("protocol://user:AStrong:P@ssword@123.10.09.1:1234/fsdfsdfsdf/ÊÊÊÊ/dfs:dfdfdf");
		raw_path_example.add("protocol://123.10.09.1/A Path");
		raw_path_example.add("protocol://user@123.10.09.1/A Path");
		raw_path_example.add("protocol://user:password@123.10.09.1/A Path");
		raw_path_example.add("protocol://123.10.09.1:1234/A Path");
		raw_path_example.add("protocol://user@123.10.09.1:1234/A Path");
		raw_path_example.add("protocol:/user@123.10.09.1:1234/A Path");
		raw_path_example.add("protocol://user@123.10.09.1:1234/A Path/");
		raw_path_example.add("protocol://user@123.10.09.1:1234/A Path/with @/");
		raw_path_example.add("protocol://123.10.09.1:1234/A Path/with @");
		raw_path_example.add("protocol://user:password@123.10.09.1:1234/A Path/with @/");
		
		String from;
		String to;
		for (int pos = 0; pos < raw_path_example.size(); pos++) {
			from = raw_path_example.get(pos);
			try {
				URILoginPasswordConfiguration ulpc = new URILoginPasswordConfiguration(from, new LinkedHashMap<String, String>());
				to = ulpc.toString();
				if (from.equals(to) == false) {
					System.out.println(from);
					System.out.println(to);
					System.out.println();
				}
			} catch (Exception e) {
				System.out.println(from);
				e.printStackTrace();
			}
		}
	}
	
}
