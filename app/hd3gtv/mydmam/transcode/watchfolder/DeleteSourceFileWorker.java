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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.AbstractFile;
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
		JobContextWFDeleteSourceFile order = (JobContextWFDeleteSourceFile) context;
		
		SourcePathIndexerElement spie = new SourcePathIndexerElement();
		spie.storagename = order.storage;
		spie.currentpath = order.path;
		spie.directory = false;
		
		/**
		 * Switch "Processed" to file.
		 */
		WatchFolderDB.switchStatus(spie.prepare_key(), Status.PROCESSED);
		
		/**
		 * Delete physically the file.
		 */
		AbstractFile root = Storage.getByName(order.storage).getRootPath();
		boolean delete_ok = root.getAbstractFile(order.path).delete();
		
		Loggers.WatchFolder.info("Delete source file: " + order.storage + ":" + order.path);
		
		root.close();
		if (delete_ok == false) {
			throw new IOException("Can't delete file \"" + order.storage + ":" + order.path + "\"");
		}
		
		/**
		 * Delete the pathindex entry for this file.
		 * (don't delete mtd, the robot will doing this regularly, via @see ContainerOperations.purge_orphan_metadatas();
		 */
		Loggers.WatchFolder.debug("Delete pathindex entry for file: " + spie.storagename + ":" + spie.currentpath);
		
		Explorer explorer = new Explorer();
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		explorer.deleteStoragePath(bulk, Arrays.asList(spie));
		bulk.terminateBulk();
		
	}
	
	protected void forceStopProcess() throws Exception {
	}
	
	protected boolean isActivated() {
		return true;
	}
	
}
