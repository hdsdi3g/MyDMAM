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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.ftpserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;

import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.auth.Password;
import hd3gtv.mydmam.db.CassandraDb;

public class FTPUser implements User {
	
	private static final ColumnFamily<String, String> CF_USER = new ColumnFamily<String, String>("ftpServerUser", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	private transient static Password password;
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_USER.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_USER.getName(), true);
				// String queue_name = CF_FTPUSER.getName();
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_USER, "last_login", CF_USER.getName() + "_last_login", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "creator_hostname", queue_name + "_creator_hostname", DeployColumnDef.ColType_UTF8Type);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "expiration_date", queue_name + "_expiration_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "update_date", queue_name + "_update_date", DeployColumnDef.ColType_LongType);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "delete", queue_name + "_delete", DeployColumnDef.ColType_Int32Type);
				// CassandraDb.declareIndexedColumn(CassandraDb.getkeyspace(), CF_QUEUE, "indexingdebug", queue_name + "_indexingdebug", DeployColumnDef.ColType_Int32Type);
			}
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't init database CFs", e);
		}
		
		try {
			password = new Password(Configuration.global.getValue("ftpserver", "master_password_key", ""));
		} catch (Exception e) {
			Loggers.FTPserver.fatal("Can't load password API for FTP Server, check configuration", e);
			System.exit(1);
		}
	}
	
	private String user_id;
	private String user_name;
	private byte[] obscured_password;
	private String group_name;
	private String domain;
	private boolean disabled;
	private long create_date;
	private long update_date;
	private long last_path_index_refreshed;
	private long last_login;
	
	/**
	 * populateGroup will init it.
	 */
	private transient FTPGroup group;
	
	private FTPUser() {
	}
	
	/**
	 * Don't forget to save.
	 * @param domain can be empty, but not null.
	 */
	public static FTPUser create(String user_name, String clear_password, String group_name, String domain) throws IOException {
		FTPUser user = new FTPUser();
		
		user.user_name = user_name;
		if (user_name == null) {
			throw new NullPointerException("\"user_name\" can't to be null");
		}
		if (user_name.isEmpty()) {
			throw new NullPointerException("\"user_name\" can't to be empty");
		}
		// TODO check bad chars in user_name
		
		user.obscured_password = password.getHashedPassword(clear_password);
		if (clear_password == null) {
			throw new NullPointerException("\"clear_password\" can't to be null");
		}
		if (clear_password.isEmpty()) {
			throw new NullPointerException("\"clear_password\" can't to be empty");
		}
		
		user.domain = domain;
		if (domain == null) {
			throw new NullPointerException("\"domain\" can't to be null");
		}
		
		user.group_name = group_name;
		if (group_name == null) {
			throw new NullPointerException("\"group_name\" can't to be null");
		}
		if (group_name.isEmpty()) {
			throw new NullPointerException("\"group_name\" can't to be empty");
		}
		
		user.disabled = false;
		user.user_id = makeUserId(user_name, domain);
		
		user.create_date = System.currentTimeMillis();
		user.update_date = user.create_date;
		user.last_login = 0;
		
		return user;
	}
	
	boolean validPassword(UsernamePasswordAuthentication auth) {
		return password.checkPassword(auth.getPassword(), this.obscured_password);
	}
	
	FTPUser updateLastLogin() throws ConnectionException {
		last_login = System.currentTimeMillis();
		save();
		return this;
	}
	
	private void populateGroup() {
		if (group == null) {
			group = FTPGroup.getFromName(group_name);
			if (group == null) {
				throw new NullPointerException("Can't found declared group \"" + group_name + "\".");
			}
		}
	}
	
	private static String makeUserId(String user_name, String domain) {
		return "ftpuser:" + domain + "#" + user_name;
	}
	
	public static FTPUser getUserByName(String user_name, String domain) throws ConnectionException {
		return getUserId(makeUserId(user_name, domain), true);
	}
	
	public static FTPUser getUserId(String user_id, boolean only_valid_users) throws ConnectionException {
		ColumnList<String> row = keyspace.prepareQuery(CF_USER).getKey(user_id).execute().getResult();
		if (row.isEmpty()) {
			return null;
		}
		FTPUser user = new FTPUser();
		user.importFromDb(user_id, row);
		user.populateGroup();
		
		if (only_valid_users == false) {
			return user;
		}
		if (user.disabled) {
			Loggers.FTPserver.warn("User is disabled: " + user_id);
			return null;
		}
		if (user.group.isDisabled()) {
			Loggers.FTPserver.warn("User's group (" + user.group.getName() + ") is disabled for " + user_id);
			return null;
		}
		try {
			user.group.getUserHomeDirectory(user);
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't load user home directory for " + user_id, e);
			return null;
		}
		return user;
	}
	
	private void importFromDb(String key, ColumnList<String> cols) {
		user_id = key;
		user_name = cols.getStringValue("user_name", "");
		obscured_password = cols.getByteArrayValue("obscured_password", new byte[0]);
		group_name = cols.getStringValue("group_name", "");
		domain = cols.getStringValue("domain", "");
		disabled = cols.getBooleanValue("disabled", false);
		create_date = cols.getLongValue("create_date", -1l);
		update_date = cols.getLongValue("update_date", -1l);
		last_path_index_refreshed = cols.getLongValue("last_path_index_refreshed", -1l);
		last_login = cols.getLongValue("last_login", -1l);
	}
	
	public void save() throws ConnectionException {
		MutationBatch mutator = keyspace.prepareMutationBatch();
		save(mutator);
		mutator.execute();
	}
	
	public void save(MutationBatch mutator) throws ConnectionException {
		mutator.withRow(CF_USER, user_id).putColumn("user_name", user_name);
		mutator.withRow(CF_USER, user_id).putColumn("obscured_password", obscured_password);
		mutator.withRow(CF_USER, user_id).putColumn("group_name", group_name);
		mutator.withRow(CF_USER, user_id).putColumn("domain", domain);
		mutator.withRow(CF_USER, user_id).putColumn("disabled", disabled);
		mutator.withRow(CF_USER, user_id).putColumn("create_date", create_date);
		mutator.withRow(CF_USER, user_id).putColumn("update_date", update_date);
		mutator.withRow(CF_USER, user_id).putColumn("last_path_index_refreshed", last_path_index_refreshed);
		mutator.withRow(CF_USER, user_id).putColumn("last_login", last_login);
	}
	
	public String getName() {
		return user_name;
	}
	
	/**
	 * @return null
	 */
	public String getPassword() {
		return null;
	}
	
	public List<Authority> getAuthorities() {
		return null;
	}
	
	public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
		return null;
	}
	
	public AuthorizationRequest authorize(AuthorizationRequest request) {
		// ConcurrentLoginRequest clr = (ConcurrentLoginRequest) request;
		// System.err.println(request.getClass());
		return request;
	}
	
	public int getMaxIdleTime() {
		return Configuration.global.getValue("ftpserver", "maxidletime", 300);
	}
	
	public boolean getEnabled() {
		return disabled == false;
	}
	
	public String getHomeDirectory() {
		populateGroup();
		try {
			return group.getUserHomeDirectory(this).getAbsolutePath();
		} catch (Exception e) {
			Loggers.FTPserver.error("Can't get home directory for user " + user_id + " (" + user_name + ")", e);
		}
		return null;
	}
	
	String getUserId() {
		return user_id;
	}
	
	/**
	 * @return never null, maybe empty
	 */
	String getDomain() {
		return domain;
	}
	
	FTPGroup getGroup() {
		populateGroup();
		return group;
	}
	
	long getLastPathIndexRefreshed() {
		return last_path_index_refreshed;
	}
	
	FTPUser setLastPathIndexRefreshed() {
		last_path_index_refreshed = System.currentTimeMillis();
		return this;
	}
	
	/*ArrayList<Session> getSessionsForUser() {
		if (sessions != null) {
		}
		return sessions;
	}*/
	
	static class ExpiredUser {
		String group_name;
		String user_id;
		long date;
		
		private ExpiredUser(String group_name, String user_id, long date) {
			this.group_name = group_name;
			this.user_id = user_id;
			this.date = date;
		}
		
	}
	
	/**
	 * @param groups_ttl Group id -> ttl (in msec)
	 * @return group id -> last_login, never null
	 */
	
	static List<ExpiredUser> getTrashableUsers() throws ConnectionException {
		HashMap<String, Long> groups_ttl = FTPGroup.getDeclaredGroupsTTLTrash();
		HashMap<String, Boolean> group_activity_last_login = FTPGroup.getDeclaredGroupsExpirationBasedOnLastActivity();
		
		List<ExpiredUser> result = new ArrayList<FTPUser.ExpiredUser>();
		Rows<String, String> rows = keyspace.prepareQuery(CF_USER).getAllRows().withColumnSlice("last_login", "group_name", "create_date").execute().getResult();
		
		String group_name;
		long ttl_eq_now;
		long date;
		for (Row<String, String> row : rows) {
			group_name = row.getColumns().getStringValue("group_name", "");
			if (groups_ttl.containsKey(group_name) == false) {
				continue;
			}
			ttl_eq_now = System.currentTimeMillis() - groups_ttl.get(group_name);
			
			if (group_activity_last_login.get(group_name)) {
				date = row.getColumns().getLongValue("last_login", 0l);
				if (date < 1) {
					/**
					 * User has never login.
					 */
					continue;
				}
			} else {
				date = row.getColumns().getLongValue("create_date", 0l);
			}
			if (date < ttl_eq_now) {
				result.add(new ExpiredUser(group_name, row.getKey(), date));
			}
		}
		return result;
	}
	
	static List<ExpiredUser> getPurgableUsers(List<ExpiredUser> trashable_users) {
		HashMap<String, Long> groups_ttl = FTPGroup.getDeclaredGroupsTTLPurge();
		
		List<ExpiredUser> result = new ArrayList<FTPUser.ExpiredUser>();
		ExpiredUser user;
		long ttl_eq_now;
		for (int pos = 0; pos < trashable_users.size(); pos++) {
			user = trashable_users.get(pos);
			if (groups_ttl.containsKey(user.group_name) == false) {
				continue;
			}
			ttl_eq_now = System.currentTimeMillis() - groups_ttl.get(user.group_name);
			if (user.date < ttl_eq_now) {
				result.add(user);
			}
		}
		return result;
	}
	
	// TODO if delete user, delete sessions and delete activities
	
}
