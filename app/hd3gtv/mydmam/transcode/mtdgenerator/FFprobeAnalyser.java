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
package hd3gtv.mydmam.transcode.mtdgenerator;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.ContainerEntryResult;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerEntry;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.validation.Comparator;
import hd3gtv.mydmam.metadata.validation.ValidatorCenter;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.mtdcontainer.Stream;
import hd3gtv.tools.ExecBinaryPath;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.Timecode;
import hd3gtv.tools.VideoConst;

public class FFprobeAnalyser implements MetadataExtractor {
	
	public boolean isEnabled() {
		try {
			ExecBinaryPath.get("ffprobe");
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	public ContainerEntryResult processFast(Container container) throws Exception {
		return processFull(container);
	}
	
	public ContainerEntryResult processFull(Container container) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		param.add("-show_streams");
		param.add("-show_format");
		param.add("-show_chapters");
		param.add("-print_format");
		param.add("json");
		param.add("-i");
		param.add(container.getPhysicalSource().getPath());
		
		ExecprocessGettext process = new ExecprocessGettext(ExecBinaryPath.get("ffprobe"), param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				if (process.getRunprocess().getExitvalue() == 1) {
					Loggers.Transcode_Metadata.error("Invalid data found when processing input, " + process + ", " + container);
				} else {
					Loggers.Transcode_Metadata.error("Problem with ffprobe, " + process + ", " + container);
				}
			}
			throw e;
		}
		
		FFprobe result = ContainerOperations.getGson().fromJson(process.getResultstdout().toString(), FFprobe.class);
		
		/**
		 * Patch mime code if no video stream
		 */
		
		if (result.hasVideo()) {
			/**
			 * Video is present and valid
			 */
			List<Stream> video_streams = result.getStreamsByCodecType("video");
			for (int pos = 0; pos < video_streams.size(); pos++) {
				if (video_streams.get(pos).isAValidVideoStreamOrAlbumArtwork()) {
					continue;
				} else {
					video_streams.get(pos).setIgnored(true);
					if (container.getSummary().getMimetype().startsWith("video")) {
						/**
						 * Need to correct bad mime category
						 */
						container.getSummary().setMimetype("audio" + container.getSummary().getMimetype().substring(5));
					}
				}
			}
		} else if (container.getSummary().getMimetype().startsWith("video")) {
			/**
			 * No video, only audio is present but with bad mime category
			 */
			container.getSummary().setMimetype("audio" + container.getSummary().getMimetype().substring(5));
		}
		
		/**
		 * Patch tags dates
		 */
		List<Stream> streams = result.getStreams();
		for (int pos = 0; pos < streams.size(); pos++) {
			patchTagDate(streams.get(pos).getTags());
		}
		patchTagDate(result.getFormat().getTags());
		
		/**
		 * Compute a summary like:
		 * "Video: DV SD PAL, Audio: PCM 16b (stereo 48.0kHz 1536kbps), Dur: 00:00:08:00 @ 28,80 Mbps", "Video: MPEG2 SD PAL, Audio: PCM 16b (mono 48.0kHz 768kbps), x2",
		 * "Audio: EAC3 (3ch 32.0kHz 384kbps), Dur: 00:00:05:00 @ 384,00 kbps"
		 */
		StringBuffer sb_summary;
		List<String> streams_list_summary = new ArrayList<String>(1);
		Stream stream;
		for (int pos = 0; pos < streams.size(); pos++) {
			sb_summary = new StringBuffer();
			stream = streams.get(pos);
			if (stream.isIgnored()) {
				continue;
			}
			String codec_name = stream.getCodec_tag_string();
			if (codec_name.indexOf("[") > -1) {
				if (stream.hasMultipleParams("codec_name")) {
					codec_name = stream.getParam("codec_name").getAsString();
				} else {
					codec_name = "Unsupported";
				}
			}
			String codec_type = stream.getCodec_type();
			if (codec_type.equalsIgnoreCase("video")) {
				sb_summary.append("Video: ");
				sb_summary.append(translateCodecName(codec_name));
				if (stream.getVideoResolution() != null) {
					sb_summary.append(" ");
					sb_summary.append(VideoConst.getSystemSummary(stream.getVideoResolution().x, stream.getVideoResolution().y, result.getFramerate()));
				}
			} else if (codec_type.equalsIgnoreCase("audio")) {
				sb_summary.append("Audio: ");
				if (codec_name.equalsIgnoreCase("twos") | codec_name.equalsIgnoreCase("sowt")) {
					codec_name = stream.getParam("codec_name").getAsString();
				}
				sb_summary.append(translateCodecName(codec_name));
				
				sb_summary.append(" (");
				if (stream.hasMultipleParams("channels")) {
					sb_summary.append(VideoConst.audioChannelCounttoString(stream.getParam("channels").getAsInt()));
				}
				if (stream.hasMultipleParams("sample_rate")) {
					sb_summary.append(" ");
					sb_summary.append(new Integer(stream.getParam("sample_rate").getAsInt()).floatValue() / 1000f);
					sb_summary.append("kHz");
				}
				if (stream.hasMultipleParams("bit_rate")) {
					sb_summary.append(" ");
					sb_summary.append(new Integer(stream.getParam("bit_rate").getAsInt()).floatValue() / 1000f);
					sb_summary.append("kbps");
				}
				sb_summary.append(")");
				
			}
			streams_list_summary.add(sb_summary.toString());
		}
		sb_summary = new StringBuffer();
		
		for (int pos = 0; pos < streams_list_summary.size(); pos++) {
			if (streams_list_summary.get(pos).startsWith("Video:")) {
				sb_summary.append(streams_list_summary.get(pos));
				sb_summary.append(", ");
			}
		}
		
		/**
		 * Do not repeat if there is more that 1 identical stream, like 4 mono channels.
		 */
		String last_stream = "";
		int count_last_stream = 0;
		for (int pos = 0; pos < streams_list_summary.size(); pos++) {
			if (streams_list_summary.get(pos).startsWith("Audio:") == false) {
				continue;
			}
			if (streams_list_summary.get(pos).equals(last_stream)) {
				count_last_stream++;
			} else {
				if (count_last_stream > 0) {
					sb_summary.append("x");
					sb_summary.append(count_last_stream + 1);
					sb_summary.append(", ");
				} else {
					sb_summary.append(streams_list_summary.get(pos));
					sb_summary.append(", ");
					last_stream = streams_list_summary.get(pos);
				}
				count_last_stream = 0;
			}
		}
		if (count_last_stream > 0) {
			sb_summary.append("x");
			sb_summary.append(count_last_stream + 1);
			sb_summary.append(", ");
		}
		if (sb_summary.toString().endsWith(", ") == false) {
			sb_summary.append(", ");
		}
		
		Timecode tc = result.getDuration();
		if (tc != null) {
			sb_summary.append("Dur: ");
			sb_summary.append(tc.toString());
			sb_summary.append(" ");
		}
		
		sb_summary.append("@ ");
		int bit_rate = result.getFormat().getBit_rate();
		if (bit_rate < 1000) {
			sb_summary.append(bit_rate);
			sb_summary.append(" bps");
		} else {
			/**
			 * like "46,61 Mbps"
			 */
			int exp = (int) (Math.log(bit_rate) / Math.log(1000));
			sb_summary.append(String.format("%.2f", bit_rate / Math.pow(1000, exp)));
			sb_summary.append(" ");
			sb_summary.append(("kMGTPE").charAt(exp - 1));
			sb_summary.append("bps");
			sb_summary.append(" ");
		}
		
		/**
		 * Store computed summary.
		 */
		if (sb_summary.toString().trim().endsWith(",")) {
			container.getSummary().putSummaryContent(result, sb_summary.toString().trim().substring(0, sb_summary.toString().trim().length() - 1));
		} else {
			container.getSummary().putSummaryContent(result, sb_summary.toString().trim());
		}
		
		return new ContainerEntryResult(result);
	}
	
