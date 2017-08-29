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

import static javax.naming.directory.SearchControls.SUBTREE_SCOPE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import hd3gtv.mydmam.Loggers;

class ActiveDirectoryBackend {
	
	private String domain;
	private String server;
	private int ldap_port;
	
	private String ldap_username;
	private String ldap_password;
	
	private static final String[] userAttributes = { "distinguishedName", "cn", "name", "uid", "sn", "givenname", "memberOf", "samaccountname", "userPrincipalName", "mail" };
	
	private List<String> organizational_unit_white_list = Collections.emptyList();
	private List<String> organizational_unit_black_list = Collections.emptyList();
	
	ActiveDirectoryBackend(String domain, String server, int ldap_port) {
		this.domain = domain;
		if (domain == null) {
			throw new NullPointerException("\"domain\" can't to be null");
		}
		this.server = server;
		if (server == null) {
			throw new NullPointerException("\"server\" can't to be null");
		}
		this.ldap_port = ldap_port;
	}
	
	void setLDAPAuth(String ldap_username, String ldap_password) {
		this.ldap_username = ldap_username;
		if (ldap_username == null) {
			throw new NullPointerException("\"ldap_username\" can't to be null");
		}
		this.ldap_password = ldap_password;
		if (ldap_password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
	}
	
	public void setOrganizationalUnitBlackList(List<String> organizational_unit_black_list) {
		this.organizational_unit_black_list = organizational_unit_black_list.stream().map(ou -> {
			return ou.toLowerCase().trim();
		}).collect(Collectors.toList());
	}
	
	public void setOrganizationalUnitWhiteList(List<String> organizational_unit_white_list) {
		this.organizational_unit_white_list = organizational_unit_white_list.stream().map(ou -> {
			return ou.toLowerCase().trim();
		}).collect(Collectors.toList());
	}
	
	private static String toDC(String domainName) {
		StringBuilder buf = new StringBuilder();
		for (String token : domainName.split("\\.")) {
			if (token.length() == 0) {
				/**
				 * Defensive check
				 */
				continue;
			}
			if (buf.length() > 0) buf.append(",");
			buf.append("DC=").append(token);
		}
		return buf.toString();
	}
	
	ADUser getUser(String username, String password) {
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null");
		}
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
		
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(Context.SECURITY_PRINCIPAL, username + "@" + domain);
		props.put(Context.SECURITY_CREDENTIALS, password);
		props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		props.put(Context.PROVIDER_URL, "ldap://" + server + ":" + String.valueOf(ldap_port) + "/");
		try {
			LdapContext context = new InitialLdapContext(props, null);
			
			String domainName = null;
			String authenticatedUser = (String) context.getEnvironment().get(Context.SECURITY_PRINCIPAL);
			if (authenticatedUser.contains("@")) {
				domainName = authenticatedUser.substring(authenticatedUser.indexOf("@") + 1);
			}
			
			if (domainName != null) {
				SearchControls controls = new SearchControls();
				controls.setSearchScope(SUBTREE_SCOPE);
				controls.setReturningAttributes(userAttributes);
				controls.setCountLimit(10);
				controls.setTimeLimit(500);
				
				NamingEnumeration<SearchResult> answer = context.search(toDC(domainName), "(& (sAMAccountName=" + username + ")(objectClass=user))", controls);
				if (answer.hasMore()) {
					Attributes attr = answer.next().getAttributes();
					Attribute user = attr.get("sAMAccountName");
					if (user != null) {
						if (Loggers.Auth.isDebugEnabled()) {
							Loggers.Auth.debug("Valid user founded from " + toString() + ", user: " + username);
						}
						return new ADUser(username, attr);
					}
				}
			}
		} catch (CommunicationException e) {
			Loggers.Auth.error("Failed to connect to " + server + ":" + String.valueOf(ldap_port), e);
		} catch (NamingException e) {
			Loggers.Auth.debug("Failed to authenticate " + username + "@" + domain + " through " + server, e);
		}
		return null;
	}
	
	private static List<String> extractOrganizationalUnits(Attributes attr) throws NamingException {
		if (attr.get("distinguishedName") == null) {
			return Collections.emptyList();
		}
		
		String dn_values = (String) attr.get("distinguishedName").get();
		
		return Arrays.asList(dn_values.split(",")).stream().filter(dn -> {
			return dn.toUpperCase().startsWith("OU=");
		}).map(ou -> {
			return ou.substring(3);
		}).collect(Collectors.toList());
	}
	
	public class ADUser {
		
		/**
		 * login, like "user"
		 */
		public String username;
		
