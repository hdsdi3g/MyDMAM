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

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceActionReceiver;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.storage.StorageLocalFile;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.FreeDiskSpaceWarningException;

public class FTPGroup implements InstanceActionReceiver, InstanceStatusItem {
	
	private static LinkedHashMap<String, FTPGroup> declared_groups;
	
	private static HashMap<String, Long> declared_groups_ttl_trash;
	private static HashMap<String, Long> declared_groups_ttl_purge;
	private static HashMap<String, Boolean> declared_groups_expiration_based_on_last_activity;
	
	static {
		declared_groups = new LinkedHashMap<String, FTPGroup>();
		declared_groups_ttl_trash = new HashMap<String, Long>();
		declared_groups_ttl_purge = new HashMap<String, Long>();
		declared_groups_expiration_based_on_last_activity = new HashMap<String, Boolean>();
		
		if (Configuration.global.isElementExists("ftpservergroups")) {
			HashMap<String, ConfigurationItem> all_groups_confs = Configuration.global.getElement("ftpservergroups");
			
			String group_name;
			FTPGroup group;
			for (Map.Entry<String, ConfigurationItem> entry : all_groups_confs.entrySet()) {
				group_name = entry.getKey();
				try {
					group = new FTPGroup(group_name, all_groups_confs);
					declared_groups.put(group_name, group);
					
					if (group.account_expiration_trash_duration > 0) {
						declared_groups_ttl_trash.put(group_name, (long) group.account_expiration_trash_duration * 1000);
						declared_groups_ttl_purge.put(group_name, (long) group.account_expiration_purge_duration * 1000);
						declared_groups_expiration_based_on_last_activity.put(group_name, group.account_expiration_based_on_last_activity);
					}
				} catch (Exception e) {
					Loggers.FTPserver.error("Can't import group declaration (" + group_name + " ) from configuration", e);
				}
			}
		}
	}
	
	public String toString() {
		return name;
	}
	
	static LinkedHashMap<String, FTPGroup> getDeclaredGroups() {
		return declared_groups;
	}
	
	static HashMap<String, Long> getDeclaredGroupsTTLTrash() {
		return declared_groups_ttl_trash;
	}
	
	static HashMap<String, Long> getDeclaredGroupsTTLPurge() {
		return declared_groups_ttl_purge;
	}
	
	static HashMap<String, Boolean> getDeclaredGroupsExpirationBasedOnLastActivity() {
		return declared_groups_expiration_based_on_last_activity;
	}
	
	static void registerAppManager(AppManager manager) {
		for (FTPGroup group : declared_groups.values()) {
			manager.registerInstanceActionReceiver(group);
			manager.getInstanceStatus().registerInstanceStatusItem(group);
		}
	}
	
	public static FTPGroup getFromName(String group_name) {
		return declared_groups.get(group_name);
	}
	
	private String name;
	private boolean disabled_group;
	
	/**
	 * Always purge_duration >= trash_duration, in sec
	 */
	private int account_expiration_trash_duration;
	
	/**
	 * Always purge_duration >= trash_duration, in sec
	 */
	private int account_expiration_purge_duration;
	
	private boolean account_expiration_based_on_last_activity;
	
	private File base_working_dir;
	private long min_disk_space_before_warn;
	private long min_disk_space_before_stop;
	
	/**
	 * Set null for disable.
	 * If domain_isolation, will index like storagename:/domain/userName
	 * Else, will index like storagename:/domain#userName or storagename:/userName if no domain
	 */
	private String pathindex_storagename;
	
	/**
	 * Set true for set user storage like /base_working_dir/domain/userName
	 * Set false for indexing ftp content like /base_working_dir/domain#userName
	 * If no domain, user storage will be like /base_working_dir/userName
	 */
	private boolean domain_isolation;
	
	private boolean short_activity_log;
	
	private File trash_directory;
	
	private ArrayList<String> users_no_activity_log;
	
