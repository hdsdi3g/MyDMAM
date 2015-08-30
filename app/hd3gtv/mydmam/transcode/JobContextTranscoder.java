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
package hd3gtv.mydmam.transcode;

import hd3gtv.mydmam.manager.JobContext;

import com.google.gson.JsonObject;

public class JobContextTranscoder extends JobContext {
	
	public String source_pathindex_key;
	public String dest_storage_name;
	
	// TODO add prefix/suffix for output file + recreate sub dir
	
	public JsonObject contextToJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("source_pathindex_key", source_pathindex_key);
		jo.addProperty("dest_storage_name", dest_storage_name);
		return jo;
	}
	
	public void contextFromJson(JsonObject json_object) {
		source_pathindex_key = json_object.get("source_pathindex_key").getAsString();
		dest_storage_name = json_object.get("dest_storage_name").getAsString();
	}
	
}
