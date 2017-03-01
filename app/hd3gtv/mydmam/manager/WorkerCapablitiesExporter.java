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
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;

public final class WorkerCapablitiesExporter {
	
	@GsonIgnore
	List<String> neededstorages;
	
	@GsonIgnore
	Class<? extends JobContext> classname;
	
	@GsonIgnore
	public List<String> hookednames;
	
	@SuppressWarnings("unused")
	private WorkerCapablitiesExporter() {
	}
	
	WorkerCapablitiesExporter(WorkerCapablities capablities) {
		neededstorages = capablities.getStoragesAvaliable();
		classname = capablities.getJobContextClass();
		hookednames = capablities.getHookedNames();
	}
	
	public static class Serializer implements JsonSerializer<WorkerCapablitiesExporter>, JsonDeserializer<WorkerCapablitiesExporter> {
		
		public WorkerCapablitiesExporter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			WorkerCapablitiesExporter result = MyDMAM.gson_kit.getGsonSimple().fromJson(json, WorkerCapablitiesExporter.class);
			JsonObject jo = json.getAsJsonObject();
			result.neededstorages = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("neededstorages"), GsonKit.type_ArrayList_String);
			result.classname = MyDMAM.gson_kit.getGson().fromJson(jo.get("classname"), GsonKit.type_Class_JobContext);
			result.hookednames = MyDMAM.gson_kit.getGson().fromJson(jo.get("hookednames"), GsonKit.type_ArrayList_String);
			return result;
		}
		
		public JsonElement serialize(WorkerCapablitiesExporter src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
			result.add("neededstorages", MyDMAM.gson_kit.getGsonSimple().toJsonTree(src.neededstorages, GsonKit.type_ArrayList_String));
			result.add("classname", MyDMAM.gson_kit.getGson().toJsonTree(src.classname, GsonKit.type_Class_JobContext));
			result.add("hookednames", MyDMAM.gson_kit.getGson().toJsonTree(src.hookednames, GsonKit.type_ArrayList_String));
			return result;
		}
		
	}
	
}
