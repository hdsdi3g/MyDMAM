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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.elasticsearch.ElasticsearchException;

/**
 * Store all Metadatas references for a StorageIndex element, and (de)serialize from/to json.
 */
public class Container implements Log2Dumpable {
	
	private ContainerOrigin origin;
	private String mtd_key;
	private List<ContainerEntry> containerEntries;
	private EntrySummary summary;
	private HashMap<String, ContainerEntry> map_type_entry;
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
		if (containerEntry.getOrigin() == null) {
			containerEntry.setOrigin(origin);
		} else if (origin.equals(containerEntry.getOrigin()) == false) {
			Log2Dump dump = new Log2Dump();
			dump.add("candidate", containerEntry);
			dump.add("reference origin", origin);
			Log2.log.error("Divergent origins", null, dump);
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
	
	public void save(boolean refresh_index_after_save) throws ElasticsearchException {
		ElasticsearchBulkOperation es_bulk = Elasticsearch.prepareBulk();
		save(es_bulk, refresh_index_after_save);
		es_bulk.terminateBulk();
	}
	
	public void save(ElasticsearchBulkOperation es_bulk, boolean refresh_index_after_save) throws ElasticsearchException {
		ContainerOperations.save(this, refresh_index_after_save, es_bulk);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("mtd.key", mtd_key);
		if (origin != null) {
			dump.add("origin.key", origin.key);
			dump.add("origin.storage", origin.storage);
		}
		for (int pos = 0; pos < containerEntries.size(); pos++) {
			dump.add("metadata." + containerEntries.get(pos).getES_Type(), ContainerOperations.getGson().toJson(containerEntries.get(pos)));
		}
		return dump;
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
