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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.web.AJSController;
import hd3gtv.tools.GsonIgnore;

public class UserView {
	
	public String key;
	public String login;
	public String fullname;
	public String domain;
	public String language;
	public String email_addr;
	public long createdate;
	public long lasteditdate;
	public long lastlogindate;
	public String lastloginipsource;
	public boolean locked_account;
	
	@GsonIgnore
	public ArrayList<String> user_groups;
	
	public JsonObject preferencies;
	public String properties;
	public JsonObject baskets;
	public JsonArray activities;
	public JsonArray notifications;
	
	public static class Serializer implements JsonSerializer<UserView> {
		
		@Override
		public JsonElement serialize(UserView src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AJSController.gson_simple.toJsonTree(src).getAsJsonObject();
			result.add("user_groups", AJSController.gson_simple.toJsonTree(src.user_groups));
			return result;
		}
		
	}
	
}
