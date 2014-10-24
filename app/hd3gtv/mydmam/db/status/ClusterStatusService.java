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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.db.status;

public class ClusterStatusService extends ClusterStatus {
	
	private boolean dostop;
	private Watch watch;
	private int sleep_time = 5000; // 5 sec
	
	public ClusterStatusService() {
		watch = new Watch();
	}
	
	public boolean isAlive() {
		return watch.isAlive();
	}
	
	private class Watch extends Thread {
		public Watch() {
			setDaemon(true);
			setName("Watch all clusters status");
		}
		
		public void run() {
			dostop = false;
			while (dostop == false) {
				try {
					refresh();
				} catch (Exception e) {
				}
				try {
					sleep(sleep_time);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
	
	public synchronized void start() {
		if (watch.isAlive() == false) {
			watch = new Watch();
			watch.start();
		}
	}
	
	public synchronized void stop() {
		dostop = true;
	}
}
