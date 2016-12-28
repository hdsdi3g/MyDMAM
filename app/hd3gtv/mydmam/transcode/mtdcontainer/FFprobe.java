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
import java.util.HashMap;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.SelfSerializing;
import hd3gtv.tools.Timecode;
import hd3gtv.tools.VideoConst.Framerate;
import hd3gtv.tools.VideoConst.Resolution;

public class FFprobe extends EntryAnalyser {
	
	private ArrayList<Chapter> chapters;
	private ArrayList<Stream> streams;
	private Format format;
	
	private static Type chapters_typeOfT = new TypeToken<ArrayList<Chapter>>() {
	}.getType();
	private static Type streams_typeOfT = new TypeToken<ArrayList<Stream>>() {
	}.getType();
	
	protected EntryAnalyser internalDeserialize(JsonObject source, Gson gson) {
		FFprobe item = new FFprobe();
		if (source.has("chapters")) {
			item.chapters = gson.fromJson(source.get("chapters").getAsJsonArray(), chapters_typeOfT);
		}
		if (item.chapters == null) {
			item.chapters = new ArrayList<Chapter>(1);
		}
		
		if (source.has("streams")) {
			item.streams = gson.fromJson(source.get("streams").getAsJsonArray(), streams_typeOfT);
			for (int pos = item.streams.size() - 1; pos > -1; pos--) {
				if (item.streams.get(pos).getCodec_type() == null) {
					item.streams.remove(pos);
				} else if (item.streams.get(pos).getCodec_type().equals("data")) {
					item.streams.remove(pos);
				}
			}
		}
		if (item.streams == null) {
			item.streams = new ArrayList<Stream>(1);
		}
		if (source.has("format")) {
			item.format = gson.fromJson(source.get("format").getAsJsonObject(), Format.class);
		}
		if (item.format == null) {
			item.format = new Format();
		}
		return item;
	}
	
	protected void extendedInternalSerializer(JsonObject current_element, EntryAnalyser _item, Gson gson) {
		FFprobe item = (FFprobe) _item;
		current_element.add("chapters", gson.toJsonTree(item.chapters, chapters_typeOfT));
		current_element.add("streams", gson.toJsonTree(item.streams, streams_typeOfT));
		current_element.add("format", gson.toJsonTree(item.format, Format.class));
	}
	
	public String getES_Type() {
		return "ffprobe";
	}
	
	protected List<Class<? extends SelfSerializing>> getSerializationDependencies() {
		List<Class<? extends SelfSerializing>> list = new ArrayList<Class<? extends SelfSerializing>>(1);
		list.add(Format.class);
		list.add(Stream.class);
		list.add(Chapter.class);
		return list;
	}
	
	/*
	 * End of serialization functions
	 * */
	
	public ArrayList<Chapter> getChapters() {
		return chapters;
	}
	
	public ArrayList<Stream> getStreams() {
		return streams;
	}
	
	public Format getFormat() {
		return format;
	}
	
	private class Cache {
		HashMap<String, List<Stream>> stream_by_codec_type;
		Timecode duration;
		Framerate framerate;
		
		Cache() {
			/**
			 * Streams lists by codec type
			 */
			stream_by_codec_type = new HashMap<String, List<Stream>>(2);
			Stream current_stream;
			String current_codec_type;
			for (int pos = 0; pos < streams.size(); pos++) {
				current_stream = streams.get(pos);
				current_codec_type = current_stream.getCodec_type();
				if (stream_by_codec_type.containsKey(current_codec_type) == false) {
					stream_by_codec_type.put(current_codec_type, new ArrayList<Stream>(1));
				}
				stream_by_codec_type.get(current_codec_type).add(current_stream);
			}
			
			/**
			 * Framerate
			 */
			if (stream_by_codec_type.containsKey("video")) {
				current_stream = stream_by_codec_type.get("video").get(0);
				
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
				
				/** "nb_frames" : "8487", */
				if (current_stream.hasMultipleParams("nb_frames")) {
					try {
						nb_frames = current_stream.getParam("nb_frames").getAsInt();
					} catch (NumberFormatException e) {
					}
				}
				
				/** "duration" : "283.182900", */
				if (current_stream.hasMultipleParams("duration")) {
					try {
						duration = current_stream.getParam("duration").getAsFloat();
					} catch (NumberFormatException e) {
					}
				}
				
				framerate = Framerate.getFramerate(r_frame_rate);
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
				
				/*String frame_rate_raw = (String) stream.get("r_frame_rate");
				if (format_name.equalsIgnoreCase("mpegts")) {
					frame_rate_raw = (String) stream.get("avg_frame_rate");
				}*/
			}
			
			if (format.hasMultipleParams("duration")) {
				try {
					if (framerate == null) {
						duration = new Timecode(format.getParam("duration").getAsFloat(), 100f);
					} else {
						duration = new Timecode(format.getParam("duration").getAsFloat(), framerate.getNumericValue());
					}
				} catch (Exception e) {
					Loggers.Transcode_Metadata.error("Can't extract duration: " + format.getParam("duration").getAsString(), e);
				}
			} else {
				duration = new Timecode("00:00:00:01", framerate.getNumericValue());
			}
			
		}
	}
	
	private transient Cache cache;
	
	/**
	 * @param like audio or video
	 * @return null if there are not stream with codec_type.
	 */
	public List<Stream> getStreamsByCodecType(String codec_type) {
		if (cache == null) {
			cache = new Cache();
		}
		if (cache.stream_by_codec_type.containsKey(codec_type)) {
			return cache.stream_by_codec_type.get(codec_type);
		}
		return null;
	}
	
	/**
	 * @return if the first video stream exists and is not ignored.
	 */
	public boolean hasVideo() {
		if (cache == null) {
			cache = new Cache();
		}
		if (cache.stream_by_codec_type.containsKey("video") == false) {
			return false;
		}
		if (cache.stream_by_codec_type.get("video").get(0).isIgnored()) {
			return false;
		}
		return true;
	}
	
	/**
	 * @return if the first audio stream exists and is not ignored.
	 */
	public boolean hasAudio() {
		if (cache == null) {
			cache = new Cache();
		}
		if (cache.stream_by_codec_type.containsKey("audio") == false) {
			return false;
		}
		if (cache.stream_by_codec_type.get("audio").get(0).isIgnored()) {
			return false;
		}
		return true;
	}
	
	public Timecode getDuration() {
		if (cache == null) {
			cache = new Cache();
		}
		return cache.duration;
	}
	
	/**
	 * @return the first valid value if there are more one video stream.
	 */
	public Framerate getFramerate() {
		if (cache == null) {
			cache = new Cache();
		}
		if (cache.framerate == null) {
			return Framerate.OTHER;
		}
		return cache.framerate;
	}
	
	/**
	 * @return for first video stream. Use streams[i].getVideoResolution() for specific stream.
	 */
	public Point getVideoResolution() {
		if (cache == null) {
			cache = new Cache();
		}
		if (cache.stream_by_codec_type.containsKey("video")) {
			return getStreamsByCodecType("video").get(0).getVideoResolution();
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
		sb.append(ContainerOperations.getGson().toJson(this));
		return sb.toString();
	}
	
}
