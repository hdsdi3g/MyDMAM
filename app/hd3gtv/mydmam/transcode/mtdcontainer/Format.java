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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Format extends FFprobeNode {
	
	private class Internal extends FFprobeNodeInternalItem {
		private String filename;
		private int nb_streams;
		private String format_name;
		private String format_long_name;
		private float duration;
		private int bit_rate;
		private long size;
	}
	
	private transient Internal internal;
	
	/*public SelfSerializing deserialize(JsonObject source, Gson gson) {
		Format item = new Format();
		item.internal = gson.fromJson(source, Internal.class);
		if (source.has("tags")) {
			item.internal.tags = source.get("tags").getAsJsonObject();
		} else {
			item.internal.tags = new JsonObject();
		}
		
		item.params = new HashMap<String, JsonPrimitive>();
		for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
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
		Format item = (Format) _item;
		JsonObject jo = new JsonObject();
		for (Map.Entry<String, JsonPrimitive> entry : item.params.entrySet()) {
			jo.add(entry.getKey(), entry.getValue());
		}
		jo.add("tags", item.internal.tags);
		return jo;
	}*/
	
	public int getBit_rate() {
		return internal.bit_rate;
	}
	
	public float getDuration() {
		return internal.duration;
	}
	
	public String getFilename() {
		return internal.filename;
	}
	
	public String getFormat_long_name() {
		return internal.format_long_name;
	}
	
	public String getFormat_name() {
		return internal.format_name;
	}
	
	public int getNb_streams() {
		return internal.nb_streams;
	}
	
	public long getSize() {
		return internal.size;
	}
	
	public Format() {
		internal = new Internal();
	}
	
	protected FFprobeNodeInternalItem getInternalItem() {
		return internal;
	}
	
	protected void setInternalItem(FFprobeNodeInternalItem internal) {
		this.internal = (Internal) internal;
	}
	
	protected FFprobeNode create() {
		return new Format();
	}
	
	protected Class<? extends FFprobeNodeInternalItem> getInternalItemClass() {
		return Internal.class;
	}
	
	protected void internalDeserialize(FFprobeNode _item, JsonObject source, Gson gson) {
	}
	
	protected void internalSerialize(JsonObject jo, FFprobeNode _item, Gson gson) {
	}
	
	protected String[] getAdditionnaries_keys_names_to_ignore_in_params() {
		return null;
	}
	
}
