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

public final class WorkerCapablitiesStatus {
	
	@GsonIgnore
	ArrayList<String> storages_available;
	
	// String name;
	
	// String category;
	
	JsonObject parameters;
	
	@SuppressWarnings("unused")
	private WorkerCapablitiesStatus() {
	}
	
	WorkerCapablitiesStatus(WorkerCapablities capablities) {
		// this.category = capablities.getCategory();
		// this.name = capablities.getName();
		this.parameters = capablities.getParameters();
		storages_available = new ArrayList<String>();
		List<String> cap_sa = capablities.getStoragesAvaliable();
		storages_available.addAll(cap_sa);
	}
	
	static class Serializer implements JsonSerializer<WorkerCapablitiesStatus>, JsonDeserializer<WorkerCapablitiesStatus> {
		private static Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		public WorkerCapablitiesStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			WorkerCapablitiesStatus result = AppManager.getSimpleGson().fromJson(json, WorkerCapablitiesStatus.class);
			JsonObject jo = json.getAsJsonObject();
			result.storages_available = AppManager.getSimpleGson().fromJson(jo.get("storages_available"), al_string_typeOfT);
			return result;
		}
		
		public JsonElement serialize(WorkerCapablitiesStatus src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("storages_available", AppManager.getSimpleGson().toJsonTree(src.storages_available, al_string_typeOfT).getAsJsonArray());
			return result;
		}
		
	}
	
}
