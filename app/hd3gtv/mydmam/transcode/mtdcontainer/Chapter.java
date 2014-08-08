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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.transcode.mtdcontainer;

import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Chapter implements SelfSerializing {
	
	private int id;
	private String time_base;
	private long start;
	private float start_time;
	private long end;
	private float end_time;
	private transient HashMap<String, JsonPrimitive> tags;
	
	public SelfSerializing deserialize(JsonObject source, Gson gson) {
		Chapter item = gson.fromJson(source, Chapter.class.getGenericSuperclass());
		item.tags = FFprobe.getTagsFromSource(source, gson);
		return item;
	}
	
	public JsonObject serialize(SelfSerializing _item, Gson gson) {
		Chapter item = (Chapter) _item;
		JsonObject jo = gson.toJsonTree(item, Chapter.class.getGenericSuperclass()).getAsJsonObject();
		FFprobe.pushTagsToJson(jo, item.tags, gson);
		return jo.getAsJsonObject();
	}
	
	public long getEnd() {
		return end;
	}
	
	public float getEnd_time() {
		return end_time;
	}
	
	public int getId() {
		return id;
	}
	
	public long getStart() {
		return start;
	}
	
	public float getStart_time() {
		return start_time;
	}
	
	public HashMap<String, JsonPrimitive> getTags() {
		return tags;
	}
	
	public String getTime_base() {
		return time_base;
	}
	
}
