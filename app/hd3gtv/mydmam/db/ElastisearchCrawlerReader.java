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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.db;

import hd3gtv.log2.Log2;

import java.util.ArrayList;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

public class ElastisearchCrawlerReader {
	
	private class Sort {
		Sort(String field, SortOrder order) {
			this.field = field;
			this.order = order;
		}
		
		String field;
		SortOrder order;
	}
	
	private Client client;
	private String[] indices;
	private String[] types;
	private QueryBuilder query;
	private ArrayList<Sort> sorts;
	private int size;
	
	private int max_retry = 5;
	
	ElastisearchCrawlerReader() {
		client = Elasticsearch.getClient();
		query = QueryBuilders.matchAllQuery();
		sorts = new ArrayList<ElastisearchCrawlerReader.Sort>();
		size = 0;
	}
	
	public ElastisearchCrawlerReader setIndices(String... indices) {
		this.indices = indices;
		return this;
	}
	
	public ElastisearchCrawlerReader setTypes(String... types) {
		this.types = types;
		return this;
	}
	
	/**
	 * matchAllQuery by default;
	 */
	public ElastisearchCrawlerReader setQuery(QueryBuilder query) {
		this.query = query;
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		return this;
	}
	
	public ElastisearchCrawlerReader addSort(String field, SortOrder order) {
		if (field == null) {
			throw new NullPointerException("\"field\" can't to be null");
		}
		if (order == null) {
			throw new NullPointerException("\"order\" can't to be null");
		}
		sorts.add(new Sort(field, order));
		return this;
	}
	
	public ElastisearchCrawlerReader setSize(int size) {
		this.size = size;
		return this;
	}
	
	private SearchResponse execute(SearchRequestBuilder request) {
		for (int pos_retry = 0; pos_retry < max_retry; pos_retry++) {
			try {
				return Elasticsearch.getClient().search(request.request()).actionGet();
			} catch (NoNodeAvailableException e) {
				try {
					/**
					 * Wait before to retry, after the 2nd try.
					 */
					Thread.sleep(pos_retry * 100);
				} catch (InterruptedException e1) {
					Log2.log.error("Stop sleep", e1);
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
					Log2.log.error("The last (" + max_retry + ") try has failed, throw error", e);
					throw e;
				}
				
			}
		}
		return null;
	}
	
	public void allReader(ElastisearchCrawlerHit crawler) {
		if (crawler == null) {
			throw new NullPointerException("\"crawler\" can't to be null");
		}
		
		try {
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
			request.setQuery(query);
			request.setVersion(true);
			for (int pos_s = 0; pos_s < sorts.size(); pos_s++) {
				request.addSort(sorts.get(pos_s).field, (sorts.get(pos_s).order));
			}
			if (size > 0) {
				request.setSize(size);
			}
			
			SearchResponse response = execute(request);
			SearchHit[] hits = response.getHits().hits();
			int count_remaining = (int) response.getHits().getTotalHits();
			int totalhits = count_remaining;
			
			boolean can_continue = true;
			while (can_continue) {
				for (int pos = 0; pos < hits.length; pos++) {
					if (crawler.onFoundHit(hits[pos]) == false) {
						return;
					}
					
					count_remaining--;
					if (can_continue == false) {
						count_remaining = 0;
						break;
					}
				}
				if (count_remaining == 0) {
					break;
				}
				request.setFrom(totalhits - count_remaining);
				response = execute(request);
				hits = response.getHits().hits();
				if (hits.length == 0) {
					can_continue = false;
				}
			}
		} catch (IndexMissingException ime) {
			/**
			 * No items == no callbacks
			 */
			return;
		}
	}
	
}
