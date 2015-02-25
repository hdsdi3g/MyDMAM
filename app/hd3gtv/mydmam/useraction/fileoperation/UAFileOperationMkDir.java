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
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

public class UAFileOperationMkDir extends BaseFileOperation {
	
	protected String getSubLongName() {
		return "create directory";
	}
	
	protected String getSubMessageBaseName() {
		return "mkdir";
	}
	
	public UAJobProcess createProcess() {
		return new UAFileOperationMkDir();
	}
	
	public String getDescription() {
		return "Create new sub directory from current directory";
	}
	
	public boolean isPowerfulAndDangerous() {
		return false;
	}
	
	public Serializable prepareEmptyConfiguration() {
		return new UAFileOperationMkDirConfigurator();
	}
	
	public class Capability extends UACapability {
		
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
	
	/**
	 * UAJobProcess part
	 */
	
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		UAFileOperationMkDirConfigurator conf = user_configuration.getObject(UAFileOperationMkDirConfigurator.class);
		
		if (conf.newpathname == null) {
			throw new NullPointerException("\"newpathname\" can't to be null");
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("user", userprofile.key);
		
		if (conf.newpathname.trim().equals("")) {
			Log2.log.debug("\"newpathname\" is empty", dump);
			return;
		}
		
		dump.add("newpathname", conf.newpathname);
		
		progression.updateStep(1, source_elements.size());
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			File current_dir = Explorer.getLocalBridgedElement(entry.getValue());
			if (current_dir == null) {
				throw new NullPointerException("Can't found current_dir: " + entry.getValue().storagename + ":" + entry.getValue().currentpath);
			}
			if (current_dir.exists() == false) {
				throw new FileNotFoundException("Can't found current_dir: " + entry.getValue().storagename + ":" + entry.getValue().currentpath);
			}
			if (current_dir.isDirectory() == false) {
				throw new FileNotFoundException("current_dir is not a dir: " + entry.getValue().storagename + ":" + entry.getValue().currentpath);
			}
			if (current_dir.canWrite() == false) {
				throw new IOException("Can't write to current_dir: " + entry.getValue().storagename + ":" + entry.getValue().currentpath);
			}
			conf.newpathname = conf.newpathname.replace("\\", File.separator);
			conf.newpathname = conf.newpathname.replace("/", File.separator);
			
			String current_dir_path = current_dir.getCanonicalPath();
			File dest_dir = new File(current_dir_path + File.separator + conf.newpathname);
			
			if (dest_dir.exists()) {
				continue;
			}
			
			if (dest_dir.getCanonicalPath().startsWith(current_dir_path) == false) {
				dump.add("current_dir_path", current_dir_path);
				dump.add("dest_dir", dest_dir.getCanonicalPath());
				Log2.log.security("User try to create a sub directory outside the choosed directory", dump);
				throw new IOException("Invalid newpathname: " + conf.newpathname);
			}
			
			if (dest_dir.mkdirs() == false) {
				dump.add("dest_dir", dest_dir);
				Log2.log.debug("Can't create correctly directories", dump);
				throw new IOException("Can't create correctly directories: " + dest_dir.getPath());
			}
			
			// TODO mkdir in db
			if (stop) {
				return;
			}
			progression.incrStep();
		}
		
	}
	
}
