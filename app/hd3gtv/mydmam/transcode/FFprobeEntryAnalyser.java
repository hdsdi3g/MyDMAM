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

package hd3gtv.mydmam.transcode;

import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.EntrySerialiserBridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class FFprobeEntryAnalyser extends EntryAnalyser {
	
	public static final String type = "ffprobe";
	
	public String getES_Type() {
		return type;
	}
	
	protected FFprobeEntryAnalyser create() {
		return new FFprobeEntryAnalyser();
	}
	
	public EntrySerialiserBridge<?> createEntrySerialiserBridge() {
		return new EntrySerialiserBridge<FFprobeEntryAnalyser>(this);
	}
	
	/*		EntrySummary entry = (EntrySummary) _entry;
		for (Map.Entry<String, JsonElement> item : source.entrySet()) {
			if (item.getKey().equals("previews")) {
				Type typeOfT = new TypeToken<HashMap<String, Preview>>() {
				}.getType();
				entry.previews = gson.fromJson(item.getValue().getAsJsonObject(), typeOfT);
			} else if (item.getKey().equals("mimetype")) {
				entry.mimetype = item.getValue().getAsString();
			} else if (item.getKey().equals("master_as_preview")) {
				entry.master_as_preview = item.getValue().getAsBoolean();
			} else {
				if (entry.summaries == null) {
					entry.summaries = new HashMap<String, String>();
				}
				entry.summaries.put(item.getKey(), item.getValue().getAsString());
			}
		}
	}
	
	protected JsonObject internalSerialize(Entry _item, Gson gson) {
		EntrySummary src = (EntrySummary) _item;
		JsonObject jo = new JsonObject();
		
		if (src.previews != null) {
			jo.add("previews", gson.toJsonTree(src.previews));
		}
		
		jo.addProperty("mimetype", src.mimetype);
		jo.addProperty("master_as_preview", src.master_as_preview);
		if (src.summaries != null) {
			for (Map.Entry<String, String> entry : src.summaries.entrySet()) {
				jo.addProperty(entry.getKey(), entry.getValue());
			}
		}
		return jo;
	*/
	
	@Override
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		// TODO Auto-generated method stub
	}
	
	@Override
	protected void extendedInternalDeserialize(EntryAnalyser _item, JsonObject source, Gson gson) {
		// TODO Auto-generated method stub
		
	}
	
}
