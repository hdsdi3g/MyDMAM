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

import models.ACLUser;

public class AuthenticationBackend {
	
	static {
		try {
			refreshConfiguration();
		} catch (Exception e) {
			Log2.log.error("Error with loading authentication configuration", e);
		}
	}
	
	private static List<Authenticator> authenticators;
	
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
		
		authenticators = new ArrayList<Authenticator>(elements.size());
		
		LinkedHashMap<String, ?> configuration_element;
		for (int pos = 0; pos < elements.size(); pos++) {
			configuration_element = elements.get(pos);
			String element_source = (String) configuration_element.get("source");
			if (element_source.equals("local")) {
				String path = (String) configuration_element.get("path");
				String masterkey = (String) configuration_element.get("masterkey");
				authenticators.add(new AuthenticatorLocalsqlite(new File(path), masterkey));
			} else if (element_source.equals("ad")) {
				String domain = (String) configuration_element.get("domain");
				String server = (String) configuration_element.get("server");
				int port = (Integer) configuration_element.get("port");
				authenticators.add(new AuthenticatorActivedirectory(domain, server, port));
			} else {
				Log2.log.error("Can't import \"auth/backend\" " + (pos + 1) + " configuration item", null, new Log2Dump("item", configuration_element.toString()));
			}
		}
		
		if (authenticators.isEmpty()) {
			throw new NullPointerException("No authentication backend is correctly set");
		}
	}
	
	/**
	 * Try to get User, authenticator after authenticator, until it found a correct user.
	 * @return null if user & password are invalid, unknow, lock...
	 */
	public static AuthenticationUser authenticate(String username, String password) {
		Authenticator authenticator;
		AuthenticationUser authenticationUser;
		Log2Dump dump;
		for (int pos = 0; pos < authenticators.size(); pos++) {
			authenticator = authenticators.get(pos);
			dump = new Log2Dump();
			dump.add("username", username);
			dump.addAll(authenticator);
			try {
				authenticationUser = authenticator.getUser(username, password);
				if (authenticationUser != null) {
					Log2.log.info("Valid user found for this authentication method", dump);
					return authenticationUser;
				}
			} catch (IOException e) {
				Log2.log.error("Invalid authentication method", e, dump);
			} catch (InvalidAuthenticatorUserException e) {
				dump.add("cause", e.getMessage());
				dump.add("from", e.getCause());
				Log2.log.debug("Invalid user for this authentication method", dump);
			}
		}
		return null;
	}
	
	public static void checkFirstPlayBoot() throws Exception {
		if (authenticators == null) {
			throw new NullPointerException("No backend");
		}
		if (authenticators.size() == 0) {
			throw new NullPointerException("No backend");
		}
		if ((authenticators.get(0) instanceof AuthenticatorLocalsqlite) == false) {
			/**
			 * Admin has set a configuration : no need to setup a first boot
			 */
			return;
		}
		AuthenticatorLocalsqlite authenticatorlocalsqlite = (AuthenticatorLocalsqlite) authenticators.get(0);
		if (authenticatorlocalsqlite.isUserExists(ACLUser.ADMIN_NAME) == false) {
			// TODO create admin user
		} else if (authenticatorlocalsqlite.isEnabledUser(ACLUser.ADMIN_NAME) == false) {
			throw new Exception("User " + ACLUser.ADMIN_NAME + " is disabled in sqlite file !");
		}
		
	}
	
}
