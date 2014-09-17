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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

public class ElastisearchCrawlerReader {
	
	private Client client;
	private String[] indices;
	private String[] types;
	private QueryBuilder query;
	
	ElastisearchCrawlerReader(Client client) {
		this.client = client;
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		query = QueryBuilders.matchAllQuery();
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
			
			SearchResponse response = request.execute().actionGet();
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
				response = request.execute().actionGet();
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
