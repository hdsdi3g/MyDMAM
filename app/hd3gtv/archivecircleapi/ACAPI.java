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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.archivecircleapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthenticationException;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.GsonIgnoreStrategy;

public class ACAPI {
	
	static final Logger log = Logger.getLogger(ACAPI.class);
	
	private static final String BASE_URL = "/acapi/1.0/";
	private String host;
	private String basic_auth;
	private int tcp_port = 8081;
	
	final Gson gson_simple;
	final Gson gson;
	
	public ACAPI(String host, String user, String password) {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		MyDMAM.registerBaseSerializers(builder);
		gson_simple = builder.create();
		
		builder.registerTypeAdapter(ACNode.class, new ACNode.Deseralizer(this));
		builder.registerTypeAdapter(ACNodesEntry.class, new ACNodesEntry.Deseralizer(this));
		builder.registerTypeAdapter(ACFile.class, new ACFile.Deseralizer(this));
		builder.registerTypeAdapter(ACPositionType.class, new ACPositionType.Deseralizer(this));
		builder.registerTypeAdapter(ACFileLocations.class, new ACFileLocations.Deseralizer(this));
		builder.registerTypeAdapter(ACFileLocationCache.class, new ACFileLocationCache.Deseralizer(this));
		builder.registerTypeAdapter(ACFileLocationPack.class, new ACFileLocationPack.Deseralizer(this));
		builder.registerTypeAdapter(ACFileLocationTape.class, new ACFileLocationTape.Deseralizer(this));
		
		gson = builder.create();
		
		this.host = host;
		if (host == null) {
			throw new NullPointerException("\"host\" can't to be null");
		}
		if (user == null) {
			throw new NullPointerException("\"user\" can't to be null");
		}
		if (password == null) {
			throw new NullPointerException("\"password\" can't to be null");
		}
		
		basic_auth = "Basic " + new String(new Base64().encode((user + ":" + password).getBytes()));
	}
	
	public void setTcp_port(int tcp_port) throws IndexOutOfBoundsException {
		if (tcp_port < 1 | tcp_port > 65535) {
			throw new IndexOutOfBoundsException("Invalid TCP port: " + tcp_port);
		}
		this.tcp_port = tcp_port;
	}
	
	/**
	 * @return null if error
	 */
	private <T extends ACAPIResult> T request(String query, Class<T> return_class, LinkedHashMap<String, String> query_strings) {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		if (return_class == null) {
			throw new NullPointerException("\"return_class\" can't to be null");
		}
		if (query_strings == null) {
			throw new NullPointerException("\"query_strings\" can't to be null");
		}
		
		HttpURLConnection connection = null;
		T result = null;
		try {
			StringBuilder full_query = new StringBuilder(BASE_URL);
			full_query.append(query);
			
			if (log.isTraceEnabled()) {
				query_strings.put("pretty", "true");
			}
			
			if (query_strings.isEmpty() == false) {
				full_query.append("?");
				query_strings.forEach((k, v) -> {
					try {
						full_query.append(URLEncoder.encode(k, "UTF-8"));
						full_query.append("=");
						full_query.append(URLEncoder.encode(v, "UTF-8"));
						full_query.append("&");
					} catch (Exception e) {
					}
				});
			}
			
			if (full_query.toString().endsWith("&")) {
				full_query.deleteCharAt(full_query.length() - 1);
			}
			
			URL url = new URL("http", host, tcp_port, full_query.toString());
			if (log.isTraceEnabled()) {
				log.trace("HTTP Request: " + url.toString());
			}
			
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			// connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			// connection.setRequestProperty("Content-Language", "en-US");
			connection.setUseCaches(false);
			connection.setConnectTimeout(30);
			connection.setRequestProperty("Authorization", basic_auth);
			
			int status = connection.getResponseCode();
			if (status == 401) {
				throw new AuthenticationException("Bad creditentials for " + url.toString());
			} else if (status != 200) {
				throw new IOException("Unknow status (" + status + ") for " + url.toString());
			}
			
			String content_type = connection.getContentType();
			if (content_type.toLowerCase().equals("application/json;charset=utf-8") == false) {
				throw new IOException("Unknow content type (" + content_type + ") for " + url.toString());
			}
			
			// connection.setDoOutput(true);
			// Send request
			/*DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.close();*/
			
			InputStream is = connection.getInputStream();
			
			if (log.isTraceEnabled()) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) != -1) {
					baos.write(buffer, 0, length);
				}
				String json_raw = baos.toString("UTF-8");
				log.trace("HTTP Response: " + json_raw);
				result = gson.fromJson(json_raw, return_class);
				IOUtils.closeQuietly(is);
			} else {
				InputStreamReader isr = new InputStreamReader(is);
				result = gson.fromJson(isr, return_class);
				IOUtils.closeQuietly(isr);
			}
		} catch (Exception e) {
			log.error(e);
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
	
	public ACNode getNode() {
		return request("node", ACNode.class, new LinkedHashMap<>());
	}
	
	public ACFile getFile(String share, String path, boolean show_location_directories) {
		StringBuilder sb = new StringBuilder();
		sb.append("file/");
		sb.append(share);
		
		if (path.startsWith("/")) {
			sb.append(path);
		} else {
			sb.append("/");
			sb.append(path);
		}
		
		LinkedHashMap<String, String> args = new LinkedHashMap<>(1);
		if (show_location_directories) {
			args.put("showLocation", "true");
		}
		
		return request(sb.toString(), ACFile.class, args);
	}
}
