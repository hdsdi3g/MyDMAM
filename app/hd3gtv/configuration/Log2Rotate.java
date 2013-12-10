/*
 * This file is part of YAML Configuration for MyDMAM
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
package hd3gtv.configuration;

import hd3gtv.log2.Log2;
import hd3gtv.log2.LogHandlerToLogfile;

import java.io.File;

class Log2Rotate {
	
	private LogHandlerToLogfile loghandler;
	private String logfile;
	
	Log2Rotate() {
		
		if (Configuration.global.isElementExists("log2") == false) {
			return;
		}
		
		logfile = Configuration.global.getValue("log2", "rotate_file", null);
		if (logfile == null) {
			return;
		}
		
		long maxsize = Configuration.global.getValue("log2", "rotate_maxsize", 1000000l);
		int maxfilelogs = Configuration.global.getValue("log2", "rotate_maxfilelogs", 10);
		
		loghandler = new LogHandlerToLogfile(new File(logfile), maxsize, maxfilelogs);
		Log2.log = new Log2(loghandler);
		Rotate rotate = new Rotate();
		rotate.start();
	}
	
	private class Rotate extends Thread {
		public Rotate() {
			setName("Log2Rotate");
			setDaemon(true);
		}
		
		public void run() {
			System.out.println("Start log rotate for file :");
			System.out.println(" " + logfile);
			while (true) {
				try {
					loghandler.rotatefile();
					Thread.sleep(60000);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}
	}
	
}
