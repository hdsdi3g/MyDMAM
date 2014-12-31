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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.metadata;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.TriggerJobCreator;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.JobContextPathScan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkerIndexer extends WorkerNG {
	
	private volatile List<MetadataIndexer> analysis_indexers;
	
	private HashMap<String, Long> lastindexeddatesforstoragenames;
	
	public WorkerIndexer(AppManager manager) throws ClassNotFoundException {
		if (isActivated() == false) {
			return;
		}
		analysis_indexers = new ArrayList<MetadataIndexer>();
		lastindexeddatesforstoragenames = new HashMap<String, Long>();
		
		LinkedHashMap<String, String> s_bridge = Configuration.global.getValues("analysing_storageindexes");
		for (Map.Entry<String, String> entry : s_bridge.entrySet()) {
			JobContextAnalyst analyst = new JobContextAnalyst();
			analyst.storagename = entry.getKey();
			analyst.currentpath = entry.getValue();
			analyst.force_refresh = false;
			
			JobContextPathScan context_hook = new JobContextPathScan(entry.getKey());
			TriggerJobCreator trigger_creator = new TriggerJobCreator(manager, context_hook);
			trigger_creator.setOptions(this.getClass(), "Pathindex metadata indexer", "MyDMAM Internal");
			trigger_creator.add("Analyst directory", analyst);
			manager.triggerJobsRegister(trigger_creator);
			
			lastindexeddatesforstoragenames.put(entry.getKey(), 0l);
		}
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		JobContextAnalyst analyst_context = (JobContextAnalyst) context;
		MetadataIndexer metadataIndexer = new MetadataIndexer(analyst_context.force_refresh);
		analysis_indexers.add(metadataIndexer);
		
		long min_index_date = 0;
		if (lastindexeddatesforstoragenames.containsKey(analyst_context.storagename)) {
			min_index_date = lastindexeddatesforstoragenames.get(analyst_context.storagename);
		} else {
			lastindexeddatesforstoragenames.put(analyst_context.storagename, 0l);
		}
		
		metadataIndexer.process(analyst_context.storagename, analyst_context.currentpath, min_index_date);
		analysis_indexers.remove(metadataIndexer);
	}
	
	public void forceStopProcess() throws Exception {
		for (int pos = 0; pos < analysis_indexers.size(); pos++) {
			analysis_indexers.get(pos).stop();
		}
		analysis_indexers.clear();
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.METADATA;
	}
	
	public String getWorkerLongName() {
		return "Metadata Indexer";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return WorkerCapablities.createList(JobContextAnalyst.class, Explorer.getBridgedStoragesName());
	}
	
	protected boolean isActivated() {
		return Configuration.global.isElementExists("storageindex_bridge") & Configuration.global.isElementExists("analysing_storageindexes");
	}
	
}
