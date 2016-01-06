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

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.tools.CopyMove;

public abstract class ServiceNG {
	
	private boolean want_stop_service;
	private ServiceThread servicethread;
	private AppManager manager;
	
	private UIFrame uiframe;
	
	public ServiceNG(String[] args, String app_name) throws Exception {
		CassandraDb.autotest();
		
		Thread t = new Thread() {
			public void run() {
				try {
					Loggers.Manager.log(Level.ALL, "Request shutdown application");
					Thread tkill = new Thread() {
						public void run() {
							try {
								sleep(5000);
								Loggers.Manager.log(Level.ALL, "Request KILL application");
								System.exit(2);
							} catch (Exception e) {
								Loggers.Manager.log(Level.ALL, "Fatal service killing", e);
								System.exit(2);
							}
						}
					};
					tkill.setName("Shutdown KILL");
					tkill.setDaemon(true);
					tkill.start();
					stopAllServices();
				} catch (Exception e) {
					Loggers.Manager.error("Fatal service stopping", e);
					AdminMailAlert.create("Can't stop the service", true).setThrowable(e).send();
				}
				LogManager.shutdown();
			}
		};
		t.setName("Shutdown Hook");
		Runtime.getRuntime().addShutdownHook(t);
		
		manager = new AppManager(app_name);
		
		Thread watch_log_conf = new Thread() {
			public void run() {
				try {
					Loggers.Manager.debug("Init watchdog");
					
					CopyMove.checkExistsCanRead(Loggers.log4j_xml_configuration_file);
					
					long last_check_date = Loggers.log4j_xml_configuration_file.lastModified();
					while (true) {
						Thread.sleep(1000);
						
						Loggers.Manager.trace("Loop watch last XML date");
						if (last_check_date != Loggers.log4j_xml_configuration_file.lastModified()) {
							Loggers.Manager.info("XML file has changed, start refresh from: " + Loggers.log4j_xml_configuration_file);
							Loggers.refreshLogConfiguration();
							last_check_date = Loggers.log4j_xml_configuration_file.lastModified();
						}
					}
				} catch (Exception e) {
				
				}
			}
		};
		watch_log_conf.setName("Watchdog change configuration loggers");
		watch_log_conf.setDaemon(true);
		watch_log_conf.start();
		
		startAllServices();
	}
	
	protected AppManager getManager() {
		return manager;
	}
	
	/**
	 * Set false to stop Broker at start
	 */
	protected boolean startBroker() {
		return true;
	}
	
	private class ServiceThread extends Thread {
		
		public ServiceThread() {
			setName("Service");
		}
		
		public void run() {
			Loggers.Manager.debug("Start service thread");
			want_stop_service = false;
			try {
				if (Configuration.global.isElementExists("service")) {
					String uititle = Configuration.global.getValue("service", "ui", null);
					if (uititle != null) {
						try {
							uiframe = new UIFrame(uititle, manager);
							uiframe.display();
						} catch (Exception e) {
							Loggers.Manager.error("Can't display UI", e);
						}
					}
				}
				
				startService();
				
				if (startBroker()) {
					manager.startAll();
				} else {
					manager.startJustService();
				}
				
				Loggers.Manager.debug("Manager is loaded");
				while (want_stop_service == false) {
					sleep(10);
				}
				Loggers.Manager.info("Stop service required");
				
				stopService();
				
				manager.stopAll();
				
				Loggers.Manager.info("Thanks for using MyDMAM");
			} catch (InterruptedException e) {
				Loggers.Manager.error("Violent stop service", e);
			} catch (Exception e) {
				Loggers.Manager.error("ServiceManager execution error...", e);
				AdminMailAlert.create("Runtime Error Service", true).setThrowable(e).setManager(manager).send();
			}
		}
	}
	
	protected abstract void startService() throws Exception;
	
	protected abstract void stopService() throws Exception;
	
	public void startAllServices() {
		Loggers.Manager.debug("Start all services...");
		if (servicethread == null) {
			servicethread = new ServiceThread();
		}
		if (servicethread.isAlive() == false) {
			servicethread.start();
		}
	}
	
	public synchronized void stopAllServices() {
		Loggers.Manager.debug("Stop all services...");
		if (servicethread == null) {
			return;
		}
		want_stop_service = true;
		try {
			for (int pos = 0; pos < 200; pos++) {
				if (servicethread.isAlive() == false) {
					return;
				}
				Thread.sleep(10);
			}
			Loggers.Manager.error("Can't wait for stopping services...");
		} catch (Exception e) {
			Loggers.Manager.error("Services stop execution error...", e);
		}
		servicethread = null;
	}
}
