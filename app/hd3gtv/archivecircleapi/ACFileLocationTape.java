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
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;

public class ACFileLocationTape extends ACFileLocations {
	
	ACFileLocationTape() {
	}
	
	public String format;
	public String pool;
	
	@GsonIgnore
	public ArrayList<ACTape> tapes;
	
	public static class Deseralizer implements JsonDeserializer<ACFileLocationTape> {
		
		ACAPI acapi;
		
		public Deseralizer(ACAPI acapi) {
			this.acapi = acapi;
		}
		
		public ACFileLocationTape deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			ACFileLocationTape location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationTape.class);
			location.tapes = MyDMAM.gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("tapes"), GsonKit.type_ArrayList_ACTape);
			return location;
		}
	}
	
}
