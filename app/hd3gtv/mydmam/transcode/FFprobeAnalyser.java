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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.transcode;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.analysis.Analyser;
import hd3gtv.mydmam.analysis.MetadataIndexerResult;
import hd3gtv.mydmam.analysis.validation.Comparator;
import hd3gtv.mydmam.analysis.validation.ValidatorCenter;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.ExecprocessGettext;
import hd3gtv.tools.Timecode;
import hd3gtv.tools.VideoConst;
import hd3gtv.tools.VideoConst.AudioSampling;
import hd3gtv.tools.VideoConst.Framerate;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.common.primitives.Ints;

public class FFprobeAnalyser implements Analyser {
	
	private String ffprobe_bin;
	
	public FFprobeAnalyser() {
		ffprobe_bin = Configuration.global.getValue("transcoding", "ffprobe_bin", "ffprobe");
	}
	
	public boolean isEnabled() {
		return (new File(ffprobe_bin)).exists();
	}
	
	public JSONObject process(MetadataIndexerResult analysis_result) throws Exception {
		ArrayList<String> param = new ArrayList<String>();
		param.add("-show_streams");
		param.add("-show_format");
		param.add("-show_chapters");
		param.add("-print_format");
		param.add("json");
		param.add("-i");
		param.add(analysis_result.getOrigin().getPath());
		
		ExecprocessGettext process = new ExecprocessGettext(ffprobe_bin, param);
		process.setEndlinewidthnewline(true);
		try {
			process.start();
		} catch (IOException e) {
			if (e instanceof ExecprocessBadExecutionException) {
				Log2Dump dump = new Log2Dump();
				dump.add("file", analysis_result.getOrigin());
				dump.add("mime", analysis_result.getMimetype());
				if (process.getRunprocess().getExitvalue() == 1) {
					dump.add("stderr", process.getResultstderr().toString().trim());
					Log2.log.error("Invalid data found when processing input", null, dump);
				} else {
					dump.add("stdout", process.getResultstdout().toString().trim());
					dump.add("stderr", process.getResultstderr().toString().trim());
					dump.add("exitcode", process.getRunprocess().getExitvalue());
					Log2.log.error("Problem with ffprobe", null, dump);
				}
			}
			throw e;
		}
		
		JSONParser jp = new JSONParser();
		JSONObject result = (JSONObject) jp.parse(process.getResultstdout().toString());
		
		/**
		 * Patch mime code if video stream level < 0
		 */
		List<JSONObject> video_streams = getStreamNode(result, "video");
		if (video_streams != null) {
			for (int pos = 0; pos < video_streams.size(); pos++) {
				if (video_streams.get(pos).containsKey("level") == false) {
					continue;
				}
				if (((Long) video_streams.get(pos).get("level")) < 0l) {
					video_streams.get(pos).put("ignore_stream", true);
					if (analysis_result.getMimetype().startsWith("video")) {
						analysis_result.changeMimetype("audio" + analysis_result.getMimetype().substring(5));
					}
				}
			}
		}
		
		/**
		 * Patch dates
		 */
		if (result.containsKey("streams")) {
			JSONArray streams = (JSONArray) result.get("streams");
			for (int pos = 0; pos < streams.size(); pos++) {
				JSONObject stream = (JSONObject) streams.get(pos);
				if (stream.containsKey("tags")) {
					patchTagDate((JSONObject) stream.get("tags"));
				}
			}
		}
		if (result.containsKey("format")) {
			JSONObject format = (JSONObject) result.get("format");
			if (format.containsKey("tags")) {
				try {
					patchTagDate((JSONObject) format.get("tags"));
				} catch (ConcurrentModificationException cme) {
					Log2.log.error("Can't patch dates", cme, new Log2Dump("json", format.toJSONString()));
				}
			}
		}
		
		return result;
	}
	
