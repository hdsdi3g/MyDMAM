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
import hd3gtv.mydmam.useraction.fileoperation.UAFileOperationCopyMoveConfigurator.Action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import models.UserProfile;

public class UAFileOperationCopyMove extends BaseFileOperation {
	
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
		
		public boolean mustHaveLocalStorageindexBridge() {
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
		File f_destination = getDestinationFile(conf);
		dump.add("real dest", f_destination);
		
		progression.updateStep(1, source_elements.size());
		
		for (Map.Entry<String, SourcePathIndexerElement> entry : source_elements.entrySet()) {
			File source = Explorer.getLocalBridgedElement(entry.getValue());
			CopyMove.checkExistsCanRead(source);
			
			if ((conf.action == Action.MOVE) & (source.canWrite() == false)) {
				throw new IOException("\"" + source.getPath() + "\" can't to be write (erased)");
			}
			
			Log2.log.debug("Prepare " + conf.action, dump);
			
			CopyMove cm = new CopyMove(source, f_destination);
			cm.setDelete_after_copy(conf.action == Action.MOVE);
			cm.setProgression(progression);
			cm.operate();
			
			// TODO move/copy in db, with mtds
			// ContainerOperations.getByMtdKey(mtd_key)
			
			if (stop) {
				return;
			}
			progression.incrStep();
		}
	}
	
	private File getDestinationFile(UAFileOperationCopyMoveConfigurator conf) throws Exception {
		// conf.destination
		String destination = conf.destination.replace("\\", File.separator);
		destination = destination.replace("/", File.separator);
		if (destination.indexOf(":") < 1) {
			throw new IndexOutOfBoundsException("Invalid destination pathindex: " + destination);
		}
		if (destination.endsWith(File.separator)) {
			destination = destination.substring(destination.length() - 1);
		}
		Explorer explorer = new Explorer();
		SourcePathIndexerElement spie_dest = explorer.getelementByIdkey(SourcePathIndexerElement.hashThis(destination));
		if (spie_dest == null) {
			throw new FileNotFoundException("\"" + destination + "\" in storage index");
		}
		
		File f_destination = Explorer.getLocalBridgedElement(spie_dest);
		CopyMove.checkExistsCanRead(f_destination);
		CopyMove.checkIsDirectory(f_destination);
		CopyMove.checkIsWritable(f_destination);
		
		return f_destination;
	}
	
	/*private void processSourceElement(JobProgression progression, UAFileOperationCopyMoveConfigurator conf, File source, File destination) throws Exception {
		boolean source_is_like_file = (source.isFile() | FileUtils.isSymlink(source));
		boolean user_want_copy = (conf.action == Action.COPY);
		String destination_base_path = destination.getCanonicalPath() + File.separator;
		
		if (source_is_like_file) {
			File destination_file = new File(destination_base_path + source.getName());
			
			if (user_want_copy == false) {
				if (source.renameTo(destination_file)) {
					return;
				}
			}
			
			// copyFile(progression, source, destination_file);
			
			if (user_want_copy == false) {
				if (source.delete() == false) {
					throw new IOException("Can't delete source file \"" + source.getAbsolutePath() + "\"");
				}
			}
			return;
		}
		
	}*/
	
}
