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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class Entry {
	
	Entry() {
	}
	
	private Origin origin;
	
	public final Origin getOrigin() {
		return origin;
	}
	
	public abstract String getES_Type();
	
	public final void setOrigin(Origin origin) throws NullPointerException {
		if (origin == null) {
			throw new NullPointerException("\"origin\" can't to be null");
		}
		this.origin = origin;
	}
	
	protected abstract Entry create();
	
	public abstract EntrySerialiserBridge<?> createEntrySerialiserBridge();
	
	/**
	 * @param item from create()
	 */
	protected abstract void internalDeserialize(Entry _item, JsonObject source, Gson gson);
	
	/**
	 * @param item is same type like create()
	 */
	protected abstract JsonObject internalSerialize(Entry item, Gson gson);
	
	final Entry deserialize(JsonObject source, Gson gson) {
		Entry item = create();
		item.origin = gson.fromJson(source.get("origin"), Origin.class);
		source.remove("origin");
		internalDeserialize(item, source, gson);
		return item;
	}
	
	/**
	 * @param item is same type to create()
	 */
	final JsonObject serialize(Entry _item, Gson gson) {
		JsonObject jo = _item.internalSerialize(_item, gson);
		jo.add("origin", gson.toJsonTree(_item.origin));
		return jo;
	}
	
}