	private static void patchTagDate(JSONObject jo_tags) {
		if (jo_tags == null) {
			return;
		}
		if (jo_tags.isEmpty()) {
			return;
		}
		
		String key;
		String value;
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		HashMap<String, Object> new_values = new HashMap<String, Object>();
		List<String> remove_values = new ArrayList<String>();
		
		/**
		 * Search and prepare changes
		 */
		for (Object _entry : jo_tags.entrySet()) {
			Entry entry = (Entry) _entry;
			key = (String) entry.getKey();
			if (key.equals("creation_time")) {
				value = (String) entry.getValue();
				try {
					new_values.put(key, format.parse(value).getTime());
				} catch (ParseException e) {
					Log2.log.error("Can't parse date", e, new Log2Dump("tags", jo_tags.toJSONString()));
				}
			}
			if (key.equals("date")) {
				value = (String) jo_tags.get(key);
				try {
					if (value.length() == "0000-00-00T00:00:00Z".length()) {
						new_values.put(key, format.parse(value.substring(0, 10) + " " + value.substring(11, 19)).getTime());
					} else if (value.length() == "0000-00-00 00:00:00".length()) {
						new_values.put(key, format.parse(value).getTime());
					} else if (value.length() == "0000-00-00".length()) {
						new_values.put(key, format.parse(value.substring(0, 10) + " 00:00:00").getTime());
					} else {
						remove_values.add(key);
						new_values.put(key + "-raw", "#" + value);
					}
				} catch (ParseException e) {
					Log2.log.error("Can't parse date", e, new Log2Dump("tags", jo_tags.toJSONString()));
				}
			}
		}
		
		/**
		 * Apply changes
		 */
		for (Map.Entry<String, Object> entry : new_values.entrySet()) {
			jo_tags.put(entry.getKey(), entry.getValue());
		}
		for (int pos = 0; pos < remove_values.size(); pos++) {
			jo_tags.remove(remove_values.get(pos));
		}
		
	}
	
	public String getLongName() {
		return "FFprobe";
	}
	
