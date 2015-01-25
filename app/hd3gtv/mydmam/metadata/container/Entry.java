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

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class Entry implements SelfSerializing, Log2Dumpable {
	
	Entry() {
	}
	
	private Origin origin;
	
	public final Origin getOrigin() {
		return origin;
	}
	
	public abstract String getES_Type();
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("type", this.getClass().getName());
		dump.add("ES type", getES_Type());
		dump.add("origin", origin);
		return dump;
	}
	
	public final void setOrigin(Origin origin) throws NullPointerException {
		if (origin == null) {
			throw new NullPointerException("\"origin\" can't to be null");
		}
		this.origin = origin;
	}
	
	/**
	 * Do not overload in abstract implementations.
	 * @return null for not dependencies (this will be declared himself).
	 */
	protected abstract List<Class<? extends SelfSerializing>> getSerializationDependencies();
	
	/**
	 * @param item from create()
	 */
	protected abstract Entry internalDeserialize(JsonObject source, Gson gson);
	
	/**
	 * @param item is same type like create()
	 */
	protected abstract JsonObject internalSerialize(Entry item, Gson gson);
	
	public Entry deserialize(JsonObject source, Gson gson) {
		JsonElement j_origin = source.get("origin");
		source.remove("origin");
		Entry item = internalDeserialize(source, gson);
		item.origin = gson.fromJson(j_origin, Origin.class);
		return item;
	}
	
	public JsonObject serialize(SelfSerializing _item, Gson gson) {
		Entry item = (Entry) _item;
		JsonObject jo = item.internalSerialize(item, gson);
		jo.add("origin", gson.toJsonTree(item.origin));
		return jo;
	}
	
}
