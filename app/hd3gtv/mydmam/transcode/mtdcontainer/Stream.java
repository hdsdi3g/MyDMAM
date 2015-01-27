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
import hd3gtv.tools.VideoConst.AudioSampling;

import java.awt.Point;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class Stream extends FFprobeNode {
	
	public Stream() {
		internal = new Internal();
	}
	
	private class Internal extends FFprobeNodeInternalItem {
		private Disposition disposition;
		private String codec_type;
		private String codec_tag;
		private String codec_tag_string;
		private float duration;
		private int index;
		private int bit_rate;
		private boolean ignore_stream;
	}
	
	private transient Internal internal;
	
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
	
	public boolean isIgnored() {
		return internal.ignore_stream;
	}
	
	public void setIgnored(boolean ignore_stream) {
		internal.ignore_stream = ignore_stream;
		putParam("ignore_stream", new JsonPrimitive(ignore_stream));
	}
	
	public Point getVideoResolution() {
		if (hasMultipleParams("width", "height")) {
			return new Point(getParam("width").getAsInt(), getParam("height").getAsInt());
		}
		return null;
	}
	
	private transient AudioSampling cache_audiosampling;
	
	/**
	 * @return the first valid value if there are more one audio stream.
	 */
	public AudioSampling getAudioSampling() {
		if (getCodec_type().equals("audio") == false) {
			return null;
		}
		if (hasMultipleParams("sample_rate") == false) {
			return null;
		}
		if (cache_audiosampling == null) {
			cache_audiosampling = AudioSampling.parseAS(getParam("sample_rate").getAsString());
		}
		return cache_audiosampling;
	}
	
	/**
	 * @return in kbits per sec or -1
	 */
	public float getBitrate() {
		try {
			return new Integer(getBit_rate()).floatValue() / 1000f;
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump("raw bitrate", getParam("bitrate").getAsString());
			Log2.log.error("Can't extract bitrate", e, dump);
		}
		return -1;
	}
	
	public boolean isAValidVideoStreamOrAlbumArtwork() {
		if (getCodec_type().equals("video") == false) {
			return false;
		}
		if (hasMultipleParams("level") == false) {
			/**
			 * No level ? It's a real video stream.
			 */
			return true;
		}
		if (getParam("level").getAsInt() >= 0l) {
			/**
			 * level > 0 : video stream is a real video stream.
			 */
			return true;
		}
		if (getCodec_tag().equals("0x0000") == false) {
			/**
			 * codec_tag != 0x0000: this stream is correct.
			 */
			return true;
		}
		if (getParam("codec_name").getAsString().equals("mjpeg")) {
			/**
			 * MJPEG, but not a valid codec_tag ? this stream is an album artwork.
			 */
			return false;
		}
		if (getParam("codec_name").getAsString().equals("png")) {
			/**
			 * png, but not a valid codec_tag ? this stream is an album artwork.
			 */
			return false;
		}
		return true;
	}
}
