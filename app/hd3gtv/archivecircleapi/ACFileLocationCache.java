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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.archivecircleapi;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import hd3gtv.tools.GsonIgnore;

public class ACFileLocationCache extends ACFileLocations {
	
	ACFileLocationCache() {
	}
	
	@GsonIgnore
	public ArrayList<String> nodes;
	
	static class Deseralizer implements JsonDeserializer<ACFileLocationCache> {
		Type type_AL_String = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACFileLocationCache deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACFileLocationCache location = acapi.gson_simple.fromJson(json, ACFileLocationCache.class);
			location.nodes = acapi.gson_simple.fromJson(json.getAsJsonObject().get("nodes"), type_AL_String);
			return location;
		}
	}
	
}
