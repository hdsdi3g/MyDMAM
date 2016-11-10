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
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class ACPositionType {
	
	ACPositionType() {
	}
	
	public ArrayList<String> cache;
	public ArrayList<String> disk;
	public ArrayList<String> nearline;
	public ArrayList<String> offline;
	
	static class Deseralizer implements JsonDeserializer<ACPositionType> {
		Type type_AL_String = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACPositionType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACPositionType pt = new ACPositionType();
			JsonObject jo = json.getAsJsonObject();
			
			pt.cache = acapi.gson_simple.fromJson(jo.get("cache"), type_AL_String);
			pt.disk = acapi.gson_simple.fromJson(jo.get("disk"), type_AL_String);
			pt.nearline = acapi.gson_simple.fromJson(jo.get("nearline"), type_AL_String);
			pt.offline = acapi.gson_simple.fromJson(jo.get("offline"), type_AL_String);
			return pt;
		}
	}
	
}
