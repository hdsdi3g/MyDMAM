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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.bcastautomation.TimedEventStore.TimedEvent;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.StoppableThread;

public class BCAWatcher implements InstanceStatusItem {
	
	public static final String CF_NAME = "broadcastAutomationEvents";
	public static final String DB_COL_CONTENT_NAME = "content";
	
	private BCAEngine engine;
	private long sleep_time = 1000;
	private Watch watch;
	private File directory_watch_asrun;
	private File directory_watch_playlist;
	private boolean delete_asrun_after_watch;
	private boolean delete_playlist_after_watch;
	private long max_retention_duration;
	private HashMap<String, ConfigurationItem> import_other_properties_configuration;
	
	private AutomationEventProcessor processor;
	
	public BCAWatcher(AppManager manager) throws ReflectiveOperationException, IOException, ConnectionException {
		if (Configuration.global.isElementExists("broadcast_automation") == false) {
			return;
		}
		engine = getEngine();
		
		sleep_time = Configuration.global.getValue("broadcast_automation", "sleep_time", 1000);
		max_retention_duration = TimeUnit.HOURS.toMillis(Configuration.global.getValue("broadcast_automation", "max_retention_duration", 24));
		
		if (Configuration.global.isElementKeyExists("broadcast_automation", "import_other_properties")) {
			import_other_properties_configuration = Configuration.getElement(Configuration.global.getElement("broadcast_automation"), "import_other_properties");
		}
		
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
		
		processor = new AutomationEventProcessor();
		
		watch = new Watch();
		watch.start();
	}
	
	public static BCAEngine getEngine() throws ReflectiveOperationException {
		String engine_class_name = Configuration.global.getValue("broadcast_automation", "engine_class", null);
		if (engine_class_name == null) {
			throw new NullPointerException("broadcast_automation engine_class is not definited");
		}
		
		Class<?> engine_class = Class.forName(engine_class_name);
		MyDMAM.checkIsAccessibleClass(engine_class, false);
		return (BCAEngine) engine_class.newInstance();
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
			super("Watch all BCA events");
		}
		
