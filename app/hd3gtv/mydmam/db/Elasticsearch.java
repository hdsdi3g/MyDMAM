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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.db;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@SuppressWarnings("unchecked")
public class Elasticsearch {
	
	private static InetSocketTransportAddress[] transportadresses;
	public static final char[] forbidden_query_chars = { '/', '\\', '%', '[', ']', '(', ')', '{', '}', '"', '~' };
	private static TransportClient client;
	
	public static void refeshconfiguration() {
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				Log2.log.error("Can't close properly client", e);
			}
		}
		try {
			if (Configuration.global.isElementExists("elasticsearch") == false) {
				throw new Exception("No configuration found");
			}
			
			String clustername = Configuration.global.getValue("elasticsearch", "clustername", null);
			List<ConfigurationClusterItem> clusterservers = Configuration.global.getClusterConfiguration("elasticsearch", "transport", "127.0.0.1", 9300);
			Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clustername).build();
			
			Log2Dump dump = new Log2Dump();
			dump.add("clustername", clustername);
			
			transportadresses = new InetSocketTransportAddress[clusterservers.size()];
			for (int pos = 0; pos < clusterservers.size(); pos++) {
				transportadresses[pos] = new InetSocketTransportAddress(clusterservers.get(pos).address, clusterservers.get(pos).port);
				dump.addAll(clusterservers.get(pos));
			}
			
			client = new TransportClient(settings).addTransportAddresses(transportadresses);
			
			dump.addAll(getDump());
			
			Log2.log.info("Client configuration", dump);
		} catch (Exception e) {
			Log2.log.error("Can't load client configuration", e);
			try {
				client.close();
			} catch (Exception e1) {
				Log2.log.error("Can't close client", e1);
			}
		}
	}
	
	/**
	 * @return client Don't close it !
	 */
	public static synchronized TransportClient getClient() {
		if (client == null) {
			refeshconfiguration();
		} else if (client.connectedNodes().isEmpty()) {
			refeshconfiguration();
		}
		if (client.connectedNodes().isEmpty()) {
			Log2.log.error("Can't maintain connection with the database", null);
		}
		return client;
	}
	
	public static Log2Dump getDump() {
		Log2Dump dump = new Log2Dump();
		ClusterStateResponse csr = client.admin().cluster().prepareState().execute().actionGet();
		dump.add("get-clustername", csr.getClusterName().toString());
		return dump;
	}
	
	public static void deleteIndexRequest(String index_name) throws ElasticsearchException {
		try {
			client.admin().indices().delete(new DeleteIndexRequest(index_name)).actionGet();
		} catch (IndexMissingException e) {
		}
	}
	
	/**
	 * Do all checks, test if exists, and if has result
	 * @return null if no/impossible result
	 */
	public static JSONObject getJSONFromSimpleResponse(GetResponse response) {
		if (response == null) {
			return null;
		}
		if (response.isExists() == false) {
			return null;
		}
		JSONParser jp = new JSONParser();
		
		try {
			Object o = jp.parse(response.getSourceAsString());
			if (o instanceof JSONObject) {
				return (JSONObject) o;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Do all checks, test if exists, and if has result
	 * @return null if no/impossible result
	 */
	public static JSONObject getJSONFromSimpleResponse(SearchHit hit) {
		if (hit == null) {
			return null;
		}
		JSONParser jp = new JSONParser();
		
		try {
			Object o = jp.parse(hit.getSourceAsString());
			if (o instanceof JSONObject) {
				return (JSONObject) o;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public static void enableTTL(String index_name, String type) throws IOException, ParseException {
		if (client.admin().indices().exists(new IndicesExistsRequest(index_name)).actionGet().isExists() == false) {
			return;
		}
		
		StringBuffer sb_url = new StringBuffer();
		sb_url.append("http://");
		sb_url.append(transportadresses[0].address().getHostName());
		sb_url.append(":9200");
		sb_url.append("/");
		sb_url.append(index_name);
		sb_url.append("/");
		sb_url.append(type);
		sb_url.append("/_mapping?pretty=true");
		
		URL url = new URL(sb_url.toString());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		
		InputStream isr = connection.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		int len;
		byte[] buffer = new byte[0xFFF];
		while ((len = isr.read(buffer)) > 0) {
			baos.write(buffer, 0, len);
		}
		connection.disconnect();
		
		JSONParser jsonparser = new JSONParser();
		JSONObject jo_root = (JSONObject) jsonparser.parse(new String(baos.toByteArray()));
		JSONObject jo_index = (JSONObject) jo_root.get(index_name);
		JSONObject jo_mappings = (JSONObject) jo_index.get("mappings");
		JSONObject jo_type = (JSONObject) jo_mappings.get(type);
		if (jo_type.containsKey("_ttl")) {
			JSONObject jo_ttl = (JSONObject) jo_type.get("_ttl");
			boolean isenabledvalue = (Boolean) jo_ttl.get("enabled");
			if (isenabledvalue) {
				return;
			}
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("index_name", index_name);
		dump.add("type", type);
		Log2.log.info("Activate TTL on ES", dump);
		
		connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("PUT");
		connection.setDoOutput(true);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		
		JSONObject jo_enabled = new JSONObject();
		jo_enabled.put("enabled", true);
		
		JSONObject jo_ttl = new JSONObject();
		jo_ttl.put("_ttl", jo_enabled);
		
		JSONObject request = new JSONObject();
		request.put(type, jo_ttl);
		
		OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
		osw.write(request.toJSONString());
		osw.flush();
		osw.close();
		
		if (connection.getResponseCode() != 200) {
			throw new IOException("Bad response from ES node : " + sb_url.toString());
		}
		
	}
	
	/*
	CloseIndexRequest cir = new CloseIndexRequest("pathindex");
	CloseIndexResponse ciresp = client.admin().indices().close(cir).actionGet();
	System.out.println(ciresp.isAcknowledged());
	
	UpdateSettingsRequest usr = new UpdateSettingsRequest("pathindex");
	...
	UpdateSettingsResponse usresp = client.admin().indices().updateSettings(usr).actionGet();
	System.out.println(usresp.toString());
	
	OpenIndexRequest oir = new OpenIndexRequest("pathindex");
	OpenIndexResponse oiresp = client.admin().indices().open(oir).actionGet();
	System.out.println(oiresp.isAcknowledged());
	
	ClusterStateRequest csr = new ClusterStateRequest();
	ClusterStateResponse csresp = client.admin().cluster().state(csr).actionGet();
	System.out.println(csresp.getState().getMetaData().index("pathindex").getSettings().getAsBoolean("index.ttl.enabled", false));
	
	CreateIndexRequest cri = new CreateIndexRequest(index);
	System.out.println(client.admin().indices().create(cri).actionGet().isAcknowledged());
	*/
	
	public static ElastisearchCrawlerReader createCrawlerReader() {
		return new ElastisearchCrawlerReader(client);
	}
	
	public static ElastisearchMultipleCrawlerReader createMultipleCrawlerReader() {
		return new ElastisearchMultipleCrawlerReader(client);
	}
	
}