	private FTPGroup(String group_name, HashMap<String, ConfigurationItem> all_groups_confs) throws Exception {
		name = group_name;
		disabled_group = Configuration.getValueBoolean(all_groups_confs, group_name, "disabled");
		account_expiration_trash_duration = Configuration.getValue(all_groups_confs, group_name, "account_expiration_trash_duration", 0);
		account_expiration_purge_duration = Configuration.getValue(all_groups_confs, group_name, "account_expiration_purge_duration", 0);
		if (account_expiration_purge_duration < account_expiration_trash_duration) {
			account_expiration_purge_duration = account_expiration_trash_duration;
			Loggers.FTPserver.warn("FTP group definition: account_expiration_purge_duration can't < account_expiration_trash_duration, for " + group_name);
		}
		account_expiration_based_on_last_activity = Configuration.getValueBoolean(all_groups_confs, group_name, "account_expiration_based_on_last_activity");
		base_working_dir = new File(Configuration.getValue(all_groups_confs, group_name, "base_working_dir", ""));
		
		if (base_working_dir.getPath().equalsIgnoreCase(new File("").getPath())) {
			throw new FileNotFoundException("\"base_working_dir\" is no set in configuration");
		}
		if (base_working_dir.exists()) {
			CopyMove.checkExistsCanRead(base_working_dir);
			CopyMove.checkIsDirectory(base_working_dir);
			CopyMove.checkIsWritable(base_working_dir);
		} else {
			FileUtils.forceMkdir(base_working_dir);
		}
		
		users_no_activity_log = Configuration.getValues(all_groups_confs, group_name, "no_activity_log", null);
		if (users_no_activity_log == null) {
			users_no_activity_log = new ArrayList<>();
		}
		
		min_disk_space_before_warn = Configuration.getValue(all_groups_confs, group_name, "min_disk_space_before_warn", 0);
		min_disk_space_before_stop = Configuration.getValue(all_groups_confs, group_name, "min_disk_space_before_stop", 0);
		pathindex_storagename = Configuration.getValue(all_groups_confs, group_name, "pathindex_storagename", null);
		domain_isolation = Configuration.getValueBoolean(all_groups_confs, group_name, "domain_isolation");
		short_activity_log = Configuration.getValueBoolean(all_groups_confs, group_name, "short_activity_log");
		trash_directory = new File(Configuration.getValue(all_groups_confs, group_name, "trash_directory", base_working_dir.getAbsolutePath() + File.separator + "_trash"));
		
		if (trash_directory.exists()) {
			CopyMove.checkExistsCanRead(trash_directory);
			CopyMove.checkIsDirectory(trash_directory);
			CopyMove.checkIsWritable(trash_directory);
		} else {
			FileUtils.forceMkdir(trash_directory);
		}
		
		if (pathindex_storagename != null) {
			Storage.registerStorage(new StorageLocalFile(base_working_dir, pathindex_storagename, false, 3600));
		}
		
	}
	