		public void run() {
			SearchFileFilter sff = new SearchFileFilter();
			SortFileByDate sfbd = new SortFileByDate();
			
			HashMap<File, Long> files_dates = new HashMap<>();
			ArrayList<File> file_refs_to_remove = new ArrayList<>();
			ArrayList<ScheduleFileStatus> all_status = new ArrayList<>();
			
			while (isWantToRun()) {
				try {
					Loggers.BroadcastAutomation.trace("Loop the Search and import asruns");
					
					List<File> as_runs_files = Arrays.asList(directory_watch_asrun.listFiles(sff));
					as_runs_files.sort(sfbd);
					
					all_status.clear();
					
					as_runs_files.forEach(file -> {
						if (files_dates.containsKey(file)) {
							if (files_dates.get(file) == file.lastModified()) {
								return;
							}
						}
						files_dates.put(file, file.lastModified());
						
						try {
							Loggers.BroadcastAutomation.info("Start process asrun file import from \"" + file.getPath() + "\"");
							all_status.add(engine.processScheduleFile(file, processor));
						} catch (Exception e) {
							Loggers.BroadcastAutomation.warn("Can't open file", e);
						}
					});
					
					if (all_status.isEmpty() == false) {
						int count = all_status.stream().mapToInt(status -> {
							return status.getEventCount();
						}).sum();
						
						if (count > 0) {
							Loggers.BroadcastAutomation.debug("Asrun file import is done, found " + count + " events");
							processor.close();
						}
						
						if (delete_asrun_after_watch) {
							all_status.stream().forEach(status -> {
								if (status.getLastEventStartDate() < (System.currentTimeMillis() - max_retention_duration)) {
									File sch = status.getScheduleFile();
									Loggers.BroadcastAutomation.info("Delete asrun file \"" + sch.getPath() + "\"");
									sch.delete();
									files_dates.remove(sch);
								}
							});
						}
					}
					
					/**
					 * Search and import playlists
					 */
					List<File> playlist_files = Arrays.asList(directory_watch_playlist.listFiles(sff));
					playlist_files.sort(sfbd);
					
					all_status.clear();
					
					playlist_files.forEach(file -> {
						if (files_dates.containsKey(file)) {
							if (files_dates.get(file) == file.lastModified()) {
								return;
							}
						}
						files_dates.put(file, file.lastModified());
						
						try {
							processor.prepareActualFutureEventList();
							Loggers.BroadcastAutomation.info("Start process playlist file import from \"" + file.getPath() + "\"");
							all_status.add(engine.processScheduleFile(file, processor));
						} catch (Exception e) {
							Loggers.BroadcastAutomation.warn("Can't open file", e);
						}
					});
					
					if (all_status.isEmpty() == false) {
						int count = all_status.stream().mapToInt(status -> {
							return status.getEventCount();
						}).sum();
						
						if (count > 0) {
							Loggers.BroadcastAutomation.debug("Playlist file import is done, found " + count + " events");
							processor.close();
						}
						
						if (delete_playlist_after_watch) {
							all_status.stream().forEach(status -> {
								if (status.getLastEventStartDate() < (System.currentTimeMillis() - max_retention_duration)) {
									File sch = status.getScheduleFile();
									Loggers.BroadcastAutomation.info("Delete playlist file \"" + sch.getPath() + "\"");
									sch.delete();
									files_dates.remove(sch);
								}
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
		Loggers.BroadcastAutomation.info("Stop watching...");
		watch.wantToStop();
	}
	
	public class AutomationEventProcessor implements BCAAutomationEventHandler {
		private MessageDigest md;
		private TimedEvent t_event;
		private HashSet<String> actual_event_list;
		private TimedEventStore database;
		
		private AutomationEventProcessor() throws ConnectionException {
			database = new TimedEventStore(CassandraDb.getkeyspace(), CF_NAME, max_retention_duration);
			actual_event_list = new HashSet<>();
			
			try {
				md = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
			}
		}
		
		private void prepareActualFutureEventList() throws Exception {
			actual_event_list.clear();
			database.getAllKeys(future -> {
				actual_event_list.add(future);
			}, aired_key -> {
			}, nonaired_key -> {
				actual_event_list.add(nonaired_key);
			});
			Loggers.BroadcastAutomation.debug("Event list as currently " + actual_event_list.size() + " future and non-aired events");
		}
		
		public void onAutomationEvent(BCAAutomationEvent event) {
			if (event.getStartDate() < (System.currentTimeMillis() - max_retention_duration)) {
				if (Loggers.BroadcastAutomation.isTraceEnabled()) {
					Loggers.BroadcastAutomation.trace("Process event: event \"" + event.getName() + "\" at the " + new Date(event.getStartDate()) + " is too old. It will not be added to database. "
							+ event.serialize(import_other_properties_configuration).toString());
				}
				return;
			}
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
			if (import_other_properties_configuration != null) {
				md.update(event.getOtherProperties(import_other_properties_configuration).toString().getBytes());
			}
			String event_key = MyDMAM.byteToString(md.digest());
			
			if (event.isPlaylist() | event.isOnair()) {
				if (actual_event_list.contains(event_key)) {
					if (Loggers.BroadcastAutomation.isTraceEnabled()) {
						Loggers.BroadcastAutomation.trace("Process event: event [" + event_key + "] \"" + event.getName() + "\" at the " + new Date(event.getStartDate())
								+ " is already added in database. " + event.serialize(import_other_properties_configuration).toString());
					}
					actual_event_list.remove(event_key);
					return;
				}
			}
			
			if (t_event == null) {
				try {
					t_event = database.createEvent(event_key, event.getStartDate(), event.getLongDuration());
				} catch (ConnectionException e) {
					Loggers.BroadcastAutomation.warn("Can't push to database", e);
					return;
				}
			} else {
				t_event = t_event.createAnother(event_key, event.getStartDate(), event.getLongDuration());
			}
			t_event.getMutator().putColumn(DB_COL_CONTENT_NAME, event.serialize(import_other_properties_configuration).toString());
			
			if (Loggers.BroadcastAutomation.isTraceEnabled()) {
				Loggers.BroadcastAutomation.trace("Process event: event [" + event_key + "] \"" + event.getName() + "\" at the " + new Date(event.getStartDate()) + " will be added in database. "
						+ event.serialize(import_other_properties_configuration).toString());
			}
		}
		
		private void close() throws ConnectionException {
			if (t_event != null) {
				t_event.close();
			}
			
			if (actual_event_list.isEmpty() == false) {
				Loggers.BroadcastAutomation.debug("Start purge obsolete " + actual_event_list.size() + " event(s)");
				
				MutationBatch mutator = CassandraDb.prepareMutationBatch(CassandraDb.getDefaultKeyspacename());
				ColumnFamily<String, String> cf = new ColumnFamily<String, String>(CF_NAME, StringSerializer.get(), StringSerializer.get());
				
				actual_event_list.forEach((event_key) -> {
					Loggers.BroadcastAutomation.trace("Process event: clean obsolete event [" + event_key + "] from database");
					mutator.withRow(cf, event_key).delete();
				});
				mutator.execute();
			}
		}
	}
	
}
