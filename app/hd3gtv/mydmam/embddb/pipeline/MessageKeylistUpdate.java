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
 * Copyright (C) hdsdi3g for hd3g.tv 6 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.HistoryJournal.HistoryEntry;

class MessageKeylistUpdate implements MessageDStoreMapper {
	
	String database;
	String class_name;
	
	boolean has_next_list;
	ArrayList<KeyEntry> entries;
	
	class KeyEntry {
		long update_date;
		// long delete_date;
		String key;
		int data_size;
		String data_digest;
	}
	
	MessageKeylistUpdate(String database, String class_name, List<HistoryEntry> history_entries) {
		this.database = database;
		if (database == null) {
			throw new NullPointerException("\"database\" can't to be null");
		}
		this.class_name = class_name;
		if (class_name == null) {
			throw new NullPointerException("\"class_name\" can't to be null");
		}
		entries = new ArrayList<>();
		// TODO history_entries: sorted, uniq (last), limited, and mapped to entries list
		// TODO set has_next_list if is over capacited
	}
	
	@SuppressWarnings("unused")
	private MessageKeylistUpdate() {
	}
	
	static MessageKeylistUpdate fromJson(JsonElement json) {
		return MyDMAM.gson_kit.getGson().fromJson(json, MessageKeylistUpdate.class);
	}
	
	JsonElement toDataBlock() {
		return MyDMAM.gson_kit.getGson().toJsonTree(this);
	}
	
	public String getClassName() {
		return class_name;
	}
	
	public String getDatabase() {
		return database;
	}
	
}
