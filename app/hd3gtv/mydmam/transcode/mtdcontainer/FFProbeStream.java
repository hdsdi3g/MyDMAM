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
import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonDeSerializer;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.tools.VideoConst.AudioSampling;
import hd3gtv.tools.VideoConst.Resolution;

public class FFProbeStream extends FFprobeNode {
	
	@GsonIgnore
	private FFProbeStreamDisposition disposition;
	
	public static class Serializer implements GsonDeSerializer<FFProbeStream> {
		
		public JsonElement serialize(FFProbeStream src, Type typeOfSrc, JsonSerializationContext context) {
			return src.node_content;
		}
		
		public FFProbeStream deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			FFProbeStream item = new FFProbeStream();
			item.node_content = json.getAsJsonObject();
			item.disposition = MyDMAM.gson_kit.getGsonSimple().fromJson(item.node_content.get("disposition"), FFProbeStreamDisposition.class);
			return item;
		}
		
	}
	
	public FFProbeStreamDisposition getDisposition() {
		return disposition;
	}
	
	public int getBit_rate() {
		return getParam("bit_rate").getAsInt();
	}
	
	public String getCodec_tag() {
		return getParam("codec_tag").getAsString();
	}
	
	public String getCodec_tag_string() {
		return getParam("codec_tag_string").getAsString();
	}
	
	public String getCodec_type() {
		return getParam("codec_type").getAsString();
	}
	
	public float getDuration() {
		return getParam("duration").getAsFloat();
	}
	
	public int getIndex() {
		return getParam("index").getAsInt();
	}
	
	/**
	 * @param input_pos can be 0 if ffmpeg will not open other streams.
	 * @return "[2:3]" => "[input_pos:index]"
	 */
	public String getMapReference(int input_pos) {
		return "[" + input_pos + ":" + getIndex() + "]";
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
