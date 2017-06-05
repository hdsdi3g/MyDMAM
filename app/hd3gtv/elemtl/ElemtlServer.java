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
package hd3gtv.elemtl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.commons.io.IOUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.XmlData;

public class ElemtlServer {
	
	private static final float VIDEO_SYSTEM_FPS = (float) Configuration.global.getValue("elemtl", "video_system_fps", 25d);
	
	private URL server;
	
	public ElemtlServer(URL server) {
		this.server = server;
		if (server == null) {
			throw new NullPointerException("\"server\" can't to be null");
		}
	}
	
	public String toString() {
		return server.toString();
	}
	
	/**
	 * @param request can be null
	 * @return null if error
	 */
	private XmlData request(XmlData request, String url_path) {
		if (url_path == null) {
			throw new NullPointerException("\"url_path\" can't to be null");
		}
		
		HttpURLConnection connection = null;
		XmlData result = null;
		try {
			StringBuilder full_query = new StringBuilder(server.toString());
			
			if (url_path.startsWith("/")) {
				full_query.append(url_path);
			} else {
				full_query.append("/");
				full_query.append(url_path);
			}
			
			URL url = new URL(server, full_query.toString());
			if (Loggers.Elemtl.isTraceEnabled()) {
				Loggers.Elemtl.trace("full_query  : " + full_query.toString());
				Loggers.Elemtl.trace("HTTP Request: " + url.toString());
			}
			
			connection = (HttpURLConnection) url.openConnection();
			if (request == null) {
				connection.setRequestMethod("GET");
				connection.setDoOutput(false);
			} else {
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/xml");
				connection.setDoOutput(true);
			}
			connection.setRequestProperty("accept", "application/xml");
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setConnectTimeout(10);
			
			if (request != null) {
				byte[] raw_datas = request.getBytes();
				if (Loggers.Elemtl.isTraceEnabled()) {
					Loggers.Elemtl.trace("Send to " + url + " " + new String(raw_datas));
				}
				OutputStream os = connection.getOutputStream();
				os.write(raw_datas);
				os.close();
			}
			
			/*String content_type = connection.getContentType();
			if (content_type.toLowerCase().equals("application/json;charset=utf-8") == false) {
				throw new IOException("Unknow content type (" + content_type + ") for " + url.toString());
			}*/
			
			// connection.setDoOutput(true);
			// Send request
			/*DataOutputStream wr = new DataOutputStream();
			wr.writeBytes(urlParameters);
			wr.close();*/
			
			InputStream is = connection.getInputStream();
			result = XmlData.loadFromSteam(is);
			
			if (Loggers.Elemtl.isTraceEnabled()) {
				Loggers.Elemtl.trace("XML Response from " + url + " " + new String(result.getBytes()));
			}
			IOUtils.closeQuietly(is);
			
			int status = connection.getResponseCode();
			if (status == 401) {
				throw new IOException("Bad creditentials for " + url.toString());
			} else if (status == 404) {
				throw new IOException("Not found " + url.toString());
			} else if (status != 200) {
				Loggers.Elemtl.warn("Unknow status (" + status + ") for " + url.toString());
			}
			
		} catch (Exception e) {
			Loggers.Elemtl.error(e);
		} finally {
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception e2) {
				}
			}
		}
		
		return result;
	}
	
	public Calendar getEventStartupTime(int event_id) throws IOException {
		XmlData rq = XmlData.createEmptyDocument();
		Document document = rq.getDocument();
		
		Element cue_point = document.createElement("cue_point");
		Element g_c_t = document.createElement("get_current_time");
		g_c_t.setTextContent("1");
		cue_point.appendChild(g_c_t);
		document.appendChild(cue_point);
		
		XmlData rp = request(rq, "/api/live_events/" + event_id + "/cue_point");
		
		if (rp == null) {
			throw new IOException("Can't get offset");
		}
		
		Element splice_time = XmlData.getElementByName(rp.getDocumentElement(), "splice_time");
		int hours = Integer.parseInt(XmlData.getElementByName(splice_time, "hours").getTextContent());
		int minutes = Integer.parseInt(XmlData.getElementByName(splice_time, "minutes").getTextContent());
		int seconds = Integer.parseInt(XmlData.getElementByName(splice_time, "seconds").getTextContent());
		int frames = Integer.parseInt(XmlData.getElementByName(splice_time, "frames").getTextContent());
		
		int msec = Math.round(((float) frames / VIDEO_SYSTEM_FPS) * 1000f);
		
		Calendar c = Calendar.getInstance();
		c.add(Calendar.HOUR, -hours);
		c.add(Calendar.MINUTE, -minutes);
		c.add(Calendar.SECOND, -seconds);
		c.add(Calendar.MILLISECOND, -msec);
		
		return c;
	}
	
	public void createCuePoint(long date, int duration_time_sec, int event_id, int cue_point_id) throws IOException {
		if (System.currentTimeMillis() > date) {
			throw new NullPointerException("Startup date is in the past: " + new Date(date));
		}
		if (duration_time_sec < 1) {
			throw new NullPointerException("Duration is too short: " + duration_time_sec);
		}
		
		XmlData rq = XmlData.createEmptyDocument();
		Document document = rq.getDocument();
		
		Element cue_point = document.createElement("cue_point");
		
		rq.setTextContent(cue_point, "event_id", String.valueOf(cue_point_id));
		
		Element splice_time = document.createElement("splice_time");
		
		long event_startup = getEventStartupTime(event_id).getTimeInMillis();
		
		int hours = (int) (((date - event_startup) / (1000 * 60 * 60)));
		int minutes = (int) (((date - event_startup) / (1000 * 60)) % 60);
		int seconds = (int) ((date - event_startup) / 1000) % 60;
		
		rq.setTextContent(splice_time, "hours", String.valueOf(hours));
		rq.setTextContent(splice_time, "minutes", String.valueOf(minutes));
		rq.setTextContent(splice_time, "seconds", String.valueOf(seconds));
		rq.setTextContent(splice_time, "frames", "0");
		
		cue_point.appendChild(splice_time);
		
		rq.setTextContent(cue_point, "duration", String.valueOf(duration_time_sec));
		
		document.appendChild(cue_point);
		
		XmlData rp = request(rq, "/api/live_events/" + event_id + "/cue_point");
		
		if (Loggers.Elemtl.isDebugEnabled()) {
			Loggers.Elemtl.debug("Ok: " + new String(rp.getBytes()));
		}
	}
	
	public void removeCuePoint(int event_id, int cue_point_id) throws IOException {
		XmlData rq = XmlData.createEmptyDocument();
		Document document = rq.getDocument();
		
		Element cue_point = document.createElement("cue_point");
		rq.setTextContent(cue_point, "cancel_event_id", String.valueOf(cue_point_id));
		document.appendChild(cue_point);
		
		XmlData rp = request(rq, "/api/live_events/" + event_id + "/cue_point");
		
		if (Loggers.Elemtl.isDebugEnabled()) {
			Loggers.Elemtl.debug("Ok: " + new String(rp.getBytes()));
		}
	}
	
}