		/**
		 * Full login, like "user@DOMAIN"
		 */
		public String userprincipal;
		/**
		 * User's Full Name
		 */
		public String commonname;
		/**
		 * User's mail
		 */
		public String mail;
		
		/**
		 * User's groups (root group)
		 * OU=MyService,OU=MyCompany => [MyService, MyCompany]
		 */
		public ArrayList<String> organizational_units;
		
		private ADUser(String sAMAccountName, Attributes attr) throws NamingException, NullPointerException {
			this.username = sAMAccountName;
			
			try {
				userprincipal = (String) attr.get("userPrincipalName").get();
				commonname = (String) attr.get("cn").get();
			} catch (NullPointerException e) {
				NamingEnumeration<? extends Attribute> na = attr.getAll();
				Attribute next;
				LinkedHashMap<String, String> err = new LinkedHashMap<>();
				while (na.hasMore()) {
					next = na.next();
					err.put(next.getID(), (String) next.get());
				}
				throw new NullPointerException("Attribute missing for " + sAMAccountName + ". Actual attributes are " + err);
			}
			
			if (attr.get("mail") != null) {
				mail = (String) attr.get("mail").get();
			}
			
			organizational_units = new ArrayList<String>(extractOrganizationalUnits(attr));
		}
		
		public String getDomain() {
			return domain;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("userprincipal: ");
			sb.append(userprincipal);
			sb.append(", commonname: ");
			sb.append(commonname);
			sb.append(", organizational_units: ");
			sb.append(organizational_units);
			sb.append(", mail: ");
			sb.append(mail);
			return sb.toString();
		}
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Type: ad, domain: ");
		sb.append(domain);
		sb.append(", server: ");
		sb.append(server);
		sb.append(", ldap_port: ");
		sb.append(ldap_port);
		return sb.toString();
	}
	
	List<ADUser> searchUsers(String q) {
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(Context.SECURITY_PRINCIPAL, ldap_username + "@" + domain);
		props.put(Context.SECURITY_CREDENTIALS, ldap_password);
		props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		props.put(Context.PROVIDER_URL, "ldap://" + server + ":" + String.valueOf(ldap_port) + "/");
		try {
			LdapContext context = new InitialLdapContext(props, null);
			
			String domainName = null;
			String authenticatedUser = (String) context.getEnvironment().get(Context.SECURITY_PRINCIPAL);
			if (authenticatedUser.contains("@")) {
				domainName = authenticatedUser.substring(authenticatedUser.indexOf("@") + 1);
			}
			
			if (domainName == null) {
				return null;
			}
			
			SearchControls controls = new SearchControls();
			controls.setSearchScope(SUBTREE_SCOPE);
			controls.setReturningAttributes(userAttributes);
			controls.setCountLimit(50);
			controls.setTimeLimit(1000);
			
			NamingEnumeration<SearchResult> answer = context.search(toDC(domainName), "(& (name=*" + q + "*)(objectClass=user))", controls);
			return Collections.list(answer).stream().map(search_result -> {
				return search_result.getAttributes();
			}).filter(attributes -> {
				/**
				 * Filter by organizational_unit_white/black_list
				 */
				if (organizational_unit_white_list.isEmpty() && organizational_unit_black_list.isEmpty()) {
					return true;
				}
				try {
					List<String> organizational_units = extractOrganizationalUnits(attributes);
					
					boolean wl = true;
					if (organizational_unit_white_list.isEmpty() == false) {
						wl = organizational_units.stream().anyMatch(ou -> {
							return organizational_unit_white_list.contains(ou.toLowerCase());
						});
					}
					
					boolean bl = true;
					if (organizational_unit_black_list.isEmpty() == false) {
						bl = organizational_units.stream().noneMatch(ou -> {
							return organizational_unit_black_list.contains(ou.toLowerCase());
						});
					}
					
					return wl && bl;
				} catch (NamingException e) {
					throw new RuntimeException("Can't process AD search results", e);
				}
			}).map(attributes -> {
				try {
					return new ADUser((String) attributes.get("sAMAccountName").get(), attributes);
				} catch (Exception e) {
					Loggers.Auth.warn("Can't load AD user", e);
				}
				return null;
			}).filter(user -> {
				return user != null;
			}).limit(10).collect(Collectors.toList());
		} catch (CommunicationException e) {
			Loggers.Auth.error("Failed to connect to " + server + ":" + String.valueOf(ldap_port), e);
		} catch (NamingException e) {
			Loggers.Auth.debug("Failed to authenticate " + ldap_username + "@" + domain + " through " + server, e);
		}
		return null;
	}
	
}
