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
package hd3gtv.mydmam.metadata.container;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import org.elasticsearch.ElasticsearchException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.pathindexing.WebCacheInvalidation;

/**
 * Store all Metadatas references for a StorageIndex element, and (de)serialize from/to json.
 */
public class Container {
	
	private ContainerOrigin origin;
	private String mtd_key;
	private List<ContainerEntry> containerEntries;
	private EntrySummary summary;
	private HashMap<String, ContainerEntry> map_type_entry;
	
	@Deprecated
	private HashMap<Class<? extends ContainerEntry>, ContainerEntry> map_class_entry;
	
	public Container(String mtd_key, ContainerOrigin origin) {
		this.origin = origin;
		if (origin == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		this.mtd_key = mtd_key;
		containerEntries = new ArrayList<ContainerEntry>();
		summary = null;
		map_type_entry = new HashMap<String, ContainerEntry>();
		map_class_entry = new HashMap<Class<? extends ContainerEntry>, ContainerEntry>();
	}
	
	/**
	 * Add origin in entry, if missing.
	 */
	public void addEntry(ContainerEntry containerEntry) {
		if (containerEntry.hasOrigin() == false) {
			containerEntry.setOrigin(origin);
		} else if (origin.equals(containerEntry.getOrigin()) == false) {
			Loggers.Metadata.error("Divergent origins, candidate: " + containerEntry + ", reference origin: " + origin);
			throw new NullPointerException("Can't add entry " + containerEntry.getES_Type() + ", incompatible origins");
		}
		containerEntry.container = this;
		containerEntries.add(containerEntry);
		
		map_type_entry.put(containerEntry.getES_Type(), containerEntry);
		map_class_entry.put(containerEntry.getClass(), containerEntry);
		
		if (containerEntry instanceof EntrySummary) {
			summary = (EntrySummary) containerEntry;
		}
	}
	
	public EntrySummary getSummary() {
		return summary;
	}
	
	public List<ContainerEntry> getEntries() {
		return containerEntries;
	}
	
	public ContainerEntry getByType(String type) {
		ContainerEntry result = map_type_entry.get(type);
		result.container = this;
		return result;
	}
	
	public <T extends ContainerEntry> T getByType(String type, Class<T> class_type) {
		if (map_type_entry.containsKey(type) == false) {
			return null;
		}
		
		ContainerEntry c_e = map_type_entry.get(type);
		if (class_type.isAssignableFrom(c_e.getClass()) == false) {
			Loggers.Metadata.error("Invalid transtype for type " + type + ": want item (" + class_type + ") is not compatible with declared item (" + c_e.getClass() + ")");
			return null;
		}
		
		@SuppressWarnings("unchecked")
		T result = (T) map_type_entry.get(type);
		result.container = this;
		return result;
	}
	
	public ContainerOrigin getOrigin() {
		return origin;
	}
	
	void changeAllOrigins(ContainerOrigin new_origin) {
		for (int pos_e = 0; pos_e < containerEntries.size(); pos_e++) {
			containerEntries.get(pos_e).setOrigin(new_origin);
		}
		mtd_key = new_origin.getUniqueElementKey();
	}
	
	public String getMtd_key() {
		return mtd_key;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends ContainerEntry> T getByClass(Class<T> class_of_T) {
		if (map_class_entry.containsKey((Class<?>) class_of_T)) {
			T result = (T) map_class_entry.get((Class<?>) class_of_T);
			result.container = this;
			return result;
		} else {
			return null;
		}
	}
	
	public boolean containAnyMatchContainerEntry(Stream<Class<? extends ContainerEntry>> s_class_of_T) {
		return s_class_of_T.anyMatch(c -> {
			return map_class_entry.containsKey(c);
		});
	}
	
	public void save() throws ElasticsearchException {
		ElasticsearchBulkOperation es_bulk = Elasticsearch.prepareBulk();
		save(es_bulk);
		es_bulk.terminateBulk();
		
		try {
			WebCacheInvalidation.addInvalidation(this.getOrigin().getPathindexElement().storagename);
		} catch (FileNotFoundException e) {
			Loggers.Metadata.error("Can't found origin pathindexElement, " + this, e);
		}
	}
	
	/**
	 * Don't forget to invalidate Play Cache Stat with WebCacheInvalidation.
	 */
	public void save(ElasticsearchBulkOperation es_bulk) throws ElasticsearchException {
		ContainerOperations.save(this, false, es_bulk);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("mtd.key: ");
		sb.append(mtd_key);
		if (origin != null) {
			sb.append(", origin.key: ");
			sb.append(origin.key);
			sb.append(", origin.storage: ");
			sb.append(origin.storage);
		}
		
		for (int pos = 0; pos < containerEntries.size(); pos++) {
			sb.append(", metadata." + containerEntries.get(pos).getES_Type() + ": ");
			try {
				sb.append(MyDMAM.gson_kit.getGson().toJson(containerEntries.get(pos)));
			} catch (Exception e) {
				/**
				 * Check serializators.
				 */
				Loggers.Metadata.error("Problem during serialization with " + containerEntries.get(pos).getClass().getName(), e);
			}
		}
		return sb.toString();
	}
	
	public File getPhysicalSource() throws IOException {
		ContainerOrigin o = getOrigin();
		if (o == null) {
			return null;
		}
		return o.getPhysicalSource();
	}
	
	public boolean hasRenderers() {
		for (int pos_e = 0; pos_e < containerEntries.size(); pos_e++) {
			if (containerEntries.get(pos_e) instanceof EntryRenderer) {
				EntryRenderer renderer = (EntryRenderer) containerEntries.get(pos_e);
				if (renderer.isEmpty() == false) {
					return true;
				}
			}
		}
		return false;
	}
}
