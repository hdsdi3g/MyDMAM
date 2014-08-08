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

public class Format implements SelfSerializing {
	
	private String filename;
	private int nb_streams;
	private String format_name;
	private String format_long_name;
	private float duration;
	private int bit_rate;
	private long size;
	
	private transient HashMap<String, JsonPrimitive> tags;
	private transient HashMap<String, JsonPrimitive> params;
	
	@Override
	public SelfSerializing deserialize(JsonObject source, Gson gson) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public JsonObject serialize(SelfSerializing _item, Gson gson) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public HashMap<String, JsonPrimitive> getParams() {
		return params;
	}
	
	public HashMap<String, JsonPrimitive> getTags() {
		return tags;
	}
	
	public int getBit_rate() {
		return bit_rate;
	}
	
	public float getDuration() {
		return duration;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public String getFormat_long_name() {
		return format_long_name;
	}
	
	public String getFormat_name() {
		return format_name;
	}
	
	public int getNb_streams() {
		return nb_streams;
	}
	
	public long getSize() {
		return size;
	}
	
}
