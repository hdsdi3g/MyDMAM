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

/**
 * TODO Replace Task and Job
 */
public final class JobNG {
	
	@GsonIgnore
	JobContext context;
	
	// TODO manage job lifetime, and differents status
	
	static class Serializer implements JsonSerializer<JobNG>, JsonDeserializer<JobNG> {
		public JobNG deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext jcontext) throws JsonParseException {
			JsonObject json = (JsonObject) jejson;
			String context_class = json.get("context_class").getAsString();
			json.remove("context_class");
			
			JobNG job = AppManager.getGson().fromJson(json, JobNG.class);
			try {
				job.context = (JobContext) Class.forName(context_class).newInstance();
				job.context.contextFromJson(json.getAsJsonObject("context"));
			} catch (Exception e) {
				throw new JsonParseException("Invalid context class", e);
			}
			return job;
		}
		
		public JsonElement serialize(JobNG src, Type typeOfSrc, JsonSerializationContext jcontext) {
			JsonObject result = (JsonObject) AppManager.getGson().toJsonTree(src);
			result.addProperty("context_class", src.getClass().getName());
			result.add("context", src.context.contextToJson());
			return null;
		}
		
	}
	
}
