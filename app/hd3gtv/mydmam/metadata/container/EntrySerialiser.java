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

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EntrySerialiser<T extends EntryBase> {
	
	T entrybase;
	Serializer serializer;
	Deserializer deserializer;
	static Gson gson;
	
	public EntrySerialiser(T entrybase) {
		this.entrybase = entrybase;
		if (entrybase == null) {
			throw new NullPointerException("\"entrybase\" can't to be null");
		}
		serializer = new Serializer();
		deserializer = new Deserializer();
		/**
		 * Refresh static reference at each load.
		 */
		gson = Container.getGsonBuilder().create();
	}
	
	private final class Serializer implements JsonSerializer<T> {
		
		public Serializer() {
			Container.getGsonBuilder().registerTypeAdapter(entrybase.getClass(), this);
		}
		
		public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
			return entrybase.serialize(src, gson);
		}
	}
	
	private final class Deserializer implements JsonDeserializer<T> {
		
		public Deserializer() {
			Container.getGsonBuilder().registerTypeAdapter(entrybase.getClass(), this);
		}
		
		@SuppressWarnings("unchecked")
		public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return (T) entrybase.deserialize(Container.getJsonObject(json, false), gson);
		}
	}
	
}
