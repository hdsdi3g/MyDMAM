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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticsearchException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.metadata.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.DistantFileRecovery;
import hd3gtv.mydmam.storage.IgnoreFiles;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.storage.StorageCrawler;
import hd3gtv.mydmam.transcode.JobContextTranscoder;
import hd3gtv.mydmam.transcode.ProcessingKit;
import hd3gtv.mydmam.transcode.ProcessingKitEngine;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.watchfolder.AbstractFoundedFile.Status;
import hd3gtv.tools.Timecode;

public class WatchFolderEntry extends Thread implements InstanceStatusItem {
	
	private String name;
	private String source_storage;
	private ArrayList<String> limit_to_file_extentions;
	private long time_to_wait_growing_file;
	private long time_to_sleep_between_scans;
	private long min_file_size;
	private List<Target> targets;
	private AppManager manager;
	private Explorer explorer;
	private List<MustContainType> must_contain;
	private ProcessingKitEngine process_kit_engine;
	
	public enum MustContainType {
		video, audio
	}
	
	private boolean want_to_stop;
	
	class Target {
		String storage;
		String profile;
		String dest_file_prefix;
		String dest_file_suffix;
		boolean keep_input_dir_to_dest;
		boolean copy_source_file_to_dest;
		transient ProcessingKit process_kit;
		
		Target init(LinkedHashMap<String, ?> conf) throws Exception {
			if (conf.containsKey("storage") == false) {
				throw new NullPointerException("\"storage\" can't to be null");
			}
			Loggers.Transcode_WatchFolder.trace("Init entry (target) from confguration: " + conf);
			
			storage = (String) conf.get("storage");
			if (Storage.getAllStoragesNames().contains(storage) == false) {
				Loggers.Transcode_WatchFolder.trace("Actual configured storages: " + Storage.getAllStoragesNames());
				throw new IOException("Can't found storage declaration \"" + storage + "\"");
			}
			
			if (conf.containsKey("profile") == false) {
				throw new NullPointerException("\"profile\" can't to be null");
			}
			profile = (String) conf.get("profile");
			
			if (TranscodeProfile.getTranscodeProfile(profile) == null) {
				process_kit = process_kit_engine.get(profile);
				if (process_kit == null) {
					throw new NullPointerException("Can't found transcode profile/processing kit \"" + profile + "\" in \"" + name + "\" watch folder configuration");
				}
			}
			
			if (conf.containsKey("dest_file_prefix")) {
				dest_file_prefix = (String) conf.get("dest_file_prefix");
			}
			if (conf.containsKey("dest_file_suffix")) {
				dest_file_suffix = (String) conf.get("dest_file_suffix");
			}
			
			keep_input_dir_to_dest = false;
			if (conf.containsKey("keep_input_dir_to_dest")) {
				keep_input_dir_to_dest = (Boolean) conf.get("keep_input_dir_to_dest");
			}
			
			copy_source_file_to_dest = false;
			if (conf.containsKey("copy_source_file_to_dest")) {
				copy_source_file_to_dest = (Boolean) conf.get("copy_source_file_to_dest");
			}
			
			if (Loggers.Transcode_WatchFolder.isDebugEnabled()) {
				LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
				log.put("storage", storage);
				if (process_kit != null) {
					log.put("process_kit", profile);
				} else {
					log.put("profile", profile);
				}
				log.put("dest_file_prefix", dest_file_prefix);
				log.put("dest_file_suffix", dest_file_suffix);
				log.put("keep_input_dir_to_dest", keep_input_dir_to_dest);
				log.put("copy_source_file_to_dest", copy_source_file_to_dest);
				Loggers.Transcode_WatchFolder.debug("Init watchfolder target: " + log.toString());
			}
			
			return this;
		}
		
