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
import hd3gtv.mydmam.transcode.watchfolder.AbstractFoundedFile.Status;
import hd3gtv.mydmam.useraction.fileoperation.CopyMove;

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
	
	private transient boolean want_to_stop;
	
	class Target {
		String storage;
		String profile;
		
		// TODO add prefix/suffix for output file + recreate sub dir
		
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
		
		JobNG prepareTranscodeJob(String path_index_key, String simple_file_name, MutationBatch mutator) throws ConnectionException {
			JobContextTranscoder job_transcode = new JobContextTranscoder();
			job_transcode.source_pathindex_key = path_index_key;
			job_transcode.dest_storage_name = storage;
			job_transcode.neededstorages = Arrays.asList(storage);
			/** transcoding profile name */
			job_transcode.hookednames = Arrays.asList(profile);
			// TODO add prefix/suffix for output file + recreate sub dir
			
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
		List<Target> targets = new ArrayList<WatchFolderEntry.Target>(c_targets.size());
		
		for (int pos = 0; pos < c_targets.size(); pos++) {
			targets.add(new Target().init(c_targets.get(pos)));
		}
		
		time_to_wait_growing_file = Configuration.getValue(all_wf_confs, name, "time_to_wait_growing_file", 1000);
		time_to_sleep_between_scans = Configuration.getValue(all_wf_confs, name, "time_to_sleep_between_scans", 10000);
		min_file_size = Configuration.getValue(all_wf_confs, name, "min_file_size", 10000);
		temp_directory = new File(Configuration.getValue(all_wf_confs, name, "temp_directory", System.getProperty("java.io.tmpdir")));
		CopyMove.checkExistsCanRead(temp_directory);
		CopyMove.checkIsDirectory(temp_directory);
		CopyMove.checkIsWritable(temp_directory);
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
					
					if (new_files_to_add.isEmpty() == false) {
						WatchFolderDB.push(new_files_to_add);
						bulk = Elasticsearch.prepareBulk();
						try {
							explorer.refreshStoragePath(bulk, Arrays.asList(SourcePathIndexerElement.prepareStorageElement(source_storage)), false);
							bulk.terminateBulk();
						} catch (Exception e) {
							if (e instanceof ElasticsearchException) {
								Log2.log.error("Trouble during Elasticsearch updating", e);
							} else {
								throw e;
							}
						}
					}
					
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
		} catch (Exception e) {
			Log2.log.error("Fatal exception", e, new Log2Dump("name", name));
			AdminMailAlert.create("Fatal and not managed exception for WatchFolder", true).addDump(new Log2Dump("watch folder name", name)).setManager(manager).setThrowable(e).send();
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
					ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
					bulk.getConfiguration().setRefresh(true);
					explorer.refreshCurrentStoragePath(bulk, Arrays.asList(pi_item), true);
					bulk.terminateBulk();
					pi_item = explorer.getelementByIdkey(validated_file.getPathIndexKey());
				} catch (Exception e) {
					Log2.log.error("Can't update ES index", e, validated_file);
					return;
				}
			}
		} else {
			Log2.log.error("Can't found current item in ES", null, validated_file);
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
				Log2.log.error("Can't download found file to temp directory", e, validated_file);
				AdminMailAlert.create("Can't download watch folder found file to temp directory", false).addDump(validated_file).setThrowable(e).send();
				return;
			}
		}
		
		Container indexing_result = null;
		try {
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			MetadataIndexingOperation indexing = new MetadataIndexingOperation(physical_source).setReference(pi_item).setLimit(MetadataIndexingLimit.ANALYST);
			indexing_result = indexing.doIndexing();
			ContainerOperations.save(indexing_result, true, bulk);
			bulk.terminateBulk();
		} catch (Exception e) {
			Log2.log.error("Can't analyst MTD", null, validated_file);
			validated_file.status = Status.ERROR;
			WatchFolderDB.push(Arrays.asList(validated_file));
			return;
		}
		
		if (download_temp) {
			try {
				FileUtils.forceDelete(physical_source);
			} catch (Exception e) {
				Log2.log.error("Can't delete temp file", e, validated_file);
				AdminMailAlert.create("Can't delete temp file", false).addDump(validated_file).setThrowable(e).send();
			}
		}
		
		/**
		 * Process active files: transcoding jobs
		 */
		// indexing_result.getSummary().getMimetype();
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		ArrayList<JobNG> jobs_to_watch = new ArrayList<JobNG>(targets.size());
		for (int pos = 0; pos < targets.size(); pos++) {
			jobs_to_watch.add(targets.get(pos).prepareTranscodeJob(pi_item.prepare_key(), validated_file.getName(), mutator));
		}
		
		JobContextWFDeleteSourceFile delete_source = new JobContextWFDeleteSourceFile();
		delete_source.neededstorages = Arrays.asList(validated_file.storage_name);
		delete_source.path = validated_file.path;
		delete_source.storage = validated_file.storage_name;
		
		AppManager.createJob(delete_source).setCreator(getClass()).setName("Delete watchfolder source " + validated_file.getName()).setRequiredCompletedJob(jobs_to_watch).setDeleteAfterCompleted()
				.publish(mutator);
		mutator.execute();
	}
}
