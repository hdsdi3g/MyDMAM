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
package hd3gtv.mydmam.manager;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class JobCreatorSerializer<T extends JobCreator> implements JsonSerializer<T>, JsonDeserializer<T> {
	
	private Class<T> referer_class;
	
	JobCreatorSerializer(Class<T> referer_class) {
		this.referer_class = referer_class;
	}
	
	public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jo = json.getAsJsonObject();
		T result = MyDMAM.gson_kit.getGsonSimple().fromJson(json, referer_class);
		result.declarations = MyDMAM.gson_kit.getGson().fromJson(jo.get("declarations"), GsonKit.type_ArrayList_JobCreatorJobDeclaration);
		return result;
	}
	
	public JsonElement serialize(T src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
		result.add("declarations", MyDMAM.gson_kit.getGson().toJsonTree(src.declarations, GsonKit.type_ArrayList_JobCreatorJobDeclaration));
		return result;
	}
	
}
