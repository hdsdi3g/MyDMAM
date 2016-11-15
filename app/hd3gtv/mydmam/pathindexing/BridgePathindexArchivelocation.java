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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.pathindexing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import hd3gtv.archivecircleapi.ACAPI;
import hd3gtv.archivecircleapi.ACFile;

public class BridgePathindexArchivelocation {
	
	public class Bridge {
		private String storagename;
		private String archive_sharename;
		private String archive_rootpath;
	}
	
	private LinkedHashMap<String, Bridge> map_storagename_bridge;
	private ACAPI acapi;
	
	public BridgePathindexArchivelocation(ACAPI acapi, List<LinkedHashMap<String, ?>> raw_configuration) {
		this.acapi = acapi;
		if (acapi == null) {
			throw new NullPointerException("\"acapi\" can't to be null");
		}
		if (raw_configuration == null) {
			throw new NullPointerException("\"raw_configuration\" can't to be null");
		}
		map_storagename_bridge = new LinkedHashMap<>(raw_configuration.size() + 1);
		
		raw_configuration.forEach(entry -> {
			final Bridge bridge = new Bridge();
			if (entry.containsKey("storage") == false) {
				throw new NullPointerException("\"bridge.storage\" can't to be null");
			}
			if (entry.containsKey("share") == false) {
				throw new NullPointerException("\"bridge.share\" can't to be null");
			}
			bridge.storagename = (String) entry.get("storage");
			bridge.archive_sharename = (String) entry.get("share");
			
			if (entry.containsKey("root")) {
				bridge.archive_rootpath = (String) entry.get("root");
			} else {
				bridge.archive_rootpath = "/";
			}
			map_storagename_bridge.put(bridge.storagename, bridge);
		});
	}
	
	/**
	 * Without configuration
	 */
	public BridgePathindexArchivelocation() {
		map_storagename_bridge = new LinkedHashMap<>(1);
	}
	
	public ACFile getExternalLocation(String storagename, String path) {
		if (map_storagename_bridge.containsKey(storagename) == false) {
			throw new NullPointerException("Can't found Bridge for " + storagename);
		}
		
		Bridge b = map_storagename_bridge.get(storagename);
		
		StringBuilder full_path = new StringBuilder();
		if (b.archive_rootpath.endsWith("/") == false) {
			full_path.append(b.archive_rootpath.substring(b.archive_rootpath.length() - 1));
		} else {
			full_path.append(b.archive_rootpath);
		}
		if (path.equals("/") == false) {
			if (path.startsWith("/") == false) {
				full_path.append("/");
			}
			full_path.append(path);
		}
		
		return acapi.getFile(b.archive_sharename, full_path.toString(), true);
	}
	
	public ArrayList<String> getExternalLocationStorageList() {
		final ArrayList<String> result = new ArrayList<>(1);
		map_storagename_bridge.forEach((k, v) -> {
			result.add(k);
		});
		return result;
	}
}
