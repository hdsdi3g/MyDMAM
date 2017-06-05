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
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.InstanceStatusItem;
import hd3gtv.tools.CopyMove;

public class BCAWatcher implements InstanceStatusItem {
	
	public static final String CF_NAME = "broadcastAutomationEvents";
	public static final String DB_COL_CONTENT_NAME = "content";
	
	private BCAEngine engine;
	private long sleep_time = 1000;
	private BCAWatcherThread watch;
	private File directory_watch_asrun;
	private File directory_watch_playlist;
	private boolean delete_asrun_after_watch;
	private boolean delete_playlist_after_watch;
	private long max_retention_duration;
	private HashMap<String, ConfigurationItem> import_other_properties_configuration = null;
	
	public BCAWatcher(AppManager manager) throws ReflectiveOperationException, IOException, ConnectionException {
		try {
			engine = getEngine();
		} catch (NullPointerException e) {
			Loggers.BroadcastAutomation.debug("BCA is disabled: " + e.getMessage());
			return;
		}
		
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
		manager.registerInstanceStatusAction(this);
		
		watch = new BCAWatcherThread(this);
		
		String catch_handler_class = Configuration.global.getValue("broadcast_automation", "catch_handler", null);
		if (catch_handler_class != null) {
			watch.getProcessor().addEventCatcher(MyDMAM.factory.create(catch_handler_class, BCAEventCatcherHandler.class));
		}
		
		watch.start();
	}
	
	public static BCAEngine getEngine() throws ReflectiveOperationException {
		String engine_class_name = Configuration.global.getValue("broadcast_automation", "engine_class", null);
		if (engine_class_name == null) {
			throw new NullPointerException("broadcast_automation engine_class is not definited");
		}
		
		return MyDMAM.factory.create(engine_class_name, BCAEngine.class);
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
	
	public synchronized void stop() {
		if (watch == null) {
			return;
		}
		Loggers.BroadcastAutomation.info("Stop watching...");
		watch.wantToStop();
	}
	
	File getDirectoryWatchAsrun() {
		return directory_watch_asrun;
	}
	
	File getDirectoryWatchPlaylist() {
		return directory_watch_playlist;
	}
	
	long getMaxRetentionDuration() {
		return max_retention_duration;
	}
	
	long getSleepTime() {
		return sleep_time;
	}
	
	BCAEngine getCurrentEngine() {
		return engine;
	}
	
	boolean isDeleteAsrunAfterWatch() {
		return delete_asrun_after_watch;
	}
	
	boolean isDeletePlaylistAfterWatch() {
		return delete_playlist_after_watch;
	}
	
	HashMap<String, ConfigurationItem> getImportOtherPropertiesConfiguration() {
		return import_other_properties_configuration;
	}
}
