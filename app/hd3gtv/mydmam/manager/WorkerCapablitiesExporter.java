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
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.tools.GsonIgnore;

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
	
	static class Serializer implements JsonSerializer<WorkerCapablitiesExporter>, JsonDeserializer<WorkerCapablitiesExporter> {
		private static Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		private static Type class_jobcontext_typeOfT = new TypeToken<Class<? extends JobContext>>() {
		}.getType();
		
		public WorkerCapablitiesExporter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			WorkerCapablitiesExporter result = AppManager.getSimpleGson().fromJson(json, WorkerCapablitiesExporter.class);
			JsonObject jo = json.getAsJsonObject();
			result.neededstorages = AppManager.getSimpleGson().fromJson(jo.get("neededstorages"), al_string_typeOfT);
			result.classname = AppManager.getGson().fromJson(jo.get("classname"), class_jobcontext_typeOfT);
			result.hookednames = AppManager.getGson().fromJson(jo.get("hookednames"), al_string_typeOfT);
			return result;
		}
		
		public JsonElement serialize(WorkerCapablitiesExporter src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("neededstorages", AppManager.getSimpleGson().toJsonTree(src.neededstorages, al_string_typeOfT));
			result.add("classname", AppManager.getGson().toJsonTree(src.classname, class_jobcontext_typeOfT));
			result.add("hookednames", AppManager.getGson().toJsonTree(src.hookednames, al_string_typeOfT));
			return result;
		}
		
	}
	
}
