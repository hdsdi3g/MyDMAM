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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;
import hd3gtv.mydmam.storage.DistantFileRecovery;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.transcode.watchfolder.AbstractFoundedFile.Status;

class DeleteSourceFileWorker extends WorkerNG {
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Delete watched folders source files after processing";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return WorkerCapablities.createList(JobContextWFDeleteSourceFile.class, Storage.getAllStoragesNames());
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		is_stop = false;
		
		JobContextWFDeleteSourceFile order = (JobContextWFDeleteSourceFile) context;
		
		SourcePathIndexerElement spie = new SourcePathIndexerElement();
		spie.storagename = order.storage;
		spie.currentpath = order.path;
		spie.directory = false;
		
		/**
		 * Switch "Processed" to file.
		 */
		WatchFolderDB.switchStatus(spie.prepare_key(), Status.PROCESSED);
		
		Explorer explorer = new Explorer();
		
		if (order.send_to != null) {
			if (order.send_to.isEmpty() == false) {
				LinkedHashMap<String, SourcePathIndexerElement> send_to_map = explorer.getelementByIdkeys(order.send_to);
				if (send_to_map.isEmpty() == false) {
					ArrayList<SourcePathIndexerElement> send_to = new ArrayList<SourcePathIndexerElement>(send_to_map.values());
					
					for (int pos = 0; pos < send_to.size(); pos++) {
						progression.updateStep(pos, send_to.size());
						progression.update("Copy source file to " + send_to.get(pos));
						sendSourceTo(spie, send_to.get(pos));
					}
					if (is_stop) {
						return;
					}
					progression.updateStep(send_to.size(), send_to.size());
					progression.update("Copy source file operation is ended");
				}
			}
		}
		
		/**
		 * Delete physically the file.
		 */
		AbstractFile root = Storage.getByName(order.storage).getRootPath();
		AbstractFile source_file = root.getAbstractFile(order.path);
		spie.size = source_file.length();
		spie.date = source_file.lastModified();
		boolean delete_ok = source_file.delete();
		
		Loggers.Transcode_WatchFolder.info("Delete source file: " + order.storage + ":" + order.path);
		
		root.close();
		if (delete_ok == false) {
			throw new IOException("Can't delete file \"" + order.storage + ":" + order.path + "\"");
		}
		
		/**
		 * Delete the pathindex entry for this file.
		 * (don't delete mtd, the robot will doing this regularly, via @see ContainerOperations.purge_orphan_metadatas();
		 */
		Loggers.Transcode_WatchFolder.debug("Delete pathindex entry for file: " + spie.storagename + ":" + spie.currentpath);
		
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		explorer.deleteStoragePath(bulk, Arrays.asList(spie));
		bulk.terminateBulk();
		
		DistantFileRecovery.manuallyReleaseFile(spie);
		
		if (order.clean_after_done) {
			String wf_item_key = spie.prepare_key();
			List<String> jk = order.getReferer().getRequiredJobKeys();
			
			Loggers.Transcode_WatchFolder.info("Clean DB entries after processing: WF Key [" + wf_item_key + "] and relative jobkeys: " + jk);// TODO in debug
			
			/**
			 * Remove WF entry in db just after remove source file (in case of source file is removed)
			 */
			WatchFolderDB.remove(wf_item_key);
			
			/**
			 * Remove all relative done jobs
			 */
			JobNG.Utility.removeJobsByKeys(jk);
		}
	}
	
	private void sendSourceTo(SourcePathIndexerElement source, SourcePathIndexerElement dest) throws Exception {
		if (is_stop) {
			return;
		}
		Loggers.Transcode_WatchFolder.debug("Copy source file: " + source.storagename + ":" + source.currentpath + " to: " + dest);
		
		File physical_source = Storage.getLocalFile(source);
		if (physical_source == null) {
			try {
				physical_source = DistantFileRecovery.getFile(source, true);
			} catch (Exception e) {
				throw new Exception("Can't download source file to temp directory " + source.toString(), e);
			}
		}
		
		if (is_stop) {
			return;
		}
		
		try {
			File dest_dir = Storage.getLocalFile(dest);
			if (dest_dir == null) {
				AbstractFile af = Storage.getByName(dest.storagename).getRootPath().getAbstractFile(dest.currentpath + "/" + physical_source.getName());
				OutputStream of_os = af.getOutputStream(65536);
				FileUtils.copyFile(physical_source, of_os);
				IOUtils.closeQuietly(of_os);
				IOUtils.closeQuietly(af);
			} else {
				FileUtils.copyFile(physical_source, new File(dest_dir.getAbsolutePath() + File.separator + physical_source.getName()));
			}
		} catch (Exception e) {
			throw new Exception("Can't copy source file to dest " + physical_source.getPath() + " -> " + dest.toString(), e);
		}
		
	}
	
	private boolean is_stop;
	
	protected void forceStopProcess() throws Exception {
		is_stop = true;
	}
	
	protected boolean isActivated() {
		return true;
	}
	
}
