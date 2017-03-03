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

package hd3gtv.mydmam.transcode.mtdcontainer;

import java.awt.Point;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonDeSerializer;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.tools.Timecode;
import hd3gtv.tools.VideoConst.Framerate;
import hd3gtv.tools.VideoConst.Resolution;

public class FFprobe extends EntryAnalyser {
	
	@GsonIgnore
	private ArrayList<FFProbeChapter> chapters;
	
	@GsonIgnore
	private ArrayList<FFProbeStream> streams;
	
	@GsonIgnore
	private FFProbeFormat format;
	
	public static final String ES_TYPE = "ffprobe";
	
	public String getES_Type() {
		return ES_TYPE;
	}
	
	public ArrayList<FFProbeChapter> getChapters() {
		return chapters;
	}
	
	public ArrayList<FFProbeStream> getStreams() {
		return streams;
	}
	
	public FFProbeFormat getFormat() {
		return format;
	}
	
	/**
	 * @param like audio or video
	 * @return null if there are not stream with codec_type. NEVER return a "video" stream who is an attached_pic.
	 */
	public List<FFProbeStream> getStreamsByCodecType(String codec_type) {
		return streams.stream().filter(s -> {
			return s.isAttachedPic() == false && s.getCodec_type().equalsIgnoreCase(codec_type);
		}).collect(Collectors.toList());
	}
	
	public FFProbeStream getAttachedPicStream() {
		Optional<FFProbeStream> o_result = streams.stream().filter(s -> {
			return s.isAttachedPic() == true;
		}).findFirst();
		
		if (o_result.isPresent()) {
			return o_result.get();
		}
		return null;
	}
	
	/**
	 * @return if the first video stream exists and is not ignored. Ignore "video" streams who is an attached_pic.
	 */
	public boolean hasVideo() {
		return streams.stream().anyMatch(s -> {
			return s.isAttachedPic() == false && s.getCodec_type().equalsIgnoreCase("video");
		});
	}
	
	/**
	 * @return if the first audio stream exists and is not ignored.
	 */
	public boolean hasAudio() {
		return streams.stream().anyMatch(s -> {
			return s.getCodec_type().equalsIgnoreCase("audio");
		});
	}
	
	public Timecode getDuration() {
		Framerate framerate = getFramerate();
		
		if (format.hasMultipleParams("duration")) {
			try {
				if (framerate == null) {
					return new Timecode(format.getParam("duration").getAsFloat(), 100f);
				} else {
					return new Timecode(format.getParam("duration").getAsFloat(), framerate.getNumericValue());
				}
			} catch (Exception e) {
				Loggers.Transcode_Metadata.error("Can't extract duration: " + format.getParam("duration").getAsString(), e);
			}
		}
		
		if (framerate == null) {
			return new Timecode("00:00:00:01", 100f);
		} else {
			return new Timecode("00:00:00:01", framerate.getNumericValue());
		}
	}
	
