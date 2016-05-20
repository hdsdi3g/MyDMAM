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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.auth;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.web.PrivilegeNG;
import hd3gtv.tools.GsonIgnoreStrategy;

public class AuthTurret {
	
	private static final ColumnFamily<String, String> CF_AUTH = new ColumnFamily<String, String>("mgrAuth", StringSerializer.get(), StringSerializer.get());
	
	private Gson gson_simple;
	private Gson gson;
	final JsonParser parser = new JsonParser();
	private Password password;
	
	private Keyspace keyspace;
	private Cache cache;
	
	// TODO domain isolation or not
	
	public AuthTurret(Keyspace keyspace) throws ConnectionException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
		
		/**
		 * Load Cassandra access
		 */
		this.keyspace = keyspace;
		if (keyspace == null) {
			throw new NullPointerException("\"keyspace\" can't to be null");
		}
		if (CassandraDb.isColumnFamilyExists(keyspace, CF_AUTH.getName()) == false) {
			CassandraDb.createColumnFamilyString(keyspace.getKeyspaceName(), CF_AUTH.getName(), false);
		}
		
		/**
		 * Init Gson tools
		 */
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		/**
		 * Outside of this package serializers
		 */
		builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		
		gson_simple = builder.create();
		
		/**
		 * Inside of this package serializers
		 */
		builder.registerTypeAdapter(Properties.class, new PropertiesSerializer());
		gson = builder.create();
		
		password = new Password("");// TODO from conf
		
		cache = new Cache(this, TimeUnit.MINUTES.toMillis(10));// TODO from conf
		
		/**
		 * Import & destroy XML account_export file
		 */
		File account_export = new File(Configuration.getGlobalConfigurationDirectory().getParent() + File.separator + "account_export.xml");
		if (account_export.exists()) {
			Loggers.Auth.info("You should remove account_export xml file... (" + account_export.getAbsolutePath() + ")");
		}
		
		/**
		 * Peuplate DB Default users
		 */
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		/** Create admin role if needed */
		RoleNG default_admin_role = new RoleNG("All privileges");
		RoleNG admin_role = getByRoleKey(default_admin_role.getKey());
		if (admin_role == null) {
			Loggers.Auth.info("Admin role is absent, create it.");
			default_admin_role.update(PrivilegeNG.getAllPrivilegesName());
			default_admin_role.save(mutator.withRow(CF_AUTH, default_admin_role.getKey()));
			cache.all_roles.put(default_admin_role.getKey(), default_admin_role);
			admin_role = default_admin_role;
		} else {
			/** Grant all privilege if it't not the actual case */
			if (admin_role.getPrivileges().containsAll(PrivilegeNG.getAllPrivilegesName()) == false) {
				Loggers.Auth.info("Admin role not containt all and same privileges, update it.");
				admin_role.update(PrivilegeNG.getAllPrivilegesName());
				admin_role.save(mutator.withRow(CF_AUTH, admin_role.getKey()));
				cache.all_roles.put(admin_role.getKey(), admin_role);
			}
		}
		
		/**
		 * Create guest role if needed
		 */
		RoleNG default_guest_role = new RoleNG("Default");
		if (getByRoleKey(default_guest_role.getKey()) == null) {
			Loggers.Auth.info("Default role is absent, create it.");
			default_guest_role.save(mutator.withRow(CF_AUTH, default_guest_role.getKey()));
			cache.all_roles.put(default_guest_role.getKey(), default_guest_role);
		}
		
		/**
		 * Create admin group if needed
		 */
		GroupNG default_admin_group = new GroupNG("Administrators");
		if (getByGroupKey(default_admin_group.getKey()) == null) {
			Loggers.Auth.info("Admin group is absent, create it.");
			default_admin_group.update(Arrays.asList(admin_role));
			default_admin_group.save(mutator.withRow(CF_AUTH, default_admin_group.getKey()));
			cache.all_groups.put(default_admin_group.getKey(), default_admin_group);
		}
		
		/**
		 * Create newusers group if needed
		 */
		GroupNG default_newusers_group = new GroupNG("New users");
		if (getByGroupKey(default_newusers_group.getKey()) == null) {
			Loggers.Auth.info("Admin group is absent, create it.");
			default_newusers_group.update(Arrays.asList(getByRoleKey(default_guest_role.getKey())));
			default_newusers_group.save(mutator.withRow(CF_AUTH, default_newusers_group.getKey()));
			cache.all_groups.put(default_newusers_group.getKey(), default_newusers_group);
		}
		
