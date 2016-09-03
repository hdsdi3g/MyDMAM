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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.StoppableThread;

public class BCAWatcher implements InstanceStatusItem {
	
	private BCAEngine engine;
	private long sleep_time = 1000;
	private Watch watch;
	private File directory_watch_asrun;
	private File directory_watch_playlist;
	private boolean delete_asrun_after_watch;
	private boolean delete_playlist_after_watch;
	private long max_retention_duration;
	
	private AutomationEventProcessor asrun_processor;
	private AutomationEventProcessor playlist_processor;
	
	public BCAWatcher(AppManager manager, Configuration configuration) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
		if (Configuration.global.isElementExists("broadcast_automation") == false) {
			return;
		}
		sleep_time = Configuration.global.getValue("broadcast_automation", "sleep_time", 1000);
		max_retention_duration = TimeUnit.HOURS.toMillis(Configuration.global.getValue("broadcast_automation", "max_retention_duration", 24));
		
		String engine_class_name = Configuration.global.getValue("broadcast_automation", "engine_class", null);
		
		Class<?> engine_class = Class.forName(engine_class_name);
		MyDMAM.checkIsAccessibleClass(engine_class, false);
		engine = (BCAEngine) engine_class.newInstance();
		
		directory_watch_asrun = new File(Configuration.global.getValue("broadcast_automation", "watch_asrun", "?"));
		directory_watch_asrun = directory_watch_asrun.getCanonicalFile();
		CopyMove.checkExistsCanRead(directory_watch_asrun);
		CopyMove.checkIsDirectory(directory_watch_asrun);
		
		directory_watch_playlist = new File(Configuration.global.getValue("broadcast_automation", "watch_playlist", "?"));
		directory_watch_playlist = directory_watch_playlist.getCanonicalFile();
		CopyMove.checkExistsCanRead(directory_watch_playlist);
		CopyMove.checkIsDirectory(directory_watch_playlist);
		
		delete_asrun_after_watch = Configuration.global.getValueBoolean("broadcast_automation", "delete_asrun_after_watch");
		if (delete_asrun_after_watch) {
			CopyMove.checkIsWritable(directory_watch_asrun);
		}
		
		delete_playlist_after_watch = Configuration.global.getValueBoolean("broadcast_automation", "delete_playlist_after_watch");
		if (delete_playlist_after_watch) {
			CopyMove.checkIsWritable(directory_watch_playlist);
		}
		
		Loggers.BroadcastAutomation.info("Init engine watcher: " + getInstanceStatusItem().toString());
		manager.getInstanceStatus().registerInstanceStatusItem(this);
		
		asrun_processor = new AutomationEventProcessor(true);
		playlist_processor = new AutomationEventProcessor(false);
		
