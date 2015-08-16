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

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.util.HashMap;
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
	}
	
	WatchFolderEntry(String name, HashMap<String, ConfigurationItem> configuration) {
		this.name = name;
		
		/*source_storage = (String) configuration.get("source_storage");
		time_to_wait_growing_file = (Long) configuration.get("time_to_wait_growing_file");
		time_to_sleep_between_scans = (Long) configuration.get("time_to_sleep_between_scans");
		min_file_size = (Long) configuration.get("min_file_size");
		temp_directory = (String) configuration.get("temp_directory");*/
		// List<Target> targets;
	}
	
	/*
	    : LocalhostDebug
	    targets:
	        -
	            storage: LocalhostDebugOut
	            profile: ffmpeg_lowres_lq
	    time_to_wait_growing_file: 1000
	    time_to_sleep_between_scans: 10000
	    min_file_size: 10000
	    temp_directory: /tmp

	* */
	
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
