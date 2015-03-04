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
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

import org.apache.commons.io.FileUtils;

public class UAFileOperationTrash extends BaseFileOperation {
	
	private Explorer explorer = new Explorer();
	private String trash_directory_name;
	
	public UAFileOperationTrash() {
		trash_directory_name = "Trash";
	}
	
	protected String getSubLongName() {
		return "move to trash file/directory";
	}
	
	protected String getSubMessageBaseName() {
		return "trash";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationTrash();
	}
	
	public String getDescription() {
		return "Delete files (move to trash)";
	}
	
	public boolean isPowerfulAndDangerous() {
		return false;
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
		
		public boolean mustHaveLocalStorageindexBridge() {
			return true;
		}
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		if (internal_configuration.containsKey("trash_directory")) {
			trash_directory_name = (String) internal_configuration.get("trash_directory");
		}
		return new Capability();
	}
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		dump.add("trash_directory_name", trash_directory_name);
		
		progression.update("Prepare trashs directories");
		
		HashMap<String, File> trashs_dirs = new HashMap<String, File>();
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			String storagename = entry.getValue().storagename;
			if (trashs_dirs.containsKey(storagename)) {
				continue;
			}
			File storage_dir = Explorer.getLocalBridgedElement(entry.getValue().prepareStorageElement(storagename));
			File trash_dir = new File(storage_dir.getPath() + File.separator + trash_directory_name);
			
			if (trash_dir.exists() == false) {
				FileUtils.forceMkdir(trash_dir);
			} else {
				CopyMove.checkExistsCanRead(trash_dir);
				CopyMove.checkIsWritable(trash_dir);
				CopyMove.checkIsDirectory(trash_dir);
			}
			trashs_dirs.put(storagename, trash_dir);
			
			if (stop) {
				return;
			}
		}
		
		progression.update("Move item(s) to trash(s) directorie(s)");
		progression.updateStep(1, source_elements.size());
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			progression.incrStep();
			File current_element = Explorer.getLocalBridgedElement(entry.getValue());
			CopyMove.checkExistsCanRead(current_element);
			CopyMove.checkIsWritable(current_element.getParentFile());
			
			File trash_dir = trashs_dirs.get(entry.getValue().storagename);
			trash_dir = new File(trash_dir.getPath() + File.separator + userprofile.getBaseFileName_BasedOnEMail());
			// TODO ...
			/*File new_file = new File(current_element.getParentFile() + File.separator + conf.newname);
			
			if (new_file.exists()) {
				continue;
			}
			
			if (current_element.renameTo(new_file) == false) {
				dump.add("new_file", new_file);
				Log2.log.debug("Can't rename correctly file", dump);
				throw new IOException("Can't rename correctly file: " + current_element.getPath() + " to \"" + conf.newname + "\"");
			}
			
			SourcePathIndexerElement dest = entry.getValue().clone();
			dest.currentpath = dest.currentpath.substring(0, dest.currentpath.lastIndexOf("/")) + "/" + conf.newname;
			if (dest.currentpath.startsWith("//")) {
				dest.currentpath = dest.currentpath.substring(1);
			}
			ContainerOperations.copyMoveMetadatas(entry.getValue(), dest, false, this);
			
			if (stop) {
				return;
			}
			
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			explorer.refreshStoragePath(bulk, Arrays.asList(dest), false);
			bulk.terminateBulk();*/
			
			if (stop) {
				return;
			}
		}
	}
}
