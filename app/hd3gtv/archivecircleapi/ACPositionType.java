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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class ACPositionType {
	
	ACPositionType() {
	}
	
	public ArrayList<String> cache;
	public ArrayList<String> disk;
	public ArrayList<String> nearline;
	public ArrayList<String> offline;
	
	public static class Deseralizer implements JsonDeserializer<ACPositionType> {
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACPositionType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACPositionType pt = new ACPositionType();
			JsonObject jo = json.getAsJsonObject();
			
			pt.cache = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("cache"), GsonKit.type_ArrayList_String);
			pt.disk = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("disk"), GsonKit.type_ArrayList_String);
			pt.nearline = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("nearline"), GsonKit.type_ArrayList_String);
			pt.offline = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("offline"), GsonKit.type_ArrayList_String);
			return pt;
		}
	}
	
}
