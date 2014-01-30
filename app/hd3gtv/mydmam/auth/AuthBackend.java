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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.IOException;

import javax.naming.CommunicationException;
import javax.naming.NamingException;

public class AuthBackend {
	
	static {
		// TODO tableau pour la config
		/**
		 * @see Configuration.global.importLog2Configuration(configuration, logrotate_enabled);
		 */
		/*
		???
		auth:
			backend:
				-
				    domain: localhost.localdomain
				    source: local
				    path: auth.db
				-
				    domain: mydomain.local
				    source: ad
				    server: 192.168.0.1
				    port: 389
				    path: auth.db
		 * */
		
	}
	
	public static void authenticate(String username, String password, String domain) throws IOException {
		Log2Dump dump = new Log2Dump();
		dump.add("username", username);
		dump.add("domain", domain);
		
		if (true) {
			/**
			 * TODO if AD
			 */
			String server = ""; // TODO get server from configuration
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
