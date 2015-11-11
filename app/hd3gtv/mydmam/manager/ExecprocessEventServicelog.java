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
package hd3gtv.mydmam.manager;

import java.io.File;
import java.io.IOException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.ExecprocessEvent;

public class ExecprocessEventServicelog implements ExecprocessEvent {
	
	private String execname;
	
	public ExecprocessEventServicelog(String execname) {
		this.execname = execname;
	}
	
	public void onStart(String commandline, File working_directory) {
		Loggers.Manager.info("Start " + execname + " process: \t" + commandline);
	}
	
	public void onEnd(int exitvalue, long execution_duration) {
		Loggers.Manager.info("End of " + execname + " process after " + (double) execution_duration / 1000d + " sec");
	}
	
	public void onKill(long execution_duration) {
		Loggers.Manager.info("Terminate of " + execname + " process after " + (double) execution_duration / 1000d + " sec");
	}
	
	public void onError(IOException ioe) {
		Loggers.Manager.error("Error with " + execname, ioe);
	}
	
	public void onError(InterruptedException ie) {
		Loggers.Manager.warn("Error with " + execname, ie);
	}
	
	public void onStdout(String message) {
		Loggers.Manager.debug(execname + ":stdout\t" + message);
	}
	
	public void onStderr(String message) {
		Loggers.Manager.warn(execname + ":stderr\t" + message);
	}
	
}
