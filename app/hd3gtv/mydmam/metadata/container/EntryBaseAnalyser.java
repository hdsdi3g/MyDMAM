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

public abstract class EntryBaseAnalyser extends EntryBase {
	
	/**
	 * @param _item The same as create()
	 */
	protected abstract void extendedInternalSerializer(JsonObject current_element, EntryBaseAnalyser _item, Gson gson);
	
	/**
	 * Patch output JSON with metadata-provider-type = analyser
	 */
	protected final JsonObject internalSerialize(EntryBase _item, Gson gson) {
		JsonObject jo = new JsonObject();
		extendedInternalSerializer(jo, (EntryBaseAnalyser) _item, gson);
		jo.addProperty("metadata-provider-type", "analyser");
		return jo;
	}
	
}