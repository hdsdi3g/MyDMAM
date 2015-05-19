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
import java.util.Arrays;
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
import org.elasticsearch.search.sort.SortBuilder;

public class ElastisearchMultipleCrawlerReader {
	
	private Client client;
	private List<QueryItem> queries;
	private String[] default_indices;
	private String[] default_types;
	private int default_maxsize = 0;
	private int default_from = 0;
	private List<SortBuilder> default_sort = new ArrayList<SortBuilder>(1);
	
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
	
	public ElastisearchMultipleCrawlerReader setDefaultFrom(int default_from) {
		this.default_from = default_from;
		return this;
	}
	
	/**
	 * Use SortBuilders.fieldSort("").order()
	 */
	public ElastisearchMultipleCrawlerReader setDefaultSort(SortBuilder... sort) {
		if (sort == null) {
			default_sort = new ArrayList<SortBuilder>(1);
			return this;
		}
		if (sort.length == 0) {
			default_sort = new ArrayList<SortBuilder>(1);
			return this;
		}
		default_sort = Arrays.asList(sort);
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
	
	public void allReader(ElastisearchCrawlerMultipleHits crawler) throws Exception {
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
				if (crawler.onMultipleResponse(response, Arrays.asList(hits)) == false) {
					return;
				}
			}
		} catch (IndexMissingException ime) {
			/**
			 * No items == no callbacks
			 */
			return;
		}
	}
	
	/**
	 * Mergue all results requests in one.
	 */
	public void allReader(final ElastisearchCrawlerHit crawler) throws Exception {
		allReader(new ElastisearchCrawlerMultipleHits() {
			@Override
			public boolean onMultipleResponse(SearchResponse response, List<SearchHit> hits) throws Exception {
				for (int pos = 0; pos < hits.size(); pos++) {
					crawler.onFoundHit(hits.get(pos));
				}
				return false;
			}
		});
	}
	
	public class QueryItem {
		private String[] indices;
		private String[] types;
		private QueryBuilder query;
		private int maxsize;
		private int from;
		private List<SortBuilder> sort;
		
		private QueryItem() {
			query = QueryBuilders.matchAllQuery();
			this.indices = default_indices;
			this.types = default_types;
			this.maxsize = default_maxsize;
			this.from = default_from;
			this.sort = default_sort;
		}
		
		public QueryItem setIndices(String... indices) {
			this.indices = indices;
			return this;
		}
		
		public QueryItem setTypes(String... types) {
			this.types = types;
			return this;
		}
		
		public QueryItem setFrom(int from) {
			this.from = from;
			return this;
		}
		
		/**
		 * Use SortBuilders.fieldSort("").order()
		 */
		public QueryItem setSort(SortBuilder... sort) {
			if (sort == null) {
				this.sort = new ArrayList<SortBuilder>(1);
				return this;
			}
			if (sort.length == 0) {
				this.sort = new ArrayList<SortBuilder>(1);
				return this;
			}
			this.sort = Arrays.asList(sort);
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
			if (from > 0) {
				request.setFrom(from);
			}
			for (int pos_s = 0; pos_s < sort.size(); pos_s++) {
				request.addSort(sort.get(pos_s));
			}
			request.setQuery(query);
			return request;
		}
	}
	
}
