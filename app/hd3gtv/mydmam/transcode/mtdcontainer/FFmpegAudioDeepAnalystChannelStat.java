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

import org.apache.commons.lang.math.NumberUtils;

public class FFmpegAudioDeepAnalystChannelStat {
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("channel: ");
		sb.append(channel);
		sb.append(", dc_offset: ");
		sb.append(dc_offset);
		sb.append(", min_level: ");
		sb.append(min_level);
		sb.append(", max_level: ");
		sb.append(max_level);
		sb.append(", min_difference: ");
		sb.append(min_difference);
		sb.append(", max_difference: ");
		sb.append(max_difference);
		sb.append(", mean_difference: ");
		sb.append(mean_difference);
		sb.append(", peak_level: ");
		sb.append(peak_level);
		sb.append(", rms_level: ");
		sb.append(rms_level);
		sb.append(", rms_peak: ");
		sb.append(rms_peak);
		sb.append(", rms_trough: ");
		sb.append(rms_trough);
		sb.append(", crest_factor: ");
		sb.append(crest_factor);
		sb.append(", flat_factor: ");
		sb.append(flat_factor);
		sb.append(", peak_count: ");
		sb.append(peak_count);
		return sb.toString();
	}
	
	public int channel;
	public float dc_offset;
	public float min_level;
	public float max_level;
	public float min_difference;
	public float max_difference;
	public float mean_difference;
	
	/**
	 * dB
	 */
	public float peak_level;
	
	/**
	 * dB
	 */
	public float rms_level;
	
	/**
	 * dB
	 */
	public float rms_peak;
	
	/**
	 * dB
	 */
	public float rms_trough;
	
	/**
	 * dB
	 */
	public float crest_factor;
	
	/**
	 * dB
	 */
	public float flat_factor;
	
	public long peak_count;
	
	/**
	 * @param line like "DC offset: -0.000243"
	 */
	public void parseFromFFmpegLine(String line) throws IndexOutOfBoundsException, NumberFormatException {
		String[] content = line.trim().split(":");
		if (content.length != 2) {
			throw new IndexOutOfBoundsException("Invalid input: " + line);
		}
		String name = content[0].trim();
		Number value = -144.49f;
		if (content[1].trim().equals("-inf") == false) {
			value = NumberUtils.createNumber(content[1].trim());
		}
		
		if (name.startsWith("Channel")) {
			channel = value.intValue();
		} else if (name.startsWith("DC offset")) {
			dc_offset = value.floatValue();
		} else if (name.startsWith("Min level")) {
			min_level = value.floatValue();
		} else if (name.startsWith("Max level")) {
			max_level = value.floatValue();
		} else if (name.startsWith("Min difference")) {
			min_difference = value.floatValue();
		} else if (name.startsWith("Max difference")) {
			max_difference = value.floatValue();
		} else if (name.startsWith("Mean difference")) {
			mean_difference = value.floatValue();
		} else if (name.startsWith("Peak level dB")) {
			peak_level = value.floatValue();
		} else if (name.startsWith("RMS level dB")) {
			rms_level = value.floatValue();
		} else if (name.startsWith("RMS peak dB")) {
			rms_peak = value.floatValue();
		} else if (name.startsWith("RMS trough dB")) {
			rms_trough = value.floatValue();
		} else if (name.startsWith("Crest factor")) {
			crest_factor = value.floatValue();
		} else if (name.startsWith("Flat factor")) {
			flat_factor = value.floatValue();
		} else if (name.startsWith("Peak count")) {
			peak_count = value.longValue();
		} else if (name.startsWith("Bit depth")) {
			/**
			 * Do nothing, invalid value returned by ffmpeg.
			 */
		} else {
			throw new IndexOutOfBoundsException("Unexpected content: " + line);
		}
	}
	
}
