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

import java.util.ArrayList;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.manager.JobContext;

public class JobContextWFDeleteSourceFile extends JobContext {
	
	String storage;
	String path;
	
	/**
	 * Remove all JobsKeys and the WF entry.
	 */
	boolean clean_after_done;
	
	/**
	 * List of pathindex keys
	 */
	ArrayList<String> send_to;
	
	public JsonObject contextToJson() {
		JsonObject jo = new JsonObject();
		jo.addProperty("storage", storage);
		jo.addProperty("path", path);
		jo.addProperty("clean_after_done", clean_after_done);
		jo.add("send_to", MyDMAM.gson_kit.getGsonSimple().toJsonTree(send_to, GsonKit.type_ArrayList_String));
		return jo;
	}
	
	public void contextFromJson(JsonObject json_object) {
		storage = json_object.get("storage").getAsString();
		path = json_object.get("path").getAsString();
		
		if (json_object.has("clean_after_done")) {
			clean_after_done = json_object.get("clean_after_done").getAsBoolean();
		} else {
			clean_after_done = false;
		}
		
		send_to = MyDMAM.gson_kit.getGsonSimple().fromJson(json_object.get("send_to"), GsonKit.type_ArrayList_String);
	}
	
}
