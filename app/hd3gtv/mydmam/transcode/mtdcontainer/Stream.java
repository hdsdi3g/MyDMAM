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

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.VideoConst.AudioSampling;
import hd3gtv.tools.VideoConst.Resolution;

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
	
	protected void internalDeserialize(FFprobeNode _item, JsonObject source, Gson gson) {// TODO correct
	}
	
	protected void internalSerialize(JsonObject jo, FFprobeNode _item, Gson gson) {// TODO correct
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
	
	/**
	 * @param input_pos can be 0 if ffmpeg will not open other streams.
	 * @return "[2:3]" => "[input_pos:index]"
	 */
	public String getMapReference(int input_pos) {
		return "[" + input_pos + ":" + internal.index + "]";
	}
	
	public Point getVideoResolution() {
		if (hasMultipleParams("width", "height")) {
			return new Point(getParam("width").getAsInt(), getParam("height").getAsInt());
		}
		return null;
	}
	
	public Resolution getStandardizedVideoResolution() {
		Point res = getVideoResolution();
		if (res == null) {
			return Resolution.OTHER;
		}
		return Resolution.getResolution(res.x, res.y);
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
	
	private transient int audio_channel_count = -1;
	
	public int getAudioChannelCount() {
		if (audio_channel_count > -1) {
			return audio_channel_count;
		}
		if (getCodec_type().equals("audio") == false) {
			return -1;
		}
		if (hasMultipleParams("channels") == false) {
			return 0;
		}
		audio_channel_count = getParam("channels").getAsInt();
		return audio_channel_count;
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
	
	public boolean isAttachedPic() {
		return getDisposition().attached_pic == 1;
	}
	
}
