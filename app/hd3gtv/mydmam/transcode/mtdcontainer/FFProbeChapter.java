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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.gson.GsonDeSerializer;

public class FFProbeChapter extends FFprobeNode {
	
	public long getEnd() {
		return getParam("end").getAsLong();
	}
	
	public float getEnd_time() {
		return getParam("end_time").getAsFloat();
	}
	
	public int getId() {
		return getParam("id").getAsInt();
	}
	
	public long getStart() {
		return getParam("start").getAsLong();
	}
	
	public float getStart_time() {
		return getParam("start_time").getAsFloat();
	}
	
	public String getTime_base() {
		return getParam("time_base").getAsString();
	}
	
	public static class Serializer implements GsonDeSerializer<FFProbeChapter> {
		
		public JsonElement serialize(FFProbeChapter src, Type typeOfSrc, JsonSerializationContext context) {
			return src.node_content;
		}
		
		public FFProbeChapter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			FFProbeChapter item = new FFProbeChapter();
			item.node_content = json.getAsJsonObject();
			return item;
		}
		
	}
	
}
