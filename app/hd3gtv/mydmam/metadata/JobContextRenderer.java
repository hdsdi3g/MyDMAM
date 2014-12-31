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
package hd3gtv.mydmam.metadata;

import hd3gtv.mydmam.manager.JobContext;

import com.google.gson.JsonObject;

public abstract class JobContextRenderer extends JobContext {
	
	String origin_pathindex_key;
	
	public JobContextRenderer() {
	}
	
	public JsonObject contextToJson() {
		JsonObject json_object = new JsonObject();
		json_object.addProperty("origin", origin_pathindex_key);
		return json_object;
	}
	
	public void contextFromJson(JsonObject json_object) {
		origin_pathindex_key = json_object.get("origin").getAsString();
	}
	
}
