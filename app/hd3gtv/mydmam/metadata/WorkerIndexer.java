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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.TriggerJobCreator;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.metadata.MetadataCenter.MetadataConfigurationItem;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.JobContextPathScan;
import hd3gtv.mydmam.storage.Storage;

/**
 * Used for regular analysts
 */
public class WorkerIndexer extends WorkerNG {
	
	private volatile List<MetadataIndexer> analysis_indexers;
	
	private HashMap<String, Long> lastindexeddatesforstoragenames;
	private Explorer explorer = new Explorer();
	
	public WorkerIndexer(AppManager manager) throws ClassNotFoundException {
		if (isActivated() == false) {
			return;
		}
		analysis_indexers = new ArrayList<MetadataIndexer>();
		lastindexeddatesforstoragenames = new HashMap<String, Long>();
		
		for (int pos = 0; pos < MetadataCenter.conf_items.size(); pos++) {
			MetadataConfigurationItem item = MetadataCenter.conf_items.get(pos);
			
			Loggers.Metadata.debug("Set metadata configuration item: " + item);
			
			JobContextMetadataAnalyst analyst = new JobContextMetadataAnalyst();
			analyst.neededstorages = Arrays.asList(item.storage_label_name);
			analyst.currentpath = item.currentpath;
			analyst.force_refresh = false;
			
			JobContextPathScan context_hook = new JobContextPathScan();
			context_hook.neededstorages = Arrays.asList(item.storage_label_name);
			TriggerJobCreator trigger_creator = new TriggerJobCreator(manager, context_hook);
			trigger_creator.createThis("Analyst " + item.storage_label_name + " storage", this.getClass(), "Pathindex metadata indexer", "MyDMAM Internal", analyst);
			manager.register(trigger_creator);
			
			lastindexeddatesforstoragenames.put(item.storage_label_name, 0l);
		}
	}
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		JobContextMetadataAnalyst analyst_context = (JobContextMetadataAnalyst) context;
		if (analyst_context.neededstorages == null) {
			throw new NullPointerException("\"neededstorages\" can't to be null");
		}
		if (analyst_context.neededstorages.isEmpty()) {
			throw new IndexOutOfBoundsException("\"neededstorages\" can't to be empty");
		}
		String storagename;
		MetadataIndexer metadataIndexer;
		
		for (int pos = 0; pos < analyst_context.neededstorages.size(); pos++) {
			progression.updateStep(pos + 1, analyst_context.neededstorages.size());
			
			metadataIndexer = new MetadataIndexer(analyst_context.force_refresh);
			analysis_indexers.add(metadataIndexer);
			storagename = analyst_context.neededstorages.get(pos);
			long min_index_date = 0;
			if (lastindexeddatesforstoragenames.containsKey(storagename)) {
				min_index_date = lastindexeddatesforstoragenames.get(storagename);
			} else {
				lastindexeddatesforstoragenames.put(storagename, 0l);
			}
			metadataIndexer.process(explorer.getelementByIdkey(Explorer.getElementKey(storagename, analyst_context.currentpath)), min_index_date, progression);
			analysis_indexers.remove(metadataIndexer);
		}
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
		return WorkerCapablities.createList(JobContextMetadataAnalyst.class, Storage.getLocalAccessStoragesName());
	}
	
	protected boolean isActivated() {
		return MetadataCenter.conf_items.isEmpty() == false;
	}
	
}
