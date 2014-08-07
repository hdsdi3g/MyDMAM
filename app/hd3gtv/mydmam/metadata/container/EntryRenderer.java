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

import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.RenderedFile;

import java.lang.reflect.Type;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public abstract class EntryRenderer extends Entry {
	
	public List<RenderedContent> content;
	
	protected final void internalDeserialize(Entry _entry, JsonObject source, Gson gson) {
		EntryRenderer entry = (EntryRenderer) _entry;
		JsonElement item = source.get("content");
		Type typeOfT = new TypeToken<List<RenderedFile>>() {
		}.getType();
		entry.content = gson.fromJson(item.getAsJsonArray(), typeOfT);
	}
	
	protected final JsonObject internalSerialize(Entry _item, Gson gson) {
		EntryRenderer src = (EntryRenderer) _item;
		JsonObject jo = new JsonObject();
		jo.addProperty(MetadataCenter.METADATA_PROVIDER_TYPE, "renderer");
		jo.add("content", gson.toJsonTree(src.content));
		return jo;
	}
}
