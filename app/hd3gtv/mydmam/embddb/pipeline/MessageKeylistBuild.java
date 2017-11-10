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

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;

/**
 * When I recevied this request, I build a key update list limited to a specific date, and I send a KeylistUpdateMessage with this content.
 */
class MessageKeylistBuild implements MessageDStoreMapper {
	
	String database;
	String class_name;
	
	long since_date;
	
	MessageKeylistBuild(DistributedStore<?> store) {
		if (store == null) {
			throw new NullPointerException("\"store\" can't to be null");
		}
		database = store.getDatabaseName();
		class_name = store.getGenericClassName();
		since_date = store.getSavedStatus().getLastSyncDate();
	}
	
	@SuppressWarnings("unused")
	private MessageKeylistBuild() {
	}
	
	static MessageKeylistBuild fromJson(JsonElement json) {
		return MyDMAM.gson_kit.getGson().fromJson(json, MessageKeylistBuild.class);
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
