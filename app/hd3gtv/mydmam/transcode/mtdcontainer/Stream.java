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

import java.awt.Point;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class Stream extends FFprobeNode {
	
	private class Internal extends FFprobeNodeInternalItem {
		private Disposition disposition;
		private String codec_type;
		private String codec_tag;
		private String codec_tag_string;
		private float duration;
		private int index;
		private int bit_rate;
	}
	
	private transient Internal internal;
	
	public Point getResolutionAsPoint() {
		if (hasMultipleParams("width", "height")) {
			return new Point(getParam("width").getAsInt(), getParam("height").getAsInt());
		}
		return null;
	}
	
	public Disposition getDisposition() {
		return internal.disposition;
	}
	
	public int getBit_rate() {
		return internal.bit_rate;
	}
	
	public String getCodec_tag() {
		return internal.codec_tag;
	}
	
	public String getCodec_tag_string() {
		return internal.codec_tag_string;
	}
	
	public String getCodec_type() {
		return internal.codec_type;
	}
	
	public float getDuration() {
		return internal.duration;
	}
	
	public int getIndex() {
		return internal.index;
	}
	
	public Stream() {
		internal = new Internal();
	}
	
	protected FFprobeNodeInternalItem getInternalItem() {
		return internal;
	}
	
	protected void setInternalItem(FFprobeNodeInternalItem internal) {
		this.internal = (Internal) internal;
	}
	
	protected FFprobeNode create() {
		return new Stream();
	}
	
	protected Class<? extends FFprobeNodeInternalItem> getInternalItemClass() {
		return Internal.class;
	}
	
	protected void internalDeserialize(FFprobeNode _item, JsonObject source, Gson gson) {
	}
	
	protected void internalSerialize(JsonObject jo, FFprobeNode _item, Gson gson) {
		Stream item = (Stream) _item;
		jo.add("disposition", gson.toJsonTree(item.internal.disposition));
	}
	
	protected String[] getAdditionnaries_keys_names_to_ignore_in_params() {
		return new String[] { "disposition" };
	}
	
}
