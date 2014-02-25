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
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import models.ACLUser;

import org.apache.commons.net.util.Base64;

public class AuthenticationBackend {
	
	static {
		try {
			refreshConfiguration();
		} catch (Exception e) {
			Log2.log.error("Error with loading authentication configuration", e);
		}
	}
	
	private static List<Authenticator> authenticators;
	private static List<String> authenticators_domains;
	private static boolean force_select_domain;
	
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
		
		force_select_domain = Configuration.global.getValueBoolean("auth", "force_select_domain");
		
		authenticators = new ArrayList<Authenticator>(elements.size());
		authenticators_domains = new ArrayList<String>(elements.size());
		
		LinkedHashMap<String, ?> configuration_element;
		for (int pos = 0; pos < elements.size(); pos++) {
			configuration_element = elements.get(pos);
			String element_source = (String) configuration_element.get("source");
			if (element_source.equals("local")) {
				String path = (String) configuration_element.get("path");
				String masterkey = (String) configuration_element.get("masterkey");
				authenticators.add(new AuthenticatorLocalsqlite(new File(path), masterkey));
				
				String label = (String) configuration_element.get("label");
				authenticators_domains.add(label);
			} else if (element_source.equals("ad")) {
				String domain = (String) configuration_element.get("domain");
				String server = (String) configuration_element.get("server");
				int port = (Integer) configuration_element.get("port");
				authenticators.add(new AuthenticatorActivedirectory(domain, server, port));
				
				authenticators_domains.add(domain);
			} else {
				Log2.log.error("Can't import \"auth/backend\" " + (pos + 1) + " configuration item", null, new Log2Dump("item", configuration_element.toString()));
			}
		}
		
		if (authenticators.isEmpty()) {
			throw new NullPointerException("No authentication backend is correctly set");
		}
	}
	
	public static boolean isForce_select_domain() {
		return force_select_domain;
	}
	
	public static List<String> getAuthenticators_domains() {
		return authenticators_domains;
	}
	
	public static List<Authenticator> getAuthenticators() {
		return authenticators;
	}
	
	/**
	 * Try to get User with authenticator.
	 * @return null if user & password are invalid, unknow, lock...
	 */
	public static AuthenticationUser authenticate(Authenticator authenticator, String username, String password) {
		if (authenticator == null) {
			throw new NullPointerException("\"authenticator\" can't to be null");
		}
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null");
		}
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
		
		AuthenticationUser authenticationUser;
		Log2Dump dump;
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
		return null;
	}
	
	/**
	 * Try to get User, authenticator after authenticator, until it found a correct user.
	 * @return null if user & password are invalid, unknow, lock...
	 */
	public static AuthenticationUser authenticate(String username, String password) {
		AuthenticationUser authenticationUser;
		for (int pos = 0; pos < authenticators.size(); pos++) {
			authenticationUser = authenticate(authenticators.get(pos), username, password);
			if (authenticationUser != null) {
				return authenticationUser;
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
			String newpassword = passwordGenerator();
			authenticatorlocalsqlite.createUser(ACLUser.ADMIN_NAME, newpassword, "Local Admin", true);
			
			File textfile = new File("play-new-password.txt");
			FileWriter fw = new FileWriter(textfile, false);
			fw.write("Admin login: " + ACLUser.ADMIN_NAME + "\r\n");
			fw.write("Admin password: " + newpassword + "\r\n");
			fw.write("\r\n");
			fw.write("You should remove this file after keeping this password..\r\n");
			fw.write("\r\n");
			fw.write("You can change this password with mydmam-cli:\r\n");
			fw.write("$ mydmam-cli localauth -f " + authenticatorlocalsqlite.getDbfile().getAbsolutePath() + " -key " + authenticatorlocalsqlite.getMaster_password_key() + " -passwd -u "
					+ ACLUser.ADMIN_NAME + "\r\n");
			fw.write("\r\n");
			fw.write("Note: you haven't need a local authenticator if you set another backend and if you grant some new administrators\r\n");
			fw.close();
			
			Log2Dump dump = new Log2Dump();
			dump.add("login", ACLUser.ADMIN_NAME);
			dump.add("password file", textfile.getAbsoluteFile());
			dump.add("local database", authenticatorlocalsqlite.getDbfile());
			Log2.log.security("Create Play administrator account", dump);
			
		} else if (authenticatorlocalsqlite.isEnabledUser(ACLUser.ADMIN_NAME) == false) {
			throw new Exception("User " + ACLUser.ADMIN_NAME + " is disabled in sqlite file !");
		}
		
	}
	
	/**
	 * @return 12 first chars of Base64(SHA-264(random(1024b)))
	 */
	public static String passwordGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		Random r = new Random();
		byte[] fill = new byte[1024];
		r.nextBytes(fill);
		byte[] key = md.digest(fill);
		return new String(Base64.encodeBase64String(key)).substring(0, 12);
	}
	
}