	/**
	 * @return the first valid value if there are more one video stream.
	 */
	public Framerate getFramerate() {
		if (hasVideo() == false) {
			return Framerate.OTHER;
		}
		
		FFProbeStream current_stream = getStreamsByCodecType("video").get(0);
		
		String avg_frame_rate = null;
		String r_frame_rate = null;
		int nb_frames = 0;
		float duration = 0f;
		
		/**
		 * "r_frame_rate" : "30000/1001", "30/1", "25/1",
		 */
		if (current_stream.hasMultipleParams("r_frame_rate")) {
			r_frame_rate = current_stream.getParam("r_frame_rate").getAsString();
		}
		
		/**
		 * "avg_frame_rate" : "30000/1001", "0/0", "25/1",
		 */
		if (current_stream.hasMultipleParams("avg_frame_rate")) {
			avg_frame_rate = current_stream.getParam("avg_frame_rate").getAsString();
		}
		
		/**
		 * "nb_frames" : "8487",
		 */
		if (current_stream.hasMultipleParams("nb_frames")) {
			try {
				nb_frames = current_stream.getParam("nb_frames").getAsInt();
			} catch (NumberFormatException e) {
			}
		}
		
		/**
		 * "duration" : "283.182900",
		 */
		if (current_stream.hasMultipleParams("duration")) {
			try {
				duration = current_stream.getParam("duration").getAsFloat();
			} catch (NumberFormatException e) {
			}
		}
		
		Framerate framerate = Framerate.getFramerate(r_frame_rate);
		if (framerate == null) {
			framerate = Framerate.getFramerate(avg_frame_rate);
		} else if (framerate == Framerate.OTHER) {
			if (Framerate.getFramerate(avg_frame_rate) != Framerate.OTHER) {
				framerate = Framerate.getFramerate(avg_frame_rate);
			}
		}
		if (((framerate == null) | (framerate == Framerate.OTHER)) & (nb_frames > 0) & (duration > 0f)) {
			framerate = Framerate.getFramerate((float) Math.round(((float) nb_frames / duration) * 10f) / 10f);
		}
		
		// String frame_rate_raw = (String) stream.get("r_frame_rate");
		// if (format_name.equalsIgnoreCase("mpegts")) {
		// frame_rate_raw = (String) stream.get("avg_frame_rate");
		// }
		
		return framerate;
	}
	
	/**
	 * @return for first video stream. Use streams[i].getVideoResolution() for specific stream.
	 */
	public Point getVideoResolution() {
		if (hasVideo() == false) {
			return null;
		}
		
		return getStreamsByCodecType("video").get(0).getVideoResolution();
	}
	
	public Resolution getStandardizedVideoResolution() {
		Point res = getVideoResolution();
		if (res == null) {
			return Resolution.OTHER;
		}
		return Resolution.getResolution(res.x, res.y);
	}
	
	public boolean hasVerticalBlankIntervalInImage() {
		Resolution res = getStandardizedVideoResolution();
		if (res == Resolution.SD_480_VBI) {
			return true;
		}
		if (res == Resolution.SD_576_VBI) {
			return true;
		}
		return false;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ffprobe: ");
		sb.append(MyDMAM.gson_kit.getGson().toJson(this));
		return sb.toString();
	}
	
	public static class Serializer implements GsonDeSerializer<FFprobe> {
		
		public JsonElement serialize(FFprobe src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject current_element = new JsonObject();
			current_element.add("chapters", MyDMAM.gson_kit.getGson().toJsonTree(src.chapters, GsonKit.type_ArrayList_Chapter));
			current_element.add("streams", MyDMAM.gson_kit.getGson().toJsonTree(src.streams, GsonKit.type_ArrayList_Stream));
			current_element.add("format", MyDMAM.gson_kit.getGson().toJsonTree(src.format, FFProbeFormat.class));
			return current_element;
		}
		
		public FFprobe deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject source = json.getAsJsonObject();
			
			FFprobe item = new FFprobe();
			if (source.has("chapters")) {
				item.chapters = MyDMAM.gson_kit.getGson().fromJson(source.get("chapters").getAsJsonArray(), GsonKit.type_ArrayList_Chapter);
			}
			if (item.chapters == null) {
				item.chapters = new ArrayList<FFProbeChapter>(1);
			}
			
			if (source.has("streams")) {
				item.streams = MyDMAM.gson_kit.getGson().fromJson(source.get("streams").getAsJsonArray(), GsonKit.type_ArrayList_Stream);
				for (int pos = item.streams.size() - 1; pos > -1; pos--) {
					if (item.streams.get(pos).getCodec_type() == null) {
						item.streams.remove(pos);
					} else if (item.streams.get(pos).getCodec_type().equals("data")) {
						item.streams.remove(pos);
					}
				}
			}
			if (item.streams == null) {
				item.streams = new ArrayList<FFProbeStream>(1);
			}
			if (source.has("format")) {
				item.format = MyDMAM.gson_kit.getGson().fromJson(source.get("format").getAsJsonObject(), FFProbeFormat.class);
			}
			if (item.format == null) {
				item.format = new FFProbeFormat();
			}
			return item;
		}
		
	}
}
