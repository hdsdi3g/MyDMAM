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
package hd3gtv.mydmam.db;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.MultiSearchResponse.Item;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

public class ElastisearchMultipleCrawlerReader {
	
	private Client client;
	private List<QueryItem> queries;
	private String[] default_indices;
	private String[] default_types;
	private int default_maxsize = 0;
	
	ElastisearchMultipleCrawlerReader(Client client) {
		this.client = client;
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		queries = new ArrayList<QueryItem>();
	}
	
	public ElastisearchMultipleCrawlerReader setDefaultIndices(String... indices) {
		this.default_indices = indices;
		return this;
	}
	
	public ElastisearchMultipleCrawlerReader setDefaultTypes(String... types) {
		this.default_types = types;
		return this;
	}
	
	public ElastisearchMultipleCrawlerReader setDefaultMaxSize(int maxsize) {
		this.default_maxsize = maxsize;
		return this;
	}
	
	public QueryItem addNewQuery() {
		QueryItem item = new QueryItem();
		queries.add(item);
		return item;
	}
	
	public ElastisearchMultipleCrawlerReader addNewQuery(QueryBuilder query) {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		QueryItem item = new QueryItem();
		item.query = query;
		queries.add(item);
		return this;
	}
	
	public void allReader(ElastisearchCrawlerHit crawler) throws Exception {
		try {
			MultiSearchResponse.Item[] items = Elasticsearch.withRetry(new ElasticsearchWithRetry<Item[]>() {
				public Item[] call(Client client) throws NoNodeAvailableException {
					MultiSearchRequestBuilder multisearchrequestbuilder = new MultiSearchRequestBuilder(client);
					for (int pos = 0; pos < queries.size(); pos++) {
						multisearchrequestbuilder.add(queries.get(pos).getRequest());
					}
					return multisearchrequestbuilder.execute().actionGet().getResponses();
				}
			});
			
			SearchHit[] hits;
			SearchResponse response;
			for (int pos_response = 0; pos_response < items.length; pos_response++) {
				response = items[pos_response].getResponse();
				if (response == null) {
					continue;
				}
				if (response.getHits() == null) {
					continue;
				}
				hits = response.getHits().hits();
				if (hits.length == 0) {
					continue;
				}
				for (int pos_hits = 0; pos_hits < hits.length; pos_hits++) {
					if (crawler.onFoundHit(hits[pos_hits]) == false) {
						return;
					}
				}
			}
		} catch (IndexMissingException ime) {
			/**
			 * No items == no callbacks
			 */
			return;
		}
	}
	
	public class QueryItem {
		private String[] indices;
		private String[] types;
		private QueryBuilder query;
		private int maxsize;
		
		private QueryItem() {
			query = QueryBuilders.matchAllQuery();
			this.indices = default_indices;
			this.types = default_types;
			this.maxsize = default_maxsize;
		}
		
		public QueryItem setIndices(String... indices) {
			this.indices = indices;
			return this;
		}
		
		public QueryItem setTypes(String... types) {
			this.types = types;
			return this;
		}
		
		/**
		 * matchAllQuery by default;
		 */
		public QueryItem setQuery(QueryBuilder query) {
			this.query = query;
			if (query == null) {
				throw new NullPointerException("\"query\" can't to be null");
			}
			return this;
		}
		
		public QueryItem setMaxSize(int maxsize) {
			return this;
		}
		
		private SearchRequestBuilder getRequest() {
			SearchRequestBuilder request = client.prepareSearch();
			if (indices != null) {
				if (indices.length > 0) {
					request.setIndices(indices);
				}
			}
			if (types != null) {
				if (types.length > 0) {
					request.setTypes(types);
				}
			}
			if (maxsize > 0) {
				request.setSize(maxsize);
			}
			request.setQuery(query);
			return request;
		}
	}
	
}
