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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.pathindexing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.CyclicJobCreator;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.storage.Storage;

public class PathScan extends WorkerNG {
	
	private ImporterStorage importer;
	private long forced_ttl = 0;
	
	void refreshIndex(ElasticsearchBulkOperation bulk, String storage_index_label, String current_working_directory, boolean limit_to_current_directory) throws Exception {
		Storage storage = Storage.getByName(storage_index_label);
		
		importer = new ImporterStorage(storage);
		
		if (forced_ttl > 0) {
			importer.setTTL(forced_ttl);
		}
		
		importer.setCurrentworkingdir(current_working_directory);
		importer.setLimit_to_current_directory(limit_to_current_directory);
		
		String cwd = importer.getCurrentworkingdir();
		StringBuilder sb = new StringBuilder();
		sb.append("Indexing storage: ");
		sb.append(storage);
		if (cwd != null) {
			sb.append(", current working directory: ");
			sb.append(cwd);
		}
		if (limit_to_current_directory) {
			sb.append(", limit to current directory: ");
			sb.append(limit_to_current_directory);
		}
		Loggers.Pathindex.info(sb.toString());
		
		importer.index(bulk);
		
		importer = null;
	};
	
	public void forceStopProcess() throws Exception {
		if (importer != null) {
			importer.stopScan();
		}
	}
	
	/**
	 * Ignore configuration period and internal grace time.
	 */
	public void setForcedTTL(long forced_ttl) {
		this.forced_ttl = forced_ttl;
	}
	
	/**
	 * @return this
	 */
	public PathScan cyclicJobsRegister(AppManager manager) throws ConnectionException, ClassNotFoundException {
		if (Storage.hasRegularIndexing() == false) {
			return this;
		}
		Loggers.Pathindex.debug("Load Cyclics for regular path indexing");
		
		List<Storage> storages = Storage.getRegularIndexingStorages();
		CyclicJobCreator cyclicjobcreator;
		for (int pos = 0; pos < storages.size(); pos++) {
			cyclicjobcreator = new CyclicJobCreator(manager, storages.get(pos).getPeriod(), TimeUnit.SECONDS, false);
			
			JobContextPathScan context = new JobContextPathScan();
			context.neededstorages = Arrays.asList(storages.get(pos).getName());
			cyclicjobcreator.createThis("Index " + storages.get(pos).getName() + " storage", getClass(), "Regular storage indexing", getWorkerVendorName(), context);
			manager.register(cyclicjobcreator);
		}
		return this;
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INDEXING;
	}
	
	public String getWorkerLongName() {
		return "Storage indexing";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	private ArrayList<String> storages_avaliable;
	
	public List<WorkerCapablities> getWorkerCapablities() {
		if (storages_avaliable == null) {
			storages_avaliable = new ArrayList<String>();
			
			List<Storage> storages = Storage.getRegularIndexingStorages();
			for (int pos = 0; pos < storages.size(); pos++) {
				storages_avaliable.add(storages.get(pos).getName());
			}
		}
		return WorkerCapablities.createList(JobContextPathScan.class, storages_avaliable);
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		if (context.neededstorages == null) {
			throw new NullPointerException("\"neededstorages\" can't to be null");
		}
		if (context.neededstorages.size() == 0) {
			throw new IndexOutOfBoundsException("\"storages\" can't to be empty");
		}
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		for (int pos = 0; pos < context.neededstorages.size(); pos++) {
			progression.updateStep(pos + 1, context.neededstorages.size());
			refreshIndex(bulk, context.neededstorages.get(pos), null, false);
		}
		bulk.terminateBulk();
	}
	
	protected boolean isActivated() {
		return Storage.getRegularIndexingStorages().isEmpty() == false;
	}
	
}
