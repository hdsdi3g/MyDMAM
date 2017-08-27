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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.validation.ValidationException;

import com.google.common.collect.Lists;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.archivecircleapi.ACNode;
import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.AuthTurret;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.pathindexing.BridgePathindexArchivelocation;
import play.Play;
import play.Play.Mode;
import play.cache.Cache;
import play.cache.CacheImpl;
import play.cache.EhCacheImpl;
import play.data.validation.Validation.ValidationResult;
import play.libs.Crypto;

public class PlayBootstrap {
	
	private AuthTurret auth;
	private JSi18nCached i18n_cache;
	private BridgePathindexArchivelocation bridge_pathindex_archivelocation;
	private String revision_hash_query;
	private AJSProcessTimeLog ajs_process_time_log;
	private AJSProcessTimeLog jsressource_process_time_log;
	
	/**
	 * A Cache that will be resist to a service restart (in case of automatic re-compilation). Only used in dev mode.
	 */
	private final InternalDevCache permanent_dev_cache;
	
	public PlayBootstrap() {
		if (Play.mode == Play.Mode.PROD) {
			revision_hash_query = "?rev=" + Crypto.passwordHash(Crypto.encryptAES(GitInfo.getFromRoot().getActualRepositoryInformation())).substring(0, 4);
		} else {
			revision_hash_query = "";
			enableProcessTimeLogs();
		}
		
		try {
			CassandraDb.getkeyspace();
		} catch (ConnectionException e) {
			Loggers.Play.fatal("Can't access to keyspace", e);
			System.exit(1);
		}
		
		i18n_cache = new JSi18nCached();
		
		try {
			JSSourceManager.init(this);
		} catch (Exception e) {
			Loggers.Play_JSSource.fatal("Can't init JS Source manager", e);
			System.exit(1);
		}
		
		permanent_dev_cache = new InternalDevCache();
		if (Play.mode == Mode.DEV) {
			Cache.forcedCacheImpl = permanent_dev_cache;
			Cache.cacheImpl = permanent_dev_cache;
		}
		
		try {
			auth = new AuthTurret(CassandraDb.getkeyspace());
		} catch (ConnectionException e) {
			Loggers.Play.fatal("Can't access to Cassandra", e);
			System.exit(1);
		} catch (Exception e) {
			Loggers.Play.fatal("Can't load Auth (secure)", e);
			System.exit(1);
		}
		
		ACAPI acapi = ACAPI.loadFromConfiguration();
		if (acapi != null) {
			ACNode node = acapi.getNode();
			if (node == null) {
				Loggers.Play.warn("Can't init ACAPI now");
			} else {
				if (Loggers.Play.isInfoEnabled() & node.nodes != null) {
					StringBuilder sb = new StringBuilder();
					node.nodes.forEach(n -> {
						sb.append(n.toString());
						sb.append(" ");
					});
					Loggers.Play.info("Init ACAPI with nodes [" + sb.toString().trim() + "]");
					bridge_pathindex_archivelocation = new BridgePathindexArchivelocation(acapi, Configuration.global.getListMapValues("acapi", "bridge"));
				}
			}
		}
		
		if (bridge_pathindex_archivelocation == null) {
			bridge_pathindex_archivelocation = new BridgePathindexArchivelocation();
		}
	}
	
	public String getRevisionHashQuery() {
		return revision_hash_query;
	}
	
	public AuthTurret getAuth() {
		return auth;
	}
	
	public JSi18nCached getI18nCache() {
		return i18n_cache;
	}
	
	public JSi18nCached refreshI18nCache() {
		i18n_cache = new JSi18nCached();
		return i18n_cache;
	}
	
	public String getSessionTTL() {
		if (JSSourceManager.isJsDevMode()) {
			return "24h";
		} else {
			return "1h";
		}
	}
	
	public void clearPlayCache() {
		if (Play.mode == Mode.DEV) {
			permanent_dev_cache.forceClear();
		} else {
			Cache.clear();
		}
	}
	
	public BridgePathindexArchivelocation getBridgePathindexArchivelocation() {
		return bridge_pathindex_archivelocation;
	}
	
	private class InternalDevCache implements CacheImpl {
		
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
	
	public static void validate(ValidationResult... allvr) throws ValidationException {
		if (allvr == null) {
			throw new NullPointerException("\"allvr\" can't to be null");
		}
		if (allvr.length == 0) {
			throw new NullPointerException("\"allvr\" can't to be empty");
		}
		
		ArrayList<ValidationResult> list_vr = Lists.newArrayList(allvr);
		
		List<ValidationResult> iserror_vr = list_vr.stream().filter(vr -> {
			return vr.ok == false;
		}).collect(Collectors.toList());
		
		if (iserror_vr.isEmpty()) {
			return;
		}
		
		String all_errors = iserror_vr.stream().map(vr -> {
			return vr.error.message();
		}).collect(Collectors.joining(", "));
		
		throw new ValidationException(all_errors);
	}
	
	/**
	 * @return maybe null
	 */
	public AJSProcessTimeLog getAJSProcessTimeLog() {
		return ajs_process_time_log;
	}
	
	/**
	 * @return maybe null
	 */
	public AJSProcessTimeLog getJSRessourceProcessTimeLog() {
		return jsressource_process_time_log;
	}
	
	void enableProcessTimeLogs() {
		ajs_process_time_log = new AJSProcessTimeLog();
		jsressource_process_time_log = new AJSProcessTimeLog();
	}
	
	void disableProcessTimeLogs() {
		ajs_process_time_log = null;
		jsressource_process_time_log = null;
	}
	
}
