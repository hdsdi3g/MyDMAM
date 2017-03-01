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

public final class JobCreatorDeclarationSerializer implements JsonSerializer<JobCreator.Declaration>, JsonDeserializer<JobCreator.Declaration> {
	
	public JobCreator.Declaration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jo = json.getAsJsonObject();
		JobCreator.Declaration result = MyDMAM.gson_kit.getGsonSimple().fromJson(json, JobCreator.Declaration.class);
		result.contexts = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("contexts"), GsonKit.type_ArrayList_JobContext);
		return result;
	}
	
	public JsonElement serialize(JobCreator.Declaration src, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
		result.add("contexts", MyDMAM.gson_kit.getGson().toJsonTree(src.contexts, GsonKit.type_ArrayList_JobContext));
		return result;
	}
	
}
