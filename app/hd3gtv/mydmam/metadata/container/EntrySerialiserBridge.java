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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EntrySerialiserBridge<T extends Entry> {
	
	private T entry;
	
	public EntrySerialiserBridge(T entry) {
		this.entry = entry;
		if (entry == null) {
			throw new NullPointerException("\"entry\" can't to be null");
		}
		Operations.getGsonBuilder().registerTypeAdapter(entry.getClass(), new Serializer());
		Operations.getGsonBuilder().registerTypeAdapter(entry.getClass(), new Deserializer());
		/**
		 * Refresh static reference at each load.
		 */
	}
	
	private final class Serializer implements JsonSerializer<T> {
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return entry.serialize(src, Operations.getGson());
		}
	}
	
	private final class Deserializer implements JsonDeserializer<T> {
		@SuppressWarnings("unchecked")
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return (T) entry.deserialize(Operations.getJsonObject(json, false), Operations.getGson());
		}
	}
	
}
