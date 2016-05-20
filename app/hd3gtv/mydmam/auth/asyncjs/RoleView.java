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
package hd3gtv.mydmam.auth.asyncjs;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.web.AJSController;
import hd3gtv.tools.GsonIgnore;

public class RoleView {
	
	public String key;
	public String role_name;
	
	@GsonIgnore
	public ArrayList<String> privileges;
	
	public static class Serializer implements JsonSerializer<RoleView> {
		
		@Override
		public JsonElement serialize(RoleView src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AJSController.gson_simple.toJsonTree(src).getAsJsonObject();
			result.add("privileges", AJSController.gson_simple.toJsonTree(src.privileges));
			return result;
		}
		
	}
	
}
