/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
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
import hd3gtv.tools.ExecprocessEvent;

import java.io.IOException;

public class FFmpegEvents implements ExecprocessEvent {
	
	boolean display_raw_ffmpegmessages;
	
	private String jobref;
	
	private String last_message;
	
	public FFmpegEvents(String jobref) {
		display_raw_ffmpegmessages = Configuration.global.getValueBoolean("transcoding", "ffmpeg_displayrawmessages");
		this.jobref = jobref;
	}
	
	public void onStart() {
	}
	
	public void onEnd() {
	}
	
	public void onKill() {
		Log2.log.error("FFmpeg is killed for " + jobref, null);
	}
	
	public void onError(IOException ioe) {
		Log2.log.error("FFmpeg error for " + jobref, ioe);
	}
	
	public void onError(InterruptedException ie) {
		Log2.log.error("FFmpeg threads error for " + jobref, ie);
	}
	
	public void onStdout(String message) {
		if (display_raw_ffmpegmessages) {
			System.out.println("[" + jobref + "] " + message);
		}
	}
	
	public void onStderr(String message) {
		if (display_raw_ffmpegmessages) {
			System.out.println("[" + jobref + "] " + message);
		}
		last_message = message;
	}
	
	public String getLast_message() {
		return last_message;
	}
}