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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.useraction.dummy;

import hd3gtv.mydmam.useraction.UACapability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class UADummyCapability extends UACapability {
	
	private LinkedHashMap<String, ?> internal_configuration;
	
	public UADummyCapability(LinkedHashMap<String, ?> internal_configuration) {
		this.internal_configuration = internal_configuration;
		file_p = internal_configuration.containsKey("file");
		dir_p = internal_configuration.containsKey("dir");
		root_p = internal_configuration.containsKey("root");
	}
	
	private boolean file_p = false;
	private boolean dir_p = false;
	private boolean root_p = false;
	
	public boolean enableFileProcessing() {
		return file_p;
	}
	
	public boolean enableDirectoryProcessing() {
		return dir_p;
	}
	
	public boolean enableRootStorageindexProcessing() {
		return root_p;
	}
	
	public List<String> getStorageindexesWhiteList() {
		ArrayList<String> list = new ArrayList<String>();
		if (internal_configuration.containsKey("showonly")) {
			list.add((String) internal_configuration.get("showonly"));
		}
		return list;
	}
	
	/*void checkValidity(SourcePathIndexerElement element) {
		checkValidity(element);
	}*/
	
}
