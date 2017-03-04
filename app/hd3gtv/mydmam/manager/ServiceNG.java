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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import ext.StartPlayNoLazyLoad;
import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.bcastautomation.BCAWatcher;
import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.ftpserver.FTPGroup;
import hd3gtv.mydmam.ftpserver.FTPOperations;
import hd3gtv.mydmam.ftpserver.FTPlet;
import hd3gtv.mydmam.mail.AdminMailAlert;
import hd3gtv.mydmam.metadata.WorkerIndexer;
import hd3gtv.mydmam.pathindexing.PathScan;
import hd3gtv.mydmam.transcode.TranscoderWorker;
import hd3gtv.mydmam.transcode.watchfolder.WatchFolderTranscoder;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;
import play.Play;
import play.Play.Mode;
import play.server.Server;

public final class ServiceNG {
	
	private boolean want_stop_service;
	private ServiceThread servicethread;
	private AppManager manager;
	private ShutdownHook shutdown_hook;
	
	private final boolean enable_play;
	private final boolean enable_ftpserver;
	private final boolean enable_background_services;
	
	public ServiceNG(boolean enable_play, boolean enable_ftpserver, boolean enable_background_services) throws Exception {
		CassandraDb.autotest();
		
		this.enable_play = enable_play;
		this.enable_ftpserver = enable_ftpserver;
		this.enable_background_services = enable_background_services;
		
		shutdown_hook = new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdown_hook);
		
		StringJoiner names = new StringJoiner(", ");
		if (enable_play) {
			names.add("Play server");
		}
		if (enable_ftpserver) {
			names.add("FTP Server");
		}
		if (enable_background_services) {
			names.add("Background services");
		}
		
		manager = new AppManager();
		manager.setAppName(names.toString());
		
		WatchdogLogConf watch_log_conf = new WatchdogLogConf();
		watch_log_conf.start();
		