		watch = new Watch();
		watch.start();
	}
	
	public String getReferenceKey() {
		return "default";
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return BCAWatcher.class;
	}
	
	public JsonElement getInstanceStatusItem() {
		JsonObject jo = new JsonObject();
		jo.addProperty("engine_class", engine.getClass().getName());
		jo.addProperty("engine_name", engine.getName());
		jo.addProperty("engine_vendor", engine.getVendorName());
		jo.addProperty("engine_version", engine.getVersion());
		
		jo.addProperty("directory_watch_asrun", directory_watch_asrun.getPath());
		jo.addProperty("directory_watch_playlist", directory_watch_playlist.getPath());
		jo.addProperty("delete_asrun_after_watch", delete_asrun_after_watch);
		jo.addProperty("delete_playlist_after_watch", delete_playlist_after_watch);
		return jo;
	}
	
	private class SearchFileFilter implements FilenameFilter {
		boolean functionnal = true;
		List<String> ext;
		
		public SearchFileFilter() {
			ext = engine.getValidFileExtension();
			if (ext == null) {
				functionnal = false;
			}
			if (ext.isEmpty()) {
				functionnal = false;
			}
		}
		
		public boolean accept(File dir, String name) {
			if (functionnal == false) {
				return true;
			}
			for (int pos = 0; pos < ext.size(); pos++) {
				if (name.toLowerCase().endsWith("." + ext.get(pos).toLowerCase())) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	private class SortFileByDate implements Comparator<File> {
		
		public int compare(File o1, File o2) {
			if (o1.lastModified() > o2.lastModified()) {
				return -1;
			}
			if (o1.lastModified() < o2.lastModified()) {
				return 1;
			}
			return 0;
		}
		
	}
	
	private class Watch extends StoppableThread {
		public Watch() {
			super("Watch all clusters status");
		}
		
		public void run() {
			SearchFileFilter sff = new SearchFileFilter();
			SortFileByDate sfbd = new SortFileByDate();
			
			HashMap<File, Long> files_dates = new HashMap<>();
			ArrayList<File> file_refs_to_remove = new ArrayList<>();
			
			AtomicInteger count = new AtomicInteger(0);
			
			while (isWantToRun()) {
				try {
					/**
					 * Search and import asruns
					 */
					List<File> as_runs_files = Arrays.asList(directory_watch_asrun.listFiles(sff));
					as_runs_files.sort(sfbd);
					
					count.set(0);
					
					as_runs_files.forEach(file -> {
						if (files_dates.containsKey(file)) {
							if (files_dates.get(file) == file.lastModified()) {
								return;
							}
						} else {
							files_dates.put(file, file.lastModified());
						}
						
						try {
							count.set(engine.processAsRunFile(file, asrun_processor));
						} catch (Exception e) {
							Loggers.BroadcastAutomation.warn("Can't open file", e);
						}
					});
					
					if (count.get() > 0) {
						asrun_processor.close();
						
						if (delete_asrun_after_watch) {
							as_runs_files.forEach(file -> {
								file.delete();
								files_dates.remove(file);
							});
						}
					}
					
					/**
					 * Search and import playlists
					 */
					List<File> playlist_files = Arrays.asList(directory_watch_playlist.listFiles(sff));
					playlist_files.sort(sfbd);
					
					count.set(0);
					
					playlist_files.forEach(file -> {
						if (files_dates.containsKey(file)) {
							if (files_dates.get(file) == file.lastModified()) {
								return;
							}
						} else {
							files_dates.put(file, file.lastModified());
						}
						
						try {
							count.set(engine.processPlaylistFile(file, playlist_processor));
						} catch (Exception e) {
							Loggers.BroadcastAutomation.warn("Can't open file", e);
						}
					});
					
					if (count.get() > 0) {
						playlist_processor.close();
						
						if (delete_playlist_after_watch) {
							playlist_files.forEach(file -> {
								file.delete();
								files_dates.remove(file);
							});
						}
					}
					
					/**
					 * Clean internal file db
					 */
					file_refs_to_remove.clear();
					files_dates.keySet().forEach(file -> {
						if (file.exists() == false) {
							file_refs_to_remove.add(file);
						}
					});
					file_refs_to_remove.forEach(file -> {
						files_dates.remove(file);
					});
					
				} catch (Exception e) {
					Loggers.BroadcastAutomation.error("Error during watching", e);
				}
				stoppableSleep(sleep_time);
			}
		}
	}
	
	public synchronized void stop() {
		if (watch == null) {
			return;
		}
		watch.wantToStop();
	}
	
	public class AutomationEventProcessor {
		private boolean is_asrun;
		
		private MessageDigest md;
		
		private AutomationEventProcessor(boolean is_asrun) {
			this.is_asrun = is_asrun;
			
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
			}
		}
		
		public void onAutomationEvent(BCAAutomationEvent event) {
			md.reset();
			if (event.isRecording()) {
				md.update(Longs.toByteArray(1l));
			} else {
				md.update(Longs.toByteArray(0l));
			}
			if (event.isAutomationPaused()) {
				md.update(Longs.toByteArray(1l));
			} else {
				md.update(Longs.toByteArray(0l));
			}
			md.update(event.getAutomationId().getBytes());
			md.update(event.getChannel().getBytes());
			md.update(event.getSOM().toString().getBytes());
			md.update(event.getDuration().toString().getBytes());
			md.update(Longs.toByteArray(event.getStartDate()));
			md.update(event.getVideoSource().getBytes());
			md.update(event.getMaterialType().getBytes());
			md.update(event.getFileId().getBytes());
			md.update(event.getName().getBytes());
			md.update(event.getComment().getBytes());
			md.update(event.getOtherProperties().toString().getBytes());
			MyDMAM.byteToString(md.digest());
			
			// max_retention_duration
			
			System.out.print(new Date(event.getStartDate()));
			System.out.print(" ");
			System.out.print(event.isAutomationPaused());
			System.out.print(" ");
			System.out.print(event.getFileId());
			System.out.print(" ");
			// System.out.print(event.getOtherProperties());
			System.out.print("\t\t");
			System.out.print(event.getName());
			System.out.println();
			
			// TODO push to database
			// TODO if not is_asrun, delete future list during import the new list, and ignore same (== key) events
		}
		
		private void close() {
			// TODO
			System.out.println();
		}
	}
	
}
