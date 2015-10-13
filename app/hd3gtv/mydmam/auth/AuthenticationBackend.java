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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.apache.commons.net.util.Base64;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import models.ACLUser;

public class AuthenticationBackend {
	
	static {
		try {
			refreshConfiguration();
		} catch (Exception e) {
			Loggers.Auth.error("Error with loading authentication configuration", e);
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
		File auth_file;
		for (int pos = 0; pos < elements.size(); pos++) {
			configuration_element = elements.get(pos);
			String element_source = (String) configuration_element.get("source");
			if (element_source.equals("local")) {
				String path = (String) configuration_element.get("path");
				
				auth_file = new File(path);
				if (auth_file.exists() == false) {
					if (auth_file.isAbsolute() == false) {
						if (auth_file.getParentFile().getName().equals("conf") & Configuration.getGlobalConfigurationDirectory().getParentFile().getName().equals("conf")) {
							/**
							 * SQLite file is located in Play conf directory, and Play /app.d/ is also located in conf directory.
							 * We consider that conf directory is the same.
							 */
							auth_file = new File(Configuration.getGlobalConfigurationDirectory().getParent() + File.separator + auth_file.getName());
						}
					}
				}
				if (auth_file.exists() == false) {
					throw new FileNotFoundException(path);
				}
				
				String masterkey = (String) configuration_element.get("masterkey");
				authenticators.add(new AuthenticatorLocalsqlite(auth_file, masterkey));
				
				String label = (String) configuration_element.get("label");
				authenticators_domains.add(label);
			} else if (element_source.equals("ad")) {
				String domain = (String) configuration_element.get("domain");
				String server = (String) configuration_element.get("server");
				int port = (Integer) configuration_element.get("port");
				authenticators.add(new AuthenticatorActivedirectory(domain, server, port));
				
				authenticators_domains.add(domain);
			} else {
				Loggers.Auth.error("Can't import \"auth/backend\" " + (pos + 1) + " configuration item: " + configuration_element);
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
	 * Try to get User with authenticator or throws exception
	 */
	public static AuthenticationUser authenticate(Authenticator authenticator, String username, String password) throws InvalidAuthenticatorUserException {
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
		try {
			authenticationUser = authenticator.getUser(username, password);
			if (authenticationUser != null) {
				Loggers.Auth.debug("Valid user found for this authentication method, username: " + username + ", " + authenticator);
				return authenticationUser;
			}
		} catch (IOException e) {
			Loggers.Auth.error("Invalid authentication method: " + username + ", " + authenticator, e);
		}
		return null;
	}
	
	/**
	 * Try to get User, authenticator after authenticator, until it found a correct user or throws exception
	 */
	public static AuthenticationUser authenticate(String username, String password) throws InvalidAuthenticatorUserException {
		AuthenticationUser authenticationUser;
		for (int pos = 0; pos < authenticators.size(); pos++) {
			try {
				
				authenticationUser = authenticate(authenticators.get(pos), username, password);
				if (authenticationUser != null) {
					return authenticationUser;
				}
			} catch (InvalidAuthenticatorUserException e) {
				Loggers.Auth.debug("Invalid user for this authentication method, authenticator: " + authenticators.get(pos), e);
			}
		}
		throw new InvalidAuthenticatorUserException("Can't authenticate with " + username);
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
			
			Loggers.Auth.info(
					"Create Play administrator account, login: " + ACLUser.ADMIN_NAME + ", password file: " + textfile.getAbsoluteFile() + ", local database: " + authenticatorlocalsqlite.getDbfile());
					
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
