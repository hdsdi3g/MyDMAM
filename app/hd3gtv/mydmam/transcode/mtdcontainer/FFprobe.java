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

package hd3gtv.mydmam.transcode.mtdcontainer;

import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class FFprobe extends EntryAnalyser {
	
	private ArrayList<Chapter> chapters;
	private ArrayList<Stream> streams;
	private Format format;
	
	private static Type chapters_typeOfT = new TypeToken<ArrayList<Chapter>>() {
	}.getType();
	private static Type streams_typeOfT = new TypeToken<ArrayList<Stream>>() {
	}.getType();
	
	static Type json_map_typeOfT = new TypeToken<HashMap<String, JsonPrimitive>>() {
	}.getType();
	
	static HashMap<String, JsonPrimitive> getTagsFromSource(JsonObject source, Gson gson) {
		HashMap<String, JsonPrimitive> tags = null;
		if (source.has("tags")) {
			tags = gson.fromJson(source.get("tags"), json_map_typeOfT);
		} else {
			tags = new HashMap<String, JsonPrimitive>(1);
		}
		return tags;
	}
	
	static void pushTagsToJson(JsonObject destination, HashMap<String, JsonPrimitive> tags, Gson gson) {
		if (tags == null) {
			destination.add("tags", gson.toJsonTree(new HashMap<String, JsonPrimitive>(1), json_map_typeOfT));
		} else {
			destination.add("tags", gson.toJsonTree(tags, json_map_typeOfT));
		}
	}
	
	protected void extendedInternalDeserialize(EntryAnalyser _item, JsonObject source, Gson gson) {
		FFprobe item = (FFprobe) _item;
		if (source.has("chapters")) {
			item.chapters = gson.fromJson(source.get("chapters").getAsJsonArray(), chapters_typeOfT);
		}
		if (item.chapters == null) {
			item.chapters = new ArrayList<Chapter>(1);
		}
		
		if (source.has("streams")) {
			item.streams = gson.fromJson(source.get("streams").getAsJsonArray(), streams_typeOfT);
		}
		if (item.streams == null) {
			item.streams = new ArrayList<Stream>(1);
		}
		
		if (source.has("format")) {
			item.format = gson.fromJson(source.get("format").getAsJsonObject(), format.getClass().getGenericSuperclass());
		}
		if (item.format == null) {
			item.format = new Format();
		}
	}
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		FFprobe item = (FFprobe) _item;
		current_element.add("chapters", gson.toJsonTree(item.chapters, chapters_typeOfT));
		current_element.add("streams", gson.toJsonTree(item.streams, streams_typeOfT));
		current_element.add("format", gson.toJsonTree(item.format, format.getClass().getGenericSuperclass()));
	}
	
	public String getES_Type() {
		return "ffprobe";
	}
	
	protected FFprobe create() {
		return new FFprobe();
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		List<Class<? extends SelfSerializing>> list = new ArrayList<Class<? extends SelfSerializing>>(1);
		list.add(Chapter.class);
		list.add(Format.class);
		list.add(Stream.class);
		return list;
	}
}
