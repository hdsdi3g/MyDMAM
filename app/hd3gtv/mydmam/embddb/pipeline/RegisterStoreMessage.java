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
 * Copyright (C) hdsdi3g for hd3g.tv 4 nov. 2017
 * 
*/
package hd3gtv.mydmam.embddb.pipeline;

import com.google.gson.JsonElement;

import hd3gtv.mydmam.MyDMAM;

class RegisterStoreMessage {
	
	enum Action {
		REGISTER, UNREGISTER
	}
	
	Action action;
	String database;
	String class_name;
	RunningState running_state;
	
	RegisterStoreMessage(Action action, String database, Class<?> sync_class, RunningState running_state) {
		this.action = action;
		if (action == null) {
			throw new NullPointerException("\"action\" can't to be null");
		}
		this.database = database;
		if (database == null) {
			throw new NullPointerException("\"database\" can't to be null");
		}
		if (sync_class == null) {
			throw new NullPointerException("\"sync_class\" can't to be null");
		}
		this.class_name = sync_class.getName();
		this.running_state = running_state;
		if (running_state == null) {
			throw new NullPointerException("\"running_state\" can't to be null");
		}
	}
	
	@SuppressWarnings("unused")
	private RegisterStoreMessage() {
	}
	
	static RegisterStoreMessage fromJson(JsonElement json) {
		return MyDMAM.gson_kit.getGson().fromJson(json, RegisterStoreMessage.class);
	}
	
	JsonElement toDataBlock() {
		return MyDMAM.gson_kit.getGson().toJsonTree(this);
	}
	
}
