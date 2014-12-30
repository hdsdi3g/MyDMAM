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
package hd3gtv.mydmam.pathindexing;

import hd3gtv.mydmam.manager.JobContext;

import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

public class JobContextPathScan implements JobContext {
	
	private String storage_index_name;
	
	public JobContextPathScan(String storage_index_name) {
		this.storage_index_name = storage_index_name;
	}
	
	/**
	 * Only for (de)serializations needs.
	 */
	public JobContextPathScan() {
	}
	
	public String getStorageIndexName() {
		return storage_index_name;
	}
	
	public JsonObject contextToJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("storage_index_name", storage_index_name);
		return jo;
	}
	
	public void contextFromJson(JsonObject json_object) {
		storage_index_name = json_object.get("storage_index_name").getAsString();
	}
	
	public List<String> getNeededIndexedStoragesNames() {
		return Arrays.asList(storage_index_name);
	}
	
}
