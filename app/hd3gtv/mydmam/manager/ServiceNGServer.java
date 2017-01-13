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

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import play.Play;
import play.Play.Mode;
import play.server.Server;

public class ServiceNGServer extends ServiceNG {
	
	public ServiceNGServer(String[] args) throws Exception {
		super(args, "Play Server service handler");
	}
	
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
		
		if (Play.mode == Mode.DEV) {
			/**
			 * NO LAZY LOAD
			 */
			Thread t_load_on_boot = new Thread(new Runnable() {
				
				@Override
				public void run() {
					try {
						while (Play.initialized == false) {
							Thread.sleep(10);
						}
						
						String port = Play.configuration.getProperty("https.port", Play.configuration.getProperty("http.port", "80"));
						
						StringBuilder sb = new StringBuilder();
						sb.append("http");
						
						if (Play.configuration.getProperty("https.port", "").equals("") == false) {
							sb.append("s");
						}
						
						sb.append("://");
						
						String address = Play.configuration.getProperty("https.address", Play.configuration.getProperty("http.address", "127.0.0.1"));
						sb.append(address);
						
						sb.append(":");
						sb.append(port);
						sb.append("/");
						
						URL url = new URL(sb.toString());
						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(5 * 60 * 1000);
						conn.setReadTimeout(5 * 60 * 1000);
						
						Loggers.Play.debug("Connect to local Play site " + url.toString());
						
						conn.connect();
						IOUtils.closeQuietly(conn.getInputStream());
						conn.disconnect();
					} catch (Exception e) {
						Loggers.Play.debug("Trouble with start-on-boot http trigger", e);
					}
				}
			});
			t_load_on_boot.setName("Load on boot");
			t_load_on_boot.setDaemon(true);
			t_load_on_boot.start();
		}
	}
	
	protected void stopService() throws Exception {
		Play.stop();
	}
}
