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
import hd3gtv.mydmam.db.status.ElasticsearchStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
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
	
	/**
	 * @see ElasticsearchStatus
	 */
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
			ImmutableSettings.Builder settings = ImmutableSettings.builder();
			settings.put("cluster.name", clustername);
			
			Log2Dump dump = new Log2Dump();
			dump.add("clustername", clustername);
			
			transportadresses = new InetSocketTransportAddress[clusterservers.size()];
			for (int pos = 0; pos < clusterservers.size(); pos++) {
				transportadresses[pos] = new InetSocketTransportAddress(clusterservers.get(pos).address, clusterservers.get(pos).port);
				dump.addAll(clusterservers.get(pos));
			}
			client = new TransportClient(settings.build()).addTransportAddresses(transportadresses);
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
		}
		return client;
	}
	
	public static Log2Dump getDump() {
		Log2Dump dump = new Log2Dump();
		if (client != null) {
			ClusterStateResponse csr = client.admin().cluster().prepareState().execute().actionGet();
			dump.add("get-clustername", csr.getClusterName().toString());
		} else {
			dump.add("get-clustername", "<disconnected>");
		}
		return dump;
	}
	
	/**
	 * Protected to some IndexMissingException
	 */
	public static void deleteIndexRequest(String index_name) throws ElasticsearchException {
		try {
			getClient().admin().indices().delete(new DeleteIndexRequest(index_name)).actionGet();
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
	
	public static void enableTTL(String index_name, String type) throws IOException {
		if (isIndexExists(index_name) == false) {
			/**
			 * No index, no enable ttl.
			 */
			return;
		}
		GetMappingsRequest request = new GetMappingsRequest().indices(index_name);
		ImmutableOpenMap<String, ImmutableOpenMap<String, MappingMetaData>> global_mapping = getClient().admin().indices().getMappings(request).actionGet().getMappings();
		
		if (global_mapping.containsKey(index_name)) {
			ImmutableOpenMap<String, MappingMetaData> mapping = global_mapping.get(index_name);
			if (mapping.containsKey(type)) {
				Map<String, Object> mapping_mtd = ((MappingMetaData) mapping.get(type)).getSourceAsMap();
				if (mapping_mtd.containsKey("_ttl")) {
					Map<String, Object> mapping_ttl = (Map<String, Object>) mapping_mtd.get("_ttl");
					if (mapping_ttl.containsKey("enabled")) {
						if ((Boolean) mapping_ttl.get("enabled") == true) {
							return;
						}
					}
				}
			} else {
				/**
				 * No actual mapping for this type in index, no enable ttl.
				 */
				return;
			}
		}
		addMappingToIndex(index_name, type, "{\"_ttl\": {\"enabled\": true}}");
	}
	
	/*
	CloseIndexRequest cir = new CloseIndexRequest("pathindex");
	CloseIndexResponse ciresp = client.admin().indices().close(cir).actionGet();
	System.out.println(ciresp.isAcknowledged());
	
	OpenIndexRequest oir = new OpenIndexRequest("pathindex");
	OpenIndexResponse oiresp = client.admin().indices().open(oir).actionGet();
	System.out.println(oiresp.isAcknowledged());
	
	ClusterStateRequest csr = new ClusterStateRequest();
	ClusterStateResponse csresp = client.admin().cluster().state(csr).actionGet();
	System.out.println(csresp.getState().getMetaData().index("pathindex").getSettings().getAsBoolean("index.ttl.enabled", false));
	*/
	
	public static ElastisearchCrawlerReader createCrawlerReader() {
		return new ElastisearchCrawlerReader(getClient());
	}
	
	public static ElastisearchMultipleCrawlerReader createMultipleCrawlerReader() {
		return new ElastisearchMultipleCrawlerReader(getClient());
	}
	
	public static boolean isIndexExists(String index_name) {
		return getClient().admin().indices().exists(new IndicesExistsRequest(index_name)).actionGet().isExists();
	}
	
	public static boolean createIndex(String index_name) {
		return getClient().admin().indices().prepareCreate(index_name).execute().actionGet().isAcknowledged();
	}
	
	public static boolean addMappingToIndex(String index_name, String type, String json_mapping_source) {
		// Inspired by http://stackoverflow.com/questions/22071198/adding-mapping-to-a-type-from-java-how-do-i-do-it
		Client client = getClient();
		PutMappingRequestBuilder request = client.admin().indices().preparePutMapping(index_name);
		request.setType(type);
		request.setSource(json_mapping_source);
		return request.execute().actionGet().isAcknowledged();
	}
	
}
