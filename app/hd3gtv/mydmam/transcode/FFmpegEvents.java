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

import java.io.IOException;

import hd3gtv.mydmam.Loggers;

public class FFmpegEvents extends ExecprocessTranscodeEvent {
	
	private String last_message;
	
	public void onStart() {
	}
	
	public void onEnd() {
	}
	
	public void onKill() {
		Loggers.Transcode.error("FFmpeg is killed for " + jobref);
	}
	
	public void onError(IOException ioe) {
		Loggers.Transcode.error("FFmpeg error for " + jobref, ioe);
	}
	
	public void onError(InterruptedException ie) {
		Loggers.Transcode.error("FFmpeg threads error for " + jobref, ie);
	}
	
	public void onStdout(String message) {
		if (Loggers.Transcode.isTraceEnabled()) {
			Loggers.Transcode.trace("ffmpeg-stdout [" + jobref + "] " + message);
		}
	}
	
	public void onStderr(String message) {
		if (Loggers.Transcode.isTraceEnabled()) {
			Loggers.Transcode.trace("ffmpeg-stderr [" + jobref + "] " + message);
		}
		last_message = message;
	}
	
	public String getLast_message() {
		return last_message;
	}
}