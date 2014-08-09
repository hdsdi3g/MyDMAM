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

public class Chapter extends FFprobeNode {
	
	private class Internal extends FFprobeNodeInternalItem {
		private int id;
		private String time_base;
		private long start;
		private float start_time;
		private long end;
		private float end_time;
	}
	
	private transient Internal internal;
	
	public long getEnd() {
		return internal.end;
	}
	
	public float getEnd_time() {
		return internal.end_time;
	}
	
	public int getId() {
		return internal.id;
	}
	
	public long getStart() {
		return internal.start;
	}
	
	public float getStart_time() {
		return internal.start_time;
	}
	
	public String getTime_base() {
		return internal.time_base;
	}
	
	protected FFprobeNode create() {
		return new Chapter();
	}
	
	protected FFprobeNodeInternalItem getInternalItem() {
		return internal;
	}
	
	protected Class<? extends FFprobeNodeInternalItem> getInternalItemClass() {
		return Internal.class;
	}
	
	protected String[] getAdditionnaries_keys_names_to_ignore_in_params() {
		return null;
	}
	
	protected void setInternalItem(FFprobeNodeInternalItem internal) {
		this.internal = (Internal) internal;
	}
	
	public Chapter() {
		internal = new Internal();
	}
	
	protected void internalDeserialize(FFprobeNode _item, JsonObject source, Gson gson) {
	}
	
	protected void internalSerialize(JsonObject jo, FFprobeNode _item, Gson gson) {
	}
	
}
