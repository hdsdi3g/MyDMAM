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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.metadata.container;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonDeSerializer;

public abstract class ContainerEntryDeSerializer<T extends ContainerEntry> implements GsonDeSerializer<T> {
	
	/**
	 * You must overload internalSerialize()
	 */
	public final T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		T item = null;
		JsonObject source = json.getAsJsonObject();
		JsonElement j_origin = source.get("origin");
		if (j_origin != null) {
			source.remove("origin");
			item = internalDeserialize(source);
			if (item == null) {
				throw new NullPointerException("Can't deserialize json " + source.toString() + " in class " + getClass().getName());
			}
			item.origin = MyDMAM.gson_kit.getGsonSimple().fromJson(j_origin, ContainerOrigin.class);
		} else {
			item = internalDeserialize(source);
		}
		
		return item;
	}
	
	/**
	 * You should overload internalSerialize()
	 */
	public final JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject jo = internalSerialize(src);
		if (jo == null) {
			Loggers.Metadata.warn("Return null for serialize by " + getClass());
			jo = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
		}
		jo.add("origin", MyDMAM.gson_kit.getGsonSimple().toJsonTree(src.origin));
		return jo;
	}
	
	protected abstract T internalDeserialize(JsonObject source);
	
	/**
	 * By default with GsonSimple
	 */
	protected JsonObject internalSerialize(T item) {
		return MyDMAM.gson_kit.getGsonSimple().toJsonTree(item).getAsJsonObject();
	}
	
}
