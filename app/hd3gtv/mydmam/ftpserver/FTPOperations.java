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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.GsonIgnoreStrategy;

public class FTPOperations {
	
	private static Explorer explorer = new Explorer();
	private static final long DELAY_ACTIVE_GROUPS_PATHINDEXING = TimeUnit.SECONDS.toMillis(60);
	private static final long DELAY_BACKGROUD_OPERATIONS = TimeUnit.MINUTES.toMillis(1); // XXX set to 10
	
	private static final Gson gson;
	// private static final Gson simple_gson;
	// private static final Gson pretty_gson;
	
	private static FTPOperations global;
	
	static {
		global = new FTPOperations();
		
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		
		/*builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		builder.registerTypeAdapter(new TypeToken<ArrayList<WatchFolderEntry>>() {
		}.getType(), new WatchFolderEntry.SerializerList());
		builder.registerTypeAdapter(new TypeToken<LinkedHashMap<String, TranscoderWorker>>() {
		}.getType(), new TranscoderWorker.SerializerMap());
		builder.registerTypeAdapter(TranscodeProfile.class, new TranscodeProfile.Serializer());*/
		
		// simple_gson = builder.create();
		
		/*builder.registerTypeAdapter(InstanceStatus.class, new InstanceStatus.Serializer());
		builder.registerTypeAdapter(InstanceAction.class, new InstanceAction.Serializer());
		builder.registerTypeAdapter(JobNG.class, new JobNG.Serializer());
		builder.registerTypeAdapter(JobAction.class, new JobAction.Serializer());
		builder.registerTypeAdapter(GsonThrowable.class, new GsonThrowable.Serializer());
		builder.registerTypeAdapter(WorkerCapablitiesExporter.class, new WorkerCapablitiesExporter.Serializer());
		builder.registerTypeAdapter(WorkerExporter.class, new WorkerExporter.Serializer());
		
		builder.registerTypeAdapter(JobContext.class, new JobContext.Serializer());
		builder.registerTypeAdapter(new TypeToken<ArrayList<JobContext>>() {
		}.getType(), new JobContext.SerializerList());
		
		builder.registerTypeAdapter(JobCreatorDeclarationSerializer.class, new JobCreatorDeclarationSerializer());
		builder.registerTypeAdapter(TriggerJobCreator.class, TriggerJobCreator.serializer);
		builder.registerTypeAdapter(CyclicJobCreator.class, CyclicJobCreator.serializer);*/
		
		gson = builder.create();
		// pretty_gson = builder.setPrettyPrinting().create();
	}
	
	public static FTPOperations get() {
		return global;
	}
	
	public static Gson getGson() {
		return gson;
	}
	
	/*public static Gson getSimpleGson() {
		return simple_gson;
	}*/
	
	private boolean stop;
	private Internal internal;
	
	public void start() {
		stop();
		
		internal = new Internal();
		internal.start();
	}
	
	/**
	 * Blocking
	 */
	public synchronized void stop() {
		if (internal == null) {
			return;
		}
		stop = true;
		try {
			while (internal.isAlive()) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			Loggers.FTPserver.warn("Can't sleep during stop", e);
		}
	}
	
	private HashSet<FTPUser> active_users_with_group_pathindex;
	
	private FTPOperations() {
		active_users_with_group_pathindex = new HashSet<FTPUser>();
	}
	
	synchronized void addActiveUser(FTPUser ftp_user) {
		if (active_users_with_group_pathindex.contains(ftp_user)) {
			return;
		}
		if (ftp_user.getGroup().getPathindexStoragename() != null) {
			active_users_with_group_pathindex.add(ftp_user);
		}
	}
	
	synchronized void removeActiveUser(FTPUser ftp_user) {
		active_users_with_group_pathindex.remove(ftp_user);
	}
	
	private class Internal extends Thread {
		
		public Internal() {
			setName("FTPWatchdog");
			setDaemon(true);
		}
		
