/*
 * This file is part of Java Simple ServiceManager
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2008-2014
 * 
*/

package hd3gtv.javasimpleservice;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.taskqueue.WorkerGroup;
import hd3gtv.tools.TimeUtils;

import java.lang.management.ManagementFactory;

public abstract class ServiceManager implements ServiceInformations {
	
	private ServiceInformations serviceinformations;
	private ServiceMessagemanager messagemanager;
	private long javastarttime;
	private boolean stopservice;
	private ServiceThread servicethread;
	private UIFrame uiframe;
	private WorkerGroup workergroup;
	private IsAlive isalive;
	
	private static String instancename;
	private static String instancenamepid;
	
	public static String getInstancename(boolean addpid) {
		if (instancename == null) {
			instancename = Configuration.global.getValue("service", "workername", "unknown-pleaseset-" + String.valueOf(System.currentTimeMillis()));
		}
		if (instancenamepid == null) {
			instancenamepid = ManagementFactory.getRuntimeMXBean().getName();
			instancenamepid = instancenamepid.substring(0, instancenamepid.indexOf("@"));
		}
		if (addpid) {
			return instancename + "#" + instancenamepid;
		} else {
			return instancename;
		}
	}
	
	public static String getInstancePID() {
		if (instancenamepid == null) {
			instancenamepid = ManagementFactory.getRuntimeMXBean().getName();
			instancenamepid = instancenamepid.substring(0, instancenamepid.indexOf("@"));
		}
		return instancenamepid;
	}
	
	protected abstract void startApplicationService() throws Exception;
	
	protected abstract void stopApplicationService() throws Exception;
	
	/**
	 * Le service c'est init, on le lance juste apres ceci.
	 */
	protected abstract void postClassInit() throws Exception;
	
	/**
	 * If this system property is not defined, it's defined.
	 * It allows to define a default value for a key and always have a no null value for this property key.
	 * @param key the key like -Dkey=value
	 * @param defaultvalue the value to set
	 */
	public static final void injectDefaultSystemProperty(String key, String defaultvalue) {
		String currentvalue = System.getProperty(key, "");
		if (currentvalue.trim().equals("")) {
			System.setProperty(key, defaultvalue);
			
			Log2Dump dump = new Log2Dump();
			dump.add(key, defaultvalue);
			
			StringBuffer sb = new StringBuffer();
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			// 0 = Thread, 1 = ServiceManager, 2 = le vrai call
			sb.append(stack[2].getClassName());
			sb.append(".");
			sb.append(stack[2].getMethodName());
			sb.append("(");
			sb.append(stack[2].getFileName());
			sb.append(":");
			sb.append(stack[2].getLineNumber());
			sb.append(")");
			dump.add("by", sb);
			
		}
	}
	
	public void setWorkergroup(WorkerGroup workergroup) {
		this.workergroup = workergroup;
	}
	
	public synchronized void restart() {
		Log2.log.info("Manual restart service");
		stopService();
		startService();
	}
	
	public final ServiceInformations getServiceinformations() {
		return serviceinformations;
	}
	
	public final ServiceMessagemanager getMessagemanager() {
		return messagemanager;
	}
	
	public final long getJavaUptime() {
		return System.currentTimeMillis() - javastarttime;
	}
	
	public final String getJavaUptimeTime() {
		return TimeUtils.secondsToYWDHMS(getJavaUptime() / 1000);
	}
	
	/**
	 * A la creation de la classe, lance le service.
	 * Arrete le service a la fermeture.
	 * @param serviceinformations peut etre null si la classe finale implemente ServiceInformations
	 */
	public ServiceManager(String[] args, ServiceInformations serviceinformations) {
		javastarttime = System.currentTimeMillis();
		
		if (args == null) {
			throw new NullPointerException("\"args\" can't to be null");
		}
		if (serviceinformations == null) {
			if (this instanceof ServiceInformations) {
				this.serviceinformations = this;
			} else {
				throw new NullPointerException("\"serviceinformations\" can't to be null");
			}
		} else {
			this.serviceinformations = serviceinformations;
		}
		
		try {
			messagemanager = new ServiceMessagemanager(this);
		} catch (Exception e) {
			Log2.log.error("Can't load mail configuration", e);
			System.exit(1);
		}
		
		try {
			CassandraDb.autotest();
			
			isalive = new IsAlive(this);
			
			postClassInit();
			
			Thread t = new Thread() {
				public void run() {
					try {
						Log2.log.info("Request shutdown application");
						Thread tkill = new Thread() {
							public void run() {
								try {
									sleep(8000);
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
						stopService();
					} catch (Exception e) {
						Log2.log.error("Fatal service stopping", e);
						messagemanager.sendMessage(new ServiceMessageError("Can't stop the service", e));
					}
				}
			};
			t.setName("Shutdown Hook");
			Runtime.getRuntime().addShutdownHook(t);
			
		} catch (Exception e) {
			Log2.log.error("Fatal service error", e, serviceinformations);
			messagemanager.sendMessage(new ServiceMessageError("Serious internal error", e, serviceinformations));
			System.exit(2);
		}
		
		startService();
	}
	
	public boolean isWorkingToShowUIStatus() {
		if (workergroup != null) {
			return workergroup.isWorking();
		}
		return false;
	}
	
	public long refreshUIStatusDelay() {
		return 500;
	}
	
	private class ServiceThread extends Thread {
		
		ServiceManager servicemanager;
		
		public ServiceThread(ServiceManager servicemanager) {
			setName("Service");
			this.servicemanager = servicemanager;
		}
		
		public void run() {
			stopservice = false;
			try {
				isalive.start();
				
				if (Configuration.global.isElementExists("service")) {
					String uititle = Configuration.global.getValue("service", "ui", null);
					if (uititle != null) {
						try {
							uiframe = new UIFrame(uititle, servicemanager);
							uiframe.display();
						} catch (Exception e) {
							Log2.log.error("Can't display UI", e);
						}
					}
				}
				
				Log2.log.info("Startup service...", getServiceinformations());
				startApplicationService();
				
				while (stopservice == false) {
					sleep(10);
				}
				
				isalive.stopWatch();
				
				stopApplicationService();
				
			} catch (InterruptedException e) {
				Log2.log.error("Violent stop service", e);
			} catch (Exception e) {
				Log2.log.error("ServiceManager execution error...", e, getServiceinformations());
				messagemanager.sendMessage(new ServiceMessageError("Runtime Error Service", e, getServiceinformations()));
			}
		}
	}
	
	public final synchronized void startService() {
		if (servicethread == null) {
			servicethread = new ServiceThread(this);
		}
		
		if (servicethread.isAlive() == false) {
			servicethread.start();
		} else {
			Log2.log.error("Service is already started", null);
		}
	}
	
	public final synchronized void stopService() {
		if (servicethread == null) {
			return;
		}
		stopservice = true;
		try {
			for (int pos = 0; pos < 100; pos++) {
				if (servicethread.isAlive() == false) {
					return;
				}
				Thread.sleep(10);
			}
			Log2.log.error("Can't wait for stopping ServiceManager...", null, getServiceinformations());
		} catch (Exception e) {
			Log2.log.error("ServiceManager stop execution error...", e, getServiceinformations());
		}
	}
}
