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

public class FFmpegAudioDeepAnalystSilenceDetect {
	
	/**
	 * in msec
	 */
	public long from;
	
	/**
	 * in msec
	 */
	public long to;
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("from: ");
		sb.append((double) from / 1000d);
		sb.append(" sec to ");
		sb.append((double) to / 1000d);
		sb.append(" sec, ");
		return sb.toString();
	}
	
	/**
	 * @param line like "silence_start: 447.441" or "silence_end: 923.741 | silence_duration: 4.2"
	 */
	public void parseFromFFmpegLine(String line) throws IndexOutOfBoundsException, NumberFormatException {
		String[] content = line.trim().split(":");
		if (content.length == 1) {
			throw new IndexOutOfBoundsException("Invalid input: " + line);
		}
		String name = content[0].trim();
		
		if (name.equals("silence_start")) {
			from = Math.round(Double.parseDouble(content[1].trim()) * 1000d);
		} else if (name.equals("silence_end")) {
			to = Math.round(Double.parseDouble(content[1].substring(0, content[1].indexOf("|") - 1).trim()) * 1000d);
		} else {
			throw new IndexOutOfBoundsException("Unexpected content: " + line);
		}
	}
	
}
