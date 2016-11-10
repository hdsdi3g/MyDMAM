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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public abstract class ACFileLocations {
	
	ACFileLocations() {
	}
	
	public String type;
	
	static class Deseralizer implements JsonDeserializer<ACFileLocations> {
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACFileLocations deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jo = json.getAsJsonObject();
			String type = jo.get("type").getAsString();
			
			if (type.equalsIgnoreCase("CACHE")) {
				return acapi.gson.fromJson(json, ACFileLocationCache.class);
			} else if (type.equalsIgnoreCase("PACK")) {
				return acapi.gson.fromJson(json, ACFileLocationPack.class);
			} else if (type.equalsIgnoreCase("TAPE")) {
				return acapi.gson.fromJson(json, ACFileLocationTape.class);
			} else {
				throw new JsonParseException("Unknow type: " + type);
			}
		}
	}
	
}
