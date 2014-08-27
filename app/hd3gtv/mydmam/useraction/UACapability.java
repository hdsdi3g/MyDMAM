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

import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class UACapability {
	
	public abstract UACapability getFromConfiguration(HashMap<String, ConfigurationItem> internal_configuration);
	
	public abstract boolean enableFileProcessing();
	
	public abstract boolean enableDirectoryProcessing();
	
	public abstract boolean enableRootStorageindexProcessing();
	
	public abstract boolean mustHaveLocalStorageindexBridge();
	
	public List<String> getStorageindexesWhiteList() {
		return new ArrayList<String>();
	}
	
	void checkValidity(SourcePathIndexerElement element) throws IOException {
		if ((enableFileProcessing() == false) & (element.directory == false)) {
			throw new IOException("Element is a file, and file processing is not available");
		}
		if ((enableDirectoryProcessing() == false) & element.directory) {
			throw new IOException("Element is a directory, and directory processing is not available");
		}
		if (element.prepare_key().equalsIgnoreCase(SourcePathIndexerElement.ROOT_DIRECTORY_KEY)) {
			throw new IOException("Element is the root storage, it will not be available");
		}
		if ((enableRootStorageindexProcessing() == false) & (element.parentpath == null)) {
			throw new IOException("Element is a storage index root, and this is not available");
		}
		List<String> white_list = getStorageindexesWhiteList();
		if (white_list != null) {
			if (white_list.isEmpty() == false) {
				if (white_list.contains(element.storagename) == false) {
					throw new IOException("Storage index for element is not in white list.");
				}
			}
		}
		if (mustHaveLocalStorageindexBridge()) {
			if (Explorer.getBridgedStoragesName().contains(element.storagename) == false) {
				throw new IOException("Storage index for element has not a storage index bridge");
			}
		}
		return;
	}
	
}
