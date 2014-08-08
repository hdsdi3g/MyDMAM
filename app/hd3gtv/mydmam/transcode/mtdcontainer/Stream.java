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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.container.SelfSerializing;

import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Stream implements SelfSerializing {
	
	private Disposition disposition;
	private String codec_type;
	private String codec_tag;
	private String codec_tag_string;
	private int duration;
	private int index;
	private int bit_rate;
	
	private transient HashMap<String, JsonPrimitive> tags;
	private transient HashMap<String, JsonPrimitive> params;
	
	public SelfSerializing deserialize(JsonObject source, Gson gson) {
		Stream item = gson.fromJson(source, Stream.class.getGenericSuperclass());
		item.tags = FFprobe.getTagsFromSource(source, gson);
		
		item.params = new HashMap<String, JsonPrimitive>();
		for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
			if (entry.getKey().equals("disposition")) {
				continue;
			}
			if (entry.getKey().equals("tags")) {
				continue;
			}
			if (entry.getValue().isJsonPrimitive()) {
				item.params.put(entry.getKey(), entry.getValue().getAsJsonPrimitive());
			} else {
				Log2Dump dump = new Log2Dump();
				dump.add("key", entry.getKey());
				dump.add("value", entry.getValue().toString());
				dump.add("source", source.toString());
				Log2.log.debug("Item is not a primitive !", dump);
			}
		}
		return item;
	}
	
	public JsonObject serialize(SelfSerializing _item, Gson gson) {
		Stream item = (Stream) _item;
		JsonObject jo = gson.toJsonTree(item.params, FFprobe.json_map_typeOfT).getAsJsonObject();
		
		FFprobe.pushTagsToJson(jo, item.tags, gson);
		jo.add("disposition", gson.toJsonTree(item.disposition));
		
		return jo.getAsJsonObject();
	}
	
	public boolean hasMultipleParams(String... list_params) {
		for (int pos = 0; pos < list_params.length; pos++) {
			if (params.containsKey(list_params[pos]) == false) {
				return false;
			}
		}
		return true;
	}
	
	public Point getResolutionAsPoint() {
		if (hasMultipleParams("width", "height")) {
			return new Point(params.get("width").getAsInt(), params.get("height").getAsInt());
		}
		return null;
	}
	
	public HashMap<String, JsonPrimitive> getParams() {
		return params;
	}
	
	public Disposition getDisposition() {
		return disposition;
	}
	
	public HashMap<String, JsonPrimitive> getTags() {
		return tags;
	}
	
	public int getBit_rate() {
		return bit_rate;
	}
	
	public String getCodec_tag() {
		return codec_tag;
	}
	
	public String getCodec_tag_string() {
		return codec_tag_string;
	}
	
	public String getCodec_type() {
		return codec_type;
	}
	
	public int getDuration() {
		return duration;
	}
	
	public int getIndex() {
		return index;
	}
	
}
