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

import java.io.File;
import java.io.IOException;

import hd3gtv.mydmam.Loggers;

public class FFmpegEvents extends ExecprocessTranscodeEvent {
	
	private String last_message;
	
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
	
	public void onStart(String commandline, File working_directory) {
		if (working_directory != null) {
			Loggers.Transcode.info("start ffmpeg: " + commandline + "\t in " + working_directory);
		} else {
			Loggers.Transcode.info("start ffmpeg: " + commandline);
		}
	}
	
	public void onEnd(int exitvalue, long execution_duration) {
		Loggers.Transcode.debug("End ffmpeg execution, after " + (double) execution_duration / 1000d + " sec");
	}
	
	public void onKill(long execution_duration) {
		Loggers.Transcode.error("FFmpeg is killed for " + jobref + ", after " + (double) execution_duration / 1000d + " sec");
	}
}