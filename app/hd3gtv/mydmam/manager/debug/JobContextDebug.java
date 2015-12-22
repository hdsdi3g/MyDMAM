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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.manager.debug;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import hd3gtv.mydmam.manager.JobContext;

public class JobContextDebug extends JobContext {
	
	public JobContextDebug() {
	}
	
	public JobContextDebug(String storage, String hook) {
		this.neededstorages = Arrays.asList(storage);
		this.hookednames = Arrays.asList(hook);
	}
	
	public JsonObject contextToJson() {
		JsonObject result = new JsonObject();
		result.addProperty("sample", "value");
		JsonArray ja = new JsonArray();
		ja.add(new JsonPrimitive("foo1"));
		ja.add(new JsonPrimitive("foo2"));
		ja.add(new JsonPrimitive("foo3"));
		result.add("anarray", ja);
		result.add("a null value", JsonNull.INSTANCE);
		JsonObject sub = new JsonObject();
		sub.addProperty("item1", 1);
		sub.addProperty("item2", "foo");
		sub.addProperty("item3", "bar");
		sub.addProperty("date", System.currentTimeMillis());
		result.add("sub_values", sub);
		return result;
	}
	
	public void contextFromJson(JsonObject json_object) {
	}
	
}
