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
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

import org.apache.commons.io.FileUtils;

public class UAFileOperationTrash extends BaseFileOperation {
	
	private Explorer explorer = new Explorer();
	private String trash_directory_name;
	private SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
	
	public UAFileOperationTrash(String trash_directory_name) {
		this.trash_directory_name = trash_directory_name;
	}
	
	public UAFileOperationTrash() {
	}
	
	protected String getSubLongName() {
		return "move to trash file/directory";
	}
	
	protected String getSubMessageBaseName() {
		return "trash";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationTrash(trash_directory_name);
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
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		if (internal_configuration.containsKey("trash_directory")) {
			trash_directory_name = (String) internal_configuration.get("trash_directory");
		}
		return new Capability();
	}
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		String user_base_directory_name = userprofile.getBaseFileName_BasedOnEMail();
		
		if (trash_directory_name == null) {
			trash_directory_name = "Trash";
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		dump.add("trash_directory_name", trash_directory_name);
		dump.add("user_base_directory_name", user_base_directory_name);
		dump.add("source_elements", source_elements.values());
		Log2.log.debug("Prepare trash", dump);
		
		progression.update("Prepare trashs directories");
		
		File current_user_trash_dir;
		HashMap<String, File> trashs_dirs = new HashMap<String, File>();
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			String storagename = entry.getValue().storagename;
			if (trashs_dirs.containsKey(storagename)) {
				continue;
			}
			File storage_dir = Explorer.getLocalBridgedElement(SourcePathIndexerElement.prepareStorageElement(storagename));
			current_user_trash_dir = new File(storage_dir.getPath() + File.separator + trash_directory_name + File.separator + user_base_directory_name);
			
			if (current_user_trash_dir.exists() == false) {
				FileUtils.forceMkdir(current_user_trash_dir);
			} else {
				CopyMove.checkExistsCanRead(current_user_trash_dir);
				CopyMove.checkIsWritable(current_user_trash_dir);
				CopyMove.checkIsDirectory(current_user_trash_dir);
			}
			trashs_dirs.put(storagename, current_user_trash_dir);
			
			if (stop) {
				return;
			}
		}
		
		progression.update("Move item(s) to trash(s) directorie(s)");
		progression.updateStep(1, source_elements.size());
		
		Date now = new Date();
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			progression.incrStep();
			File current_element = Explorer.getLocalBridgedElement(entry.getValue());
			CopyMove.checkExistsCanRead(current_element);
			CopyMove.checkIsWritable(current_element);
			
			current_user_trash_dir = trashs_dirs.get(entry.getValue().storagename);
			
			File f_destination = new File(current_user_trash_dir.getPath() + File.separator + simpledateformat.format(now) + "_" + current_element.getName());
			
			if (current_element.isDirectory()) {
				FileUtils.moveDirectory(current_element, f_destination);
			} else {
				FileUtils.moveFile(current_element, f_destination);
			}
			
			if (stop) {
				return;
			}
			
			ContainerOperations.copyMoveMetadatas(entry.getValue(), entry.getValue().storagename, "/" + trash_directory_name + "/" + user_base_directory_name, false, this);
			
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			explorer.deleteStoragePath(bulk, Arrays.asList(entry.getValue()));
			bulk.terminateBulk();
			
			if (stop) {
				return;
			}
		}
		
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		ArrayList<SourcePathIndexerElement> spie_trashs_dirs = new ArrayList<SourcePathIndexerElement>();
		for (String storage_name : trashs_dirs.keySet()) {
			SourcePathIndexerElement root_trash_directory = SourcePathIndexerElement.prepareStorageElement(storage_name);
			root_trash_directory.parentpath = root_trash_directory.prepare_key();
			root_trash_directory.directory = true;
			root_trash_directory.currentpath = "/" + trash_directory_name;
			spie_trashs_dirs.add(root_trash_directory);
		}
		
		explorer.refreshStoragePath(bulk, spie_trashs_dirs, false);
		bulk.terminateBulk();
	}
}
