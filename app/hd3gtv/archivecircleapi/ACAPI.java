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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.http.auth.AuthenticationException;
import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.gson.GsonKit;

public class ACAPI {
	
	static final Logger log = Logger.getLogger(ACAPI.class);
	private static boolean gson_registed = false;
	public static final int OLD_TRANSFERT_JOBS_RETENTION_DAYS = 30;
	
	private static final String BASE_URL = "/acapi/1.0/";
	private String host;
	private String basic_auth;
	private int tcp_port = 8081;
	
	public ACAPI(String host, String user, String password) {
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
		
		if (gson_registed == false) {
			gson_registed = true;
			ACAPI.registerGson(MyDMAM.gson_kit);
		}
	}
	
	public void setTcp_port(int tcp_port) throws IndexOutOfBoundsException {
		if (tcp_port < 1 | tcp_port > 65535) {
			throw new IndexOutOfBoundsException("Invalid TCP port: " + tcp_port);
		}
		this.tcp_port = tcp_port;
	}
	
	/**
	 * @param query without "/acapi/1.0/"
	 * @return null if error
	 */
	private <T> T request(String method, String query, Class<T> return_class, LinkedHashMap<String, String> query_strings, JsonObject post_playload) {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		if (query_strings == null) {
			query_strings = new LinkedHashMap<>();
		}
		
		HttpURLConnection connection = null;
		T result = null;
		OutputStream so_out = null;
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
			connection.setRequestMethod(method.toUpperCase());
			// connection.setRequestProperty("Content-Language", "en-US");
			// connection.setUseCaches(false);
			connection.setConnectTimeout(30);
			connection.setRequestProperty("Authorization", basic_auth);
			
			if (post_playload != null) {
				byte[] b_post_playload = post_playload.toString().getBytes(MyDMAM.UTF8);
				connection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
				connection.setRequestProperty("Content-Length", Integer.toString(b_post_playload.length));
				connection.setDoOutput(true);
				so_out = connection.getOutputStream();
				so_out.write(b_post_playload);
			}
			
			int status = connection.getResponseCode();
			if (status == 401) {
				throw new AuthenticationException("Bad creditentials for " + method + " " + url.toString());
			} else if (status == 404) {
				throw new IOException("Not Found " + method + " " + url.toString());
			} else if (status >= 400) {
				InputStream is = connection.getErrorStream();
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int length;
				while ((length = is.read(buffer)) != -1) {
					baos.write(buffer, 0, length);
				}
				IOUtils.closeQuietly(is);
				
				if (status == 400) {
					throw new IOException("Invalid request for " + method + " " + url.toString() + ": " + baos.toString("UTF-8"));
				} else if (status == 403) {
					throw new IOException("Forbidden " + method + " " + url.toString() + ": " + baos.toString("UTF-8"));
				} else if (status > 399) {
					log.warn("Invalid status: " + status + " for " + url.toString() + ": " + baos.toString("UTF-8"));
				}
			}
			
			if (return_class != null) {
				/*String content_type = connection.getContentType();
				if (content_type.toLowerCase().equals("application/json;charset=utf-8") == false) { // text/html;charset=utf-8
					throw new IOException("Unknow content type (" + content_type + ") for " + url.toString());
				}*/
				
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
			}
		} catch (Exception e) {
			log.error("Invalid error", e);
		} finally {
			if (so_out != null) {
				IOUtils.closeQuietly(so_out);
			}
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception e2) {
				}
			}
		}
		
		if (so_out != null) {
			IOUtils.closeQuietly(so_out);
		}
		
		return result;
	}
	
	public ACNode getNode() {
		return request("GET", "node", ACNode.class, new LinkedHashMap<>(), null);
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
		
		return request("GET", sb.toString(), ACFile.class, args, null);
	}
	
	/**
	 * @return null if the file is in the cache.
	 */
	public ACTransferJob destage(ACFile ac_file, String external_id, boolean urgent, String destination_node) {
		if (ac_file.bestLocation == ACLocationType.CACHE) {
			return null;
		}
		
		JsonObject body = new JsonObject();
		body.addProperty("type", "RestoreJob");
		body.addProperty("share", ac_file.share);
		if (urgent) {
			body.addProperty("priority", 100);
		} else {
			body.addProperty("priority", 0);
		}
		body.addProperty("node", destination_node);
		body.addProperty("workflow", "DESTAGE");
		
		JsonArray files = new JsonArray();
		JsonObject file = new JsonObject();
		file.addProperty("src", "/" + ac_file.share + "/" + ac_file.path);
		file.addProperty("dst", "/");
		if (external_id != null) {
			file.addProperty("externalId", external_id);
		}
		files.add(file);
		body.add("files", files);
		
		return request("POST", "transferJob", ACTransferJob.class, null, body);
	}
	
	public ACTransferJob getTransfertJob(int job_id) {
		LinkedHashMap<String, String> args = new LinkedHashMap<>();
		args.put("max", "10000");
		args.put("offset", "0");
		args.put("sort", "id");
		args.put("order", "asc");
		args.put("showCounters", "true");
		return request("GET", "transferJob/" + job_id, ACTransferJob.class, args, null);
	}
	
	/**
	 * @return don't return file details
	 */
	public ArrayList<ACTransferJob> getAllTransfertsJobs(boolean dont_purge_old) {
		LinkedHashMap<String, String> args = new LinkedHashMap<>();
		args.put("max", "100");
		args.put("offset", "0");
		args.put("sort", "id");
		args.put("order", "asc");
		args.put("showCounters", "true");
		JsonObject response = request("GET", "transferJob", JsonObject.class, args, null);
		
		ArrayList<ACTransferJob> all_transferts_jobs = new ArrayList<>();
		
		response.get("transferJobs").getAsJsonArray().forEach(t_job -> {
			all_transferts_jobs.add(MyDMAM.gson_kit.getGson().fromJson(t_job, ACTransferJob.class));
		});
		
		int count_total = response.get("count").getAsInt() - response.get("size").getAsInt();
		while (count_total > 0) {
			args.put("offset", String.valueOf(Integer.parseInt(args.get("offset")) + 100));
			response = request("GET", "transferJob", JsonObject.class, args, null);
			count_total = count_total - response.get("size").getAsInt();
			
			response.get("transferJobs").getAsJsonArray().forEach(t_job -> {
				all_transferts_jobs.add(MyDMAM.gson_kit.getGson().fromJson(t_job, ACTransferJob.class));
			});
		}
		if (dont_purge_old == false) {
			/**
			 * Search old jobs
			 */
			List<ACTransferJob> old_jobs = all_transferts_jobs.stream().filter(j -> {
				return j.lastUpdated < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(OLD_TRANSFERT_JOBS_RETENTION_DAYS);
			}).collect(Collectors.toList());
			
			/**
			 * Remove old jobs from current list
			 */
			all_transferts_jobs.removeIf(j -> {
				return old_jobs.stream().anyMatch(old -> {
					return j.id == old.id;
				});
			});
			
			old_jobs.stream().filter(old -> {
				return old.running == false;
			}).forEach(old -> {
				log.warn("Remove old transfert job: " + old);
				deleteTransfertJob(old.id);
			});
		}
		
		return all_transferts_jobs;
	}
	
	/**
	 * Don't try to remove running job...
	 * @param job_id
	 */
	public void deleteTransfertJob(int job_id) {
		request("DELETE", "transferJob/" + job_id, null, null, null);
	}
	
	// TODO tape service >> mailbox, refresh, enter, out, position
	
	public static ACAPI loadFromConfiguration() {
		String host = Configuration.global.getValue("acapi", "host", "");
		if (host.equals("")) {
			return null;
		}
		
		String user = Configuration.global.getValue("acapi", "user", "");
		String password = Configuration.global.getValue("acapi", "password", "");
		int port = Configuration.global.getValue("acapi", "port", 8081);
		
		ACAPI acapi = new ACAPI(host, user, password);
		acapi.setTcp_port(port);
		
		return acapi;
	}
	
	private static void registerGson(GsonKit gson_kit) {
		gson_kit.registerDeserializer(ACNode.class, ACNode.class, json -> {
			ACNode node = gson_kit.getGsonSimple().fromJson(json, ACNode.class);
			node.nodes = gson_kit.getGson().fromJson(json.getAsJsonObject().get("nodes"), GsonKit.type_ArrayList_ACNodesEntry);
			return node;
		});
		
		gson_kit.registerDeserializer(ACNodesEntry.class, ACNodesEntry.class, json -> {
			ACNodesEntry nodes = gson_kit.getGsonSimple().fromJson(json, ACNodesEntry.class);
			nodes.ipAddresses = gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("ipAddresses"), GsonKit.type_ArrayList_InetAddr);
			return nodes;
		});
		
		gson_kit.registerDeserializer(ACFile.class, ACFile.class, json -> {
			ACFile node = gson_kit.getGsonSimple().fromJson(json, ACFile.class);
			JsonObject jo = json.getAsJsonObject();
			if (node.type == ACFileType.directory) {
				node.files = gson_kit.getGsonSimple().fromJson(jo.get("files"), GsonKit.type_ArrayList_String);
				node.sub_locations = gson_kit.getGson().fromJson(jo.get("locations"), ACItemLocations.class);
			} else if (node.type == ACFileType.file) {
				node.this_locations = gson_kit.getGson().fromJson(jo.get("locations"), GsonKit.type_ArrayList_ACFileLocations);
			} else {
				throw new NullPointerException("node");
			}
			return node;
		});
		
		gson_kit.registerDeserializer(ACPositionType.class, ACPositionType.class, json -> {
			ACPositionType pt = new ACPositionType();
			JsonObject jo = json.getAsJsonObject();
			
			pt.cache = gson_kit.getGsonSimple().fromJson(jo.get("cache"), GsonKit.type_ArrayList_String);
			pt.disk = gson_kit.getGsonSimple().fromJson(jo.get("disk"), GsonKit.type_ArrayList_String);
			pt.nearline = gson_kit.getGsonSimple().fromJson(jo.get("nearline"), GsonKit.type_ArrayList_String);
			pt.offline = gson_kit.getGsonSimple().fromJson(jo.get("offline"), GsonKit.type_ArrayList_String);
			return pt;
		});
		
		gson_kit.registerDeserializer(ACFileLocations.class, ACFileLocations.class, json -> {
			JsonObject jo = json.getAsJsonObject();
			String type = jo.get("type").getAsString();
			
			if (type.equalsIgnoreCase("CACHE")) {
				return gson_kit.getGson().fromJson(json, ACFileLocationCache.class);
			} else if (type.equalsIgnoreCase("PACK")) {
				return gson_kit.getGson().fromJson(json, ACFileLocationPack.class);
			} else if (type.equalsIgnoreCase("TAPE")) {
				return gson_kit.getGson().fromJson(json, ACFileLocationTape.class);
			} else {
				throw new JsonParseException("Unknow type: " + type);
			}
		});
		
		gson_kit.registerDeserializer(ACFileLocationCache.class, ACFileLocationCache.class, json -> {
			ACFileLocationCache location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationCache.class);
			location.nodes = gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("nodes"), GsonKit.type_ArrayList_String);
			return location;
		});
		
		gson_kit.registerDeserializer(ACFileLocationPack.class, ACFileLocationPack.class, json -> {
			ACFileLocationPack location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationPack.class);
			location.partitions = gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("partitions"), GsonKit.type_ArrayList_ACPartition);
			return location;
		});
		
		gson_kit.registerDeserializer(ACFileLocationTape.class, ACFileLocationTape.class, json -> {
			ACFileLocationTape location = MyDMAM.gson_kit.getGsonSimple().fromJson(json, ACFileLocationTape.class);
			location.tapes = gson_kit.getGsonSimple().fromJson(json.getAsJsonObject().get("tapes"), GsonKit.type_ArrayList_ACTape);
			return location;
		});
	}
	
}