	private static void patchTagDate(JsonObject tags) {
		if (tags == null) {
			return;
		}
		String key;
		String value;
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		HashMap<String, JsonPrimitive> new_values = new HashMap<String, JsonPrimitive>();
		List<String> remove_values = new ArrayList<String>();
		
		/**
		 * Search and prepare changes
		 */
		for (Map.Entry<String, JsonElement> entry : tags.entrySet()) {
			key = (String) entry.getKey();
			value = entry.getValue().getAsString();
			try {
				if (key.equals("creation_time")) {
					new_values.put(key, new JsonPrimitive(format.parse(value).getTime()));
				} else if (key.equals("date")) {
					if (value.length() == "0000-00-00T00:00:00Z".length()) {
						new_values.put(key, new JsonPrimitive(format.parse(value.substring(0, 10) + " " + value.substring(11, 19)).getTime()));
					} else if (value.length() == "0000-00-00 00:00:00".length()) {
						new_values.put(key, new JsonPrimitive(format.parse(value).getTime()));
					} else if (value.length() == "0000-00-00".length()) {
						new_values.put(key, new JsonPrimitive(format.parse(value.substring(0, 10) + " 00:00:00").getTime()));
					} else {
						remove_values.add(key);
						new_values.put(key + "-raw", new JsonPrimitive("#" + value));
					}
				}
			} catch (ParseException e) {
				Loggers.Transcode_Metadata.error("Can't parse date, tags: " + tags, e);
			}
		}
		
		/**
		 * Apply changes
		 */
		for (Map.Entry<String, JsonPrimitive> entry : new_values.entrySet()) {
			tags.add(entry.getKey(), entry.getValue());
		}
		for (int pos = 0; pos < remove_values.size(); pos++) {
			tags.remove(remove_values.get(pos));
		}
	}
	
