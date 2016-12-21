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

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ContainerEntry implements SelfSerializing {
	
	ContainerEntry() {
	}
	
	private ContainerOrigin origin;
	
	transient Container container;
	
	public final boolean hasOrigin() {
		return (origin != null);
	}
	
	public final ContainerOrigin getOrigin() {
		if (hasOrigin() == false) {
			throw new NullPointerException("Origin is not set because this Entry is not get from database");
		}
		return origin;
	}
	
	public abstract String getES_Type();
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("type: ");
		sb.append(this.getClass().getName());
		sb.append(", ES type: ");
		sb.append(getES_Type());
		sb.append(", origin: ");
		sb.append(origin);
		return sb.toString();
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
		ContainerEntry item;
		JsonElement j_origin = source.get("origin");
		if (j_origin != null) {
			source.remove("origin");
			item = internalDeserialize(source, gson);
			if (item == null) {
				throw new NullPointerException("Can't deserialize json " + source.getAsString() + " in class " + getClass().getName());
			}
			item.origin = gson.fromJson(j_origin, ContainerOrigin.class);
		} else {
			item = internalDeserialize(source, gson);
		}
		
		return item;
	}
	
	public JsonObject serialize(SelfSerializing _item, Gson gson) {
		ContainerEntry item = (ContainerEntry) _item;
		JsonObject jo = item.internalSerialize(item, gson);
		jo.add("origin", gson.toJsonTree(item.origin));
		return jo;
	}
	
}
