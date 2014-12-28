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
package hd3gtv.mydmam.manager;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;

public abstract class ServiceNG {
	
	private boolean want_stop_service;
	private ServiceThread servicethread;
	private AppManager manager;
	
	// private UIFrame uiframe; //TODO #78.3, add uiframe
	
	public ServiceNG(String[] args, String app_name) throws Exception {
		CassandraDb.autotest();
		
		Thread t = new Thread() {
			public void run() {
				try {
					Log2.log.info("Request shutdown application");
					Thread tkill = new Thread() {
						public void run() {
							try {
								sleep(5000);
								Log2.log.error("Request KILL application", null);
								System.exit(2);
							} catch (Exception e) {
								Log2.log.error("Fatal service killing", e);
								System.exit(2);
							}
						}
					};
					tkill.setName("Shutdown KILL");
					tkill.setDaemon(true);
					tkill.start();
					stopAllServices();
				} catch (Exception e) {
					Log2.log.error("Fatal service stopping", e);
					AdminMailAlert.create("Can't stop the service", true).setThrowable(e).send();
				}
			}
		};
		t.setName("Shutdown Hook");
		Runtime.getRuntime().addShutdownHook(t);
		
		manager = new AppManager(app_name);
		
		startAllServices();
	}
	
	protected AppManager getManager() {
		return manager;
	}
	
	private class ServiceThread extends Thread {
		
		public ServiceThread() {
			setName("MyDMAM Service");
		}
		
		public void run() {
			want_stop_service = false;
			try {
				/*if (Configuration.global.isElementExists("service")) {
					String uititle = Configuration.global.getValue("service", "ui", null);
					if (uititle != null) {
						try {
							// TODO #78.3, add UIFrame
							uiframe = new UIFrame(uititle, servicemanager);
							uiframe.display();
						} catch (Exception e) {
							Log2.log.error("Can't display UI", e);
						}
					}
				}*/
				
				startService();
				
				manager.startAll();
				
				while (want_stop_service == false) {
					sleep(10);
				}
				
				stopService();
				
				manager.stopAll();
				
			} catch (InterruptedException e) {
				Log2.log.error("Violent stop service", e);
			} catch (Exception e) {
				Log2.log.error("ServiceManager execution error...", e);
				AdminMailAlert.create("Runtime Error Service", true).setThrowable(e).setManager(manager).send();
			}
		}
	}
	
	protected abstract void startService() throws Exception;
	
	protected abstract void stopService() throws Exception;
	
	public void startAllServices() {
		if (servicethread == null) {
			servicethread = new ServiceThread();
		}
		if (servicethread.isAlive() == false) {
			servicethread.start();
		}
	}
	
	public synchronized void stopAllServices() {
		if (servicethread == null) {
			return;
		}
		want_stop_service = true;
		try {
			for (int pos = 0; pos < 100; pos++) {
				if (servicethread.isAlive() == false) {
					return;
				}
				Thread.sleep(10);
			}
			Log2.log.error("Can't wait for stopping ServiceManager...", null);
		} catch (Exception e) {
			Log2.log.error("ServiceManager stop execution error...", e);
		}
		servicethread = null;
	}
}
