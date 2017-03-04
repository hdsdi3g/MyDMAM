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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2013
 * 
*/
package hd3gtv.mydmam;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.manager.ServiceNG;
import hd3gtv.tools.ApplicationArgs;

public class MainClass {
	
	public static void main(String[] args) throws Exception {
		Loggers.Manager.info("Start application");
		Loggers.displayCurrentConfiguration();
		MyDMAM.testIllegalKeySize();
		
		boolean enable_play = Configuration.global.isElementExists("play");
		boolean enable_ftpserver = Configuration.global.isElementExists("ftpserverinstances");
		boolean enable_background_services = true;
		
		ApplicationArgs aargs = new ApplicationArgs(args);
		String fa = aargs.getFirstAction();
		if (fa != null) {
			if (fa.equalsIgnoreCase("play")) {
				if (enable_play == false) {
					throw new Exception("Wan't to start Play server, but it not enabled in configuration");
				}
				enable_ftpserver = false;
				enable_background_services = false;
			} else if (fa.equalsIgnoreCase("ftpserver")) {
				if (enable_ftpserver == false) {
					throw new Exception("Wan't to start FTP Server, but it not enabled in configuration");
				}
				enable_play = false;
				enable_background_services = false;
			} else if (fa.equalsIgnoreCase("services")) {
				enable_play = false;
				enable_ftpserver = false;
			} else {
				throw new Exception("Action \"" + fa + "\" is not supported");
			}
		}
		
		new ServiceNG(enable_play, enable_ftpserver, enable_background_services).startAllServices();
	}
}
