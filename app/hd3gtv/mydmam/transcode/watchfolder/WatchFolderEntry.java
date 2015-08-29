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
package hd3gtv.mydmam.transcode.watchfolder;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.IgnoreFiles;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.storage.StorageCrawler;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.watchfolder.AbstractFoundedFile.Status;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;

class WatchFolderEntry implements Runnable {
	
	private String name;
	private String source_storage;
	private long time_to_wait_growing_file;
	private long time_to_sleep_between_scans;
	private long min_file_size;
	private String temp_directory;
	private List<Target> targets;
	private AppManager manager;
	
	private transient boolean want_to_stop;
	
	class Target {
		String storage;
		String profile;
		
		Target init(LinkedHashMap<String, ?> conf) throws Exception {
			if (conf.containsKey(storage) == false) {
				throw new NullPointerException("\"storage\" can't to be null");
			}
			storage = (String) conf.get("storage");
			Storage.getByName(storage).testStorageConnection();
			
			if (conf.containsKey(profile) == false) {
				throw new NullPointerException("\"profile\" can't to be null");
			}
			profile = (String) conf.get("profile");
			
			if (TranscodeProfile.getTranscodeProfile(profile) == null) {
				throw new NullPointerException("Can't found transcode profile \"" + profile + "\" in \"" + name + "\" watch folder configuration");
			}
			return this;
		}
		
	}
	
	WatchFolderEntry(AppManager manager, String name, HashMap<String, ConfigurationItem> all_wf_confs) throws Exception {
		this.manager = manager;
		this.name = name;
		
		source_storage = Configuration.getValue(all_wf_confs, name, "source_storage", "");
		Storage.getByName(source_storage).testStorageConnection();
		
		List<LinkedHashMap<String, ?>> c_targets = Configuration.getValuesList(all_wf_confs, name, "targets");
		if (c_targets == null) {
			throw new NullPointerException("\"targets\" can't to be null, in " + name + " watchfolder");
		}
		if (c_targets.isEmpty()) {
			throw new IndexOutOfBoundsException("\"targets\" can't to be empty, in " + name + " watchfolder");
		}
		List<Target> targets = new ArrayList<WatchFolderEntry.Target>(c_targets.size());
		
		for (int pos = 0; pos < c_targets.size(); pos++) {
			targets.add(new Target().init(c_targets.get(pos)));
		}
		
		time_to_wait_growing_file = Configuration.getValue(all_wf_confs, name, "time_to_wait_growing_file", 1000);
		time_to_sleep_between_scans = Configuration.getValue(all_wf_confs, name, "time_to_sleep_between_scans", 10000);
		min_file_size = Configuration.getValue(all_wf_confs, name, "min_file_size", 10000);
		temp_directory = Configuration.getValue(all_wf_confs, name, "temp_directory", System.getProperty("java.io.tmpdir"));
		
	}
	
	synchronized void stopWatchfolderScans() {
		want_to_stop = true;
	}
	
	private class Crawler implements StorageCrawler {
		
		ArrayList<AbstractFoundedFile> founded;
		
		public Crawler() {
			founded = new ArrayList<AbstractFoundedFile>();
		}
		
		public boolean onFoundFile(AbstractFile file, String storagename) {
			founded.add(new AbstractFoundedFile(file, storagename));
			return true;
		}
		
		public void onNotFoundFile(String path, String storagename) {
		}
		
		public boolean isSearchIsRecursive() {
			return true;
		}
		
		public boolean canSelectfileInSearch() {
			return true;
		}
		
		public boolean canSelectdirInSearch() {
			return false;
		}
		
		public boolean canSelectHiddenInSearch() {
			return false;
		}
		
		public int maxPathWidthCrawl() {
			return 10;
		}
		
		public IgnoreFiles getRules() {
			return IgnoreFiles.default_list;
		}
		
		public boolean onStartSearch(AbstractFile search_root_path) {
			founded.clear();
			return true;
		}
		
		public void onEndSearch() {
		}
		
		public String getCurrentWorkingDir() {
			return null;
		}
		
	}
	
