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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.ElasticsearchException;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.IgnoreFiles;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.storage.StorageCrawler;
import hd3gtv.mydmam.transcode.JobContextTranscoder;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.watchfolder.AbstractFoundedFile.Status;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.Timecode;

class WatchFolderEntry implements Runnable {
	
	private String name;
	private String source_storage;
	private long time_to_wait_growing_file;
	private long time_to_sleep_between_scans;
	private long min_file_size;
	private File temp_directory;
	private List<Target> targets;
	private AppManager manager;
	private Explorer explorer;
	private List<MustContainType> must_contain;
	
	public enum MustContainType {
		video, audio
	}
	
	private transient boolean want_to_stop;
	
	class Target {
		String storage;
		String profile;
		String dest_file_prefix;
		String dest_file_suffix;
		boolean keep_input_dir_to_dest;
		
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
				throw new NullPointerException("Can't found transcode profile \"" + profile + "\" in \"" + name + "\" watch folder configuration");
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
			
			if (Loggers.Transcode_WatchFolder.isDebugEnabled()) {
				LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
				log.put("storage", storage);
				log.put("profile", profile);
				log.put("dest_file_prefix", dest_file_prefix);
				log.put("dest_file_suffix", dest_file_suffix);
				log.put("keep_input_dir_to_dest", keep_input_dir_to_dest);
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
	}
	
	WatchFolderEntry(AppManager manager, String name, HashMap<String, ConfigurationItem> all_wf_confs) throws Exception {
		this.manager = manager;
		this.name = name;
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
		temp_directory = new File(Configuration.getValue(all_wf_confs, name, "temp_directory", System.getProperty("java.io.tmpdir")));
		
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
		
		CopyMove.checkExistsCanRead(temp_directory);
		CopyMove.checkIsDirectory(temp_directory);
		CopyMove.checkIsWritable(temp_directory);
		
		if (Loggers.Transcode_WatchFolder.isInfoEnabled()) {
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put("name", name);
			log.put("source_storage", source_storage);
			log.put("targets", targets.size());
			log.put("time_to_wait_growing_file", time_to_wait_growing_file);
			log.put("time_to_sleep_between_scans", time_to_sleep_between_scans);
			log.put("min_file_size", min_file_size);
			log.put("must_contain", must_contain);
			log.put("temp_directory", temp_directory);
			Loggers.Transcode_WatchFolder.info("Load watchfolder entry " + log);
		}
		
	}
	
	public String getName() {
		return name;
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
			return IgnoreFiles.default_list;
		}
		
		public boolean onStartSearch(AbstractFile search_root_path) {
			Loggers.Transcode_WatchFolder.debug("Start search for \"" + name + "\" in " + search_root_path.getPath());
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
							active_file.status = Status.ERROR;
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
							Loggers.Transcode_WatchFolder.trace("Set found file to validated files in " + name + ": " + db_entry_file);
							validated_files.add(db_entry_file);
						} else {
							Loggers.Transcode_WatchFolder.trace("This file has stopped to grow, wait the time to validate in " + name + ": " + db_entry_file);
						}
						active_file.last_checked = db_entry_file.last_checked;
					}
					
					WatchFolderDB.push(active_files);
					
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
		boolean download_temp = false;
		if (physical_source == null) {
			download_temp = true;
			try {
				physical_source = Storage.getDistantFile(pi_item, temp_directory);
			} catch (IOException e) {
				Loggers.Transcode_WatchFolder.error("Can't download found file to temp directory " + name + " for " + validated_file, e);
				AdminMailAlert.create("Can't download watch folder found file to temp directory", false).setThrowable(e).send();
				validated_file.status = Status.ERROR;
				return;
			}
		}
		
		Container indexing_result = null;
		try {
			Loggers.Transcode_WatchFolder.trace("Save item to ES " + name + " for " + validated_file);
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			MetadataIndexingOperation indexing = new MetadataIndexingOperation(physical_source).setReference(pi_item).setLimit(MetadataIndexingLimit.ANALYST);
			indexing_result = indexing.doIndexing();
			ContainerOperations.save(indexing_result, true, bulk);
			bulk.terminateBulk();
		} catch (Exception e) {
			Loggers.Transcode_WatchFolder.error("Can't analyst MTD " + name + " for " + validated_file, e);
			validated_file.status = Status.ERROR;
			WatchFolderDB.push(Arrays.asList(validated_file));
			return;
		}
		
		if (download_temp) {
			try {
				Loggers.Transcode_WatchFolder.trace("Delete temp file " + name + " for " + validated_file + " file: " + physical_source);
				FileUtils.forceDelete(physical_source);
			} catch (Exception e) {
				Loggers.Transcode_WatchFolder.error("Can't delete temp file " + name + " for " + validated_file, e);
				AdminMailAlert.create("Can't delete temp file", false).setThrowable(e).send();
			}
		}
		
		/**
		 * Process active files: transcoding jobs
		 */
		Timecode duration = null;
		
		FFprobe ffprobe = indexing_result.getByClass(FFprobe.class);
		
		if (must_contain.contains(MustContainType.video.name()) | must_contain.contains(MustContainType.audio.name())) {
			if (ffprobe == null) {
				Loggers.Transcode_WatchFolder.error("Invalid file dropped in watchfolder: it must be a media file " + name + " for " + validated_file);
				AdminMailAlert.create("Invalid file dropped in watchfolder: it must be a media file", false).send();
				validated_file.status = Status.ERROR;
				return;
			} else if (must_contain.contains(MustContainType.video.name()) & (ffprobe.hasVideo() == false)) {
				Loggers.Transcode_WatchFolder.error("Invalid file dropped in watchfolder: it must have a video track " + name + " for " + validated_file);
				AdminMailAlert.create("Invalid file dropped in watchfolder: it must have a video track", false).send();
				validated_file.status = Status.ERROR;
				return;
			} else if (must_contain.contains(MustContainType.audio.name()) & (ffprobe.hasAudio() == false)) {
				Loggers.Transcode_WatchFolder.error("Invalid file dropped in watchfolder: it must have an audio track " + name + " for " + validated_file);
				AdminMailAlert.create("Invalid file dropped in watchfolder: it must have an audio track", false).send();
				validated_file.status = Status.ERROR;
				return;
			}
			duration = ffprobe.getDuration();
		}
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		ArrayList<JobNG> jobs_to_watch = new ArrayList<JobNG>(targets.size());
		
		String sub_dir_name = validated_file.path.substring(0, validated_file.path.length() - (validated_file.getName().length() + 1));
		
		Loggers.Transcode_WatchFolder.trace("Prepare all transcode jobs " + name + " for " + validated_file + ", in " + sub_dir_name);
		
		for (int pos = 0; pos < targets.size(); pos++) {
			jobs_to_watch.add(targets.get(pos).prepareTranscodeJob(pi_item.prepare_key(), validated_file.getName(), sub_dir_name, duration, mutator));
		}
		
		JobContextWFDeleteSourceFile delete_source = new JobContextWFDeleteSourceFile();
		delete_source.neededstorages = Arrays.asList(validated_file.storage_name);
		delete_source.path = validated_file.path;
		delete_source.storage = validated_file.storage_name;
		
		Loggers.Transcode_WatchFolder.trace("Prepare delete source job " + name + " for " + validated_file + " " + delete_source.contextToJson());
		
		AppManager.createJob(delete_source).setCreator(getClass()).setName("Delete watchfolder source " + validated_file.getName()).setRequiredCompletedJob(jobs_to_watch).setDeleteAfterCompleted()
				.publish(mutator);
				
		mutator.execute();
	}
}
