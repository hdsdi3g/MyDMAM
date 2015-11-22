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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.FreeDiskSpaceWarningException;

public class FTPGroup {
	
	// TODO Publish in InstanceStatus
	// TODO enable/disable groups in instance status
	
	private static LinkedHashMap<String, FTPGroup> declared_groups;
	
	private static HashMap<String, Long> declared_groups_ttl_trash;
	private static HashMap<String, Long> declared_groups_ttl_purge;
	private static HashMap<String, Boolean> declared_groups_expiration_based_on_last_activity;
	
	private static void init_group() {
		declared_groups = new LinkedHashMap<String, FTPGroup>();
		declared_groups_ttl_trash = new HashMap<String, Long>();
		declared_groups_ttl_purge = new HashMap<String, Long>();
		declared_groups_expiration_based_on_last_activity = new HashMap<String, Boolean>();
		
		if (Configuration.global.isElementExists("ftpservergroups") == false) {
			return;
		}
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
	
	static LinkedHashMap<String, FTPGroup> getDeclaredGroups() {
		if (declared_groups == null) {
			init_group();
		}
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
	
	public static FTPGroup getFromName(String group_name) {
		if (declared_groups == null) {
			init_group();
		}
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
	private String pathindex_storagename_for_live_update;
	
	/**
	 * Set true for set user storage like /base_working_dir/domain/userName
	 * Set false for indexing ftp content like /base_working_dir/domain#userName
	 * If no domain, user storage will be like /base_working_dir/userName
	 */
	private boolean domain_isolation;
	
	private boolean short_activity_log;
	
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
		
		min_disk_space_before_warn = Configuration.getValue(all_groups_confs, group_name, "min_disk_space_before_warn", 0);
		min_disk_space_before_stop = Configuration.getValue(all_groups_confs, group_name, "min_disk_space_before_stop", 0);
		
		pathindex_storagename_for_live_update = Configuration.getValue(all_groups_confs, group_name, "pathindex_storagename_for_live_update", null);
		domain_isolation = Configuration.getValueBoolean(all_groups_confs, group_name, "domain_isolation");
		
		short_activity_log = Configuration.getValueBoolean(all_groups_confs, group_name, "short_activity_log");
	}
	
	/**
	 * Create it if needed.
	 */
	File getUserHomeDirectory(FTPUser user) throws NullPointerException, IOException {
		StringBuilder sb = new StringBuilder();
		sb.append(base_working_dir.getAbsolutePath());
		sb.append(File.separator);
		if (user.getDomain().equals("") == false) {
			if (domain_isolation) {
				sb.append(user.getDomain());
				sb.append(File.separator);
			} else {
				sb.append(user.getDomain());
				sb.append("#");
			}
		}
		sb.append(user.getName());
		
		File home_directory = new File(sb.toString());
		
		if (home_directory.exists()) {
			CopyMove.checkExistsCanRead(home_directory);
			CopyMove.checkIsDirectory(home_directory);
			CopyMove.checkIsWritable(home_directory);
		} else {
			FileUtils.forceMkdir(home_directory);
		}
		
		return home_directory;
	}
	
	synchronized boolean isDisabled() {
		return disabled_group;
	}
	
	String getName() {
		return name;
	}
	
	String getPathindexStoragenameLiveUpdate() {
		return pathindex_storagename_for_live_update;
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
	}
	
}