		/**
		 * Create admin user if needed, and create a password file:
		 */
		UserNG default_admin_user = new UserNG(this, "admin", "local");
		if (getByUserKey(default_admin_user.getKey()) == null) {
			Loggers.Auth.info("Admin user is absent, create it.");
			default_admin_user.chpassword(createAdminPasswordTextFile("admin"));
			default_admin_user.update("Default administrator", Locale.getDefault().getLanguage(), AdminMailAlert.getAdminAddr("root@localhost"), false);
			default_admin_user.setUserGroups(Arrays.asList(getByGroupKey(default_admin_group.getKey())));
			default_admin_user.save(mutator.withRow(CF_AUTH, default_admin_user.getKey()));
			cache.all_users.put(default_admin_user.getKey(), default_admin_user);
		}
		
		if (mutator.isEmpty() == false) {
			mutator.execute();
		}
	}
	
	/**
	 * @return clear text generated password
	 */
	public String createAdminPasswordTextFile(String admin_loggin) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		String newpassword = Password.passwordGenerator();
		
		File textfile = new File("play-new-password.txt");
		FileWriter fw = new FileWriter(textfile, false);
		fw.write("Admin login: " + admin_loggin + "\r\n");
		fw.write("Admin password: " + newpassword + "\r\n");
		fw.write("\r\n");
		fw.write("You should remove this file after keeping this password..\r\n");
		fw.write("\r\n");
		/*fw.write("You can change this password with mydmam-cli:\r\n");
		fw.write("$ mydmam-cli localauth -f " + authenticatorlocalsqlite.getDbfile().getAbsolutePath() + " -key " + authenticatorlocalsqlite.getMaster_password_key() + " -passwd -u "
		fw.write("\r\n");
		*/
		fw.write("Note: you haven't need a local authenticator if you set another backend and if you grant some new administrators\r\n");
		fw.close();
		
		Loggers.Auth.info("Create admin password file account, password file: " + textfile.getAbsoluteFile());
		
		return newpassword;
	}
	
	private class PropertiesSerializer implements JsonSerializer<Properties>, JsonDeserializer<Properties> {
		
		public Properties deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject src = json.getAsJsonObject();
			Properties result = new Properties();
			
			for (Map.Entry<String, JsonElement> entry : src.entrySet()) {
				result.setProperty(entry.getKey(), entry.getValue().getAsJsonPrimitive().getAsString());
			}
			
			return result;
		}
		
		public JsonElement serialize(Properties src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = new JsonObject();
			Object value;
			for (Map.Entry<Object, Object> entry : src.entrySet()) {
				value = entry.getValue();
				if (value == null) {
					continue;
				} else {
					result.addProperty((String) entry.getKey(), (String) value);
				}
			}
			return result;
		}
	}
	
	public Gson getGson() {
		return gson;
	}
	
	public Gson getGsonSimple() {
		return gson_simple;
	}
	
	Password getPassword() {
		return password;
	}
	
	ColumnFamilyQuery<String, String> prepareQuery() {
		return keyspace.prepareQuery(CF_AUTH);
	}
	
	void onConnectionException(ConnectionException e) {
		Loggers.Auth.warn("Can't access to database", e);
	}
	
	public ArrayList<RoleNG> getRolesByGroupList(ArrayList<GroupNG> groups) {
		HashMap<RoleNG, Object> result = new HashMap<RoleNG, Object>();
		
		groups.forEach(group -> {
			group.getGroupRoles().forEach(role -> {
				result.put(role, this);
			});
		});
		
		return new ArrayList<RoleNG>(result.keySet());
	}
	
	public HashSet<String> getPrivilegesByRoleList(ArrayList<RoleNG> roles) {
		HashSet<String> result = new HashSet<String>();
		
		roles.forEach(role -> {
			role.getPrivileges().forEach(privilege -> {
				result.add(privilege);
			});
		});
		
		return result;
	}
	
	public HashSet<String> getPrivilegesByGroupList(ArrayList<GroupNG> groups) {
		HashSet<String> result = new HashSet<String>();
		
		groups.forEach(group -> {
			group.getGroupRoles().forEach(role -> {
				role.getPrivileges().forEach(privilege -> {
					result.add(privilege);
				});
			});
		});
		
		return result;
	}
	
	public boolean isForceSelectDomain() {
		// TODO from conf
		return false;
	}
	
	public ArrayList<String> declaredDomainList() {
		return cache.getAll_domains();
	}
	
	public UserNG getByUserKey(String user_key) {
		return cache.getAll_users().get(user_key);
	}
	
	public GroupNG getByGroupKey(String group_key) {
		return cache.getAll_groups().get(group_key);
	}
	
	public RoleNG getByRoleKey(String role_key) {
		return cache.getAll_roles().get(role_key);
	}
	
	public HashMap<String, UserNG> getAllUsers() {
		return cache.getAll_users();
	}
	
	public HashMap<String, GroupNG> getAllGroups() {
		return cache.getAll_groups();
	}
	
	public HashMap<String, RoleNG> getAllRoles() {
		return cache.getAll_roles();
	}
	
	private class Cache {
		private long last_groups_fetch_date;
		private long last_users_fetch_date;
		private long last_roles_fetch_date;
		private long ttl;
		private HashMap<String, UserNG> all_users;
		private HashMap<String, GroupNG> all_groups;
		private HashMap<String, RoleNG> all_roles;
		private ArrayList<String> all_domains;
		
		private AuthTurret referer;
		
		public Cache(AuthTurret referer, long ttl) {
			this.ttl = ttl;
			this.referer = referer;
			if (referer == null) {
				throw new NullPointerException("\"referer\" can't to be null");
			}
			
			last_groups_fetch_date = 0;
			last_users_fetch_date = 0;
			last_roles_fetch_date = 0;
			all_users = new HashMap<String, UserNG>();
			all_groups = new HashMap<String, GroupNG>();
			all_roles = new HashMap<String, RoleNG>();
			all_domains = new ArrayList<String>();
			
		}
		
		private synchronized void resetCache() {
			last_groups_fetch_date = 0;
			last_users_fetch_date = 0;
			last_roles_fetch_date = 0;
			
			all_users.clear();
			all_groups.clear();
			all_roles.clear();
			all_domains.clear();
		}
		
		public HashMap<String, GroupNG> getAll_groups() {
			if (last_groups_fetch_date + ttl < System.currentTimeMillis()) {
				synchronized (all_groups) {
					Loggers.Auth.debug("Do a Group cache refresh");
					last_groups_fetch_date = System.currentTimeMillis();
					
					try {
						CassandraDb.allRowsReader(CF_AUTH, new AllRowsFoundRow() {
							
							public void onFoundRow(Row<String, String> row) throws Exception {
								if (row.getKey().startsWith("group:") == false) {
									return;
								}
								if (all_groups.containsKey(row.getKey())) {
									all_groups.get(row.getKey()).loadFromDb(row.getColumns());
								} else {
									GroupNG group = new GroupNG(referer, row.getKey(), false);
									group.loadFromDb(row.getColumns());
									all_groups.put(row.getKey(), group);
								}
							}
						}, GroupNG.COLS_NAMES_LIMITED_TO_DB_IMPORT);
					} catch (ConnectionException e1) {
						onConnectionException(e1);
					} catch (Exception e) {
						Loggers.Auth.warn("Generic error during all rows", e);
					}
				}
			}
			return all_groups;
		}
		
		public HashMap<String, RoleNG> getAll_roles() {
			if (last_roles_fetch_date + ttl < System.currentTimeMillis()) {
				synchronized (all_roles) {
					Loggers.Auth.debug("Do a Role cache refresh");
					last_roles_fetch_date = System.currentTimeMillis();
					
					try {
						CassandraDb.allRowsReader(CF_AUTH, new AllRowsFoundRow() {
							
							public void onFoundRow(Row<String, String> row) throws Exception {
								if (row.getKey().startsWith("role:") == false) {
									return;
								}
								if (all_roles.containsKey(row.getKey())) {
									all_roles.get(row.getKey()).loadFromDb(row.getColumns());
								} else {
									RoleNG role = new RoleNG(referer, row.getKey(), false);
									role.loadFromDb(row.getColumns());
									all_roles.put(row.getKey(), role);
								}
							}
						}, RoleNG.COLS_NAMES_LIMITED_TO_DB_IMPORT);
					} catch (ConnectionException e1) {
						onConnectionException(e1);
					} catch (Exception e) {
						Loggers.Auth.warn("Generic error during all rows", e);
					}
				}
			}
			return all_roles;
		}
		
		public HashMap<String, UserNG> getAll_users() {
			if (last_users_fetch_date + ttl < System.currentTimeMillis()) {
				synchronized (all_users) {
					Loggers.Auth.debug("Do an User cache refresh");
					last_users_fetch_date = System.currentTimeMillis();
					all_domains.clear();
					try {
						CassandraDb.allRowsReader(CF_AUTH, new AllRowsFoundRow() {
							
							public void onFoundRow(Row<String, String> row) throws Exception {
								if (row.getKey().startsWith("user:") == false) {
									return;
								}
								UserNG user;
								if (all_users.containsKey(row.getKey())) {
									user = all_users.get(row.getKey());
									user.loadFromDb(row.getColumns());
								} else {
									user = new UserNG(referer, row.getKey(), false);
									user.loadFromDb(row.getColumns());
									all_users.put(row.getKey(), user);
								}
								user.getUserGroups().forEach(group -> {
									if (all_domains.contains(group) == false) {
										all_domains.add(group.getKey());
									}
								});
							}
						}, UserNG.COLS_NAMES_LIMITED_TO_DB_IMPORT);
					} catch (ConnectionException e1) {
						onConnectionException(e1);
					} catch (Exception e) {
						Loggers.Auth.warn("Generic error during all rows", e);
					}
				}
			}
			return all_users;
		}
		
		public ArrayList<String> getAll_domains() {
			getAll_users();
			return all_domains;
		}
		
	}
	
	public UserNG authenticate(String remote_address, String username, String password, String domain, String language) throws InvalidUserAuthentificationException {
		if (remote_address == null) {
			throw new NullPointerException("\"remote_address\" can't to be null");
		}
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null");
		}
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
		if (domain == null) {
			return authenticate(remote_address, username, password, language);
		}
		if (domain.trim().isEmpty()) {
			return authenticate(remote_address, username, password, language);
		}
		// TODO authenticate for domain
		/*try {
			authenticationUser = authenticator.getUser(username, password);
			if (authenticationUser != null) {
				Loggers.Auth.debug("Valid user found for this authentication method, username: " + username + ", " + authenticator);
				return authenticationUser;
			}
		} catch (IOException e) {
			Loggers.Auth.error("Invalid authentication method: " + username + ", " + authenticator, e);
		}*/
		
		// TODO if user don't exists in db, add it (w/o password) in new_user group
		// TODO sync user long name, email, groups, and last-edit if user is from AD
		// TODO set user.doLoginOperations(remote_address, language) + save
		
		return null;
	}
	
	public UserNG authenticate(String remote_address, String username, String password, String language) throws InvalidUserAuthentificationException {
		// TODO authenticate for each domain
		/*for (int pos = 0; pos < authenticators.size(); pos++) {
			try {
				
				authenticationUser = authenticate(authenticators.get(pos), username, password);
				if (authenticationUser != null) {
					return authenticationUser;
				}
			} catch (InvalidAuthenticatorUserException e) {
				Loggers.Auth.debug("Invalid user for this authentication method, authenticator: " + authenticators.get(pos), e);
			}
		}
		throw new InvalidAuthenticatorUserException("Can't authenticate with " + username);*/
		return null;
	}
	
	/**
	 * Create + save
	 */
	public UserNG createUser(String login, String domain) throws ConnectionException {
		UserNG newuser = new UserNG(this, login, domain);
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		newuser.save(mutator.withRow(CF_AUTH, newuser.getKey()));
		mutator.execute();
		cache.resetCache();
		return newuser;
	}
	
	/**
	 * Create + save
	 */
	public GroupNG createGroup(String group_name) throws ConnectionException {
		GroupNG newgroup = new GroupNG(group_name);
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		newgroup.save(mutator.withRow(CF_AUTH, newgroup.getKey()));
		mutator.execute();
		cache.resetCache();
		return newgroup;
	}
	
	/**
	 * Create + save
	 */
	public RoleNG createRole(String role_name) throws ConnectionException {
		RoleNG newrole = new RoleNG(role_name);
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		newrole.save(mutator.withRow(CF_AUTH, newrole.getKey()));
		mutator.execute();
		cache.resetCache();
		return newrole;
	}
	
	/**
	 * Just save
	 */
	public void saveAll(ArrayList<AuthEntry> items) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		items.forEach(item -> {
			item.save(mutator.withRow(CF_AUTH, item.getKey()));
		});
		mutator.execute();
		cache.resetCache();
	}
	
	/**
	 * Juste delete
	 */
	public void deleteAll(List<AuthEntry> items) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		items.forEach(item -> {
			item.delete(mutator.withRow(CF_AUTH, item.getKey()));
		});
		mutator.execute();
		cache.resetCache();
	}
	
}