	private File makeUserHomeDirectoryPath(String user_name, String user_domain) {
		if (user_name == null) {
			throw new NullPointerException("\"user_name\" can't to be null");
		}
		if (user_domain == null) {
			throw new NullPointerException("\"user_domain\" can't to be null");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(base_working_dir.getAbsolutePath());
		sb.append(File.separator);
		if (user_domain.equals("") == false) {
			if (domain_isolation) {
				sb.append("#");
				sb.append(user_domain);
				sb.append(File.separator);
			} else {
				sb.append(user_domain);
				sb.append("#");
			}
		}
		sb.append(user_name);
		return new File(sb.toString());
	}
	
	/**
	 * Create it if needed.
	 */
	File getUserHomeDirectory(String user_name, String user_domain) throws NullPointerException, IOException {
		File home_directory = makeUserHomeDirectoryPath(user_name, user_domain);
		
		if (home_directory.exists()) {
			CopyMove.checkExistsCanRead(home_directory);
			CopyMove.checkIsDirectory(home_directory);
			CopyMove.checkIsWritable(home_directory);
		} else {
			Loggers.FTPserver.debug("Create user home directory: " + home_directory);
			FileUtils.forceMkdir(home_directory);
		}
		return home_directory;
	}
	
	void trashUserHomeDirectory(FTPUser user) throws NullPointerException, IOException {
		File home_directory = makeUserHomeDirectoryPath(user.getName(), user.getDomain());
		if (home_directory.exists() == false) {
			return;
		}
		File dest = new File(trash_directory.getAbsolutePath() + File.separator + home_directory.getName() + File.separator + Loggers.dateFilename(System.currentTimeMillis()));
		FileUtils.moveDirectory(home_directory, dest);
	}
	
	void deleteUserHomeDirectory(FTPUser user) throws NullPointerException, IOException {
		if (user.getName() == null) {
			throw new NullPointerException("\"user.getName()\" can't to be null");
		}
		if (user.getDomain() == null) {
			throw new NullPointerException("\"user.getDomain()\" can't to be null");
		}
		
		File home_directory = makeUserHomeDirectoryPath(user.getName(), user.getDomain());
		if (home_directory.exists()) {
			FileUtils.forceDelete(home_directory);
			return;
		}
		File user_trash = new File(trash_directory.getAbsolutePath() + File.separator + home_directory.getName());
		if (user_trash.exists()) {
			FileUtils.forceDelete(user_trash);
			return;
		}
		
		Loggers.FTPserver.warn("Can't found \"" + home_directory + "\" or \"" + user_trash + "\" for delete user (" + user + ") home directory");
	}
	
	/**
	 * @return simple user.
	 */
	private FTPUser getExpiredUserFromAnUserDir(File user_content_dir) throws IOException {
		String group_name = name;
		String user_name = null;
		String domain = null;
		
		if (domain_isolation) {
			if (base_working_dir.getAbsolutePath().equals(user_content_dir.getParentFile().getParentFile().getAbsolutePath()) == false) {
				throw new IOException("Invalid parent path, base_working_dir: " + base_working_dir + ", user_content_dir: " + user_content_dir);
			}
			
			user_name = user_content_dir.getName();
			domain = user_content_dir.getParentFile().getName();
			if (domain.indexOf("#") == 0) {
				domain = domain.substring(1);
			} else {
				Loggers.FTPserver.warn("Invalid file name for an user_content_dir: " + user_content_dir);
			}
		} else {
			if (base_working_dir.getAbsolutePath().equals(user_content_dir.getParentFile().getAbsolutePath()) == false) {
				throw new IOException("Invalid parent path, base_working_dir: " + base_working_dir + ", user_content_dir: " + user_content_dir);
			}
			
			String dirname = user_content_dir.getName();
			int pos_hash = dirname.indexOf("#");
			user_name = dirname;
			domain = "";
			
			if (pos_hash > -1) {
				domain = dirname.substring(0, pos_hash);
				user_name = dirname.substring(pos_hash + 1);
			}
		}
		return FTPUser.createSimpleUser(group_name, user_name, domain);
	}
	
	private ValidUserDirsFileFilter valid_user_dirs = new ValidUserDirsFileFilter();
	
	private class ValidUserDirsFileFilter implements FileFilter {
		public boolean accept(File pathname) {
			if (pathname.isDirectory() == false) {
				return false;
			}
			if (pathname.getAbsolutePath().equals(trash_directory.getAbsolutePath())) {
				return false;
			}
			return CopyMove.isHidden(pathname) == false;
		}
	}
	
	private void listAllActualUsersInDomain(ArrayList<FTPUser> users, String domain) throws IOException {
		File[] all_users_files = new File(base_working_dir + File.separator + "#" + domain).listFiles(valid_user_dirs);
		for (int pos = 0; pos < all_users_files.length; pos++) {
			users.add(getExpiredUserFromAnUserDir(all_users_files[pos]));
		}
	}
	
	List<FTPUser> listAllActualUsers() throws IOException {
		ArrayList<FTPUser> users = new ArrayList<FTPUser>();
		
		File[] all_users_files = base_working_dir.listFiles(valid_user_dirs);
		
		File dir;
		for (int pos = 0; pos < all_users_files.length; pos++) {
			dir = all_users_files[pos];
			if (dir.getName().startsWith("#")) {
				/**
				 * It's a domain dir, search to sub dir
				 */
				listAllActualUsersInDomain(users, dir.getName().substring(1));
			} else {
				/**
				 * It's an user
				 */
				users.add(getExpiredUserFromAnUserDir(dir));
			}
			
		}
		return users;
	}
	
	synchronized boolean isDisabled() {
		return disabled_group;
	}
	
	String getName() {
		return name;
	}
	
	String getPathindexStoragename() {
		return pathindex_storagename;
	}
	
	public boolean isShortActivityLog() {
		return short_activity_log;
	}
	
	private transient long last_free_space;
	
	/**
	 * Check only if group is enabled.
	 */
	void checkFreeSpace() {
		if (disabled_group) {
			return;
		}
		long actual_free_space = 0;
		try {
			if (min_disk_space_before_warn > 0) {
				if (min_disk_space_before_stop > 0) {
					actual_free_space = FreeDiskSpaceWarningException.check(base_working_dir, min_disk_space_before_warn, min_disk_space_before_stop);
				} else {
					actual_free_space = FreeDiskSpaceWarningException.check(base_working_dir, min_disk_space_before_warn);
				}
			} else if (min_disk_space_before_stop > 0) {
				actual_free_space = FreeDiskSpaceWarningException.check(base_working_dir, min_disk_space_before_stop, min_disk_space_before_stop);
			}
		} catch (FreeDiskSpaceWarningException fdswe) {
			Loggers.FTPserver.warn("Check storage free space for group " + name, fdswe);
			if (last_free_space <= actual_free_space) {
				/**
				 * More free space, don't alert.
				 */
				AdminMailAlert.create("Check storage free space for group " + name, false).setThrowable(fdswe).send();
			}
		} catch (IOException ioe) {
			Loggers.FTPserver.error("Not enough free space: disable group " + name, ioe);
			AdminMailAlert.create("Not enough free space: disable group " + name, false).setThrowable(ioe).send();
			disabled_group = true;
		}
		last_free_space = actual_free_space;
		Loggers.FTPserver.debug("Do a check free space for group: " + this + ", " + (last_free_space / (1024 * 1024)) + " Mb remaining.");
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return FTPGroup.class;
	}
	
	public Class<? extends InstanceActionReceiver> getClassToCallback() {
		return FTPGroup.class;
	}
	
	public String getReferenceKey() {
		return name;
	}
	
	public void doAnAction(JsonObject order) throws Exception {
		if (order.has("disable")) {
			disabled_group = true;
		} else if (order.has("enable")) {
			disabled_group = false;
		}
	}
	
	private static final Gson _gson_simple = new Gson();
	
	public JsonElement getInstanceStatusItem() {
		JsonObject jo = new JsonObject();
		jo.addProperty("name", name);
		jo.addProperty("disabled_group", disabled_group);
		jo.addProperty("account_expiration_trash_duration", account_expiration_trash_duration);
		jo.addProperty("account_expiration_purge_duration", account_expiration_purge_duration);
		jo.addProperty("account_expiration_based_on_last_activity", account_expiration_based_on_last_activity);
		jo.addProperty("base_working_dir", base_working_dir.getAbsolutePath());
		jo.addProperty("min_disk_space_before_warn", min_disk_space_before_warn);
		jo.addProperty("min_disk_space_before_stop", min_disk_space_before_stop);
		jo.addProperty("pathindex_storagename", pathindex_storagename);
		jo.addProperty("domain_isolation", domain_isolation);
		jo.addProperty("short_activity_log", short_activity_log);
		jo.addProperty("last_free_space", last_free_space);
		jo.addProperty("trash_directory", trash_directory.getAbsolutePath());
		jo.addProperty("users_no_activity_log", _gson_simple.toJson(users_no_activity_log));
		return jo;
	}
	
	public boolean isUserHasActivityDisabled(String simple_username) {// TODO
		return users_no_activity_log.contains(simple_username);
	}
	
}
