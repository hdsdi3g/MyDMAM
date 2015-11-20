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

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.pathindexing.Explorer;

public class FTPOperations {
	
	private static FTPOperations global;
	
	static {
		global = new FTPOperations();
	}
	
	public static FTPOperations get() {
		return global;
	}
	
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
	
	private HashSet<Session> opened_sessions;
	
	private FTPOperations() {
		opened_sessions = new HashSet<Session>();
	}
	
	synchronized void addSession(Session session) {
		if (opened_sessions.contains(session)) {
			return;
		}
		if (session.getUser().getGroup().getPathindexStoragenameLiveUpdate() != null) {
			opened_sessions.add(session);
		}
	}
	
	synchronized void closeSession(Session session) {
		opened_sessions.remove(session);
	}
	
	private static Explorer explorer = new Explorer();
	public static final long DELAY_2_INDEX_FOR_PATHINDEX = TimeUnit.SECONDS.toMillis(60);
	
	private class Internal extends Thread {
		
		public Internal() {
			setName("FTPWatchdog");
			setDaemon(true);
		}
		
		public void run() {
			stop = false;
			
			HashSet<FTPUser> current_active_users = new HashSet<FTPUser>(1);
			ElasticsearchBulkOperation bulk_op;
			
			try {
				while (stop == false) {
					synchronized (opened_sessions) {
						current_active_users.clear();
						for (Session session : opened_sessions) {
							current_active_users.add(session.getUser());
						}
					}
					
					bulk_op = Elasticsearch.prepareBulk();
					
					for (FTPUser ftpuser : current_active_users) {
						if (ftpuser.getLastPathIndexRefreshed() + DELAY_2_INDEX_FOR_PATHINDEX < System.currentTimeMillis()) {
							// TODO live index
							// String storage_name = .getPathindexStoragenameLiveUpdate();
							// explorer.refreshCurrentStoragePath(bulk_op, elements, purge_before);
							// explorer.refreshStoragePath(bulk_op, elements, purge_before);
							ftpuser.setLastPathIndexRefreshed();
						}
					}
					
					// TODO *regular* index for all group has pathindex_storagename_for_live_update, but not from current_active_users
					bulk_op.terminateBulk();
					
					// TODO Group checks tasks
					
					for (int pos_sleep = 0; pos_sleep < 10 * 60 * 10; pos_sleep++) {
						if (stop) {
							return;
						}
						sleep(100);
					}
				}
			} catch (InterruptedException e) {
				Loggers.FTPserver.error("Can't sleep", e);
			}
		}
	}
}
