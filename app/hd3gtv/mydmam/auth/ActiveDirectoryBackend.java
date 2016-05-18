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

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;

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

class ActiveDirectoryBackend {
	
	// TODO load AD groups, load from conf
	
	private String domain;
	private String server;
	private int ldap_port;
	
	private static final String[] userAttributes = { "distinguishedName", "cn", "name", "uid", "sn", "givenname", "memberOf", "samaccountname", "userPrincipalName", "mail" };
	
	public ActiveDirectoryBackend(String domain, String server, int ldap_port) {
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public ADUser getUser(String username, String password) throws NullPointerException, IOException, InvalidUserAuthentificationException {
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null");
		}
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
		
		Hashtable props = new Hashtable();
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
				String principalName = username + "@" + domainName;
				SearchControls controls = new SearchControls();
				controls.setSearchScope(SUBTREE_SCOPE);
				controls.setReturningAttributes(userAttributes);
				NamingEnumeration<SearchResult> answer = context.search(toDC(domainName), "(& (userPrincipalName=" + principalName + ")(objectClass=user))", controls);
				if (answer.hasMore()) {
					Attributes attr = answer.next().getAttributes();
					Attribute user = attr.get("userPrincipalName");
					if (user != null) {
						return new ADUser(attr);
					}
				}
			}
			return null;
		} catch (CommunicationException e) {
			throw new IOException("Failed to connect to " + server + ":" + String.valueOf(ldap_port), e);
		} catch (NamingException e) {
			throw new InvalidUserAuthentificationException("Failed to authenticate " + username + "@" + domain + " through " + server, e);
		}
	}
	
	class ADUser {
		
		/**
		 * Login, like "user@DOMAIN"
		 */
		private String userprincipal;
		/**
		 * User's Full Name
		 */
		private String commonname;
		/**
		 * User's mail
		 */
		private String mail;
		
		/**
		 * User's group (root group)
		 */
		private String group;
		
		private ADUser(Attributes attr) throws NamingException {
			userprincipal = (String) attr.get("userPrincipalName").get();
			commonname = (String) attr.get("cn").get();
			
			/*NamingEnumeration<? extends Attribute> na = attr.getAll();
			Attribute next;
			while (na.hasMore()) {
				next = na.next();
				System.out.print(next.getID());
				System.out.print("\t\t");
				System.out.println(next.get());
			}*/
			
			if (attr.get("mail") != null) {
				mail = (String) attr.get("mail").get();
			}
			
			String dn_values = (String) attr.get("distinguishedName").get();
			Arrays.asList(dn_values.split(",")).forEach(v -> {
				if (v.toLowerCase().startsWith("ou")) {
					group = v.substring(3);
				}
			});
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("userprincipal: ");
			sb.append(userprincipal);
			sb.append(", commonname: ");
			sb.append(commonname);
			sb.append(", group: ");
			sb.append(group);
			sb.append(", mail: ");
			sb.append(mail);
			return sb.toString();
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("domain: ");
		sb.append(domain);
		sb.append(", server: ");
		sb.append(server);
		sb.append(", ldap_port: ");
		sb.append(ldap_port);
		return sb.toString();
	}
	
}
