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

import hd3gtv.log2.Log2;
import hd3gtv.tools.ExecprocessEvent;

import java.io.IOException;

public class ExecprocessEventServicelog implements ExecprocessEvent {
	
	private String execname;
	
	public ExecprocessEventServicelog(String execname) {
		this.execname = execname;
	}
	
	public void onStart() {
		Log2.log.info("Start " + execname + " process");
	}
	
	public void onEnd() {
		Log2.log.info("End of " + execname + " process");
	}
	
	public void onKill() {
		Log2.log.info("Terminate of " + execname + " process");
	}
	
	public void onError(IOException ioe) {
		Log2.log.error("Error with " + execname, ioe);
	}
	
	public void onError(InterruptedException ie) {
		Log2.log.error("Error with " + execname, ie);
	}
	
	public void onStdout(String message) {
		Log2.log.rawEvent(execname, message, false);
	}
	
	public void onStderr(String message) {
		Log2.log.rawEvent(execname, message, true);
	}
	
}
