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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.transcode.mtdcontainer;

import java.util.ArrayList;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.metadata.container.ContainerEntryDeSerializer;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;

public class FFmpegAudioDeepAnalyst extends EntryAnalyser {
	
	public static final String ES_TYPE = "ffaudioda";
	
	public String getES_Type() {
		return ES_TYPE;
	}
	
	public boolean canBeSendedToWebclients() {
		return true;
	}
	
	/**
	 * LUFS
	 */
	public float integrated_loudness = Float.MIN_VALUE;
	/**
	 * LUFS
	 */
	public float integrated_loudness_threshold = 0f;
	/**
	 * LU
	 */
	public float loudness_range_LRA = 0f;
	/**
	 * LUFS
	 */
	public float loudness_range_threshold = 0f;
	/**
	 * LUFS
	 */
	public float loudness_range_LRA_low = 0f;
	/**
	 * LUFS
	 */
	public float loudness_range_LRA_high = 0f;
	/**
	 * dBFS
	 */
	public float true_peak = Float.MIN_VALUE;
	
	public long number_of_samples;
	
	@GsonIgnore
	public ArrayList<FFmpegAudioDeepAnalystChannelStat> channels_stat;
	
	public FFmpegAudioDeepAnalystChannelStat overall_stat;
	
	@GsonIgnore
	public ArrayList<FFmpegAudioDeepAnalystSilenceDetect> silences;
	
	/**
	 * dBFS
	 */
	public int silencedetect_level_threshold;
	
	/**
	 * dBFS
	 */
	public int silencedetect_min_duration;
	
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(", integrated loudness: ");
		sb.append(integrated_loudness);
		sb.append(", threshold: ");
		sb.append(integrated_loudness_threshold);
		sb.append(", loudness range LRA: ");
		sb.append(loudness_range_LRA);
		sb.append(", threshold: ");
		sb.append(loudness_range_threshold);
		sb.append(", LRA low: ");
		sb.append(loudness_range_LRA_low);
		sb.append(", LRA high: ");
		sb.append(loudness_range_LRA_high);
		sb.append(", true peak: ");
		sb.append(true_peak);
		
		if (channels_stat != null) {
			sb.append(", Channel stat: ");
			for (int pos = 0; pos < channels_stat.size(); pos++) {
				sb.append(channels_stat.get(pos).toString());
			}
		}
		if (overall_stat != null) {
			sb.append(", Overall channels stat: ");
			sb.append(overall_stat.toString());
		}
		if (silences != null) {
			sb.append(", Silences: ");
			for (int pos = 0; pos < silences.size(); pos++) {
				sb.append(silences.get(pos).toString());
			}
		}
		return sb.toString();
	}
	
	public static class Serializer extends ContainerEntryDeSerializer<FFmpegAudioDeepAnalyst> {
		
		protected FFmpegAudioDeepAnalyst internalDeserialize(JsonObject source) {
			FFmpegAudioDeepAnalyst ffada = MyDMAM.gson_kit.getGsonSimple().fromJson(source, FFmpegAudioDeepAnalyst.class);
			ffada.channels_stat = MyDMAM.gson_kit.getGson().fromJson(source.get("channels_stat"), GsonKit.type_ArrayList_FFmpegAudioDeepAnalystChannelStat);
			ffada.silences = MyDMAM.gson_kit.getGson().fromJson(source.get("silences"), GsonKit.type_ArrayList_FFmpegAudioDeepAnalystSilenceDetect);
			return ffada;
		}
		
		protected JsonObject internalSerialize(FFmpegAudioDeepAnalyst item) {
			JsonObject current_element = super.internalSerialize(item);
			current_element.add("channels_stat", MyDMAM.gson_kit.getGson().toJsonTree(item.channels_stat, GsonKit.type_ArrayList_FFmpegAudioDeepAnalystChannelStat));
			current_element.add("silences", MyDMAM.gson_kit.getGson().toJsonTree(item.silences, GsonKit.type_ArrayList_FFmpegAudioDeepAnalystSilenceDetect));
			return current_element;
		}
	}
	
}