		if (enable_ftpserver) {
			ftp_servers = new ArrayList<FtpServer>();
			FTPGroup.registerAppManager(this.manager);
		}
	}
	
	private ArrayList<FtpServer> ftp_servers;
	private FTPOperations ftp_server_operations;
	private WatchFolderTranscoder wf_trancoder;
	private BCAWatcher bca_watcher;
	
	private void startInternalServices() throws Exception {
		if (enable_ftpserver) {
			HashMap<String, ConfigurationItem> all_instances_confs = Configuration.global.getElement("ftpserverinstances");
			if (all_instances_confs.isEmpty()) {
				return;
			}
			
			HashMap<String, Ftplet> ftplets = new HashMap<String, Ftplet>(1);
			ftplets.put("default", new FTPlet());
			
			for (Map.Entry<String, ConfigurationItem> entry : all_instances_confs.entrySet()) {
				try {
					ftp_servers.add(FTPOperations.ftpServerFactory(entry.getKey(), all_instances_confs, ftplets));
				} catch (Exception e) {
					Loggers.FTPserver.error("Can't load FTP server instance " + entry.getKey(), e);
				}
			}
			
			if (ftp_servers.isEmpty()) {
				return;
			}
			
			ftp_server_operations = FTPOperations.get();
			ftp_server_operations.start();
		}
		
		if (enable_play) {
			/*
			 * java
			 * ok -javaagent:c:\..\play\framework/play-1.3.0.jar
			 * ok -Dservice.config.apply=debug
			 * ok -noverify
			 * ok -Dfile.encoding=utf-8
			 * nope -Xdebug
			 * nope -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
			 * ok -Dplay.debug=yes
			 * nope -classpath <>
			 * ok -Dapplication.path=C:\...\mydmam
			 * ok -Dplay.id=
			 * ok play.server.Server ""
			 */
			try {
				Class.forName("play.modules.docviewer.DocumentationGenerator");
			} catch (Exception e) {
				Loggers.Play.fatal("Missing play-docviewer.jar in class path !");
				System.exit(1);
			}
			
			System.setProperty("application.path", MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath());
			System.setProperty("play.id", "");
			
			if (Configuration.global.getValueBoolean("play", "debug")) {
				System.setProperty("play.debug", "yes");
			}
			
			Server.main(new String[] {});
			
			if (Play.mode == Mode.DEV) {
				/**
				 * NO LAZY LOAD
				 */
				StartPlayNoLazyLoad t_load_on_boot = new StartPlayNoLazyLoad();
				t_load_on_boot.start();
			}
		}
		
		if (enable_background_services) {
			new PathScan().register(manager);
			new WorkerIndexer(manager);
			wf_trancoder = new WatchFolderTranscoder(manager);
			TranscoderWorker.declareTranscoders(manager);
			bca_watcher = new BCAWatcher(manager);
		}
	}
	
	private void stopInternalServices() throws Exception {
		if (enable_ftpserver) {
			for (int pos = 0; pos < ftp_servers.size(); pos++) {
				ftp_servers.get(pos).stop();
			}
			
			if (ftp_server_operations != null) {
				ftp_server_operations.stop();
			}
			ftp_server_operations = null;
		}
		
		if (enable_play) {
			Play.stop();
		}
		
		if (enable_background_services) {
			wf_trancoder.stopAllWatchFolders();
			bca_watcher.stop();
		}
	}
	
	public void startAllServices() {
		Loggers.Manager.debug("Start all services...");
		if (servicethread == null) {
			servicethread = new ServiceThread();
			servicethread.start();
		} else {
			if (servicethread.isAlive() == false) {
				servicethread = new ServiceThread();
				servicethread.start();
			}
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
	
	private class ServiceThread extends Thread {
		
		public ServiceThread() {
			setName("Service");
		}
		
		public void run() {
			Loggers.Manager.debug("Start service thread");
			want_stop_service = false;
			try {
				startInternalServices();
				
				manager.startAll();
				
				Loggers.Manager.debug("Manager is loaded");
				while (want_stop_service == false) {
					sleep(10);
				}
				Loggers.Manager.info("Stop service required");
				
				stopInternalServices();
				
				manager.stopAll();
				
				manager.getInstanceStatus().removeCurrentInstanceFromDb();
				
				Loggers.Manager.info("Thanks for using MyDMAM");
			} catch (InterruptedException e) {
				Loggers.Manager.error("Violent stop service", e);
			} catch (Exception e) {
				Loggers.Manager.error("ServiceManager execution error...", e);
				AdminMailAlert.create("Runtime Error Service", true).setThrowable(e).setManager(manager).send();
			}
		}
	}
	
	private class WatchdogLogConf extends Thread {
		
		WatchdogLogConf() {
			setName("Watchdog change configuration loggers");
			setDaemon(true);
		}
		
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
	}
	
	private class ShutdownHook extends Thread {
		
		ShutdownHook() {
			setName("Shutdown Hook");
		}
		
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
	}
	
	/**
	 * Blocking !
	 */
	private void waitUserInputConsole() {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
		String line;
		
		Runnable help = () -> {
			System.out.println(" ============ Start interactive mode ============");
			System.out.println(" == Enter q for stop started services and quit ==");
			System.out.println(" == Enter r for restart activated services     ==");
			System.out.println(" == Enter x for stop directly this application ==");
			System.out.println(" == Enter t for display Threads dump stack     ==");
			System.out.println(" == Enter h for display this help              ==");
			System.out.println();
		};
		help.run();
		
		try {
			while (true) {
				line = bufferedReader.readLine().trim();
				if (line.equals("")) {
					continue;
				} else if (line.toLowerCase().equals("q")) {
					System.out.println("Exit...");
					System.exit(0);
				} else if (line.toLowerCase().equals("r")) {
					System.out.println("Stop all activated services...");
					stopAllServices();
					System.out.println("Start all activated services...");
					startAllServices();
				} else if (line.toLowerCase().equals("x") | line.toLowerCase().equals("k")) {
					System.out.println("KILL");
					Runtime.getRuntime().removeShutdownHook(shutdown_hook);
					System.exit(0);
				} else if (line.toLowerCase().equals("t")) {
					ThreadMXBean bean = ManagementFactory.getThreadMXBean();
					ThreadInfo[] infos = bean.dumpAllThreads(true, true);
					
					System.out.println("=========== START THREAD DUMP ===========");
					
					for (ThreadInfo info : infos) {
						String is_native = "";
						if (info.isInNative()) {
							is_native = " Native";
						}
						
						System.out.println("#" + info.getThreadId() + " " + info.getThreadState().name() + " " + info.getThreadName() + is_native);
						StackTraceElement[] elems = info.getStackTrace();
						for (int pos = 0; pos < elems.length; pos++) {
							System.out.println("  at  " + elems[pos].toString());
						}
						
						LockInfo lckinfo = info.getLockInfo();
						if (lckinfo != null) {
							System.out.println("Lock: " + lckinfo.getClassName() + " #" + lckinfo.getIdentityHashCode());
						}
						if (info.getLockOwnerId() > -1) {
							System.out.println("Owned by: " + info.getLockOwnerName() + " #" + info.getLockOwnerId());
						}
						
						if (info.getLockedMonitors().length > 0) {
							if (info.getLockedMonitors().length == 1) {
								MonitorInfo mi = info.getLockedMonitors()[0];
								System.out.println("LockedMonitor: " + mi.getClassName() + "  " + mi.getLockedStackFrame().toString() + " #" + mi.getIdentityHashCode() + ", StackDepth = "
										+ mi.getLockedStackDepth());
								
							} else {
								System.out.println("LockedMonitors:");
								for (MonitorInfo mi : info.getLockedMonitors()) {
									System.out.println(
											" - " + mi.getClassName() + "  " + mi.getLockedStackFrame().toString() + " #" + mi.getIdentityHashCode() + ", StackDepth = " + mi.getLockedStackDepth());
								}
							}
						}
						
						if (info.getLockedSynchronizers().length > 0) {
							if (info.getLockedMonitors().length == 1) {
								LockInfo li = info.getLockedSynchronizers()[0];
								System.out.println("LockedSynchronizer: " + li.getClassName() + " #" + li.getIdentityHashCode());
							} else {
								System.out.println("LockedSynchronizers:");
								for (LockInfo li : info.getLockedSynchronizers()) {
									System.out.println(" - " + li.getClassName() + " #" + li.getIdentityHashCode());
								}
							}
						}
						
						System.out.println();
					}
					
					System.out.println("=========== END THREAD DUMP ===========");
				} else if (line.toLowerCase().equals("h")) {
					help.run();
				}
			}
		} catch (IOException e) {
			Loggers.Manager.error("Exit Console mode", e);
		}
	}
	
	public static class PlayInCli implements CliModule {
		
		public void showFullCliModuleHelp() {
			System.out.println("Start front-end http Play! server for client access and admin");
		}
		
		public String getCliModuleShortDescr() {
			return "Start Play! server service";
		}
		
		public String getCliModuleName() {
			return "play";
		}
		
		public void execCliModule(ApplicationArgs args) throws Exception {
			ServiceNG s = new ServiceNG(true, false, false);
			s.startAllServices();
			s.waitUserInputConsole();
		}
	}
	
	public static class BackgroundServicesInCli implements CliModule {
		
		public void showFullCliModuleHelp() {
			System.out.println("Start all background services activated in configuration like transcoding, path-indexing, watchfolder, BCA...");
		}
		
		public String getCliModuleShortDescr() {
			return "Start background services";
		}
		
		public String getCliModuleName() {
			return "services";
		}
		
		public void execCliModule(ApplicationArgs args) throws Exception {
			ServiceNG s = new ServiceNG(false, false, true);
			s.startAllServices();
			s.waitUserInputConsole();
		}
	}
	
	public static class FTPServerInCli implements CliModule {
		
		public void showFullCliModuleHelp() {
			System.out.println("Start FTP Server. Before, you must set/check its configuration. Else it doesn't starts");
		}
		
		public String getCliModuleShortDescr() {
			return "Start FTP Server";
		}
		
		public String getCliModuleName() {
			return "ftpserver";
		}
		
		public void execCliModule(ApplicationArgs args) throws Exception {
			ServiceNG s = new ServiceNG(false, true, false);
			s.startAllServices();
			s.waitUserInputConsole();
		}
	}
	
}
