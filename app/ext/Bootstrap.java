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

import java.util.Map;

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
import play.Play;
import play.Play.Mode;
import play.cache.Cache;
import play.cache.CacheImpl;
import play.cache.EhCacheImpl;
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
	
	public static String getSessionTTL() {
		if (JSSourceManager.isJsDevMode()) {
			return "24h";
		} else {
			return "1h";
		}
	}
	
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
		
		if (Play.mode == Mode.DEV) {
			Cache.forcedCacheImpl = permanent_dev_cache;
			Cache.cacheImpl = permanent_dev_cache;
		}
	}
	
	public static void clearPlayCache() {
		if (Play.mode == Mode.DEV) {
			permanent_dev_cache.forceClear();
		} else {
			Cache.clear();
		}
	}
	
	/**
	 * A Cache that will be resist to a service restart (in case of automatic re-compilation). Only used in dev mode.
	 */
	private static final InternalDevCache permanent_dev_cache = new InternalDevCache();
	
	private static class InternalDevCache implements CacheImpl {
		
		EhCacheImpl eh_cache = EhCacheImpl.getInstance();
		
		public void stop() {
			Loggers.Play.debug("Call stop cache will to nothing");
		}
		
		public void forceClear() {
			Loggers.Play.info("Force to clear Play internal dev cache");
			eh_cache.clear();
		}
		
		public void set(String key, Object value, int expiration) {
			eh_cache.set(key, value, expiration);
		}
		
		public boolean safeSet(String key, Object value, int expiration) {
			return eh_cache.safeSet(key, value, expiration);
		}
		
		public boolean safeReplace(String key, Object value, int expiration) {
			return eh_cache.safeReplace(key, value, expiration);
		}
		
		public boolean safeDelete(String key) {
			return eh_cache.safeDelete(key);
		}
		
		public boolean safeAdd(String key, Object value, int expiration) {
			return eh_cache.safeAdd(key, value, expiration);
		}
		
		public void replace(String key, Object value, int expiration) {
			eh_cache.replace(key, value, expiration);
		}
		
		public long incr(String key, int by) {
			return eh_cache.incr(key, by);
		}
		
		public Map<String, Object> get(String[] keys) {
			return eh_cache.get(keys);
		}
		
		public Object get(String key) {
			return eh_cache.get(key);
		}
		
		public void delete(String key) {
			eh_cache.delete(key);
		}
		
		public long decr(String key, int by) {
			return eh_cache.decr(key, by);
		}
		
		public void clear() {
			Loggers.Play.debug("Call clear cache will to nothing");
		}
		
		public void add(String key, Object value, int expiration) {
			eh_cache.add(key, value, expiration);
		}
	};
}
