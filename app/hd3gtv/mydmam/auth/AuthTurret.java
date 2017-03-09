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
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.gson.JsonParser;
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
import hd3gtv.mydmam.auth.ActiveDirectoryBackend.ADUser;
import hd3gtv.mydmam.auth.asyncjs.GroupChRole;
import hd3gtv.mydmam.auth.asyncjs.NewUser;
import hd3gtv.mydmam.auth.asyncjs.RoleChPrivileges;
import hd3gtv.mydmam.auth.asyncjs.UserAdminUpdate;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.web.PrivilegeNG;
import hd3gtv.tools.BreakReturnException;
import play.Play;

public class AuthTurret {
	
	static final ColumnFamily<String, String> CF_AUTH = new ColumnFamily<String, String>("mgrAuth", StringSerializer.get(), StringSerializer.get());
	
	final JsonParser parser = new JsonParser();
	private Password password;
	private boolean force_select_domain;
	
	private Keyspace keyspace;
	private Cache cache;
	
	private LinkedHashMap<String, ActiveDirectoryBackend> auth_backend_by_domain;
	
	private GroupNG default_newusers_group;
	
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
		 * Password API init
		 */
		String master_password_key = Configuration.global.getValue("play", "master_password_key", "");
		if (master_password_key.equals("")) {
			throw new NullPointerException("No auth.master_password_key are definited");
		}
		password = new Password(master_password_key);
		
		if (Configuration.global.isElementKeyExists("play", "backend")) {
			throw new IOException("Invalid configuration: auth.backend is present, instead of auth.backends, with a new syntax.");
		}
		
		/**
		 * Auth backend init
		 */
		auth_backend_by_domain = new LinkedHashMap<String, ActiveDirectoryBackend>(1);
		
		if (Configuration.global.isElementKeyExists("play", "backends")) {
			List<LinkedHashMap<String, ?>> conf_backends = Configuration.global.getListMapValues("play", "backends");
			conf_backends.forEach(item -> {
				String source = (String) item.get("source");
				if (source.equalsIgnoreCase("ad") == false) {
					Loggers.Auth.warn("Invalid auth.backends.source conf: only \"ad\" source is avaliable");
					return;
				}
				String server = (String) item.get("server");
				String domain = (String) item.get("domain");
				Integer port = (Integer) item.get("port");
				ActiveDirectoryBackend adb = new ActiveDirectoryBackend(domain, server, port);
				auth_backend_by_domain.put(domain, adb);
				if (Loggers.Auth.isDebugEnabled()) {
					Loggers.Auth.debug("AD configuration loaded: " + adb.toString());
				}
			});
		}
		
		/**
		 * Internal utils init
		 */
		cache = new Cache(this, TimeUnit.MINUTES.toMillis(Configuration.global.getValue("play", "cache_ttl", 10)));
		
		force_select_domain = Configuration.global.getValueBoolean("play", "force_select_domain");
		
		/**
		 * Check account_export file
		 */
		File account_export = new File(Configuration.getGlobalConfigurationDirectory().getParent() + File.separator + "account_export.xml");
		if (account_export.exists()) {
			Loggers.Auth.warn("You should remove account_export file... (" + account_export.getAbsolutePath() + ")");
		}
		
		/**
		 * Check "play-new-password.txt" file
		 */
		File playnewpassword = new File("play-new-password.txt");
		if (playnewpassword.exists()) {
			Loggers.Auth.warn("You should remove play-new-password file... (" + playnewpassword.getAbsolutePath() + ")");
		}
		
