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

import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.io.IOException;
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
	
	final void checkValidity(SourcePathIndexerElement element) throws IOException {
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
		if (Explorer.getBridgedStoragesName().contains(element.storagename) == false) {
			throw new IOException("Storage index for element has not a storage index bridge");
		}
	}
	
}
