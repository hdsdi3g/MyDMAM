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
import java.util.concurrent.DelayQueue;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.ftpserver.Session.SessionActivity;

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
	
	private SlicedQueueLists<SessionActivity> pending_activities;
	
	private FTPOperations() {
		pending_activities = new SlicedQueueLists<Session.SessionActivity>();
	}
	
	public void pushActivity(SessionActivity activity) {
		pending_activities.push(activity);
	}
	
	private class Internal extends Thread {
		
		public Internal() {
			setName("FTPWatchdog");
			setDaemon(true);
		}
		
		public void run() {
			stop = false;
			
			try {
				DelayQueue<SessionActivity> bulk = new DelayQueue<Session.SessionActivity>();
				ArrayList<SessionActivity> valid_list = new ArrayList<Session.SessionActivity>();
				
				while (stop == false) {
					pending_activities.pullNextSlice(bulk, valid_list);
					
					for (int pos = 0; pos < valid_list.size(); pos++) {
						valid_list.get(pos); // TODO SessionActivity
					}
					
					// TODO Group checks tasks
					
					sleep(1000);
				}
			} catch (InterruptedException e) {
				Loggers.FTPserver.error("Can't sleep", e);
			}
		}
	}
}
