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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;

public class UserAdminUpdate {
	
	public String user_key;
	public String new_password;
	public String properties;
	
	@GsonIgnore
	public ArrayList<String> user_groups;
	
	public static class Deserializer implements JsonDeserializer<UserAdminUpdate> {
		
		public UserAdminUpdate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			UserAdminUpdate result = MyDMAM.gson_kit.getGsonSimple().fromJson(json, UserAdminUpdate.class);
			result.user_groups = MyDMAM.gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("user_groups"), GsonKit.type_ArrayList_String);
			return result;
		}
		
	}
	
}
