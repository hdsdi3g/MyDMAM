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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;

/**
 * Store all Metadatas references for a StorageIndex element, and (de)serialize from/to json.
 */
public class Container implements Log2Dumpable {
	
	private Origin origin;
	private String mtd_key;
	private List<Entry> entries;
	private EntrySummary summary;
	private HashMap<String, Entry> map_type_entry;
	private HashMap<Class<? extends Entry>, Entry> map_class_entry;
	
	public Container(String mtd_key, Origin origin) {
		this.origin = origin;
		if (origin == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		this.mtd_key = mtd_key;
		entries = new ArrayList<Entry>();
		summary = null;
		map_type_entry = new HashMap<String, Entry>();
		map_class_entry = new HashMap<Class<? extends Entry>, Entry>();
	}
	
	/**
	 * Add origin in entry, if missing.
	 */
	public void addEntry(Entry entry) {
		if (entry.getOrigin() == null) {
			entry.setOrigin(origin);
		} else if (origin.equals(entry.getOrigin()) == false) {
			Log2Dump dump = new Log2Dump();
			dump.add("candidate", entry);
			dump.add("reference origin", origin);
			Log2.log.error("Divergent origins", null, dump);
			throw new NullPointerException("Can't add entry " + entry.getES_Type() + ", incompatible origins");
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
	public <T extends Entry> T getByClass(Class<T> class_of_T) {
		if (map_class_entry.containsKey((Class<?>) class_of_T)) {
			return (T) map_class_entry.get((Class<?>) class_of_T);
		} else {
			return null;
		}
	}
	
	public void save(boolean refresh_index_after_save) throws ElasticsearchException {
		BulkRequestBuilder bulkrequest = Operations.getClient().prepareBulk();
		Operations.save(this, refresh_index_after_save, bulkrequest);
		
		if (bulkrequest.numberOfActions() > 0) {
			BulkResponse bulkresponse = bulkrequest.execute().actionGet();
			if (bulkresponse.hasFailures()) {
				throw new ElasticsearchException(bulkresponse.buildFailureMessage());
			}
		}
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("mtd.key", mtd_key);
		if (origin != null) {
			dump.add("origin.key", origin.key);
			dump.add("origin.storage", origin.storage);
		}
		for (int pos = 0; pos < entries.size(); pos++) {
			dump.add("metadata." + entries.get(pos).getES_Type(), Operations.getGson().toJson(entries.get(pos)));
		}
		return dump;
	}
	
	public File getPhysicalSource() throws IOException {
		Origin o = getOrigin();
		if (o == null) {
			return null;
		}
		return o.getPhysicalSource();
	}
}
