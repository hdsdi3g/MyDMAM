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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.web;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import hd3gtv.mydmam.Loggers;
import play.Play;

public class StartPlayNoLazyLoad extends Thread {
	
	public StartPlayNoLazyLoad() {
		setName("Play load on boot");
		setDaemon(true);
	}
	
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
	
}
