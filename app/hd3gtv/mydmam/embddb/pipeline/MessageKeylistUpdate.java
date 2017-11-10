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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.network.Protocol;
import hd3gtv.mydmam.embddb.store.HistoryJournal.HistoryEntry;
import hd3gtv.mydmam.embddb.store.Item;
import hd3gtv.mydmam.embddb.store.ItemKey;

class MessageKeylistUpdate implements MessageDStoreMapper {
	
	String database;
	String class_name;
	
	boolean has_next_list;
	ArrayList<KeyEntry> entries;
	
	static class KeyEntry {
		long update_date;
		// long delete_date;
		String key;
		int data_size;
		String data_digest;
		
		private KeyEntry() {
		}
		
		private KeyEntry(HistoryEntry h_entry) {
			update_date = h_entry.update_date;
			key = h_entry.key.toString();
			data_size = h_entry.data_size;
			data_digest = MyDMAM.byteToString(h_entry.data_digest);
		}
		
		ItemKey getItemKey() {
			return ItemKey.fromString(key);
		}
		
		byte[] getDataDigest() {
			return MyDMAM.hexStringToByteArray(data_digest);
		}
		
	}
	
	private static final int KEY_ENTRY_JSON_SIZE;
	
	static {
		KeyEntry ke = new KeyEntry();
		ke.update_date = 0l;
		ke.data_size = 0;
		ke.key = ItemKey.EMPTY.toString();
		ke.data_digest = MyDMAM.byteToString(Item.DATA_DIGEST_INST.get());
		KEY_ENTRY_JSON_SIZE = MyDMAM.gson_kit.getGson().toJson(ke).getBytes(MyDMAM.UTF8).length;
	}
	
	MessageKeylistUpdate(DistributedStore<?> store, List<HistoryEntry> history_entries) {
		if (store == null) {
			throw new NullPointerException("\"store\" can't to be null");
		}
		database = store.getDatabaseName();
		class_name = store.getGenericClassName();
		
		if (history_entries == null) {
			throw new NullPointerException("\"history_entries\" can't to be null");
		}
		
		/**
		 * Sorted, uniq (last), limited, and mapped to entries list
		 */
		Map<ItemKey, List<HistoryEntry>> history_entries_by_keys = history_entries.stream().collect(Collectors.groupingBy(entry -> {
			return entry.key;
		}));
		
		/**
		 * Protect to not propose a too big item list.
		 */
		AtomicInteger item_count_available = new AtomicInteger(Protocol.BUFFER_SIZE - 100 / KEY_ENTRY_JSON_SIZE);
		
		List<KeyEntry> k_entries = history_entries_by_keys.keySet().stream().filter(item -> {
			return has_next_list == false;
		}).map(key -> {
			return history_entries_by_keys.get(key).stream().sorted((l, r) -> {
				/**
				 * Reverse sort: keep the most recent entry in case of > 1 updates for this key.
				 */
				if (l.update_date > r.update_date) {
					return -1;
				} else {
					return 1;
				}
			}).findFirst().get();
		}).sorted((l, r) -> {
			/**
			 * Classic sort by create date.
			 */
			if (l.update_date > r.update_date) {
				return -1;
			} else {
				return 1;
			}
		}).map(item -> {
			if (has_next_list) {
				return null;
			}
			if (item_count_available.getAndDecrement() < 0) {
				has_next_list = true;
				return null;
			}
			return new KeyEntry(item);
		}).filter(item -> {
			return item != null;
		}).collect(Collectors.toList());
		
		entries = new ArrayList<>(k_entries);
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
