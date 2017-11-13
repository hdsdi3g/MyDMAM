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
 * Copyright (C) hdsdi3g for hd3g.tv 13 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.embddb.store.ItemKey;

class MessageKeyContentBulkList implements MessageDStoreMapper {
	
	String database;
	String class_name;
	
	ArrayList<String> key_list;
	
	@SuppressWarnings("unused")
	private MessageKeyContentBulkList() {
	}
	
	MessageKeyContentBulkList(DistributedStore<?> store, Stream<ItemKey> entries_to_get) {
		if (store == null) {
			throw new NullPointerException("\"store\" can't to be null");
		}
		database = store.getDatabaseName();
		class_name = store.getGenericClassName();
		
		if (entries_to_get == null) {
			throw new NullPointerException("\"entries_to_get\" can't to be null");
		}
		key_list = new ArrayList<>(entries_to_get.map(k -> k.toString()).collect(Collectors.toList()));
	}
	
	static MessageKeyContentBulkList fromJson(JsonElement json) {
		return MyDMAM.gson_kit.getGson().fromJson(json, MessageKeyContentBulkList.class);
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
