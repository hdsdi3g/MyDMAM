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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package ext;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.archivecircleapi.ACNode;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.pathindexing.BridgePathindexArchivelocation;
import hd3gtv.mydmam.web.JSSourceManager;
import hd3gtv.mydmam.web.JSi18nCached;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class Bootstrap extends Job<Void> {
	
	private static AuthTurret auth;
	
	public static AuthTurret getAuth() {
		if (auth == null) {
			try {
				auth = new AuthTurret(CassandraDb.getkeyspace());
			} catch (ConnectionException e) {
				Loggers.Play.error("Can't access to Cassandra", e);
			} catch (Exception e) {
				Loggers.Play.error("Can't load Auth (secure)", e);
			}
		}
		return auth;
	}
	
	private static ACAPI acapi;
	
	/**
	 * @return null if not configured.
	 */
	public static ACAPI getACAPI() {
		if (acapi == null) {
			String host = Configuration.global.getValue("acapi", "host", "");
			if (host.equals("")) {
				return null;
			}
			String user = Configuration.global.getValue("acapi", "user", "");
			String password = Configuration.global.getValue("acapi", "password", "");
			int port = Configuration.global.getValue("acapi", "port", 8081);
			
			acapi = new ACAPI(host, user, password);
			acapi.setTcp_port(port);
			
			ACNode node = acapi.getNode();
			if (node == null) {
				Loggers.Play.warn("Can't init ACAPI now");
				return null;
			}
			if (Loggers.Play.isInfoEnabled() & node.nodes != null) {
				StringBuilder sb = new StringBuilder();
				node.nodes.forEach(n -> {
					sb.append(n.toString());
					sb.append(" ");
				});
				Loggers.Play.info("Init ACAPI with nodes [" + sb.toString().trim() + "]");
			}
		}
		return acapi;
	}
	
	public static BridgePathindexArchivelocation bridge_pathindex_archivelocation;
	
	public static JSi18nCached i18n_cache;
	
	public void doJob() {
		i18n_cache = new JSi18nCached();
		
		try {
			CassandraDb.getkeyspace();
		} catch (ConnectionException e) {
			Loggers.Play.error("Can't access to keyspace", e);
		}
		
		try {
			JSSourceManager.init();
		} catch (Exception e) {
			Loggers.Play_JSSource.error("Can't init", e);
		}
		
		if (getACAPI() != null) {
			bridge_pathindex_archivelocation = new BridgePathindexArchivelocation(acapi, Configuration.global.getListMapValues("acapi", "bridge"));
		} else {
			bridge_pathindex_archivelocation = new BridgePathindexArchivelocation();
		}
	}
}