	@Override
	public void run() {
		want_to_stop = false;
		long sleep_time;
		AbstractFoundedFile founded_file;
		AbstractFoundedFile db_entry_file;
		AbstractFoundedFile active_file;
		AbstractFoundedFile validated_file;
		List<AbstractFoundedFile> present_in_db = new ArrayList<AbstractFoundedFile>(1);
		List<AbstractFoundedFile> new_files_to_add = new ArrayList<AbstractFoundedFile>(1);
		List<AbstractFoundedFile> active_files = new ArrayList<AbstractFoundedFile>(1);
		List<AbstractFoundedFile> validated_files = new ArrayList<AbstractFoundedFile>(1);
		ColumnPrefixDistributedRowLock<String> lock;
		
		Crawler crawler = new Crawler();
		
		try {
			while (want_to_stop == false) {
				sleep_time = time_to_sleep_between_scans;
				while (sleep_time > 0 & (want_to_stop == false)) {
					Thread.sleep(10);
					sleep_time -= 10;
				}
				try {
					
					/**
					 * Scan
					 * Regular push dirlist to Cassandra presence status CF, for all instances
					 */
					Storage.getByName(source_storage).dirList(crawler);
					if (crawler.founded.isEmpty()) {
						continue;
					}
					
					/**
					 * Check founded files
					 * If a file is founded... nothing special.
					 * But compare last status with Cassandra, for all instances => feed actived files list
					 */
					present_in_db = WatchFolderDB.get(crawler.founded);
					new_files_to_add.clear();
					active_files.clear();
					
					for (int pos = 0; pos < crawler.founded.size(); pos++) {
						founded_file = crawler.founded.get(pos);
						if (present_in_db.contains(founded_file)) {
							/**
							 * File to check
							 */
							active_files.add(founded_file);
						} else {
							/**
							 * New file to add
							 */
							new_files_to_add.add(founded_file);
						}
					}
					
					WatchFolderDB.push(new_files_to_add);
					
					if (active_files.isEmpty()) {
						continue;
					}
					
					/**
					 * Check actived files => if file is still static => feed validated files list
					 */
					validated_files.clear();
					
					for (int pos = active_files.size() - 1; pos > -1; pos--) {
						active_file = active_files.get(pos);
						db_entry_file = present_in_db.get(present_in_db.indexOf(active_file));
						
						if (db_entry_file.status == Status.ERROR) {
							/**
							 * Ignore error files
							 */
							active_files.remove(pos);
							continue;
						}
						if (db_entry_file.status == Status.PROCESSED | db_entry_file.status == Status.IN_PROCESSING) {
							/**
							 * Ignore validated files, but refresh db entries.
							 */
							continue;
						}
						if (db_entry_file.status != Status.PROCESSED) {
							/**
							 * All enums should have been tested...
							 */
							throw new NullPointerException("Impossible status");
						}
						
						if (active_file.size < db_entry_file.size) {
							active_file.status = Status.ERROR;
							/**
							 * The found file has shrink !
							 */
							Log2.log.info("Watch folder \"" + name + "\" has found a shrinked file", active_file);
							continue;
						}
						
						if ((active_file.date / 10000) < (db_entry_file.date / 10000)) {
							active_file.status = Status.ERROR;
							/**
							 * The found file is going back to the past !
							 * With 10 seconds of margin.
							 */
							Log2.log.info("Watch folder \"" + name + "\" has found a file who is going back to the past", active_file);
							continue;
						}
						
						if (active_file.size > db_entry_file.size | active_file.date > db_entry_file.date) {
							/**
							 * The found file has been updated.
							 */
							continue;
						}
						
						if (db_entry_file.last_checked + time_to_wait_growing_file < System.currentTimeMillis()) {
							validated_files.add(db_entry_file);
							active_file.status = Status.IN_PROCESSING;
						} else {
							Log2.log.debug("This file has stopped to grow, wait the time to validate", db_entry_file);
						}
						active_file.last_checked = db_entry_file.last_checked;
					}
					
					WatchFolderDB.push(active_files);
					
					/**
					 * For all validate files,
					 * Lock it in Cassandra, and process it.
					 */
					
					for (int pos = 0; pos < validated_files.size(); pos++) {
						validated_file = validated_files.get(pos);
						lock = null;
						try {
							try {
								lock = WatchFolderDB.prepareLock(validated_file.getPathIndexKey());
								lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
								lock.expireLockAfter(500, TimeUnit.MILLISECONDS);
								lock.failOnStaleLock(false);
								lock.acquire();
								
								performFoundAndValidatedFile(validated_file);
								
								lock.release();
							} catch (StaleLockException e) {
								/**
								 * The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false)
								 */
								Log2.log.error("Can't lock key: abandoned lock", e, validated_file);
							} catch (BusyLockException e) {
								Log2.log.debug("Can't lock key, it's currently locked.", validated_file);
							} finally {
								if (lock != null) {
									lock.release();
								}
							}
						} catch (Exception e) {
							if (e instanceof ConnectionException) {
								throw (ConnectionException) e;
							} else {
								Log2.log.error("Unknow exception with Cassandra, may be a fatal problem", e);
								AdminMailAlert.create("Unknow exception with Cassandra, may be a fatal problem with it", true).addDump(validated_file).setThrowable(e).setManager(manager).send();
								return;
							}
						}
					}
					
				} catch (ConnectionException e) {
					Log2.log.error("Can't access to Cassandra", e);
				}
			}
		} catch (InterruptedException e) {
			Log2.log.error("Can't sleep", e, new Log2Dump("name", name));
		}
	}
	
	void performFoundAndValidatedFile(AbstractFoundedFile validated_file) throws ConnectionException {
		/**
		 * Refresh ES pathindex for this file
		 */
		
		/**
		 * Perform File Validation for manage it and release it (and obviously validate it in Cassandra).
		 * Process active files: analyst
		 */
		
		/**
		 * Process active files: transcoding jobs
		 * Job neededstorages and hookednames (transcoding profile name) must be set & check.
		 */
		/*JobContextTranscoder job_transcode = new JobContextTranscoder();
		// TODO job_transcode
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		JobNG new_job = AppManager.createJob(job_transcode).setCreator(getClass()).setName("Transcode from watchfolder " + validated_file.getName()).publish(mutator);
		
		JobContextWFDeleteSourceFile delete_source = new JobContextWFDeleteSourceFile();
		delete_source.neededstorages = Arrays.asList(validated_file.storage_name);
		delete_source.path = validated_file.path;
		delete_source.storage = validated_file.storage_name;
		
		AppManager.createJob(delete_source).setCreator(getClass()).setName("Delete watchfolder source " + validated_file.getName()).setRequiredCompletedJob(new_job).setDeleteAfterCompleted()
				.publish(mutator);
		mutator.execute();*/
	}
}
