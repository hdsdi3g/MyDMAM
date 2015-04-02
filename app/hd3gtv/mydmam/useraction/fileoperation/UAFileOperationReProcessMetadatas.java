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
package hd3gtv.mydmam.useraction.fileoperation;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.MetadataIndexer;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.UserProfile;

public class UAFileOperationReProcessMetadatas extends BaseFileOperation {
	
	private Explorer explorer = new Explorer();
	
	protected String getSubLongName() {
		return "process metadata anaylist/rendering";
	}
	
	protected String getSubMessageBaseName() {
		return "refreshmetadatas";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationReProcessMetadatas();
	}
	
	public String getDescription() {
		return "Refresh manually metadata anaylist/rendering";
	}
	
	public boolean isPowerfulAndDangerous() {
		return false;
	}
	
	public Serializable prepareEmptyConfiguration() {
		return new UAFileOperationReProcessMetadatasConfigurator();
	}
	
	public class Capability extends UACapability {
		
		public boolean enableFileProcessing() {
			return true;
		}
		
		public boolean enableDirectoryProcessing() {
			return true;
		}
		
		public boolean enableRootStorageindexProcessing() {
			return true;
		}
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		return new Capability();
	}
	
	private MetadataIndexer indexer;
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		
		UAFileOperationReProcessMetadatasConfigurator conf = user_configuration.getObject(UAFileOperationReProcessMetadatasConfigurator.class);
		
		ArrayList<SourcePathIndexerElement> items = new ArrayList<SourcePathIndexerElement>(1);
		
		long min_index_date = 0;
		if (conf.limit_to_recent != null) {
			min_index_date = conf.limit_to_recent.toDate();
		}
		if (conf.limit_processing == null) {
			conf.limit_processing = MetadataIndexingLimit.NOLIMITS;
		}
		
		ArrayList<JobNG> new_created_jobs = new ArrayList<JobNG>();
		
		SourcePathIndexerElement item;
		ElasticsearchBulkOperation bulk;
		progression.updateStep(0, source_elements.size());
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			progression.incrStep();
			item = entry.getValue();
			items.add(item);
			
			if (stop) {
				return;
			}
			
			if (conf.refresh_path_index_before) {
				bulk = Elasticsearch.prepareBulk();
				explorer.refreshStoragePath(bulk, items, false);
				bulk.terminateBulk();
			}
			
			if (stop) {
				return;
			}
			
			File current_element = Storage.getLocalFile(entry.getValue());
			CopyMove.checkExistsCanRead(current_element);
			
			Log2Dump dump = new Log2Dump();
			dump.add("user", userprofile.key);
			dump.addDate("min_index_date", min_index_date);
			dump.add("limit_processing", conf.limit_processing);
			dump.add("item", item);
			dump.add("file", current_element);
			Log2.log.info("Metadata analyst/render", dump);
			
			indexer = new MetadataIndexer(true);
			indexer.setLimitProcessing(conf.limit_processing);
			new_created_jobs.addAll(indexer.process(item, min_index_date));
		}
		
		if (new_created_jobs.isEmpty() == false) {
			progression.incrStep();
			
			List<String> job_keys = new ArrayList<String>(new_created_jobs.size());
			for (int pos = 0; pos < new_created_jobs.size(); pos++) {
				job_keys.add(new_created_jobs.get(pos).getKey());
			}
			
			JobNG.Utility.waitAllJobsProcessing(job_keys, this);
		}
		
		indexer = null;
	}
	
	public synchronized void forceStopProcess() throws Exception {
		if (indexer != null) {
			indexer.stop();
		}
		super.forceStopProcess();
	}
}
