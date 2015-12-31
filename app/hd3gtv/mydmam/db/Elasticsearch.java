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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationClusterItem;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.status.ElasticsearchStatus;
import hd3gtv.mydmam.manager.InstanceStatus;

@SuppressWarnings("unchecked")
public class Elasticsearch {
	
	private static InetSocketTransportAddress[] transportadresses;
	public static final char[] forbidden_query_chars = { '/', '\\', '%', '[', ']', '(', ')', '{', '}', '"', '~', '#', '!', ':', '-' };
	private static TransportClient client;
	
	/**
	 * @see ElasticsearchStatus
	 */
	public static void refeshconfiguration() {
		if (client != null) {
			try {
				client.close();
			} catch (Exception e) {
				Loggers.ElasticSearch.error("Can't close properly client", e);
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
			settings.put("node.name", InstanceStatus.getStatic().summary.getInstanceNamePid());
			settings.put("client.transport.ping_timeout", 10, TimeUnit.SECONDS);
			
			StringBuilder sb = new StringBuilder();
			sb.append("Elasticsearch client configuration, clustername: ");
			sb.append(clustername);
			
			transportadresses = new InetSocketTransportAddress[clusterservers.size()];
			for (int pos = 0; pos < clusterservers.size(); pos++) {
				transportadresses[pos] = new InetSocketTransportAddress(clusterservers.get(pos).address, clusterservers.get(pos).port);
				sb.append(", ");
				sb.append(clusterservers.get(pos));
			}
			Loggers.ElasticSearch.info(sb.toString());
			
			client = new TransportClient(settings.build());
			client.addTransportAddresses(transportadresses);
		} catch (Exception e) {
			Loggers.ElasticSearch.error("Can't load client configuration", e);
			try {
				client.close();
			} catch (Exception e1) {
				Loggers.ElasticSearch.error("Can't close client", e1);
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
	
	public static ElasticsearchBulkOperation prepareBulk() {
		Loggers.ElasticSearch.debug("Prepare bulk");
		return new ElasticsearchBulkOperation();
	}
	
	public static ElasticsearchMultiGetRequest prepareMultiGetRequest() {
		return new ElasticsearchMultiGetRequest(getClient());
	}
	
	/**
	 * Protected to some IndexMissingException
	 */
	public static void deleteIndexRequest(final String index_name) throws ElasticsearchException {
		try {
			Loggers.ElasticSearch.info("Delete index " + index_name);
			Elasticsearch.withRetry(new ElasticsearchWithRetry<Void>() {
				public Void call(Client client) throws NoNodeAvailableException {
					client.admin().indices().delete(new DeleteIndexRequest(index_name)).actionGet();
					return null;
				}
			});
		} catch (IndexMissingException e) {
		}
	}
	
	/**
	 * Do all checks, test if exists, and if has result
	 * @return null if no/impossible result
	 */
	public static JsonObject getJSONFromSimpleResponse(GetResponse response) {
		if (response == null) {
			return null;
		}
		if (response.isExists() == false) {
			return null;
		}
		return getJSONFromString(response.getSourceAsString());
	}
	
	private static JsonObject getJSONFromString(String source_as_string) {
		JsonElement o = new JsonParser().parse(source_as_string);
		if (o.isJsonObject()) {
			return o.getAsJsonObject();
		}
		return null;
	}
	
	/**
	 * Do all checks, test if exists, and if has result
	 * @return null if no/impossible result
	 */
	public static JsonObject getJSONFromSimpleResponse(SearchHit hit) {
		if (hit == null) {
			return null;
		}
		return getJSONFromString(hit.getSourceAsString());
	}
	
	public static void enableTTL(String index_name, String type) throws IOException {
		if (isIndexExists(index_name) == false) {
			/**
			 * No index, no enable ttl.
			 */
			Loggers.ElasticSearch.info("Create index " + index_name);
			createIndex(index_name);
		}
		Loggers.ElasticSearch.debug("Enable TTL for index " + index_name + ": " + type);
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
				 * No actual mapping for this type in index, but create it during enabling ttl.
				 */
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
		return new ElastisearchCrawlerReader();
	}
	
	public static ElasticsearchCrawlerToBulk createCrawlerToBulk() {
		return new ElasticsearchCrawlerToBulk();
	}
	
	public static ElastisearchMultipleCrawlerReader createMultipleCrawlerReader() {
		return new ElastisearchMultipleCrawlerReader(getClient());
	}
	
	public static boolean isIndexExists(final String index_name) {
		return Elasticsearch.withRetry(new ElasticsearchWithRetry<Boolean>() {
			public Boolean call(Client client) throws NoNodeAvailableException {
				return client.admin().indices().exists(new IndicesExistsRequest(index_name)).actionGet().isExists();
			}
		});
	}
	
	public static boolean createIndex(final String index_name) {
		return Elasticsearch.withRetry(new ElasticsearchWithRetry<Boolean>() {
			public Boolean call(Client client) throws NoNodeAvailableException {
				return client.admin().indices().prepareCreate(index_name).execute().actionGet().isAcknowledged();
			}
		});
	}
	
	public static boolean addMappingToIndex(final String index_name, final String type, final String json_mapping_source) {
		Loggers.ElasticSearch.info("Add mapping to index: " + index_name + ", type: " + type + ", mapping_source: " + json_mapping_source);
		return Elasticsearch.withRetry(new ElasticsearchWithRetry<Boolean>() {
			public Boolean call(Client client) throws NoNodeAvailableException {
				// Inspired by http://stackoverflow.com/questions/22071198/adding-mapping-to-a-type-from-java-how-do-i-do-it
				PutMappingRequestBuilder request = client.admin().indices().preparePutMapping(index_name);
				request.setType(type);
				request.setSource(json_mapping_source);
				return request.execute().actionGet().isAcknowledged();
			}
		});
	}
	
	private static int max_retry = 5;
	
	static <T> T withRetry(ElasticsearchWithRetry<T> callable) throws NoNodeAvailableException {
		for (int pos_retry = 0; pos_retry < max_retry; pos_retry++) {
			try {
				return callable.call(getClient());
			} catch (NoNodeAvailableException e) {
				try {
					/**
					 * Wait before to retry, after the 2nd try.
					 */
					Thread.sleep(pos_retry * 100);
				} catch (InterruptedException e1) {
					Loggers.ElasticSearch.warn("Stop sleep", e1);
					return null;
				}
				if (pos_retry == (max_retry - 2)) {
					/**
					 * Before the last try, force refesh configuration.
					 */
					Elasticsearch.refeshconfiguration();
				} else if (pos_retry + 1 == max_retry) {
					/**
					 * The last try has failed, throw error.
					 */
					Loggers.ElasticSearch.error("The last (" + max_retry + ") try has failed, throw error", e);
					throw e;
				}
			}
		}
		return null;
	}
	
	/**
	 * With retry
	 */
	public static GetResponse get(final GetRequest request) {
		return withRetry(new ElasticsearchWithRetry<GetResponse>() {
			public GetResponse call(Client client) throws NoNodeAvailableException {
				return client.get(request).actionGet();
			}
		});
	}
	
	/**
	 * With retry
	 */
	public static IndexResponse index(final IndexRequest request) {
		return withRetry(new ElasticsearchWithRetry<IndexResponse>() {
			public IndexResponse call(Client client) throws NoNodeAvailableException {
				return client.index(request).actionGet();
			}
		});
	}
	
	/**
	 * With retry
	 */
	public static long countRequest(final String index, final QueryBuilder query, final String... types) {
		CountResponse response = withRetry(new ElasticsearchWithRetry<CountResponse>() {
			public CountResponse call(Client client) throws NoNodeAvailableException {
				CountRequestBuilder request_builder = new CountRequestBuilder(client);
				request_builder.setIndices(index);
				request_builder.setTypes(types);
				request_builder.setQuery(query);
				return request_builder.execute().actionGet();
			}
		});
		if (response == null) {
			return 0;
		}
		return response.getCount();
	}
	
	public static EmptyActionListener createEmptyActionListener(Logger logger, Class<?> source) {
		if (logger == null) {
			throw new NullPointerException("\"logger\" can't to be null");
		}
		if (source == null) {
			throw new NullPointerException("\"source\" can't to be null");
		}
		return new EmptyActionListener(logger, source);
	}
	
	@SuppressWarnings("rawtypes")
	private static class EmptyActionListener implements ActionListener {
		
		private Logger logger;
		private Class<?> source;
		
		public EmptyActionListener(Logger logger, Class<?> source) {
			this.logger = logger;
			this.source = source;
		}
		
		public void onResponse(Object response) {
			if (logger.isTraceEnabled()) {
				if (response instanceof IndexResponse) {
					logger.trace("ES has correctly executed the action submitted by " + source.getSimpleName() + ", v" + ((IndexResponse) response).getVersion());
				} else {
					logger.trace("ES has correctly executed the action submitted by " + source.getSimpleName() + " " + response.toString());
				}
			}
		}
		
		public void onFailure(Throwable e) {
			logger.warn("ES generic error, can't execute action submitted by " + source.getName(), e);
		}
		
	}
	
	/**
	 * @return -1 if not TTL
	 */
	public static long extractTTLfromHit(SearchHit hit) {
		if (hit.getFields().containsKey("_ttl") == false) {
			return -1;
		}
		return (Long) hit.getFields().get("_ttl").getValue();
	}
	
}
