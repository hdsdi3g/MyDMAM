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
package hd3gtv.mydmam.transcode.watchfolder;

import hd3gtv.mydmam.manager.JobContext;

import com.google.gson.JsonObject;

public class JobContextWFDeleteSourceFile extends JobContext {
	
	String storage;
	String path;
	
	public JsonObject contextToJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("storage", storage);
		jo.addProperty("path", path);
		return null;
	}
	
	public void contextFromJson(JsonObject json_object) {
		storage = json_object.get("storage").getAsString();
		path = json_object.get("path").getAsString();
	}
	
}