	public String getElasticSearchIndexType() {
		return "ffprobe";
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
	
	public boolean canProcessThis(String mimetype) {
		if (canProcessThisVideoOnly(mimetype)) return true;
		if (canProcessThisAudioOnly(mimetype)) return true;
		return false;
	}
	
	public static JSONObject getAnalysedProcessresult(MetadataIndexerResult analysis_result) {
		if (analysis_result == null) {
			return null;
		}
		if (analysis_result.getAnalysis_results() == null) {
			return null;
		}
		for (Map.Entry<Analyser, JSONObject> entry : analysis_result.getAnalysis_results().entrySet()) {
			if (entry.getKey() instanceof FFprobeAnalyser) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	/**
	 * @return like "Video: DV SD PAL, Audio: PCM 16b (stereo 48.0kHz 1536kbps), Dur: 00:00:08:00 @ 28,80 Mbps", "Video: MPEG2 SD PAL, Audio: PCM 16b (mono 48.0kHz 768kbps), x2",
	 *         "Audio: EAC3 (3ch 32.0kHz 384kbps), Dur: 00:00:05:00 @ 384,00 kbps"
	 */
	public String getSummary(JSONObject processresult) {
		StringBuffer sb = new StringBuffer();
		if (processresult.containsKey("format") == false) {
			return "Invalid file";
		}
		JSONObject jo_format = (JSONObject) processresult.get("format");
		
		Framerate frame_rate = Framerate.OTHER;
		
		ArrayList<String> streams_list = new ArrayList<String>();
		
		if (processresult.containsKey("streams")) {
			JSONArray streams = (JSONArray) processresult.get("streams");
			JSONObject stream;
			int width;
			int height;
			for (int pos = 0; pos < streams.size(); pos++) {
				sb = new StringBuffer();
				stream = (JSONObject) streams.get(pos);
				if (stream.containsKey("ignore_stream")) {
					continue;
				} else if (stream.containsKey("codec_type")) {
					String codec_name = (String) stream.get("codec_tag_string");
					if (codec_name.indexOf("[") > -1) {
						codec_name = (String) stream.get("codec_name");
					}
					
					String codec_type = (String) stream.get("codec_type");
					if (codec_type.equalsIgnoreCase("video")) {
						sb.append("Video: ");
						sb.append(translateCodecName(codec_name));
						
						frame_rate = getFramerate(processresult);
						width = Ints.checkedCast((Long) stream.get("width"));
						height = Ints.checkedCast((Long) stream.get("height"));
						
						sb.append(" ");
						sb.append(VideoConst.getSystemSummary(width, height, frame_rate));
					} else if (codec_type.equalsIgnoreCase("audio")) {
						sb.append("Audio: ");
						if (codec_name.equalsIgnoreCase("twos") | codec_name.equalsIgnoreCase("sowt")) {
							codec_name = (String) stream.get("codec_name");
						}
						sb.append(translateCodecName(codec_name));
						
						sb.append(" (");
						sb.append(VideoConst.audioChannelCounttoString(Ints.checkedCast((Long) stream.get("channels"))));
						
						if (stream.containsKey("sample_rate")) {
							sb.append(" ");
							sb.append(Float.parseFloat((String) stream.get("sample_rate")) / 1000f);
							sb.append("kHz");
						}
						if (stream.containsKey("bit_rate")) {
							sb.append(" ");
							sb.append(Integer.parseInt((String) stream.get("bit_rate")) / 1000);
							sb.append("kbps");
						}
						sb.append(")");
						
					}
				}
				streams_list.add(sb.toString());
			}
		}
		
		sb = new StringBuffer();
		
		for (int pos = 0; pos < streams_list.size(); pos++) {
			if (streams_list.get(pos).startsWith("Video:")) {
				sb.append(streams_list.get(pos));
				sb.append(", ");
			}
		}
		
		/**
		 * Do not repeat if there is more that 1 identical stream, like 4 mono channels.
		 */
		String last_stream = "";
		int count_last_stream = 0;
		for (int pos = 0; pos < streams_list.size(); pos++) {
			if (streams_list.get(pos).startsWith("Audio:") == false) {
				continue;
			}
			if (streams_list.get(pos).equals(last_stream)) {
				count_last_stream++;
			} else {
				if (count_last_stream > 0) {
					sb.append("x");
					sb.append(count_last_stream + 1);
					sb.append(", ");
				} else {
					sb.append(streams_list.get(pos));
					sb.append(", ");
					last_stream = streams_list.get(pos);
				}
				count_last_stream = 0;
			}
		}
		if (count_last_stream > 0) {
			sb.append("x");
			sb.append(count_last_stream + 1);
			sb.append(", ");
		}
		if (sb.toString().endsWith(", ") == false) {
			sb.append(", ");
		}
		
		Timecode tc = getDuration(processresult);
		if (tc != null) {
			sb.append("Dur: ");
			sb.append(tc.toString());
			sb.append(" ");
		}
		if (jo_format.containsKey("bit_rate")) {
			sb.append("@ ");
			int bit_rate = Integer.valueOf((String) jo_format.get("bit_rate"));
			if (bit_rate < 1000) {
				sb.append(bit_rate);
				sb.append(" bps");
			} else {
				/**
				 * like "46,61 Mbps"
				 */
				int exp = (int) (Math.log(bit_rate) / Math.log(1000));
				sb.append(String.format("%.2f", bit_rate / Math.pow(1000, exp)));
				sb.append(" ");
				sb.append(("kMGTPE").charAt(exp - 1));
				sb.append("bps");
				sb.append(" ");
			}
		}
		
		if (sb.toString().trim().endsWith(",")) {
			return sb.toString().trim().substring(0, sb.toString().trim().length() - 1);
		}
		return sb.toString().trim();
	}
	
	private static Properties translated_codecs_names;
	
	static {
		translated_codecs_names = new Properties();
		translated_codecs_names.setProperty("dvvideo", "DV");
		translated_codecs_names.setProperty("dvcp", "DV");
		translated_codecs_names.setProperty("h264", "h264");
		translated_codecs_names.setProperty("avc1", "h264");
		translated_codecs_names.setProperty("mpeg2video", "MPEG2");
		translated_codecs_names.setProperty("mx5p", "MPEG2/4:2:2");
		translated_codecs_names.setProperty("mpeg", "MPEG");
		translated_codecs_names.setProperty("mp2", "MPEG/L2");
		translated_codecs_names.setProperty("mp3", "MP3");
		translated_codecs_names.setProperty("wmv3", "WMV9");
		translated_codecs_names.setProperty("wmav2", "WMA9");
		translated_codecs_names.setProperty("aac", "AAC");
		translated_codecs_names.setProperty("eac3", "EAC3");
		translated_codecs_names.setProperty("ec-3", "EAC3");
		translated_codecs_names.setProperty("pcm_s16le", "PCM 16b");
		translated_codecs_names.setProperty("pcm_s16le_planar", "PCM 16b");
		translated_codecs_names.setProperty("pcm_s16be", "PCM 16b/BE");
		translated_codecs_names.setProperty("pcm_s24le", "PCM 24b");
		translated_codecs_names.setProperty("pcm_s24be", "PCM 24b/BE");
	}
	
	public static String translateCodecName(String ffmpeg_name) {
		return translated_codecs_names.getProperty(ffmpeg_name.toLowerCase(), ffmpeg_name);
	}
	
	public static List<JSONObject> getStreamNode(JSONObject processresult, String codec_type) {
		if (processresult.containsKey("streams") == false) {
			return null;
		}
		JSONArray streams = (JSONArray) processresult.get("streams");
		ArrayList<JSONObject> stream_list = new ArrayList<JSONObject>();
		for (int pos = 0; pos < streams.size(); pos++) {
			JSONObject stream = (JSONObject) streams.get(pos);
			if (stream.containsKey("ignore_stream")) {
				continue;
			}
			if (((String) stream.get("codec_type")).equalsIgnoreCase(codec_type)) {
				stream_list.add(stream);
			}
		}
		if (stream_list.isEmpty()) {
			return null;
		}
		return stream_list;
	}
	
	public static boolean hasVideo(JSONObject processresult) {
		List<JSONObject> streams = getStreamNode(processresult, "video");
		return (streams != null);
	}
	
	public static boolean hasAudio(JSONObject processresult) {
		List<JSONObject> streams = getStreamNode(processresult, "audio");
		return (streams != null);
	}
	
	/**
	 * @return the first valid value if there are more one audio stream.
	 */
	public static AudioSampling getAudioSampling(JSONObject processresult) {
		List<JSONObject> streams = getStreamNode(processresult, "audio");
		if (streams == null) {
			return null;
		}
		
		for (int pos = 0; pos < streams.size(); pos++) {
			JSONObject stream = streams.get(pos);
			Object samplerate = stream.get("sample_rate");
			if (samplerate == null) {
				continue;
			}
			if (samplerate instanceof String) {
				return AudioSampling.parseAS((String) samplerate);
			}
			if (samplerate instanceof Integer) {
				return AudioSampling.parseAS(Integer.toString((Integer) samplerate));
			}
		}
		return null;
	}
	
	public static Timecode getDuration(JSONObject processresult) {
		try {
			Framerate framerate = getFramerate(processresult);
			JSONObject format = (JSONObject) processresult.get("format");
			String v_duration = (String) format.get("duration");
			return new Timecode(Float.valueOf(v_duration), framerate.getNumericValue());
		} catch (Exception e) {
			Log2.log.error("Can't extract duration from file", e, new Log2Dump("processresult", processresult.toJSONString()));
		}
		return null;
	}
	
	/**
	 * @return the first valid value if there are more one audio stream.
	 */
	public static Framerate getFramerate(JSONObject processresult) {
		List<JSONObject> streams = getStreamNode(processresult, "video");
		if (streams == null) {
			return null;
		}
		if (streams.isEmpty()) {
			return null;
		}
		
		JSONObject stream = streams.get(0);
		
		String avg_frame_rate = null;
		String r_frame_rate = null;
		int nb_frames = 0;
		float duration = 0f;
		
		/**
		 * "r_frame_rate" : "30000/1001",
		 * "r_frame_rate" : "30/1",
		 * "r_frame_rate" : "25/1",
		 */
		if (stream.containsKey("r_frame_rate")) {
			r_frame_rate = (String) stream.get("r_frame_rate");
		}
		
		/**
		 * "avg_frame_rate" : "30000/1001",
		 * "avg_frame_rate" : "0/0",
		 * "avg_frame_rate" : "25/1",
		 */
		if (stream.containsKey("avg_frame_rate")) {
			avg_frame_rate = (String) stream.get("avg_frame_rate");
		}
		
		/** "nb_frames" : "8487", */
		if (stream.containsKey("nb_frames")) {
			try {
				nb_frames = Integer.valueOf((String) stream.get("nb_frames"));
			} catch (NumberFormatException e) {
			}
		}
		
		/** "duration" : "283.182900", */
		if (stream.containsKey("duration")) {
			try {
				duration = Float.valueOf((String) stream.get("duration"));
			} catch (NumberFormatException e) {
			}
		}
		
		Framerate result = Framerate.getFramerate(r_frame_rate);
		if (result == null) {
			result = Framerate.getFramerate(avg_frame_rate);
		} else if (result == Framerate.OTHER) {
			if (Framerate.getFramerate(avg_frame_rate) != Framerate.OTHER) {
				result = Framerate.getFramerate(avg_frame_rate);
			}
		}
		if (((result == null) | (result == Framerate.OTHER)) & (nb_frames > 0) & (duration > 0f)) {
			result = Framerate.getFramerate((float) Math.round(((float) nb_frames / duration) * 10f) / 10f);
		}
		
		/*String frame_rate_raw = (String) stream.get("r_frame_rate");
		if (format_name.equalsIgnoreCase("mpegts")) {
			frame_rate_raw = (String) stream.get("avg_frame_rate");
		}*/
		return result;
	}
	
	public List<String> getMimeFileListCanUsedInMasterAsPreview() {
		ArrayList<String> al = new ArrayList<String>();
		al.add("audio/mpeg");
		al.add("audio/mp4");
		al.add("audio/quicktime");
		return al;
	}
	
	private static ValidatorCenter audio_webbrowser_validation;
	
	public boolean isCanUsedInMasterAsPreview(MetadataIndexerResult metadatas_result) {
		String[] mime_list = getMimeFileListCanUsedInMasterAsPreview().toArray(new String[0]);
		
		if (metadatas_result.equalsMimetype(mime_list)) {
			/**
			 * Test for audio
			 */
			if (audio_webbrowser_validation == null) {
				audio_webbrowser_validation = new ValidatorCenter();
				audio_webbrowser_validation.addRule(this, "$.streams[?(@.codec_type == 'audio')].sample_rate", Comparator.EQUALS, 48000, 44100, 32000);
				audio_webbrowser_validation.and();
				audio_webbrowser_validation.addRule(this, "$.streams[?(@.codec_type == 'audio')].codec_name", Comparator.EQUALS, "aac", "mp3");
				audio_webbrowser_validation.and();
				audio_webbrowser_validation.addRule(this, "$.streams[?(@.codec_type == 'audio')].channels", Comparator.EQUALS, 1, 2);
				audio_webbrowser_validation.and();
				audio_webbrowser_validation.addRule(this, "$.streams[?(@.codec_type == 'audio')].bit_rate", Comparator.SMALLER_THAN, 384001);
			}
			return audio_webbrowser_validation.validate(metadatas_result.getAnalysis_results());
		}
		return false;
	}
}