		/**
		 * @param duration can be null
		 */
		JobNG prepareTranscodeJob(String path_index_key, String simple_file_name, String source_sub_directory, Timecode duration, MutationBatch mutator) throws ConnectionException {
			JobContextTranscoder job_transcode = new JobContextTranscoder();
			job_transcode.source_pathindex_key = path_index_key;
			job_transcode.dest_storage_name = storage;
			job_transcode.neededstorages = Arrays.asList(storage);
			/** transcoding profile name */
			job_transcode.hookednames = Arrays.asList(profile);
			job_transcode.setDuration(duration);
			job_transcode.dest_file_prefix = dest_file_prefix;
			job_transcode.dest_file_suffix = dest_file_suffix;
			if (keep_input_dir_to_dest) {
				job_transcode.dest_sub_directory = source_sub_directory;
			}
			
			if (Loggers.Transcode_WatchFolder.isDebugEnabled()) {
				Loggers.Transcode_WatchFolder.info("Prepare Transcode Job: " + job_transcode.contextToJson().toString());
			}
			
			return AppManager.createJob(job_transcode).setCreator(getClass()).setName("Transcode from watchfolder " + simple_file_name).publish(mutator);
		}
		
		private void addToCopySourceFileList(ArrayList<String> send_source_to_dest, ArrayList<String> send_source_to_dest_storages_names, String source_sub_directory) {
			if (copy_source_file_to_dest == false) {
				return;
			}
			String spie_key = SourcePathIndexerElement.prepareStorageElement(storage).prepare_key();
			
			if (keep_input_dir_to_dest) {
				spie_key = SourcePathIndexerElement.prepare_key(storage, source_sub_directory);
			}
			if (send_source_to_dest.contains(spie_key) == false) {
				send_source_to_dest.add(spie_key);
			}
			
			if (send_source_to_dest_storages_names.contains(storage) == false) {
				send_source_to_dest_storages_names.add(storage);
			}
		}
		
	}
	
	WatchFolderEntry(AppManager manager, ThreadGroup thread_group, String name, HashMap<String, ConfigurationItem> all_wf_confs, ProcessingKitEngine process_kit_engine) throws Exception {
		super(thread_group, "WatchFolder:" + name);
		setDaemon(true);
		
		this.manager = manager;
		this.name = name;
		this.process_kit_engine = process_kit_engine;
		explorer = new Explorer();
		
		source_storage = Configuration.getValue(all_wf_confs, name, "source_storage", "");
		Storage.getByName(source_storage).testStorageConnection();
		
		List<LinkedHashMap<String, ?>> c_targets = Configuration.getValuesList(all_wf_confs, name, "targets");
		if (c_targets == null) {
			throw new NullPointerException("\"targets\" can't to be null, in " + name + " watchfolder");
		}
		if (c_targets.isEmpty()) {
			throw new IndexOutOfBoundsException("\"targets\" can't to be empty, in " + name + " watchfolder");
		}
		targets = new ArrayList<WatchFolderEntry.Target>(c_targets.size());
		
		for (int pos = 0; pos < c_targets.size(); pos++) {
			targets.add(new Target().init(c_targets.get(pos)));
		}
		
		time_to_wait_growing_file = Configuration.getValue(all_wf_confs, name, "time_to_wait_growing_file", 1000);
		time_to_sleep_between_scans = Configuration.getValue(all_wf_confs, name, "time_to_sleep_between_scans", 10000);
		min_file_size = Configuration.getValue(all_wf_confs, name, "min_file_size", 10000);
		
		must_contain = new ArrayList<WatchFolderEntry.MustContainType>(2);
		Object raw_must_contain = Configuration.getRawValue(all_wf_confs, name, "must_contain");
		if (raw_must_contain != null) {
			if (raw_must_contain instanceof String) {
				must_contain.add(MustContainType.valueOf((String) raw_must_contain));
			} else if (raw_must_contain instanceof ArrayList) {
				ArrayList<?> al_raw_must_contain = (ArrayList<?>) raw_must_contain;
				for (int pos_almc = 0; pos_almc < al_raw_must_contain.size(); pos_almc++) {
					must_contain.add(MustContainType.valueOf((String) al_raw_must_contain.get(pos_almc)));
				}
			}
		}
		
		limit_to_file_extentions = new ArrayList<>(1);
		Object raw_limit_to_file_extentions = Configuration.getRawValue(all_wf_confs, name, "limit_to_file_extentions");
		if (raw_limit_to_file_extentions != null) {
			if (raw_limit_to_file_extentions instanceof String) {
				limit_to_file_extentions.add(((String) raw_limit_to_file_extentions).toLowerCase());
			} else if (raw_limit_to_file_extentions instanceof ArrayList) {
				ArrayList<?> al_raw_limit_to_file_extentions = (ArrayList<?>) raw_limit_to_file_extentions;
				for (int pos_almc = 0; pos_almc < al_raw_limit_to_file_extentions.size(); pos_almc++) {
					limit_to_file_extentions.add(((String) al_raw_limit_to_file_extentions.get(pos_almc)).toLowerCase());
				}
			}
		}
		
		if (Loggers.Transcode_WatchFolder.isInfoEnabled()) {
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put("name", name);
			log.put("source_storage", source_storage);
			log.put("targets", targets.size());
			log.put("time_to_wait_growing_file", time_to_wait_growing_file);
			log.put("time_to_sleep_between_scans", time_to_sleep_between_scans);
			log.put("min_file_size", min_file_size);
			if (limit_to_file_extentions.isEmpty() == false) {
				log.put("limit_to_file_extentions", limit_to_file_extentions);
			}
			log.put("must_contain", must_contain);
			Loggers.Transcode_WatchFolder.info("Load watchfolder entry " + log);
		}
		
		manager.getInstanceStatus().registerInstanceStatusItem(this);
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
			if (file.length() < min_file_size) {
				return true;
			}
			if (limit_to_file_extentions.isEmpty() == false) {
				if (limit_to_file_extentions.contains(FilenameUtils.getExtension(file.getName()).toLowerCase()) == false) {
					return true;
				}
			}
			Loggers.Transcode_WatchFolder.debug("Search has found a file: " + storagename + ":" + file.getPath());
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
			return IgnoreFiles.directory_config_list;
		}
		
