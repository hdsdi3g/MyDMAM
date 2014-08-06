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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class EntryAnalyser extends Entry {
	
	/**
	 * @param _item The same as create()
	 */
	protected abstract void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson);
	
	/**
	 * Patch output JSON with metadata-provider-type = analyser
	 */
	protected final JsonObject internalSerialize(Entry _item, Gson gson) {
		JsonObject jo = new JsonObject();
		extendedInternalSerializer(jo, (EntryAnalyser) _item, gson);
		jo.addProperty(MetadataCenter.METADATA_PROVIDER_TYPE, "analyser");
		return jo;
	}
	
}