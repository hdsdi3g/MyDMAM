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

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.gson.GsonDeSerializer;

public class FFProbeFormat extends FFprobeNode {
	
	public int getBit_rate() {
		return getParam("bit_rate").getAsInt();
	}
	
	public float getDuration() {
		return getParam("duration").getAsFloat();
	}
	
	public String getFilename() {
		return getParam("filename").getAsString();
	}
	
	public String getFormat_long_name() {
		return getParam("format_long_name").getAsString();
	}
	
	public String getFormat_name() {
		return getParam("format_name").getAsString();
	}
	
	public int getNb_streams() {
		return getParam("nb_streams").getAsInt();
	}
	
	public long getSize() {
		return getParam("size").getAsLong();
	}
	
	/**
	 * @return in kbits per sec or -1
	 */
	public float getBitrate() {
		try {
			return new Integer(getBit_rate()).floatValue() / 1000f;
		} catch (Exception e) {
			Loggers.Transcode_Metadata.error("Can't extract bitrate, raw: " + getParam("bitrate").getAsString(), e);
		}
		return -1;
	}
	
	public static class Serializer implements GsonDeSerializer<FFProbeFormat> {
		
		public JsonElement serialize(FFProbeFormat src, Type typeOfSrc, JsonSerializationContext context) {
			return src.node_content;
		}
		
		public FFProbeFormat deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			FFProbeFormat item = new FFProbeFormat();
			item.node_content = json.getAsJsonObject();
			return item;
		}
		
	}
	
}
