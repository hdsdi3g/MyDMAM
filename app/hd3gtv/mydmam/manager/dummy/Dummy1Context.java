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
package hd3gtv.mydmam.manager.dummy;

import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.JobContext;

import java.util.List;

import com.google.gson.JsonObject;

public class Dummy1Context implements JobContext {
	
	long sleep = 100;
	
	public JsonObject contextToJson() {
		return AppManager.getGson().toJsonTree(this).getAsJsonObject();
	}
	
	public void contextFromJson(JsonObject json_object) {
		Dummy1Context context = AppManager.getGson().fromJson(json_object, Dummy1Context.class);
		this.sleep = context.sleep;
	}
	
	public List<String> getNeededIndexedStoragesNames() {
		return null;
	}
	
}
