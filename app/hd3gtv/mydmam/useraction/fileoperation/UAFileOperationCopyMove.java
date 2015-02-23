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

import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UACapability;
import hd3gtv.mydmam.useraction.UAConfigurator;
import hd3gtv.mydmam.useraction.UAJobProcess;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;

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
		
		public boolean enableRootStorageindexProcessing() {
			return false;
		}
		
		public boolean mustHaveLocalStorageindexBridge() {
			return true;
		}
		
	}
	
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		return new Capability();
	}
	
	@Override
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		UAFileOperationCopyMoveConfigurator conf = user_configuration.getObject(UAFileOperationCopyMoveConfigurator.class);
		
		// TODO Auto-generated method stub
		
		/**
		 * copy or move
		 * if move > simple mv is ok ? else "copy"
		 * if copy & file > simple copy
		 * if copy & dir > recursive copy
		 * if move & file > delete source
		 * if move & dir > recursive delete source
		 */
	}
	
}