		public void run() {
			stop = false;
			
			HashSet<FTPGroup> current_active_user_groups = new HashSet<FTPGroup>(1);
			List<SourcePathIndexerElement> storages_active_group_to_refresh = new ArrayList<SourcePathIndexerElement>();
			List<SourcePathIndexerElement> storages_regular_to_refresh = new ArrayList<SourcePathIndexerElement>();
			List<SourcePathIndexerElement> storages_to_force_refresh = new ArrayList<SourcePathIndexerElement>();
			ElasticsearchBulkOperation bulk_op;
			MutationBatch mutator;
			List<FTPUser> trashable_users;
			List<FTPUser> purgeable_users;
			List<FTPUser> actual_users;
			long next_backgroud_operations = 0; // System.currentTimeMillis() + DELAY_BACKGROUD_OPERATIONS; TODO SET
			SourcePathIndexerElement storage;
			
			try {
				while (stop == false) {
					try {
						synchronized (active_users_with_group_pathindex) {
							current_active_user_groups.clear();
							for (FTPUser ftpuser : active_users_with_group_pathindex) {
								current_active_user_groups.add(ftpuser.getGroup());
							}
						}
						
						if (current_active_user_groups.isEmpty() == false) {
							bulk_op = Elasticsearch.prepareBulk();
							/**
							 * Search active groups recently added => force refresh
							 */
							storages_to_force_refresh.clear();
							for (FTPGroup ftpgroup : current_active_user_groups) {
								storage = SourcePathIndexerElement.prepareStorageElement(ftpgroup.getPathindexStoragename());
								if (storages_active_group_to_refresh.contains(storage) == false) {
									storages_to_force_refresh.add(storage);
								}
							}
							
							/**
							 * Previously active groups added => normal refresh
							 */
							storages_active_group_to_refresh.clear();
							for (FTPGroup ftpgroup : current_active_user_groups) {
								storage = SourcePathIndexerElement.prepareStorageElement(ftpgroup.getPathindexStoragename());
								if (storages_to_force_refresh.contains(storage) == false) {
									storages_active_group_to_refresh.add(storage);
								}
							}
							
							explorer.refreshStoragePath(bulk_op, storages_active_group_to_refresh, false, DELAY_ACTIVE_GROUPS_PATHINDEXING * 2);
							explorer.refreshStoragePath(bulk_op, storages_to_force_refresh, true, DELAY_ACTIVE_GROUPS_PATHINDEXING * 2);
							bulk_op.terminateBulk();
						}
						
						if (next_backgroud_operations < System.currentTimeMillis()) {
							next_backgroud_operations = System.currentTimeMillis() + DELAY_BACKGROUD_OPERATIONS;
							
							/**
							 * Trash/purge operations
							 */
							trashable_users = FTPUser.getTrashableUsers();
							if (trashable_users.isEmpty() == false) {
								mutator = CassandraDb.prepareMutationBatch();
								for (int pos = 0; pos < trashable_users.size(); pos++) {
									trashable_users.get(pos).setDisabled(true);
									trashable_users.get(pos).save(mutator);
									trashable_users.get(pos).getGroup().trashUserHomeDirectory(trashable_users.get(pos));
								}
								mutator.execute();
								
								purgeable_users = FTPUser.getPurgeableUsers(trashable_users);
								if (purgeable_users.isEmpty() == false) {
									mutator = CassandraDb.prepareMutationBatch();
									for (int pos = 0; pos < purgeable_users.size(); pos++) {
										purgeable_users.get(pos).removeUser(mutator);
										purgeable_users.get(pos).getGroup().deleteUserHomeDirectory(purgeable_users.get(pos));
									}
									mutator.execute();
								}
							} /** end trash operation */
								
							/**
							 * Group operations
							 */
							storages_regular_to_refresh.clear();
							for (FTPGroup group : FTPGroup.getDeclaredGroups().values()) {
								/**
								 * Free space
								 */
								group.checkFreeSpace();
								
								/**
								 * Orphan directories
								 */
								actual_users = group.listAllActualUsers();
								for (int pos = 0; pos < actual_users.size(); pos++) {
									if (actual_users.get(pos).isValidInDB() == false) {
										group.deleteUserHomeDirectory(actual_users.get(pos));
									}
								}
								
								/**
								 * Refresh pathindex
								 */
								if (group.getPathindexStoragename() != null) {
									if (current_active_user_groups.contains(group) == false) {
										/**
										 * Configuration set a pathindexing, and group is not active.
										 */
										storages_regular_to_refresh.add(SourcePathIndexerElement.prepareStorageElement(group.getPathindexStoragename()));
									}
								}
								
							} /** end for each groups */
								
							if (storages_regular_to_refresh.isEmpty() == false) {
								bulk_op = Elasticsearch.prepareBulk();
								explorer.refreshStoragePath(bulk_op, storages_regular_to_refresh, false, DELAY_BACKGROUD_OPERATIONS * 5);
								bulk_op.terminateBulk();
							}
							
						} /** end next_backgroud_operations */
							
					} catch (ConnectionException e) {
						Loggers.FTPserver.warn("Can't access to db", e);
					} catch (Exception e) {
						Loggers.FTPserver.error("General operation error", e);
					}
					
					sleep(DELAY_ACTIVE_GROUPS_PATHINDEXING);
				}
			} catch (InterruptedException e) {
				Loggers.FTPserver.error("Can't sleep", e);
			}
		}
	}
	
}