	public String getLongName() {
		return "FFprobe";
	}
	
	public static boolean canProcessThisVideoOnly(String mimetype) {
		if (mimetype.equalsIgnoreCase("application/gxf")) return true;
		if (mimetype.equalsIgnoreCase("application/lxf")) return true;
		if (mimetype.equalsIgnoreCase("application/mxf")) return true;
		
		if (mimetype.equalsIgnoreCase("video/mp2t")) return true;
		if (mimetype.equalsIgnoreCase("video/mp4")) return true;
		if (mimetype.equalsIgnoreCase("video/mpeg")) return true;
		if (mimetype.equalsIgnoreCase("video/quicktime")) return true;
		if (mimetype.equalsIgnoreCase("video/x-dv")) return true;
		if (mimetype.equalsIgnoreCase("video/vc1")) return true;
		if (mimetype.equalsIgnoreCase("video/ogg")) return true;
		if (mimetype.equalsIgnoreCase("video/mp2p")) return true;
		if (mimetype.equalsIgnoreCase("video/h264")) return true;
		if (mimetype.equalsIgnoreCase("video/x-flv")) return true;
		if (mimetype.equalsIgnoreCase("video/3gpp")) return true;
		if (mimetype.equalsIgnoreCase("video/x-ms-wmv")) return true;
		if (mimetype.equalsIgnoreCase("video/msvideo")) return true;
		return false;
	}
	
