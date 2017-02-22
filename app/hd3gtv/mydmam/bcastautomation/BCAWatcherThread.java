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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.bcastautomation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.StoppableThread;

final class BCAWatcherThread extends StoppableThread {
	
	private BCAWatcher watcher;
	
	public BCAWatcherThread(BCAWatcher watcher) {
		super("Watch all BCA events");
		this.watcher = watcher;
		if (watcher == null) {
			throw new NullPointerException("\"watcher\" can't to be null");
		}
	}
	
	public void run() {
		SearchFileFilter sff = new SearchFileFilter(watcher.getCurrentEngine().getValidFileExtension());
		SortFileByDate sfbd = new SortFileByDate();
		
		HashMap<File, Long> files_dates = new HashMap<>();
		ArrayList<File> file_refs_to_remove = new ArrayList<>();
		ArrayList<ScheduleFileStatus> all_status = new ArrayList<>();
		
		final BCAAutomationEventProcessor processor;
		try {
			processor = new BCAAutomationEventProcessor(BCAWatcher.CF_NAME, watcher.getMaxRetentionDuration(), watcher.getImportOtherPropertiesConfiguration());
		} catch (ConnectionException e1) {
			Loggers.BroadcastAutomation.error("Can't access to Cassandra DB, cancel BCA watcher", e1);
			return;
		}
		
		while (isWantToRun()) {
			try {
				Loggers.BroadcastAutomation.trace("Loop the Search and import asruns");
				
				List<File> as_runs_files = Arrays.asList(watcher.getDirectoryWatchAsrun().listFiles(sff));
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
						processor.clearEventList();
						Loggers.BroadcastAutomation.info("Start process asrun file import from \"" + file.getPath() + "\"");
						all_status.add(watcher.getCurrentEngine().processScheduleFile(file, processor));
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
						processor.endsOperation();
					}
					
					if (watcher.isDeleteAsrunAfterWatch()) {
						all_status.stream().forEach(status -> {
							if (status.getLastEventStartDate() < (System.currentTimeMillis() - watcher.getMaxRetentionDuration())) {
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
				List<File> playlist_files = Arrays.asList(watcher.getDirectoryWatchPlaylist().listFiles(sff));
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
						processor.clearEventList();
						processor.preparePurgeEventList();
						Loggers.BroadcastAutomation.info("Start process playlist file import from \"" + file.getPath() + "\"");
						all_status.add(watcher.getCurrentEngine().processScheduleFile(file, processor));
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
						processor.endsOperation();
					}
					
					if (watcher.isDeletePlaylistAfterWatch()) {
						all_status.stream().forEach(status -> {
							if (status.getLastEventStartDate() < (System.currentTimeMillis() - watcher.getMaxRetentionDuration())) {
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
			
			stoppableSleep(watcher.getSleepTime());
		}
	}
}