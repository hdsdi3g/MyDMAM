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
package hd3gtv.mydmam.useraction;

import hd3gtv.mydmam.storage.Storage;

import java.util.ArrayList;
import java.util.List;

public abstract class UACapability {
	
	public boolean enableFileProcessing() {
		return false;
	}
	
	public boolean enableDirectoryProcessing() {
		return false;
	}
	
	public boolean enableRootStorageindexProcessing() {
		return false;
	}
	
	public boolean onlyLocalStorages() {
		return false;
	}
	
	/**
	 * Overload this will overload onlyLocalStorages.
	 */
	public List<String> getStorageindexesWhiteList() {
		if (onlyLocalStorages()) {
			return Storage.getLocalAccessStoragesName();
		} else {
			return new ArrayList<String>();
		}
		
	}
	
	public final UACapabilityDefinition getDefinition() {
		UACapabilityDefinition definition = new UACapabilityDefinition();
		definition.directoryprocessing_enabled = enableDirectoryProcessing();
		definition.fileprocessing_enabled = enableFileProcessing();
		definition.rootstorageindexprocessing_enabled = enableRootStorageindexProcessing();
		definition.storageindexeswhitelist = getStorageindexesWhiteList();
		if (definition.storageindexeswhitelist == null) {
			definition.storageindexeswhitelist = new ArrayList<String>();
		}
		return definition;
	}
	
}
