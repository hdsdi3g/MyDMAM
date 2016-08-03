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
/**
 * 
 */
package hd3gtv.mydmam.auth.asyncjs;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.web.AJSController;
import hd3gtv.tools.GsonIgnore;

public class NewUser {
	
	public String login;
	public String fullname;
	public String domain;
	public String email_addr;
	public String password;
	public boolean locked_account;
	
	@GsonIgnore
	public ArrayList<String> user_groups;
	
	private static Type al_string_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	public static class Deserializer implements JsonDeserializer<NewUser> {
		
		public NewUser deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			NewUser result = AJSController.gson_simple.fromJson(json, NewUser.class);
			result.user_groups = AJSController.gson_simple.fromJson(json.getAsJsonObject().get("user_groups"), al_string_typeOfT);
			return result;
		}
		
	}
	
}