		public boolean onStartSearch(String storage_name, AbstractFile search_root_path) {
			Loggers.Transcode_WatchFolder.debug("Start search for \"" + name + "\" in " + storage_name + ":" + search_root_path.getPath());
			founded.clear();
			return true;
		}
		
		public void onEndSearch() {
			// Loggers.Transcode_WatchFolder.debug("End search for " + name);
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
		ElasticsearchBulkOperation bulk;
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
					
					Loggers.Transcode_WatchFolder.trace("Start scan for " + name);
					
					/**
					 * Scan
					 * Regular push dirlist to Cassandra presence status CF, for all instances
					 */
					Storage.getByName(source_storage).dirList(crawler);
					if (crawler.founded.isEmpty()) {
						Loggers.Transcode_WatchFolder.trace("No items founded in " + name);
						continue;
					}
					
					if (Loggers.Transcode_WatchFolder.isTraceEnabled()) {
						Loggers.Transcode_WatchFolder.trace("Dump founded file list");
						for (int pos = 0; pos < crawler.founded.size(); pos++) {
							Loggers.Transcode_WatchFolder.trace("Founded file: " + crawler.founded.get(pos));
						}
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
						Loggers.Transcode_WatchFolder.trace("Founded file in " + name + ": " + founded_file);
						
						if (present_in_db.contains(founded_file)) {
							/**
							 * File to check
							 */
							Loggers.Transcode_WatchFolder.trace("Founded file is active in " + name + ": " + founded_file.path);
							active_files.add(founded_file);
						} else {
							/**
							 * New file to add
							 */
							Loggers.Transcode_WatchFolder.trace("New founded file in " + name + ": " + founded_file.path);
							new_files_to_add.add(founded_file);
						}
					}
					
					if (new_files_to_add.isEmpty() == false) {
						WatchFolderDB.push(new_files_to_add);
						bulk = Elasticsearch.prepareBulk();
						try {
							Loggers.Transcode_WatchFolder.trace("Refresh ES index in " + name + " for storage " + source_storage);
							explorer.refreshStoragePath(bulk, Arrays.asList(SourcePathIndexerElement.prepareStorageElement(source_storage)), false);
							bulk.terminateBulk();
						} catch (Exception e) {
							if (e instanceof ElasticsearchException) {
								Loggers.Transcode_WatchFolder.error("Trouble during Elasticsearch updating", e);
							} else {
								throw e;
							}
						}
					}
					
					if (active_files.isEmpty()) {
						Loggers.Transcode_WatchFolder.trace("No active files for " + name);
						continue;
					}
					
					/**
					 * Check actived files => if file is still static => feed validated files list
					 */
					Loggers.Transcode_WatchFolder.trace("Check actived files for " + name);
					
					validated_files.clear();
					
					for (int pos = active_files.size() - 1; pos > -1; pos--) {
						active_file = active_files.get(pos);
						db_entry_file = present_in_db.get(present_in_db.indexOf(active_file));
						
						if (db_entry_file.status == Status.ERROR) {
							/**
							 * Ignore error files
							 */
							Loggers.Transcode_WatchFolder.trace("Ignore file for " + name + ": " + db_entry_file);
							active_files.remove(pos);
							continue;
						}
						if (db_entry_file.status == Status.PROCESSED | db_entry_file.status == Status.IN_PROCESSING) {
							/**
							 * Ignore validated files, but refresh db entries.
							 */
							Loggers.Transcode_WatchFolder.trace("Ignore validated files, but refresh db entries for " + name + ": " + db_entry_file);
							active_files.set(pos, db_entry_file);
							continue;
						}
						if (db_entry_file.status != Status.DETECTED) {
							/**
							 * All enums should have been tested...
							 */
							throw new NullPointerException("Impossible status:\t" + db_entry_file);
						}
						
						if (active_file.size < db_entry_file.size) {
							active_file.status = Status.ERROR;
							/**
							 * The found file has shrink !
							 */
							Loggers.Transcode_WatchFolder.info("Found a shrinked file in " + name + ": " + active_file);
							continue;
						}
						
						if ((active_file.date / 10000) < (db_entry_file.date / 10000)) {
							/**
							 * The found file is going back to the past !
							 * With 10 seconds of margin.
							 */
							Loggers.Transcode_WatchFolder.info("Found file is going back to the past in " + name + ": " + active_file);
							continue;
						}
						
						if (active_file.size > db_entry_file.size | active_file.date > db_entry_file.date) {
							/**
							 * The found file has been updated.
							 */
							Loggers.Transcode_WatchFolder.trace("Found file has been updated in " + name + ": " + active_file);
							continue;
						}
						
						if (db_entry_file.last_checked + time_to_wait_growing_file < System.currentTimeMillis()) {
							if (canIReadThisFile(db_entry_file) == false) {
								/**
								 * Something lock this file. Maybe a Windows copy process ?
								 */
								Loggers.Transcode_WatchFolder.trace("Found file is currently locked (can't open it) in " + name + ": " + active_file);
								continue;
							}
							Loggers.Transcode_WatchFolder.trace("Set found file to validated files in " + name + ": " + db_entry_file);
							validated_files.add(db_entry_file);
						} else {
							Loggers.Transcode_WatchFolder.trace("This file has stopped to grow, wait the time to validate in " + name + ": " + db_entry_file);
						}
						active_file.last_checked = db_entry_file.last_checked;
					}
					
					WatchFolderDB.push(active_files);
					
					if (validated_files.isEmpty()) {
						continue;
					}
					
					/**
					 * For all validated files,
					 * Lock it in Cassandra, and process it.
					 */
					Loggers.Transcode_WatchFolder.debug("For all validated files (" + validated_files.size() + "), lock it in Cassandra, and process it, in " + name);
					
					for (int pos = 0; pos < validated_files.size(); pos++) {
						validated_file = validated_files.get(pos);
						lock = null;
						try {
							try {
								Loggers.Transcode_WatchFolder.trace("Set CF Lock by " + name + " for " + validated_file);
								
								lock = WatchFolderDB.prepareLock(validated_file.getPathIndexKey());
								lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
								lock.expireLockAfter(500, TimeUnit.MILLISECONDS);
								lock.failOnStaleLock(false);
								lock.acquire();
								
								performFoundAndValidatedFile(validated_file);
								
								Loggers.Transcode_WatchFolder.trace("Release CF Lock by " + name + " for " + validated_file);
								lock.release();
							} catch (StaleLockException e) {
								/**
								 * The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false)
								 */
								Loggers.Transcode_WatchFolder.warn("Can't lock key: abandoned lock in " + name + " for " + validated_file, e);
							} catch (BusyLockException e) {
								Loggers.Transcode_WatchFolder.debug("Can't lock key, it's currently locked in " + name + " for " + validated_file, e);
							} finally {
								if (lock != null) {
									Loggers.Transcode_WatchFolder.trace("Lock release " + name + " for " + validated_file);
									lock.release();
								}
							}
						} catch (Exception e) {
							if (e instanceof ConnectionException) {
								throw (ConnectionException) e;
							} else {
								Loggers.Transcode_WatchFolder.error("Unknow exception for " + name, e);
								AdminMailAlert.create("Unknow exception, may be a fatal problem with WatchFolder " + name, true).setThrowable(e).setManager(manager).send();
								return;
							}
						}
					}
					
				} catch (ConnectionException e) {
					Loggers.Transcode_WatchFolder.error("Can't access to Cassandra " + name, e);
				}
			}
		} catch (Exception e) {
			Loggers.Transcode_WatchFolder.error("Fatal exception " + name, e);
			AdminMailAlert.create("Fatal and not managed exception for WatchFolder " + name, true).setManager(manager).setThrowable(e).send();
		}
	}
	
	void performFoundAndValidatedFile(AbstractFoundedFile validated_file) throws ConnectionException {
		/**
		 * Refresh ES pathindex for this file
		 */
		SourcePathIndexerElement pi_item = explorer.getelementByIdkey(validated_file.getPathIndexKey());
		if (pi_item != null) {
			if ((pi_item.date != validated_file.date) | (pi_item.size != validated_file.size)) {
				try {
					Loggers.Transcode_WatchFolder.trace("Refresh ES pathindex for this file in " + name + " for " + validated_file);
					
					ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
					bulk.getConfiguration().setRefresh(true);
					explorer.refreshCurrentStoragePath(bulk, Arrays.asList(pi_item), true);
					bulk.terminateBulk();
					pi_item = explorer.getelementByIdkey(validated_file.getPathIndexKey());
				} catch (Exception e) {
					Loggers.Transcode_WatchFolder.error("Can't update ES index " + name + " for " + validated_file, e);
					return;
				}
			}
		} else {
			Loggers.Transcode_WatchFolder.error("Can't found current item in ES " + name + " for " + validated_file);
			return;
		}
		
		/**
		 * Perform File Validation for manage it and release it (and obviously validate it in Cassandra).
		 * Process active files: analyst
		 */
		validated_file.status = Status.IN_PROCESSING;
		WatchFolderDB.push(Arrays.asList(validated_file));
		
		File physical_source = Storage.getLocalFile(pi_item);
		if (physical_source == null) {
			try {
				physical_source = DistantFileRecovery.getFile(pi_item, false);
			} catch (Exception e) {
				Loggers.Transcode_WatchFolder.error("Can't download found file to temp directory " + name + " for " + validated_file, e);
				AdminMailAlert.create("Can't download watch folder found file to temp directory " + name + " for " + validated_file, false).setThrowable(e).send();
				validated_file.status = Status.ERROR;
				WatchFolderDB.push(Arrays.asList(validated_file));
				return;
			}
		}
		
		Container indexing_result = null;
		try {
			Loggers.Transcode_WatchFolder.trace("Save item to ES " + name + " for " + validated_file);
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			MetadataIndexingOperation indexing = new MetadataIndexingOperation(physical_source).setReference(pi_item).setLimit(MetadataIndexingLimit.FAST);
			indexing_result = indexing.doIndexing();
			ContainerOperations.save(indexing_result, true, bulk);
			bulk.terminateBulk();
		} catch (Exception e) {
			Loggers.Transcode_WatchFolder.error("Can't analyst MTD " + name + " for " + validated_file, e);
			validated_file.status = Status.ERROR;
			WatchFolderDB.push(Arrays.asList(validated_file));
			return;
		}
		
		/**
		 * Process active files: transcoding jobs
		 */
		Timecode duration = null;
		FFprobe ffprobe = indexing_result.getByClass(FFprobe.class);
		
		if (ffprobe != null) {
			Loggers.Transcode_WatchFolder.debug("Analyst file result, " + ffprobe);
			duration = ffprobe.getDuration();
		}
		
		if (must_contain.contains(MustContainType.video) | must_contain.contains(MustContainType.audio)) {
			if (ffprobe == null) {
				Loggers.Transcode_WatchFolder.error("Invalid file dropped in watchfolder: it must be a media file " + name + " for " + validated_file);
				AdminMailAlert.create("Invalid file dropped in watchfolder: it must be a media file " + name + " for " + validated_file, false).send();
				validated_file.status = Status.ERROR;
				WatchFolderDB.push(Arrays.asList(validated_file));
				return;
			} else if (must_contain.contains(MustContainType.video) & (ffprobe.hasVideo() == false)) {
				Loggers.Transcode_WatchFolder.error("Invalid file dropped in watchfolder: it must have a video track " + name + " for " + validated_file);
				AdminMailAlert.create("Invalid file dropped in watchfolder: it must have a video track " + name + " for " + validated_file, false).send();
				validated_file.status = Status.ERROR;
				WatchFolderDB.push(Arrays.asList(validated_file));
				return;
			} else if (must_contain.contains(MustContainType.audio) & (ffprobe.hasAudio() == false)) {
				Loggers.Transcode_WatchFolder.error("Invalid file dropped in watchfolder: it must have an audio track " + name + " for " + validated_file);
				AdminMailAlert.create("Invalid file dropped in watchfolder: it must have an audio track " + name + " for " + validated_file, false).send();
				validated_file.status = Status.ERROR;
				WatchFolderDB.push(Arrays.asList(validated_file));
				return;
			}
		} else {
			Loggers.Transcode_WatchFolder.debug("WF entry (" + name + ") don't required a media file");
		}
		
		if (indexing_result.getSummary().equalsMimetype("application/zip", "application/x-compressed", "application/x-zip-compressed", "multipart/x-zip")) {
			/**
			 * Zip extraction in-place
			 */
			Loggers.Transcode_WatchFolder.info("Zip file dropped in watchfolder: extract it and delete it, " + name + " for " + validated_file);
			AbstractFile dest_adir = null;
			AbstractFile dest_afile = null;
			OutputStream fos;
			byte[] buffer = new byte[0xFFF];
			
			try {
				String base_dest_path = FilenameUtils.getFullPath(pi_item.currentpath);
				dest_adir = Storage.getByName(pi_item.storagename).getRootPath().getAbstractFile(base_dest_path);
				
				ZipInputStream zis = new ZipInputStream(new FileInputStream(physical_source));
				ZipEntry zip_element = zis.getNextEntry();
				
				while (zip_element != null) {
					if (zip_element.isDirectory()) {
						zip_element = zis.getNextEntry();
						continue;
					}
					String file_ext = FilenameUtils.getExtension(zip_element.getName());
					String file_name = FilenameUtils.getBaseName(zip_element.getName()) + "." + file_ext;
					if (file_ext.equals("")) {
						file_name = FilenameUtils.getBaseName(zip_element.getName());
					}
					
					dest_afile = dest_adir.getAbstractFile(base_dest_path + "/" + file_name);
					fos = dest_afile.getOutputStream(0xFFF);
					
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
					zip_element = zis.getNextEntry();
				}
				zis.closeEntry();
				zis.close();
				validated_file.status = Status.PROCESSED;
				FileUtils.forceDelete(physical_source);
			} catch (IOException e) {
				validated_file.status = Status.ERROR;
				Loggers.Transcode_WatchFolder.error("Can't extract Zip file dropped in watchfolder: " + name + " for " + validated_file, e);
			}
			WatchFolderDB.push(Arrays.asList(validated_file));
			IOUtils.closeQuietly(dest_adir);
			return;
		}
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		ArrayList<JobNG> jobs_to_watch = new ArrayList<JobNG>(targets.size());
		
		String sub_dir_name = validated_file.path.substring(0, validated_file.path.length() - (validated_file.getName().length() + 1));
		
		Loggers.Transcode_WatchFolder.trace("Prepare all transcode jobs " + name + " for " + validated_file + ", in " + sub_dir_name);
		
		JobNG new_job;
		ArrayList<String> send_source_to_dest = new ArrayList<String>();
		ArrayList<String> send_source_to_dest_storages_names = new ArrayList<String>();
		Target target;
		
		for (int pos = 0; pos < targets.size(); pos++) {
			target = targets.get(pos);
			
			if (target.process_kit != null) {
				if (target.process_kit.validateItem(indexing_result) == false) {
					Loggers.Transcode_WatchFolder
							.error("Invalid file dropped in watchfolder: processing kit don't validate it: " + name + " for " + validated_file + " by " + target.process_kit.getClass());
					AdminMailAlert.create("Invalid file dropped in watchfolder: processing kit don't validate it: " + name + " for " + validated_file + " by " + target.process_kit.getClass(), false)
							.send();
					validated_file.status = Status.ERROR;
					WatchFolderDB.push(Arrays.asList(validated_file));
					return;
				}
			}
			
			new_job = target.prepareTranscodeJob(pi_item.prepare_key(), validated_file.getName(), sub_dir_name, duration, mutator);
			jobs_to_watch.add(new_job);
			validated_file.map_job_target.put(new_job.getKey(), target.storage + ":" + target.profile);
			targets.get(pos).addToCopySourceFileList(send_source_to_dest, send_source_to_dest_storages_names, sub_dir_name);
		}
		WatchFolderDB.push(Arrays.asList(validated_file));
		
		JobContextWFDeleteSourceFile delete_source = new JobContextWFDeleteSourceFile();
		delete_source.neededstorages = new ArrayList<>(Arrays.asList(validated_file.storage_name));
		delete_source.path = validated_file.path;
		delete_source.storage = validated_file.storage_name;
		delete_source.send_to = send_source_to_dest;
		if (send_source_to_dest_storages_names.isEmpty() == false) {
			send_source_to_dest_storages_names.forEach(storage -> {
				if (delete_source.neededstorages.contains(storage) == false) {
					delete_source.neededstorages.add(storage);
				}
			});
		}
		
		Loggers.Transcode_WatchFolder.trace("Prepare delete source job " + name + " for " + validated_file + " " + delete_source.contextToJson());
		
		AppManager.createJob(delete_source).setCreator(
				
				getClass()).setName("Delete watchfolder source " + validated_file.getName()).setRequiredCompletedJob(jobs_to_watch).setDeleteAfterCompleted().publish(mutator);
		
		mutator.execute();
	}
	
	/**
	 * Try to read the first byte of the founded file.
	 */
	private static boolean canIReadThisFile(AbstractFoundedFile file) {
		InputStream i_stream = null;
		AbstractFile a_file = null;
		try {
			a_file = Storage.getByName(file.storage_name).getRootPath().getAbstractFile(file.path);
			i_stream = a_file.getInputStream(0xFF);
			i_stream.read();
			i_stream.close();
			a_file.close();
			return true;
		} catch (IOException e1) {
			Loggers.Transcode_WatchFolder.trace("Can't read file: " + file.storage_name + ":" + file.path, e1);
		} catch (Exception e2) {
			Loggers.Transcode_WatchFolder.warn("Error during file reading test: " + file.storage_name + ":" + file.path, e2);
		} finally {
			IOUtils.closeQuietly(i_stream);
			IOUtils.closeQuietly(a_file);
		}
		return false;
	}
	
	public static class Serializer implements JsonSerializer<WatchFolderEntry> {
		
		public JsonElement serialize(WatchFolderEntry entry, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject jo_entry = new JsonObject();
			if (entry == null) {
				return jo_entry;
			}
			
			jo_entry.addProperty("name", entry.name);
			jo_entry.addProperty("source_storage", entry.source_storage);
			jo_entry.add("targets", MyDMAM.gson_kit.getGsonSimple().toJsonTree(entry.targets));
			jo_entry.add("must_contain", MyDMAM.gson_kit.getGsonSimple().toJsonTree(entry.must_contain));
			jo_entry.add("limit_to_file_extentions", MyDMAM.gson_kit.getGsonSimple().toJsonTree(entry.limit_to_file_extentions));
			jo_entry.addProperty("min_file_size", entry.min_file_size);
			jo_entry.addProperty("time_to_sleep_between_scans", entry.time_to_sleep_between_scans);
			jo_entry.addProperty("time_to_wait_growing_file", entry.time_to_wait_growing_file);
			jo_entry.addProperty("want_to_stop", entry.want_to_stop);
			jo_entry.addProperty("isalive", entry.isAlive());
			return jo_entry;
		}
		
	}
	
	public JsonElement getInstanceStatusItem() {
		return MyDMAM.gson_kit.getGson().toJsonTree(this);
	}
	
	public String getReferenceKey() {
		return name;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return WatchFolderEntry.class;
	}
	
}