		default_newusers_group = new GroupNG(this, "New users");
		if (Play.initialized) {
			/**
			 * Peuplate DB Default users
			 */
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			
			/** Create admin role if needed */
			RoleNG default_admin_role = new RoleNG(this, "All privileges");
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
					Loggers.Auth.info("Admin role not containt all and same privileges, update it. " + admin_role.getPrivileges() + " / " + PrivilegeNG.getAllPrivilegesName());
					admin_role.update(PrivilegeNG.getAllPrivilegesName());
					admin_role.save(mutator.withRow(CF_AUTH, admin_role.getKey()));
					cache.all_roles.put(admin_role.getKey(), admin_role);
				}
			}
			
			/**
			 * Create guest role if needed
			 */
			RoleNG default_guest_role = new RoleNG(this, "Default");
			if (getByRoleKey(default_guest_role.getKey()) == null) {
				Loggers.Auth.info("Default role is absent, create it.");
				default_guest_role.save(mutator.withRow(CF_AUTH, default_guest_role.getKey()));
				cache.all_roles.put(default_guest_role.getKey(), default_guest_role);
			}
			
			/**
			 * Create admin group if needed
			 */
			GroupNG default_admin_group = new GroupNG(this, "Administrators");
			if (getByGroupKey(default_admin_group.getKey()) == null) {
				Loggers.Auth.info("Admin group is absent, create it.");
				default_admin_group.update(Arrays.asList(admin_role));
				default_admin_group.save(mutator.withRow(CF_AUTH, default_admin_group.getKey()));
				cache.all_groups.put(default_admin_group.getKey(), default_admin_group);
			} else {
				default_admin_group = getByGroupKey(default_admin_group.getKey());
			}
			
			/**
			 * Create newusers group if needed
			 */
			if (getByGroupKey(default_newusers_group.getKey()) == null) {
				Loggers.Auth.info("Admin group is absent, create it.");
				default_newusers_group.update(Arrays.asList(getByRoleKey(default_guest_role.getKey())));
				default_newusers_group.save(mutator.withRow(CF_AUTH, default_newusers_group.getKey()));
				cache.all_groups.put(default_newusers_group.getKey(), default_newusers_group);
			} else {
				default_newusers_group = getByGroupKey(default_newusers_group.getKey());
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
		} else {
			Loggers.Auth.debug("Play is not running, skip create default accounts.");
		}
	}
	
	static String makeKey(String type, String name) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(name.getBytes("UTF-8"));
			return type + ":" + MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
		} catch (UnsupportedEncodingException e) {
		}
		return null;
	}
	
	Cache getCache() {
		return cache;
	}
	
	public String resetAdminPassword() throws ConnectionException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
		UserNG default_admin_user = getByUserKey((new UserNG(this, "admin", "local").getKey()));
		if (default_admin_user == null) {
			Loggers.Auth.error("Admin user is absent, please restart a Play instance for recreate it.");
		}
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		Loggers.Auth.info("Reset Admin password.");
		
		String new_password = createAdminPasswordTextFile("admin");
		default_admin_user.chpassword(new_password);
		default_admin_user.setLocked_account(false);
		default_admin_user.updateLastEditTime();
		default_admin_user.save(mutator.withRow(CF_AUTH, default_admin_user.getKey()));
		
		cache.all_users.put(default_admin_user.getKey(), default_admin_user);
		mutator.execute();
		
		return new_password;
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
	
	/*public HashSet<String> getPrivilegesByRoleList(ArrayList<RoleNG> roles) {
		HashSet<String> result = new HashSet<String>();
		
		roles.forEach(role -> {
			role.getPrivileges().forEach(privilege -> {
				result.add(privilege);
			});
		});
		
		return result;
	}*/
	
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
		return force_select_domain;
	}
	
	public ArrayList<String> getDeclaredDomainList() {
		return cache.getAll_domains();
	}
	
	public UserNG getByUserKey(String user_key) {
		if (Loggers.Auth.isTraceEnabled()) {
			Loggers.Auth.trace("getByUserKey: " + user_key);
		}
		
		if (cache.getAll_users().containsKey(user_key) == false) {
			return null;
		}
		return cache.getAll_users().get(user_key);
	}
	
	public GroupNG getByGroupKey(String group_key) {
		if (cache.getAll_groups().containsKey(group_key) == false) {
			return null;
		}
		return cache.getAll_groups().get(group_key);
	}
	
	public RoleNG getByRoleKey(String role_key) {
		if (cache.getAll_roles().containsKey(role_key) == false) {
			return null;
		}
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
	
	class Cache {
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
			all_domains.add("local");
			auth_backend_by_domain.forEach((domain, auth) -> {
				all_domains.add(domain);
			});
			if (Loggers.Auth.isDebugEnabled()) {
				Loggers.Auth.debug("Init cache, ttl: " + ttl + ", domains: " + all_domains);
			}
		}
		
		private synchronized void resetCache() {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("Reset db cache");
			}
			last_users_fetch_date = 0;
			last_groups_fetch_date = 0;
			last_roles_fetch_date = 0;
			
			all_users.clear();
			all_groups.clear();
			all_roles.clear();
		}
		
		void updateManuallyCache(UserNG user) {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("updateManuallyCache db for: " + user.getKey());
			}
			all_users.put(user.getKey(), user);
		}
		
		void updateManuallyCache(GroupNG group) {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("updateManuallyCache db for: " + group.getKey());
			}
			last_users_fetch_date = 0;
			all_users.clear();
			all_groups.put(group.getKey(), group);
		}
		
		void updateManuallyCache(RoleNG role) {
			if (Loggers.Auth.isTraceEnabled()) {
				Loggers.Auth.trace("updateManuallyCache db for: " + role.getKey());
			}
			
			last_users_fetch_date = 0;
			all_users.clear();
			last_groups_fetch_date = 0;
			all_groups.clear();
			all_roles.put(role.getKey(), role);
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
								GroupNG group;
								if (all_groups.containsKey(row.getKey())) {
									group = all_groups.get(row.getKey());
									group.loadFromDb(row.getColumns());
								} else {
									group = new GroupNG(referer, row.getKey(), false);
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
						MutationBatch mutator = CassandraDb.prepareMutationBatch();
						ArrayList<String> privileges_to_remove = new ArrayList<String>(1);
						
						CassandraDb.allRowsReader(CF_AUTH, new AllRowsFoundRow() {
							
							public void onFoundRow(Row<String, String> row) throws Exception {
								if (row.getKey().startsWith("role:") == false) {
									return;
								}
								RoleNG role;
								if (all_roles.containsKey(row.getKey())) {
									role = all_roles.get(row.getKey());
									role.loadFromDb(row.getColumns());
								} else {
									role = new RoleNG(referer, row.getKey(), false);
									role.loadFromDb(row.getColumns());
									all_roles.put(row.getKey(), role);
								}
								
								/**
								 * Check & remove missing privileges
								 */
								if (role.getPrivileges().containsAll(PrivilegeNG.getAllPrivilegesName()) == false) {
									privileges_to_remove.clear();
									role.getPrivileges().forEach(v -> {
										if (PrivilegeNG.getAllPrivilegesName().contains(v) == false) {
											privileges_to_remove.add(v);
										}
									});
									if (privileges_to_remove.isEmpty() == false) {
										Loggers.Auth.info("Remove old privileges from \"" + role + "\": " + privileges_to_remove);
										privileges_to_remove.forEach(v -> {
											role.getPrivileges().remove(v);
										});
										role.save(mutator.withRow(CF_AUTH, role.getKey()));
									}
								}
							}
						}, RoleNG.COLS_NAMES_LIMITED_TO_DB_IMPORT);
						
						if (mutator.isEmpty() == false) {
							mutator.execute();
						}
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
			return all_domains;
		}
		
	}
	
	public UserNG authenticateWithThisDomain(String remote_address, String username, String clear_text_password, String domain, String language) throws ConnectionException {
		if (remote_address == null) {
			throw new NullPointerException("\"remote_address\" can't to be null");
		}
		if (username == null) {
			throw new NullPointerException("\"username\" can't to be null");
		}
		if (clear_text_password == null) {
			throw new NullPointerException("\"clear_text_password\" can't to be null");
		}
		if (domain == null) {
			return authenticate(remote_address, username, clear_text_password, language);
		}
		if (domain.trim().isEmpty()) {
			return authenticate(remote_address, username, clear_text_password, language);
		}
		
		if (auth_backend_by_domain.containsKey(domain)) {
			return syncADUser(auth_backend_by_domain.get(domain).getUser(username, clear_text_password), remote_address, language);
		} else if (domain.equalsIgnoreCase("local")) {
			UserNG result = getByUserKey(UserNG.computeUserKey(username, "local"));
			if (result == null) {
				if (force_select_domain) {
					Loggers.Auth.warn("Can't found this user to local auth system " + username + "@" + domain);
				}
				return null;
			}
			if (result.isLockedAccount()) {
				Loggers.Auth.warn("This user has its account locked in database " + result.getKey());
				return null;
			}
			if (result.checkValidPassword(clear_text_password) == false) {
				Loggers.Auth.warn("Invalid password proposed for " + result.getKey());
				return null;
			}
			result.doLoginOperations(remote_address, language);
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			result.save(mutator.withRow(CF_AUTH, result.getKey()));
			mutator.execute();
			return result;
		} else {
			Loggers.Auth.warn("Unknow domain name for auth " + domain);
		}
		
		return null;
	}
	
	/**
	 * Import an backend extracted user to MyDMAM UserNG system.
	 * If user don't exists in db, add it (w/o password) in new_user group
	 * Sync user long name, email, groups, and last-edit if user is from AD
	 */
	private UserNG syncADUser(ADUser aduser, String remote_address, String language) throws ConnectionException {
		if (aduser == null) {
			return null;
		}
		UserNG user = getByUserKey(UserNG.computeUserKey(aduser.username, aduser.getDomain()));
		final ArrayList<Boolean> need_to_reset_cache = new ArrayList<>(1);
		if (user == null) {
			user = new UserNG(this, aduser.username, aduser.getDomain());
			user.postCreate(aduser.commonname, aduser.mail, language);
			user.getUserGroups().add(default_newusers_group);
			need_to_reset_cache.add(true);
		} else if (user.isLockedAccount()) {
			Loggers.Auth.warn("This user has its account locked in database " + user);
			return null;
		}
		
		final UserNG result = user;
		result.doLoginOperations(remote_address, language);
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		if (aduser.organizational_units != null) {
			StringBuilder current = new StringBuilder();
			
			for (int pos = aduser.organizational_units.size() - 1; pos > -1; pos--) {
				String ou = aduser.organizational_units.get(pos);
				
				if (ou.trim().isEmpty()) {
					continue;
				}
				GroupNG current_backend_group = new GroupNG(this, current.toString() + ou);
				if (getByGroupKey(current_backend_group.getKey()) == null) {
					current_backend_group.save(mutator.withRow(CF_AUTH, current_backend_group.getKey()));
					need_to_reset_cache.add(true);
				} else {
					current_backend_group = getByGroupKey(current_backend_group.getKey());
				}
				
				if (result.getUserGroups().contains(current_backend_group) == false) {
					result.getUserGroups().add(current_backend_group);
					need_to_reset_cache.add(true);
				}
				current.append(ou);
				current.append("/");
			}
		}
		
		result.save(mutator.withRow(CF_AUTH, result.getKey()));
		mutator.execute();
		
		if (need_to_reset_cache.isEmpty() == false) {
			cache.resetCache();
		}
		
		return result;
	}
	
	public UserNG authenticate(String remote_address, String username, String clear_text_password, String language) throws ConnectionException {
		/**
		 * Try to authenticate with "local" domain
		 */
		UserNG result = authenticateWithThisDomain(remote_address, username, clear_text_password, "local", language);
		
		if (result != null) {
			return result;
		}
		
		try {
			auth_backend_by_domain.forEach((domain, auth) -> {
				/**
				 * Try to log with any backends
				 */
				ADUser thisresult = auth.getUser(username, clear_text_password);
				if (thisresult != null) {
					throw new BreakReturnException(thisresult);
				}
			});
		} catch (BreakReturnException e) {
			return syncADUser(e.get(ADUser.class), remote_address, language);
		}
		
		return result;
	}
	
	public UserNG createUserIfNotExists(NewUser request) throws ConnectionException, IndexOutOfBoundsException, AddressException {
		UserNG user = getByUserKey(UserNG.computeUserKey(request.login, request.domain));
		if (user != null) {
			throw new IndexOutOfBoundsException("User " + request.login + "@" + request.domain + " exists");
		}
		
		final UserNG newuser = new UserNG(this, request.login, request.domain);
		if (request.password != null && request.domain.equals("local")) {
			newuser.chpassword(request.password);
		}
		
		String mail = null;
		if (request.email_addr != null) {
			mail = new InternetAddress(request.email_addr).getAddress();
		}
		newuser.postCreate(request.fullname, mail, Locale.getDefault().getLanguage());
		newuser.setLocked_account(request.locked_account);
		
		if (request.user_groups != null) {
			request.user_groups.forEach(group_key -> {
				GroupNG group = getByGroupKey(group_key);
				if (group != null) {
					newuser.getUserGroups().add(group);
				}
			});
		}
		
		Loggers.Auth.info("Create new user: " + newuser.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		newuser.save(mutator.withRow(CF_AUTH, newuser.getKey()));
		mutator.execute();
		cache.updateManuallyCache(newuser);
		return newuser;
	}
	
	public UserNG changeAdminUserPasswordGroups(UserAdminUpdate request) throws ConnectionException {
		UserNG user = getByUserKey(request.user_key);
		if (user == null) {
			throw new NullPointerException("User " + request.user_key + " don't exists");
		}
		if (request.new_password != null) {
			if (request.new_password.equals("") == false) {
				user.chpassword(request.new_password);
				Loggers.Auth.info("Change password for user: " + user.toString());
			}
		}
		
		if (request.user_groups != null) {
			user.getUserGroups().clear();
			request.user_groups.forEach(group_key -> {
				GroupNG group = getByGroupKey(group_key);
				if (group != null) {
					user.getUserGroups().add(group);
				}
			});
			Loggers.Auth.info("Change user groups: " + user.toString());
		}
		
		if (request.properties != null) {
			StringReader sr = new StringReader(request.properties);
			try {
				user.getProperties().load(sr);
				Loggers.Auth.info("Change user properties: " + user.toString());
			} catch (IOException e) {
				Loggers.Auth.error("Can't change user " + user.toString() + " properties");
			}
		}
		user.updateLastEditTime();
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		user.save(mutator.withRow(CF_AUTH, user.getKey()));
		mutator.execute();
		cache.updateManuallyCache(user);
		return user;
	}
	
	public UserNG changeUserToogleLock(String key) throws ConnectionException {
		UserNG user = getByUserKey(key);
		if (user == null) {
			throw new NullPointerException("User " + key + " don't exists");
		}
		user.setLocked_account(user.isLockedAccount() == false);
		user.updateLastEditTime();
		
		Loggers.Auth.info("Toogle user lock: " + user.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		user.save(mutator.withRow(CF_AUTH, user.getKey()));
		mutator.execute();
		cache.updateManuallyCache(user);
		return user;
	}
	
	public UserNG changeUserMail(String user_key, String email_addr) throws ConnectionException, AddressException {
		UserNG user = getByUserKey(user_key);
		if (user == null) {
			throw new NullPointerException("User " + user_key + " don't exists");
		}
		String mail = null;
		if (email_addr != null) {
			mail = new InternetAddress(email_addr).getAddress();
		}
		user.setEmailAddr(mail);
		user.updateLastEditTime();
		
		Loggers.Auth.info("Change user mail: " + user.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		user.save(mutator.withRow(CF_AUTH, user.getKey()));
		mutator.execute();
		cache.updateManuallyCache(user);
		return user;
	}
	
	public GroupNG createGroup(String group_name) throws ConnectionException {
		GroupNG newgroup = new GroupNG(this, group_name);
		
		Loggers.Auth.info("Create group: " + newgroup.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		newgroup.save(mutator.withRow(CF_AUTH, newgroup.getKey()));
		mutator.execute();
		cache.updateManuallyCache(newgroup);
		return newgroup;
	}
	
	public GroupNG changeGroupRoles(GroupChRole request) throws ConnectionException {
		GroupNG group = getByGroupKey(request.group_key);
		if (group == null) {
			throw new NullPointerException("Group " + request.group_key + " don't exists");
		}
		if (request.group_roles == null) {
			return group;
		}
		
		group.getGroupRoles().clear();
		request.group_roles.forEach(role_key -> {
			RoleNG role = getByRoleKey(role_key);
			if (role != null) {
				group.getGroupRoles().add(role);
			}
		});
		
		Loggers.Auth.info("Change roles for a group: " + group.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		group.save(mutator.withRow(CF_AUTH, group.getKey()));
		mutator.execute();
		cache.updateManuallyCache(group);
		return group;
	}
	
	public RoleNG changeRolePrivileges(RoleChPrivileges request) throws ConnectionException {
		RoleNG role = getByRoleKey(request.role_key);
		if (role == null) {
			throw new NullPointerException("Role " + request.role_key + " don't exists");
		}
		if (request.privileges == null) {
			return role;
		}
		
		role.getPrivileges().clear();
		request.privileges.forEach(privilege -> {
			role.getPrivileges().add(privilege);
		});
		
		Loggers.Auth.info("Change privileges for a role: " + role.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		role.save(mutator.withRow(CF_AUTH, role.getKey()));
		mutator.execute();
		cache.updateManuallyCache(role);
		return role;
	}
	
	public RoleNG createRole(String role_name) throws ConnectionException {
		RoleNG newrole = new RoleNG(this, role_name);
		
		Loggers.Auth.info("Save new role: " + newrole.toString());
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		newrole.save(mutator.withRow(CF_AUTH, newrole.getKey()));
		mutator.execute();
		cache.updateManuallyCache(newrole);
		return newrole;
	}
	
	/**
	 * Just save
	 */
	public void saveAll(ArrayList<AuthEntry> items) throws ConnectionException {
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		items.forEach(item -> {
			if (Loggers.Auth.isDebugEnabled()) {
				Loggers.Auth.debug("Save: " + item.toString());
			}
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
			if (Loggers.Auth.isDebugEnabled()) {
				Loggers.Auth.debug("Delete: " + item.toString());
			}
			item.delete(mutator.withRow(CF_AUTH, item.getKey()));
		});
		mutator.execute();
		cache.resetCache();
	}
	
}
