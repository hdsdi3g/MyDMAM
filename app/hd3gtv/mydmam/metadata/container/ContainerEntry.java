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

public abstract class ContainerEntry implements SelfSerializing, Log2Dumpable {
	
	ContainerEntry() {
	}
	
	private ContainerOrigin origin;
	
	transient Container container;
	
	public final ContainerOrigin getOrigin() {
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
	
	public final void setOrigin(ContainerOrigin origin) throws NullPointerException {
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
	
	protected abstract ContainerEntry internalDeserialize(JsonObject source, Gson gson);
	
	protected abstract JsonObject internalSerialize(ContainerEntry item, Gson gson);
	
	public ContainerEntry deserialize(JsonObject source, Gson gson) {
		JsonElement j_origin = source.get("origin");
		source.remove("origin");
		ContainerEntry item = internalDeserialize(source, gson);
		item.origin = gson.fromJson(j_origin, ContainerOrigin.class);
		return item;
	}
	
	public JsonObject serialize(SelfSerializing _item, Gson gson) {
		ContainerEntry item = (ContainerEntry) _item;
		JsonObject jo = item.internalSerialize(item, gson);
		jo.add("origin", gson.toJsonTree(item.origin));
		return jo;
	}
	
}
