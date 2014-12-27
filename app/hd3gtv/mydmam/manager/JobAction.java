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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class JobAction {
	
	public JsonObject doAction() {
		JsonObject jo = new JsonObject();
		
		return jo;
	}
	
	static class Serializer implements JsonSerializer<JobAction>, JsonDeserializer<JobAction> {
		
		private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		@Override
		public JobAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public JsonElement serialize(JobAction src, Type typeOfSrc, JsonSerializationContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
}
