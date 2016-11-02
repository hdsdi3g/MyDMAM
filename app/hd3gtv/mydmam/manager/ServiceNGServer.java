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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.manager;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.Execprocess;
import play.Play;
import play.server.Server;

public class ServiceNGServer extends ServiceNG {
	
	public ServiceNGServer(String[] args) throws Exception {
		super(args, "Play Server service handler");
	}
	
	private Execprocess process_play;
	
	protected boolean startBroker() {
		return false;
	}
	
	protected void startService() throws Exception {
		/*
		 * java
		 * ok -javaagent:c:\..\play\framework/play-1.3.0.jar
		 * ok -Dservice.config.apply=debug
		 * ok -noverify
		 * ok -Dfile.encoding=utf-8
		 * nope -Xdebug
		 * nope -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
		 * ok -Dplay.debug=yes
		 * nope -classpath <>
		 * ok -Dapplication.path=C:\...\mydmam
		 * ok -Dplay.id=
		 * ok play.server.Server ""
		 */
		try {
			Class.forName("play.modules.docviewer.DocumentationGenerator");
		} catch (Exception e) {
			Loggers.Play.fatal("Missing play-docviewer.jar in class path !");
			System.exit(1);
		}
		
		System.setProperty("application.path", MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath());
		System.setProperty("play.id", "");
		
		if (Configuration.global.getValueBoolean("play", "debug")) {
			System.setProperty("play.debug", "yes");
		}
		
		Server.main(new String[] {});
	}
	
	protected void stopService() throws Exception {
		Play.stop();
	}
}
