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

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Elasticsearch {
	
	private static Settings settings;
	private static InetSocketTransportAddress[] transportadresses;
	public static final char[] forbidden_query_chars = { '/', '\\', '%', '[', ']', '(', ')', '{', '}', '"', '~' };
	
	public static void refeshconfiguration() {
		try {
			if (Configuration.global.isElementExists("elasticsearch") == false) {
				throw new Exception("No ElasticSearch configuration found");
			}
			
			String clustername = Configuration.global.getValue("elasticsearch", "clustername", null);
			List<ConfigurationClusterItem> clusterservers = Configuration.global.getClusterConfiguration("elasticsearch", "transport", "127.0.0.1", 9300);
			settings = ImmutableSettings.settingsBuilder().put("cluster.name", clustername).build();
			
			Log2Dump dump = new Log2Dump();
			dump.add("clustername", clustername);
			
			transportadresses = new InetSocketTransportAddress[clusterservers.size()];
			for (int pos = 0; pos < clusterservers.size(); pos++) {
				transportadresses[pos] = new InetSocketTransportAddress(clusterservers.get(pos).address, clusterservers.get(pos).port);
				dump.addAll(clusterservers.get(pos));
			}
			
			dump.addAll(getDump());
			
			Log2.log.info("ElasticSearch client configuration", dump);
		} catch (Exception e) {
			Log2.log.error("Can't load Elasticsearch client configuration", e);
		}
	}
	
	/**
	 * Don't forget to close() it !
	 */
	public static Client createClient() {
		if (transportadresses == null) {
			refeshconfiguration();
		}
		Client client = new TransportClient(settings).addTransportAddresses(transportadresses);
		return client;
	}
	
	public static Log2Dump getDump() {
		Log2Dump dump = new Log2Dump();
		Client client = createClient();
		ClusterStateResponse csr = client.admin().cluster().prepareState().execute().actionGet();
		dump.add("get-clustername", csr.getClusterName().toString());
		client.close();
		return dump;
	}
	
	public static void deleteIndexRequest(String index_name) throws ElasticSearchException {
		try {
			Client client = createClient();
			client.admin().indices().delete(new DeleteIndexRequest(index_name)).actionGet();
			client.close();
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
	
	public static void enableTTL(Client client, String index_name, String type) throws IOException, ParseException {
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
		JSONObject jo_type = (JSONObject) jo_root.get(type);
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
	
}
