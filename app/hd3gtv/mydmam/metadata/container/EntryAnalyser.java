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

import hd3gtv.mydmam.MyDMAM;

public abstract class EntryAnalyser extends ContainerEntry {
	
	/**
	 * @param _item The same type as this.
	 */
	protected abstract void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson);// TODO remove
	
	/**
	 * Patch output JSON
	 */
	protected final JsonObject internalSerialize(ContainerEntry _item, Gson gson) {// TODO move de/serializer
		JsonObject jo = MyDMAM.gson_kit.getGsonSimple().toJsonTree(_item).getAsJsonObject();
		extendedInternalSerializer(jo, (EntryAnalyser) _item, gson);
		return jo;
	}
	
	/**
	 * @return true if you wan't get JSON raw data in webclient side.
	 *         Don't set true if sometimes it contains sensitive data like some path, names or things that the end user does not have to see.
	 */
	public boolean canBeSendedToWebclients() {
		return false;
	}
	
}