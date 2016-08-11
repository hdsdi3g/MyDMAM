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
package hd3gtv.mydmam.transcode.watchfolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.transcode.ProcessingKitEngine;
import hd3gtv.mydmam.transcode.TranscodeProfile;

public class WatchFolderTranscoder {
	
	static final int TTL_CASSANDRA = (int) TimeUnit.HOURS.toSeconds(24);
	static final long TTL_ES = TimeUnit.HOURS.toMillis(24);
	
	private transient ThreadGroup wf_group;
	private ArrayList<WatchFolderEntry> wf_entries;
	
	private ProcessingKitEngine process_kit_engine;
	
	public WatchFolderTranscoder(AppManager manager) {
		
		if (Configuration.global.isElementExists("watchfoldertranscoder") == false) {
			return;
		}
		
		if (TranscodeProfile.isConfigured() == false) {
			Loggers.Transcode_WatchFolder.error("No transcoding configuration definited, cancel WatchFolderTranscoder loading");
			return;
		}
		
		HashMap<String, ConfigurationItem> all_wf_confs = Configuration.global.getElement("watchfoldertranscoder");
		
		if (all_wf_confs.isEmpty()) {
			return;
		}
		
		wf_entries = new ArrayList<WatchFolderEntry>();
		wf_group = new ThreadGroup("Watch Folder Transcoders");
		wf_group.setDaemon(true);
		process_kit_engine = new ProcessingKitEngine(manager);
		
		for (Map.Entry<String, ConfigurationItem> entry : all_wf_confs.entrySet()) {
			try {
				Loggers.Transcode_WatchFolder.info("Start watchfolder " + entry.getKey());
				WatchFolderEntry wf_entry = new WatchFolderEntry(manager, wf_group, entry.getKey(), all_wf_confs, process_kit_engine);
				wf_entry.start();
				
				wf_entries.add(wf_entry);
			} catch (Exception e) {
				Loggers.Transcode_WatchFolder.error("Can't load watchfolder " + entry.getKey(), e);
			}
		}
		
		Loggers.Transcode_WatchFolder.debug("Declare DeleteSourceFileWorker to manager");
		manager.register(new DeleteSourceFileWorker());
	}
	
	public void stopAllWatchFolders() {
		if (wf_entries == null) {
			return;
		}
		
		Loggers.Transcode_WatchFolder.info("Stop all " + wf_entries.size() + " watchfolders");
		for (int pos = 0; pos < wf_entries.size(); pos++) {
			wf_entries.get(pos).stopWatchfolderScans();
		}
		
		Loggers.Transcode_WatchFolder.debug("Wait stop for all watchfolders");
		try {
			while (wf_group.activeCount() > 0) {
				/*if (Loggers.Transcode_WatchFolder.isTraceEnabled()) {
					Loggers.Transcode_WatchFolder.trace("Wait " + wf_group.activeCount() + " watchfolder to stop");
				}*/
				Thread.sleep(10);
			}
		} catch (InterruptedException e) {
			Loggers.Transcode_WatchFolder.error("Can't wait all stopping threads", e);
		}
	}
	
}
