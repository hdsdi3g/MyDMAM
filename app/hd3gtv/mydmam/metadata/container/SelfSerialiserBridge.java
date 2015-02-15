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
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

class SelfSerialiserBridge {
	
	private SelfSerializing selfserializer;
	private static List<Class<? extends SelfSerializing>> registed;
	
	static {
		registed = new ArrayList<Class<? extends SelfSerializing>>();
	}
	
	static synchronized void registerInstance(SelfSerializing selfserializer) {
		Class<? extends SelfSerializing> candidate = selfserializer.getClass();
		
		if (registed.contains(candidate)) {
			return;
		} else {
			registed.add(candidate);
		}
		
		SelfSerialiserBridge b = new SelfSerialiserBridge(selfserializer);
		ContainerOperations.getGsonBuilder().registerTypeAdapter(candidate, b.new Serializer());
		ContainerOperations.getGsonBuilder().registerTypeAdapter(candidate, b.new Deserializer());
	}
	
	SelfSerialiserBridge(SelfSerializing selfserializer) {
		this.selfserializer = selfserializer;
	}
	
	private class Serializer implements JsonSerializer<SelfSerializing> {
		public JsonElement serialize(SelfSerializing src, Type typeOfSrc, JsonSerializationContext context) {
			return selfserializer.serialize(src, ContainerOperations.getGson());
		}
	}
	
	private class Deserializer implements JsonDeserializer<SelfSerializing> {
		public SelfSerializing deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			return (SelfSerializing) selfserializer.deserialize(ContainerOperations.getJsonObject(json, false), ContainerOperations.getGson());
		}
	}
	
}
