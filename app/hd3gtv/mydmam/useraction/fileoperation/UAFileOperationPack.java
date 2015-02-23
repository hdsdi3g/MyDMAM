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

public class UAFileOperationPack extends BaseFileOperation {
	
	@Override
	protected String getSubLongName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	protected String getSubMessageBaseName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public UAJobProcess createProcess() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isPowerfulAndDangerous() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public Serializable prepareEmptyConfiguration() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public UACapability createCapability(LinkedHashMap<String, ?> internal_configuration) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void process(JobProgression progression, UserProfile userprofile, UAConfigurator user_configuration, HashMap<String, SourcePathIndexerElement> source_elements) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
}
