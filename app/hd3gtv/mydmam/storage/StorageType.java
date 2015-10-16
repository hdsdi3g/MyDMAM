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
package hd3gtv.mydmam.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import hd3gtv.tools.CopyMove;

abstract class StorageType {
	
	abstract List<String> getPathPrefixName();
	
	abstract Storage createStorage(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) throws Exception;
	
	private static final List<StorageType> all_storage_types;
	private static final Map<String, StorageType> all_storage_type_by_prefix_name;
	
	static {
		all_storage_types = new ArrayList<StorageType>(4);
		all_storage_types.add(new StorageTypeFile());
		all_storage_types.add(new StorageTypeSMB());
		all_storage_types.add(new StorageTypeFTP());
		all_storage_types.add(new StorageTypeFTPBroadcastServer());
		
		all_storage_type_by_prefix_name = new HashMap<String, StorageType>(4);
		List<String> prefixes;
		for (int pos_st = 0; pos_st < all_storage_types.size(); pos_st++) {
			prefixes = all_storage_types.get(pos_st).getPathPrefixName();
			for (int pos_pr = 0; pos_pr < prefixes.size(); pos_pr++) {
				all_storage_type_by_prefix_name.put(prefixes.get(pos_pr), all_storage_types.get(pos_st));
			}
		}
	}
	
	public static Storage getByDefinitionConfiguration(LinkedHashMap<String, ?> storage_def) throws Exception {
		String raw_path = (String) storage_def.get("path");
		String prefix_name = raw_path.split(":")[0];
		if (all_storage_type_by_prefix_name.containsKey(prefix_name) == false) {
			throw new Exception("Invalid storage type prefix: " + prefix_name);
		}
		return all_storage_type_by_prefix_name.get(prefix_name).createStorage(raw_path, storage_def);
	}
	
	private static class StorageTypeFile extends StorageType {
		
		List<String> getPathPrefixName() {
			return Arrays.asList("file", "local");
		}
		
		Storage createStorage(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) throws Exception {
			
			int pos_colon = configuration_path.indexOf(":");
			String path = configuration_path.substring(pos_colon + 1);
			if (path.startsWith("//")) {
				path = path.substring(1);
			}
			if (path.substring(0, 1).equals(File.separator) == false) {
				/**
				 * Windows case.
				 */
				path = path.substring(1);
			}
			
			File root_path = new File(path);
			CopyMove.checkExistsCanRead(root_path);
			CopyMove.checkIsDirectory(root_path);
			
			return new StorageLocalFile(root_path);
		}
		
	}
	
	private static class StorageTypeFTP extends StorageType {
		
		List<String> getPathPrefixName() {
			return Arrays.asList("ftp_passive", "ftp_active", "ftp");
		}
		
		Storage createStorage(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) throws Exception {
			URILoginPasswordConfiguration configuration = new URILoginPasswordConfiguration(configuration_path, optional_storage_def);
			boolean ftp_active = configuration_path.startsWith("ftp_active:/");
			return new StorageFTP(configuration, ftp_active);
		}
	}
	
	private static class StorageTypeFTPBroadcastServer extends StorageType {
		
		List<String> getPathPrefixName() {
			return Arrays.asList("ftp_broadcastserver");
		}
		
		Storage createStorage(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) throws Exception {
			URILoginPasswordConfiguration configuration = new URILoginPasswordConfiguration(configuration_path, optional_storage_def);
			return new StorageFTPBroadcastServer(configuration);
		}
		
	}
	
	private static class StorageTypeSMB extends StorageType {
		
		List<String> getPathPrefixName() {
			return Arrays.asList("smb", "cifs");
		}
		
		Storage createStorage(String configuration_path, LinkedHashMap<String, ?> optional_storage_def) throws Exception {
			URILoginPasswordConfiguration configuration = new URILoginPasswordConfiguration(configuration_path, optional_storage_def);
			return new StorageSMB(configuration);
		}
		
	}
	
}
