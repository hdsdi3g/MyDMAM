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
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

import org.apache.commons.io.FileUtils;

public class UAFileOperationDelete extends BaseFileOperation {
	
	Explorer explorer = new Explorer();
	
	protected String getSubLongName() {
		return "delete file/directory";
	}
	
	protected String getSubMessageBaseName() {
		return "delete";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationDelete();
	}
	
	public String getDescription() {
		return "Delete files and directories content";
	}
	
	public boolean isPowerfulAndDangerous() {
		return true;
	}
	
	public Serializable prepareEmptyConfiguration() {
		return new UAFileOperationEmptyConfigurator();
	}
	
	public class Capability extends UACapability {
		
		public boolean enableFileProcessing() {
			return true;
		}
		
		public boolean enableDirectoryProcessing() {
			return true;
		}
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		return new Capability();
	}
	
	@Override
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		progression.updateStep(1, source_elements.size());
		
		ArrayList<File> items_to_delete = new ArrayList<File>();
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			progression.incrStep();
			File current_element = Explorer.getLocalBridgedElement(entry.getValue());
			CopyMove.checkExistsCanRead(current_element);
			CopyMove.checkIsWritable(current_element.getParentFile());
			
			if (current_element.isFile() | FileUtils.isSymlink(current_element)) {
				if (current_element.delete() == false) {
					Log2.log.debug("Can't delete correctly file", dump);
					throw new IOException("Can't delete correctly file: " + current_element.getPath());
				}
				if (stop) {
					return;
				}
			} else {
				items_to_delete.clear();
				items_to_delete.add(current_element);
				boolean can_delete_all = true;
				recursivePath(current_element, items_to_delete);
				
				progression.updateProgress(1, items_to_delete.size());
				for (int pos_idel = items_to_delete.size() - 1; pos_idel > -1; pos_idel--) {
					progression.updateProgress(items_to_delete.size() - pos_idel, items_to_delete.size());
					if (items_to_delete.get(pos_idel).delete() == false) {
						dump.add("item", items_to_delete.get(pos_idel));
						can_delete_all = false;
					}
					if (stop) {
						return;
					}
				}
				
				if (can_delete_all == false) {
					Log2.log.debug("Can't delete correctly multiple files", dump);
					throw new IOException("Can't delete multiple files from: " + current_element.getPath());
				}
			}
			
			if (stop) {
				return;
			}
			
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			
			/**
			 * Delete mtds
			 */
			Container container = ContainerOperations.getByPathIndexId(entry.getKey());
			if (container != null) {
				ContainerOperations.requestDelete(container, bulk);
				RenderedFile.purge(entry.getKey());
			}
			
			explorer.deleteStoragePath(bulk, Arrays.asList(entry.getValue()));
			
			bulk.terminateBulk();
			
			if (stop) {
				return;
			}
		}
	}
	
	private void recursivePath(File dir, ArrayList<File> items_to_delete) throws IOException {
		File[] sub = dir.listFiles();
		File item;
		for (int pos = 0; pos < sub.length; pos++) {
			item = sub[pos];
			items_to_delete.add(item);
			if ((item.isFile() == false) & (FileUtils.isSymlink(item) == false)) {
				recursivePath(item, items_to_delete);
			}
			if (stop) {
				return;
			}
		}
	}
}
