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
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.transcode.TranscodeProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

class WatchFolderEntry implements Runnable {
	
	private String name;
	private String source_storage;
	private long time_to_wait_growing_file;
	private long time_to_sleep_between_scans;
	private long min_file_size;
	private String temp_directory;
	private List<Target> targets;
	
	private transient boolean want_to_stop;
	
	class Target {
		String storage;
		String profile;
		
		Target init(LinkedHashMap<String, ?> conf) throws Exception {
			if (conf.containsKey(storage) == false) {
				throw new NullPointerException("\"storage\" can't to be null");
			}
			storage = (String) conf.get("storage");
			Storage.getByName(storage).testStorageConnection();
			
			if (conf.containsKey(profile) == false) {
				throw new NullPointerException("\"profile\" can't to be null");
			}
			profile = (String) conf.get("profile");
			
			if (TranscodeProfile.getTranscodeProfile(profile) == null) {
				throw new NullPointerException("Can't found transcode profile \"" + profile + "\" in \"" + name + "\" watch folder configuration");
			}
			return this;
		}
		
	}
	
	WatchFolderEntry(String name, HashMap<String, ConfigurationItem> all_wf_confs) throws Exception {
		this.name = name;
		
		source_storage = Configuration.getValue(all_wf_confs, name, "source_storage", "");
		Storage.getByName(source_storage).testStorageConnection();
		
		List<LinkedHashMap<String, ?>> c_targets = Configuration.getValuesList(all_wf_confs, name, "targets");
		if (c_targets == null) {
			throw new NullPointerException("\"targets\" can't to be null, in " + name + " watchfolder");
		}
		if (c_targets.isEmpty()) {
			throw new IndexOutOfBoundsException("\"targets\" can't to be empty, in " + name + " watchfolder");
		}
		List<Target> targets = new ArrayList<WatchFolderEntry.Target>(c_targets.size());
		
		for (int pos = 0; pos < c_targets.size(); pos++) {
			targets.add(new Target().init(c_targets.get(pos)));
		}
		
		time_to_wait_growing_file = Configuration.getValue(all_wf_confs, name, "time_to_wait_growing_file", 1000);
		time_to_sleep_between_scans = Configuration.getValue(all_wf_confs, name, "time_to_sleep_between_scans", 10000);
		min_file_size = Configuration.getValue(all_wf_confs, name, "min_file_size", 10000);
		temp_directory = Configuration.getValue(all_wf_confs, name, "temp_directory", System.getProperty("java.io.tmpdir"));
		
	}
	
	synchronized void stopWatchfolderScans() {
		want_to_stop = true;
	}
	
	@Override
	public void run() {
		want_to_stop = false;
		long sleep_time;
		
		try {
			while (want_to_stop == false) {
				// TODO
				
				/**
				 * Scan
				 */
				
				/**
				 * Check founded files
				 */
				
				/**
				 * Check active files
				 */
				
				/**
				 * Process active files: analyst
				 */
				
				/**
				 * Process active files: transcoding task
				 */
				
				/*
				If a file is founded, lock it in Cassandra, check presence status in Cassandra, and update it. Update ES pathindex entry.
				If a file is validated, create Job for manage it. Job neededstorages must be set & check.
				Job perform an analysis Metadata review, and create, if needed, some transcoding Jobs following associated params.*/
				
				sleep_time = time_to_sleep_between_scans;
				while (sleep_time > 0 & (want_to_stop == false)) {
					Thread.sleep(10);
					sleep_time -= 10;
				}
			}
		} catch (InterruptedException e) {
			Log2.log.error("Can't sleep", e, new Log2Dump("name", name));
		}
	}
	
}
