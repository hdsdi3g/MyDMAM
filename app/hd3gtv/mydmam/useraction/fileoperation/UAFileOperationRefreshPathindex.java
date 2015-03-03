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
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;
import hd3gtv.mydmam.useraction.fileoperation.UAFileOperationRefreshPathindexConfigurator.RefreshScope;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import models.UserProfile;

public class UAFileOperationRefreshPathindex extends BaseFileOperation {
	
	private Explorer explorer = new Explorer();
	
	protected String getSubLongName() {
		return "refresh manually pathindex";
	}
	
	protected String getSubMessageBaseName() {
		return "refreshpathindex";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationRefreshPathindex();
	}
	
	public String getDescription() {
		return "Refresh manually pathindex";
	}
	
	public boolean isPowerfulAndDangerous() {
		return false;
	}
	
	public Serializable prepareEmptyConfiguration() {
		return new UAFileOperationRefreshPathindexConfigurator();
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
		
		public boolean mustHaveLocalStorageindexBridge() {
			return true;
		}
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		return new Capability();
	}
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		UAFileOperationRefreshPathindexConfigurator conf = user_configuration.getObject(UAFileOperationRefreshPathindexConfigurator.class);
		
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		ArrayList<SourcePathIndexerElement> items = new ArrayList<SourcePathIndexerElement>(source_elements.size());
		items.addAll(source_elements.values());
		
		if (conf.refresh_scope == RefreshScope.CURRENT) {
			explorer.refreshCurrentStoragePath(bulk, items, conf.force_refresh);
		} else if (conf.refresh_scope == RefreshScope.ALL_SUB_ELEMENTS) {
			explorer.refreshStoragePath(bulk, items, conf.force_refresh);
		}
		
		bulk.terminateBulk();
	}
}
