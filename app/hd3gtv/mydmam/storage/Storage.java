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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.fileoperation.CopyMove;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Storage {
	
	private static final List<Storage> declared_storages;
	private static final Map<String, Storage> declared_storages_by_name;
	
	static {
		declared_storages = new ArrayList<Storage>();
		List<LinkedHashMap<String, ?>> raw_defs = Configuration.global.getListMapValues("storage", "definitions");
		
		LinkedHashMap<String, ?> storage_def;
		Storage storage;
		for (int pos_l = 0; pos_l < raw_defs.size(); pos_l++) {
			storage_def = raw_defs.get(pos_l);
			try {
				storage = StorageType.getByDefinitionConfiguration(storage_def);
				if (storage == null) {
					throw new NullPointerException("Can't load storage");
				}
				storage.name = (String) storage_def.get("name");
				
				storage.regular_indexing = false;
				if (storage_def.containsKey("regular_indexing")) {
					storage.regular_indexing = (Boolean) storage_def.get("regular_indexing");
				}
				
				storage.period = 3600;
				if (storage_def.containsKey("period")) {
					storage.period = (Integer) storage_def.get("period");
				}
				
				if (storage_def.containsKey("mounted")) {
					storage.mounted = new File((String) storage_def.get("mounted"));
					CopyMove.checkExistsCanRead(storage.mounted);
					CopyMove.checkIsDirectory(storage.mounted);
				}
				declared_storages.add(storage);
			} catch (Exception e) {
				Log2.log.error("Can't setup storage, check configuration", e);
			}
		}
		
		declared_storages_by_name = new HashMap<String, Storage>();
		for (int pos = 0; pos < declared_storages.size(); pos++) {
			declared_storages_by_name.put(declared_storages.get(pos).name, declared_storages.get(pos));
		}
	}
	
	private String name;
	private boolean regular_indexing;
	private int period;
	private File mounted;
	
	abstract StorageType getStorageType();
	
	public static Storage getByName(String storage_name) {
		return declared_storages_by_name.get(storage_name);
	}
	
	public static File getLocalBridgedElement(SourcePathIndexerElement element) {
		// TODO and rename
		if (element == null) {
			return null;
		}
		
		/*File base_path = bridge.get(element.storagename);
		if (base_path == null) {
			return null;
		}
		return new File(base_path.getPath() + element.currentpath);*/
		return null;
	}
	
	public static ArrayList<String> getBridgedStoragesName() {
		// TODO and rename
		return null;
	}
	
}
