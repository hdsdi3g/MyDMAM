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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class ACAPI {
	
	static final Logger log = Logger.getLogger(ACAPI.class);
	
	private static final String BASE_URL = "/acapi/1.0/";
	private String host;
	private String basic_auth;
	private int tcp_port = 8081;
	
	public ACAPI(String host, String user, String password) {
		MyDMAM.gson_kit.registerDeserializer(ACNode.class, ACNode.class, json -> {
			ACNode node = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACNode.class);
			node.nodes = MyDMAM.gson_kit.getGson().fromJson(json.getAsJsonObject().get("nodes"), GsonKit.type_ArrayList_ACNodesEntry);
			return node;
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACNodesEntry.class, ACNodesEntry.class, json -> {
			ACNodesEntry nodes = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACNodesEntry.class);
			nodes.ipAddresses = MyDMAM.gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("ipAddresses"), GsonKit.type_ArrayList_InetAddr);
			return nodes;
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACFile.class, ACFile.class, json -> {
			ACFile node = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFile.class);
			JsonObject jo = json.getAsJsonObject();
			if (node.type == ACFileType.directory) {
				node.files = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("files"), GsonKit.type_ArrayList_String);
				node.sub_locations = MyDMAM.gson_kit.getGson().fromJson(jo.get("locations"), ACItemLocations.class);
			} else if (node.type == ACFileType.file) {
				node.this_locations = MyDMAM.gson_kit.getGson().fromJson(jo.get("locations"), GsonKit.type_ArrayList_ACFileLocations);
			} else {
				throw new NullPointerException("node");
			}
			return node;
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACPositionType.class, ACPositionType.class, json -> {
			ACPositionType pt = new ACPositionType();
			JsonObject jo = json.getAsJsonObject();
			
			pt.cache = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("cache"), GsonKit.type_ArrayList_String);
			pt.disk = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("disk"), GsonKit.type_ArrayList_String);
			pt.nearline = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("nearline"), GsonKit.type_ArrayList_String);
			pt.offline = MyDMAM.gson_kit.getGsonSimple().fromJson(jo.get("offline"), GsonKit.type_ArrayList_String);
			return pt;
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACFileLocations.class, ACFileLocations.class, json -> {
			JsonObject jo = json.getAsJsonObject();
			String type = jo.get("type").getAsString();
			
			if (type.equalsIgnoreCase("CACHE")) {
				return MyDMAM.gson_kit.getGson().fromJson(json, ACFileLocationCache.class);
			} else if (type.equalsIgnoreCase("PACK")) {
				return MyDMAM.gson_kit.getGson().fromJson(json, ACFileLocationPack.class);
			} else if (type.equalsIgnoreCase("TAPE")) {
				return MyDMAM.gson_kit.getGson().fromJson(json, ACFileLocationTape.class);
			} else {
				throw new JsonParseException("Unknow type: " + type);
			}
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACFileLocationCache.class, ACFileLocationCache.class, json -> {
			ACFileLocationCache location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationCache.class);
			location.nodes = MyDMAM.gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("nodes"), GsonKit.type_ArrayList_String);
			return location;
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACFileLocationPack.class, ACFileLocationPack.class, json -> {
			ACFileLocationPack location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationPack.class);
			location.partitions = MyDMAM.gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("partitions"), GsonKit.type_ArrayList_ACPartition);
			return location;
		});
		
		MyDMAM.gson_kit.registerDeserializer(ACFileLocationTape.class, ACFileLocationTape.class, json -> {
			ACFileLocationTape location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationTape.class);
			location.tapes = MyDMAM.gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("tapes"), GsonKit.type_ArrayList_ACTape);
			return location;
		});
		
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
			
			String[] query_dirs = query.split("/");
			for (int pos_q = 0; pos_q < query_dirs.length; pos_q++) {
				if (pos_q > 0) {
					full_query.append("/");
				}
				full_query.append(URLEncoder.encode(query_dirs[pos_q], "UTF-8"));
			}
			
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
				log.trace("full_query  : " + full_query.toString());
				log.trace("HTTP Request: " + url.toString());
			}
			
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			// connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			// connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			// connection.setRequestProperty("Content-Language", "en-US");
			// connection.setUseCaches(false);
			connection.setConnectTimeout(30);
			connection.setRequestProperty("Authorization", basic_auth);
			
			int status = connection.getResponseCode();
			if (status == 401) {
				throw new AuthenticationException("Bad creditentials for " + url.toString());
			} else if (status != 200) {
				// throw new IOException("Unknow status (" + status + ") for " + url.toString());
			}
			
			log.trace("status: " + status);
			
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
				result = MyDMAM.gson_kit.getGson().fromJson(json_raw, return_class);
				IOUtils.closeQuietly(is);
			} else {
				InputStreamReader isr = new InputStreamReader(is);
				result = MyDMAM.gson_kit.getGson().fromJson(isr, return_class);
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
		
		if (path.equals("/") == false) {
			if (path.startsWith("//")) {
				sb.append(path.substring(1));
			} else if (path.startsWith("/")) {
				sb.append(path);
			} else {
				sb.append("/");
				sb.append(path);
			}
		}
		
		LinkedHashMap<String, String> args = new LinkedHashMap<>(1);
		if (show_location_directories) {
			args.put("showLocation", "true");
		}
		
		if (log.isTraceEnabled()) {
			LinkedHashMap<String, Object> lhm_log = new LinkedHashMap<String, Object>();
			lhm_log.put("share", share);
			lhm_log.put("path", path);
			lhm_log.put("show_location_directories", show_location_directories);
			lhm_log.put("URL part", sb.toString());
			log.trace("Request dump " + lhm_log);
		}
		
		return request(sb.toString(), ACFile.class, args);
	}
}
