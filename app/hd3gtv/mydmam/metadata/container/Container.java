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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Store all Metadatas references for a StorageIndex element, and (de)serialize from/to json.
 */
public class Container {
	
	private String mtd_key;
	private List<EntryBase> entries;
	
	private transient EntryBaseSummary summary;
	private transient HashMap<String, EntryBase> map_type_entry;
	private transient Origin origin;
	private transient HashMap<Class<?>, EntryBase> map_class_entry;
	
	public Container(String mtd_key) {
		this.mtd_key = mtd_key;
		if (mtd_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		entries = new ArrayList<EntryBase>();
		summary = null;
		map_type_entry = new HashMap<String, EntryBase>();
		map_class_entry = new HashMap<Class<?>, EntryBase>();
	}
	
	public void addEntry(EntryBase entry) {
		if (origin != null) {
			if (origin.equals(entry.getOrigin()) == false) {
				throw new NullPointerException("Can't add entry, incompatible origin");
			}
		} else {
			origin = entry.getOrigin();
		}
		
		entries.add(entry);
		
		map_type_entry.put(entry.getESType(), entry);
		map_class_entry.put(entry.getClass(), entry);
		
		if (entry instanceof EntryBaseSummary) {
			summary = (EntryBaseSummary) entry;
		}
	}
	
	public EntryBaseSummary getSummary() {
		return summary;
	}
	
	public List<EntryBase> getEntries() {
		return entries;
	}
	
	public EntryBase getByType(String type) {
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
	
	// public String toGSONString() {
	// gson = gson_builder.create();
	// return gson_builder.create().toJson(entries); // Non sense.
	// }
	
	/*void load(String content) {
	//gson = gson_builder.create();
		// entries = gson_builder.create().fromJson(content, List.class);
		
		Type typeOfT = new TypeToken<List<EntryBaseSummary>>() {
		}.getType();
		
		entries = gson_builder.create().fromJson(content, typeOfT);
	}*/
	
}
