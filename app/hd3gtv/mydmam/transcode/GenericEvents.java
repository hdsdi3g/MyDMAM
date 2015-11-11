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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.transcode;

import java.io.File;
import java.io.IOException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.ExecprocessGettext;

public class GenericEvents extends ExecprocessTranscodeEvent {
	
	private String process_name;
	
	public GenericEvents(String process_name) {
		this.process_name = process_name;
		if (process_name == null) {
			throw new NullPointerException("\"process_name\" can't to be null");
		}
		
	}
	
	public void onError(IOException ioe) {
		Loggers.Transcode.error(process_name + " error for " + jobref, ioe);
	}
	
	public void onError(InterruptedException ie) {
		Loggers.Transcode.error(process_name + " threads error for " + jobref, ie);
	}
	
	private StringBuffer sb = new StringBuffer();
	
	public void onStdout(String message) {
		sb.append("✓ ");
		sb.append(message);
		sb.append(ExecprocessGettext.LINESEPARATOR);
		
		if (Loggers.Transcode.isTraceEnabled()) {
			Loggers.Transcode.trace(process_name + " [" + jobref + "] ✓ " + message);
		}
	}
	
	public void onStderr(String message) {
		sb.append("✗ ");
		sb.append(message);
		sb.append(ExecprocessGettext.LINESEPARATOR);
		
		if (Loggers.Transcode.isTraceEnabled()) {
			Loggers.Transcode.trace(process_name + " [" + jobref + "] ✗ " + message);
		}
	}
	
	public String getLast_message() {
		return sb.toString().trim();
	}
	
	public void onStart(String commandline, File working_directory) {
		if (working_directory != null) {
			Loggers.Transcode.info("Start " + process_name + ": " + commandline + "\t in " + working_directory);
		} else {
			Loggers.Transcode.info("Start " + process_name + ": " + commandline);
		}
	}
	
	public void onEnd(int exitvalue, long execution_duration) {
		Loggers.Transcode.debug("End " + process_name + " execution for [" + jobref + "] with exit code = " + exitvalue + ", after " + (double) execution_duration / 1000d + " sec");
	}
	
	public void onKill(long execution_duration) {
		Loggers.Transcode.error(process_name + " is killed for [" + jobref + "] after " + (double) execution_duration / 1000d + " sec");
	}
	
}
