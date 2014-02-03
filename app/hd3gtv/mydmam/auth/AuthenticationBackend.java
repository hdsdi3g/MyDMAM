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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/

package hd3gtv.mydmam.auth;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

public class AuthenticationBackend {
	
	static {
		try {
			refreshConfiguration();
		} catch (Exception e) {
			Log2.log.error("Error with loading authentication configuration", e);
		}
	}
	
	private static List<AuthenticationConfiguration> configurations;
	
	public static void refreshConfiguration() throws Exception {
		if (Configuration.global.isElementExists("auth") == false) {
			throw new NullPointerException("Can't found \"auth\" element in configuration");
		}
		if (Configuration.global.isElementKeyExists("auth", "backend") == false) {
			throw new NullPointerException("Can't found \"auth/backend\" element in configuration");
		}
		
		List<LinkedHashMap<String, ?>> elements = Configuration.global.getListMapValues("auth", "backend");
		
		if (elements == null) {
			throw new NullPointerException("No items for \"auth/backend\" element in configuration");
		}
		
		configurations = new ArrayList<AuthenticationConfiguration>(elements.size());
		
		LinkedHashMap<String, ?> configuration_element;
		for (int pos = 0; pos < elements.size(); pos++) {
			configuration_element = elements.get(pos);
			String element_source = (String) configuration_element.get("source");
			if (element_source.equals("local")) {
				String path = (String) configuration_element.get("path");
				configurations.add(new LocalAuthentication(new File(path)));
			} else if (element_source.equals("ad")) {
				String domain = (String) configuration_element.get("domain");
				String server = (String) configuration_element.get("server");
				int port = (Integer) configuration_element.get("port");
				configurations.add(new ActivedirectoryAuthentication(domain, server, port));
			} else {
				Log2.log.error("Can't import \"auth/backend\" " + (pos + 1) + " configuration item", null, new Log2Dump("item", configuration_element.toString()));
			}
		}
		
		if (configurations.isEmpty()) {
			throw new NullPointerException("No authentication backend is correctly set");
		}
	}
	
	public static void authenticate(String username, String password) throws IOException {
		Log2Dump dump = new Log2Dump();
		dump.add("username", username);
		
		AuthenticationConfiguration authconf;
		for (int pos = 0; pos < configurations.size(); pos++) {
			authconf = configurations.get(pos);
			
			/**
			 * TODO if AD
			 */
			String server = ""; // TODO get server from configuration
			String domain = ""; // TODO get domain from configuration
			try {
				User user = ActivedirectoryUser.getUser(username, password, domain, server);
				// TODO ...
				Log2.log.security("Valid user", user);
			} catch (CommunicationException e) {
				throw new IOException("Can't contact LDAP AD server", e);
			} catch (NamingException e) {
				Log2.log.security("Unknow user", e, dump);
			}
		}
		// TODO return null
	}
	
}
