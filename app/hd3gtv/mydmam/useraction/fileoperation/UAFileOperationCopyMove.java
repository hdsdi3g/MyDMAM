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
import hd3gtv.mydmam.storage.Storage;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;
import hd3gtv.mydmam.useraction.fileoperation.UAFileOperationCopyMoveConfigurator.Action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

public class UAFileOperationCopyMove extends BaseFileOperation {
	
	private Explorer explorer = new Explorer();
	
	protected String getSubLongName() {
		return "copy/move files and directories";
	}
	
	protected String getSubMessageBaseName() {
		return "copymove";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationCopyMove();
	}
	
	public String getDescription() {
		return "Copy and move files and directories content";
	}
	
	public boolean isPowerfulAndDangerous() {
		return true;
	}
	
	public Serializable prepareEmptyConfiguration() {
		return new UAFileOperationCopyMoveConfigurator();
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
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		UAFileOperationCopyMoveConfigurator conf = user_configuration.getObject(UAFileOperationCopyMoveConfigurator.class);
		
		if (conf.destination == null) {
			throw new NullPointerException("\"destination\" can't to be null");
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		if (conf.destination.trim().equals("")) {
			Log2.log.debug("\"destination\" is empty", dump);
			return;
		}
		dump.add("path index dest", conf.destination);
		
		SourcePathIndexerElement root_destination = explorer.getelementByIdkey(conf.destination);
		if (root_destination == null) {
			throw new FileNotFoundException("\"" + root_destination + "\" in storage index");
		}
		
		File f_destination = Storage.getLocalFile(root_destination);
		CopyMove.checkExistsCanRead(f_destination);
		CopyMove.checkIsDirectory(f_destination);
		CopyMove.checkIsWritable(f_destination);
		
		dump.add("real dest", f_destination);
		
		progression.updateStep(1, source_elements.size());
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			File source = Storage.getLocalFile(entry.getValue());
			CopyMove.checkExistsCanRead(source);
			
			if ((conf.action == Action.MOVE) & (source.canWrite() == false)) {
				throw new IOException("\"" + source.getPath() + "\" can't to be write (erased)");
			}
			
			Log2.log.debug("Prepare " + conf.action, dump);
			
			CopyMove cm = new CopyMove(source, f_destination);
			cm.setDelete_after_copy(conf.action == Action.MOVE);
			cm.setProgression(progression);
			cm.setFileExistsPolicy(conf.fileexistspolicy);
			cm.operate();
			
			if (stop) {
				return;
			}
			
			SourcePathIndexerElement destination = root_destination;
			if (entry.getValue().directory) {
				/**
				 * Compute this sub directory dest path name.
				 */
				destination = root_destination.clone();
				destination.currentpath = destination.currentpath + "/" + entry.getValue().currentpath.substring(entry.getValue().currentpath.lastIndexOf("/") + 1);
				if (destination.currentpath.startsWith("//")) {
					destination.currentpath = destination.currentpath.substring(1);
				}
			}
			
			ContainerOperations.copyMoveMetadatas(entry.getValue(), destination.storagename, destination.currentpath, conf.action == Action.COPY, this);
			
			if (stop) {
				return;
			}
			
			ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
			if (conf.action == Action.MOVE) {
				explorer.deleteStoragePath(bulk, Arrays.asList(entry.getValue()));
			}
			explorer.refreshStoragePath(bulk, Arrays.asList(root_destination), false);
			bulk.terminateBulk();
			
			if (stop) {
				return;
			}
			progression.incrStep();
		}
	}
	
}
