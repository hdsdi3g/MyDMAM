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

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Store all Metadatas references for a StorageIndex element, and (de)serialize from/to json.
 */
public class Container implements Log2Dumpable {
	
	private String mtd_key;
	private List<Entry> entries;
	
	private transient EntrySummary summary;
	private transient HashMap<String, Entry> map_type_entry;
	private transient Origin origin;
	private transient HashMap<Class<?>, Entry> map_class_entry;
	
	public Container(String mtd_key) {
		this.mtd_key = mtd_key;
		if (mtd_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		entries = new ArrayList<Entry>();
		summary = null;
		map_type_entry = new HashMap<String, Entry>();
		map_class_entry = new HashMap<Class<?>, Entry>();
	}
	
	public void addEntry(Entry entry) {
		if (origin != null) {
			if (origin.equals(entry.getOrigin()) == false) {
				throw new NullPointerException("Can't add entry, incompatible origin");
			}
		} else {
			origin = entry.getOrigin();
		}
		
		entries.add(entry);
		
		map_type_entry.put(entry.getES_Type(), entry);
		map_class_entry.put(entry.getClass(), entry);
		
		if (entry instanceof EntrySummary) {
			summary = (EntrySummary) entry;
		}
	}
	
	public EntrySummary getSummary() {
		return summary;
	}
	
	public List<Entry> getEntries() {
		return entries;
	}
	
	public Entry getByType(String type) {
		return map_type_entry.get(type);
	}
	
	public Origin getOrigin() {
		return origin;
	}
	
	public String getMtd_key() {
		return mtd_key;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getByClass(Class<T> class_of_T) {
		return (T) map_class_entry.get((Class<?>) class_of_T);
	}
	
	public void save(boolean refresh_index_after_save) {
		Operations.save(this, refresh_index_after_save);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("mtd_key", mtd_key);
		if (origin != null) {
			dump.add("origin.key", origin.key);
			dump.add("origin.storage", origin.storage);
		}
		for (int pos = 0; pos < entries.size(); pos++) {
			dump.add(entries.get(pos).getES_Type(), Operations.getGson().toJson(entries.get(pos)));
		}
		return dump;
	}
	
}
