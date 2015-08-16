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

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.transcode.TranscodeProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WatchFolderTranscoder {
	
	private AppManager manager;
	
	private ThreadGroup wf_group;
	
	private List<WatchFolderEntry> wf_entries;
	
	public WatchFolderTranscoder(AppManager manager) {
		this.manager = manager;
		
		if (Configuration.global.isElementExists("watchfoldertranscoder") == false) {
			return;
		}
		
		if (TranscodeProfile.isConfigured() == false) {
			Log2.log.error("No transcoding configuration definited, cancel WatchFolderTranscoder loading", null);
			return;
		}
		
		HashMap<String, ConfigurationItem> all_wf_confs = Configuration.global.getElement("watchfoldertranscoder");
		
		if (all_wf_confs.isEmpty()) {
			return;
		}
		
		wf_entries = new ArrayList<WatchFolderEntry>();
		wf_group = new ThreadGroup("Watch Folder Transcoders");
		wf_group.setDaemon(true);
		
		for (Map.Entry<String, ConfigurationItem> entry : all_wf_confs.entrySet()) {
			try {
				WatchFolderEntry wf_entry = new WatchFolderEntry(entry.getKey(), all_wf_confs);
				wf_entries.add(wf_entry);
				Thread t = new Thread(wf_group, wf_entry);
				t.setDaemon(true);
				t.setName("Watch Folder for " + entry.getKey());
				t.start();
			} catch (Exception e) {
				Log2.log.error("Can't load watchfolder", e, new Log2Dump("name", entry.getKey()));
			}
		}
	}
	
	public void stopAllWatchFolders() {
		if (wf_entries == null) {
			return;
		}
		
		for (int pos = 0; pos < wf_entries.size(); pos++) {
			wf_entries.get(pos).stopWatchfolderScans();
		}
		
		try {
			while (wf_group.activeCount() > 0) {
				Thread.sleep(100);
			}
		} catch (InterruptedException e) {
			Log2.log.error("Can't wait all stopping threads", e);
		}
	}
}
