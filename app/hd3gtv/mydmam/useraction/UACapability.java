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
	
	public List<String> getStorageindexesWhiteList() {
		return new ArrayList<String>();
	}
	
	final List<String> getStorageindexesRealList() {
		List<String> whitelist = getStorageindexesWhiteList();
		if (whitelist == null) {
			whitelist = new ArrayList<String>(1);
		}
		List<String> bridgedstorages = Storage.getLocalAccessStoragesName();
		
		List<String> storages_to_add = new ArrayList<String>();
		if (whitelist.isEmpty()) {
			storages_to_add = bridgedstorages;
		} else {
			for (int pos = 0; pos < whitelist.size(); pos++) {
				if (bridgedstorages.contains(whitelist.get(pos))) {
					storages_to_add.add(whitelist.get(pos));
				}
			}
		}
		return storages_to_add;
	}
	
	public final UACapabilityDefinition getDefinition() {
		UACapabilityDefinition definition = new UACapabilityDefinition();
		definition.directoryprocessing_enabled = enableDirectoryProcessing();
		definition.fileprocessing_enabled = enableFileProcessing();
		definition.rootstorageindexprocessing_enabled = enableRootStorageindexProcessing();
		definition.storageindexeswhitelist = getStorageindexesRealList();
		return definition;
	}
	
}
