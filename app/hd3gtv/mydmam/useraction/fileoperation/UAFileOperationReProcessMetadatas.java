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

import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.MetadataIndexer;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		UAFileOperationReProcessMetadatasConfigurator conf = user_configuration.getObject(UAFileOperationReProcessMetadatasConfigurator.class);
		
		// TODO JobContextMetadataAnalyst
		
		/*ArrayList<SourcePathIndexerElement> items = new ArrayList<SourcePathIndexerElement>(1);
		indexer = new MetadataIndexer(conf.force_refresh);
		
		long min_index_date = 0;
		if (conf.limit_to_recent != null) {
			min_index_date = conf.limit_to_recent.toDate();
		}
		
		SourcePathIndexerElement item;
		ElasticsearchBulkOperation bulk;*/
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			
			/*item = entry.getValue();
			items.add(item);
			
			if (conf.refresh_path_index) {
				bulk = Elasticsearch.prepareBulk();
				explorer.refreshStoragePath(bulk, items, false);
				bulk.terminateBulk();
			}
			
			File current_element = Explorer.getLocalBridgedElement(entry.getValue());
			CopyMove.checkExistsCanRead(current_element);
			
			// TODO
			indexer.process(item.storagename, item.currentpath, min_index_date);
			// Container result = new MetadataIndexingOperation(current_element).setReference(reference) .addLimit(MetadataIndexingLimit.ANALYST).doIndexing();
			
			switch (conf.limit) {
			case MIMETYPE:
				
				break;
			case ANALYST:
				
				break;
			case SIMPLERENDERS:
				
				break;
			case NOLIMITS:
				
				break;
			default:
				break;
			}*/
			
			/*
			if (conf.refresh_scope == RefreshScope.CURRENT) {
				explorer.refreshCurrentStoragePath(bulk, items, conf.force_refresh);
			} else if (conf.refresh_scope == RefreshScope.ALL_SUB_ELEMENTS) {
			}*/
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