	public static boolean canProcessThisAudioOnly(String mimetype) {
		if (mimetype.equalsIgnoreCase("audio/x-wav")) return true;
		if (mimetype.equalsIgnoreCase("audio/ac3")) return true;
		if (mimetype.equalsIgnoreCase("audio/mp4")) return true;
		if (mimetype.equalsIgnoreCase("audio/mpeg")) return true;
		if (mimetype.equalsIgnoreCase("audio/ogg")) return true;
		if (mimetype.equalsIgnoreCase("audio/vorbis")) return true;
		if (mimetype.equalsIgnoreCase("audio/quicktime")) return true;
		
		if (mimetype.equalsIgnoreCase("audio/x-ms-wmv")) return true;
		if (mimetype.equalsIgnoreCase("audio/x-hx-aac-adts")) return true;
		if (mimetype.equalsIgnoreCase("audio/3gpp")) return true;
		if (mimetype.equalsIgnoreCase("audio/AMR")) return true;
		if (mimetype.equalsIgnoreCase("audio/AMR-WB")) return true;
		if (mimetype.equalsIgnoreCase("audio/amr-wb+")) return true;
		if (mimetype.equalsIgnoreCase("audio/eac3")) return true;
		if (mimetype.equalsIgnoreCase("audio/speex")) return true;
		if (mimetype.equalsIgnoreCase("audio/G719")) return true;
		if (mimetype.equalsIgnoreCase("audio/G722")) return true;
		if (mimetype.equalsIgnoreCase("audio/G7221")) return true;
		if (mimetype.equalsIgnoreCase("audio/G723")) return true;
		if (mimetype.equalsIgnoreCase("audio/G726-16")) return true;
		if (mimetype.equalsIgnoreCase("audio/G726-24")) return true;
		if (mimetype.equalsIgnoreCase("audio/G726-32")) return true;
		if (mimetype.equalsIgnoreCase("audio/G726-40")) return true;
		if (mimetype.equalsIgnoreCase("audio/G728")) return true;
		if (mimetype.equalsIgnoreCase("audio/G729")) return true;
		if (mimetype.equalsIgnoreCase("audio/G7291")) return true;
		if (mimetype.equalsIgnoreCase("audio/G729D")) return true;
		if (mimetype.equalsIgnoreCase("audio/G729E")) return true;
		if (mimetype.equalsIgnoreCase("audio/GSM")) return true;
		
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.heaac.1")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.heaac.2")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.mlp")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.mps")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.pl2")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.pl2x")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.pl2z")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dolby.pulse.1")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dra")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dts")) return true;
		if (mimetype.equalsIgnoreCase("audio/vnd.dts.hd")) return true;
		return false;
	}
	
	public boolean canProcessThisMimeType(String mimetype) {
		if (canProcessThisVideoOnly(mimetype)) return true;
		if (canProcessThisAudioOnly(mimetype)) return true;
		return false;
	}
	
	private static Properties translated_codecs_names;
	
	static {
		translated_codecs_names = new Properties();
		translated_codecs_names.setProperty("dvvideo", "DV");
		translated_codecs_names.setProperty("dvcp", "DV/DVCPro");
		translated_codecs_names.setProperty("dv5p", "DVCPro 50");
		translated_codecs_names.setProperty("h264", "h264");
		translated_codecs_names.setProperty("avc1", "h264");
		translated_codecs_names.setProperty("mpeg2video", "MPEG2");
		translated_codecs_names.setProperty("mx5p", "MPEG2/4:2:2");
		translated_codecs_names.setProperty("mpeg", "MPEG");
		translated_codecs_names.setProperty("wmv3", "WMV9");
		translated_codecs_names.setProperty("apch", "Apple ProRes 422 HQ");
		translated_codecs_names.setProperty("apcn", "Apple ProRes 422");
		translated_codecs_names.setProperty("apcs", "Apple ProRes 422 LT");
		translated_codecs_names.setProperty("apco", "Apple ProRes 422 Proxy");
		translated_codecs_names.setProperty("ap4h", "Apple ProRes 4444");
		
		translated_codecs_names.setProperty("mp2", "MPEG/L2");
		translated_codecs_names.setProperty("mp3", "MP3");
		translated_codecs_names.setProperty("wmav2", "WMA9");
		translated_codecs_names.setProperty("aac", "AAC");
		translated_codecs_names.setProperty("mp4a", "AAC");
		translated_codecs_names.setProperty("eac3", "EAC3");
		translated_codecs_names.setProperty("ec-3", "EAC3");
		translated_codecs_names.setProperty("pcm_s16le", "PCM 16b");
		translated_codecs_names.setProperty("pcm_s16le_planar", "PCM 16b");
		translated_codecs_names.setProperty("pcm_s16be", "PCM 16b/BE");
		translated_codecs_names.setProperty("pcm_s24le", "PCM 24b");
		translated_codecs_names.setProperty("pcm_s24be", "PCM 24b/BE");
		translated_codecs_names.setProperty("pcm_f32le", "PCM 32b float");
		translated_codecs_names.setProperty("pcm_f32be", "PCM 32b float/BE");
	}
	
	public static String translateCodecName(String ffmpeg_name) {
		return translated_codecs_names.getProperty(ffmpeg_name.toLowerCase(), ffmpeg_name);
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		ArrayList<String> al = new ArrayList<String>();
		al.add("audio/mpeg");
		al.add("audio/mp4");
		al.add("audio/quicktime");
		al.add("video/quicktime");
		al.add("video/mp4");
		return al;
	}
	
	private static String[] mime_list_master_as_preview;
	private static ValidatorCenter audio_webbrowser_validation;
	private static ValidatorCenter video_webbrowser_validation;
	
	public boolean isCanUsedInMasterAsPreview(Container container) {
		if (mime_list_master_as_preview == null) {
			mime_list_master_as_preview = getMimeFileListCanUsedInMasterAsPreview().toArray(new String[0]);
		}
		if (container.getSummary().equalsMimetype(mime_list_master_as_preview)) {
			if (video_webbrowser_validation == null) {
				video_webbrowser_validation = new ValidatorCenter();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].index", Comparator.EQUALS, 0);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].index", Comparator.EQUALS, 1);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].sample_rate", Comparator.EQUALS, 48000, 44100, 32000);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac");
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 384000);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].codec_name", Comparator.EQUALS, "h264");
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].width", Comparator.EQUALS_OR_SMALLER_THAN, 1920);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].height", Comparator.EQUALS_OR_SMALLER_THAN, 1080);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].level", Comparator.EQUALS_OR_SMALLER_THAN, 42);
				video_webbrowser_validation.and();
				video_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'video')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 4000000);
			}
			if (audio_webbrowser_validation == null) {
				audio_webbrowser_validation = new ValidatorCenter();
				audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].index", Comparator.EQUALS, 0);
				audio_webbrowser_validation.and();
				
				audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac", "mp3");
				audio_webbrowser_validation.and();
				audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
				audio_webbrowser_validation.and();
				audio_webbrowser_validation.addRule(FFprobe.class, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.EQUALS_OR_SMALLER_THAN, 384000);
			}
			
			if (video_webbrowser_validation.validate(container)) {
				Loggers.Transcode_Metadata.debug("YES ??");
				return true;
			} else if (audio_webbrowser_validation.validate(container)) {
				return true;
			}
		}
		return false;
	}
	
	public List<Class<? extends ContainerEntry>> getAllRootEntryClasses() {
		return Arrays.asList(FFprobe.class);
	}
	
	public PreviewType getPreviewTypeForRenderer(Container container, EntryRenderer entry) {
		return null;
	}
}
