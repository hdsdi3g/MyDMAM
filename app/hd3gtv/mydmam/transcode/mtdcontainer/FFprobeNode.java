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

import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

abstract class FFprobeNode {
	
	protected abstract FFprobeNodeInternalItem getInternalItem();
	
	protected abstract void setInternalItem(FFprobeNodeInternalItem internal);
	
	/**
	 * Don't forget to create Internal item
	 */
	protected abstract FFprobeNode create();
	
	protected abstract void internalDeserialize(FFprobeNode _item, JsonObject source, Gson gson);
	
	protected abstract void internalSerialize(JsonObject jo, FFprobeNode _item, Gson gson);
	
	private transient HashMap<String, JsonPrimitive> params;
	
	protected abstract String[] getAdditionnaries_keys_names_to_ignore_in_params();
	
	protected abstract Class<? extends FFprobeNodeInternalItem> getInternalItemClass();
	
	/*public final SelfSerializing deserialize(JsonObject source, Gson gson) {//TODO correct this shit
		FFprobeNode item = create();
		FFprobeNodeInternalItem internal = gson.fromJson(source, getInternalItemClass());
		if (source.has("tags")) {
			internal.tags = source.get("tags").getAsJsonObject();
			source.remove("tags");
		} else {
			internal.tags = new JsonObject();
		}
		item.setInternalItem(internal);
		
		String[] ignore = getAdditionnaries_keys_names_to_ignore_in_params();
		if (ignore != null) {
			for (int pos = 0; pos < ignore.length; pos++) {
				if (source.has(ignore[pos])) {
					source.remove(ignore[pos]);
				}
			}
		}
		
		item.params = new HashMap<String, JsonPrimitive>();
		for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
			if (entry.getValue().isJsonPrimitive()) {
				item.params.put(entry.getKey(), entry.getValue().getAsJsonPrimitive());
			} else {
				Loggers.Transcode_Metadata.debug("Item is not a primitive ! " + "key: " + entry.getKey() + ", value: " + entry.getValue().toString() + ", source: " + source.toString());
			}
		}
		return item;
	}
	
	public final JsonObject serialize(SelfSerializing _item, Gson gson) {
		FFprobeNode item = (FFprobeNode) _item;
		JsonObject jo = new JsonObject();
		for (Map.Entry<String, JsonPrimitive> entry : item.params.entrySet()) {
			jo.add(entry.getKey(), entry.getValue());
		}
		FFprobeNodeInternalItem internal = item.getInternalItem();
		internalSerialize(jo, item, gson);
		jo.add("tags", internal.tags);
		return jo;
	}*/
	
	public final boolean hasMultipleParams(String... list_params) {
		for (int pos = 0; pos < list_params.length; pos++) {
			if (params.containsKey(list_params[pos]) == false) {
				return false;
			}
		}
		return true;
	}
	
	protected final FFprobeNode putParam(String param_name, JsonPrimitive param_value) {
		params.put(param_name, param_value);
		return this;
	}
	
	public final JsonPrimitive getParam(String param_name) {
		if (params.containsKey(param_name) == false) {
			return null;
		}
		return params.get(param_name);
	}
	
	public final JsonObject getTags() {
		return getInternalItem().tags;
	}
}
