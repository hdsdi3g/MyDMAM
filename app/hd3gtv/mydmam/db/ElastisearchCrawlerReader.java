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

import java.util.ArrayList;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import hd3gtv.mydmam.Loggers;

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
	private int from;
	private int max_size;
	private SearchType searchtype;
	private boolean retrive_ttl;
	private boolean display_ttl_for_each_result;
	
	ElastisearchCrawlerReader() {
		client = Elasticsearch.getClient();
		query = QueryBuilders.matchAllQuery();
		sorts = new ArrayList<ElastisearchCrawlerReader.Sort>();
		size = 0;
		from = 0;
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
	
	/**
	 * get result with hit.getFields().
	 */
	public ElastisearchCrawlerReader setRetriveTTL(boolean retrive_ttl) {
		this.retrive_ttl = retrive_ttl;
		return this;
	}
	
	public ElastisearchCrawlerReader setDisplayTTLForEachResult(boolean display_ttl_for_each_result) {
		this.display_ttl_for_each_result = display_ttl_for_each_result;
		return setRetriveTTL(true);
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
	
	public ElastisearchCrawlerReader setPageSize(int size) {
		this.size = size;
		return this;
	}
	
	public ElastisearchCrawlerReader setMaximumSize(int max_size) {
		this.max_size = max_size;
		return this;
	}
	
	public ElastisearchCrawlerReader setFrom(int from) {
		this.from = from;
		return this;
	}
	
	public ElastisearchCrawlerReader setSearchType(SearchType searchtype) {
		this.searchtype = searchtype;
		return this;
	}
	
	private SearchResponse execute(final SearchRequestBuilder request) {
		return Elasticsearch.withRetry(new ElasticsearchWithRetry<SearchResponse>() {
			public SearchResponse call(Client client) throws NoNodeAvailableException {
				return client.search(request.request()).actionGet();
			}
		});
	}
	
	/**
	 * Never parallelized
	 */
	public void allReader(ElastisearchCrawlerHit crawler) throws Exception {
		allReader(crawler, null);
	}
	
	/**
	 * Never parallelized
	 */
	public void allReader(ElastisearchCrawlerHit crawler, ElastisearchStatSearch stat) throws Exception {
		if (crawler == null) {
			throw new NullPointerException("\"crawler\" can't to be null");
		}
		
		if (display_ttl_for_each_result) {
			Loggers.ElasticSearch.warn("\"display_ttl_for_each_result\" is set, verbose messages will be displayed...");
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
			if (from > 0) {
				request.setFrom(from);
			}
			if (searchtype != null) {
				request.setSearchType(searchtype);
			}
			if (retrive_ttl) {
				request.addFields("_ttl", "_source");
			}
			
			SearchResponse response = execute(request);
			
			if (stat != null) {
				float max_score = response.getHits().getMaxScore();
				if (String.valueOf(max_score).equalsIgnoreCase("NaN")) {
					max_score = 0f;
				}
				if (stat.onFirstSearch(response.isTimedOut(), response.getTookInMillis(), response.getHits().getTotalHits(), max_score) == false) {
					return;
				}
			}
			
			SearchHit[] hits = response.getHits().hits();
			int count_remaining = ((int) response.getHits().getTotalHits()) - from;
			if ((max_size > 0) & (max_size < count_remaining)) {
				count_remaining = max_size;
			}
			int totalhits = count_remaining;
			
			boolean can_continue = true;
			long ttl;
			SearchHit hit;
			while (can_continue) {
				for (int pos = 0; pos < hits.length; pos++) {
					hit = hits[pos];
					
					if (display_ttl_for_each_result) {
						ttl = Elasticsearch.extractTTLfromHit(hit);
						System.out.print("DISPLAY_TTL [");
						System.out.print(hit.getIndex());
						System.out.print("]\t[");
						System.out.print(hit.getType());
						System.out.print("]\t[");
						System.out.print(hit.getId());
						System.out.print("]\tv");
						System.out.print(hit.version());
						if (ttl > 0) {
							System.out.print("\t");
							System.out.print(ttl / 1000);
							System.out.print(" sec\t");
							System.out.print(Loggers.dateLog(System.currentTimeMillis() + ttl));
						} else {
							System.out.print("\tNO TTL\t");
						}
						System.out.println();
					}
					
					if (crawler.onFoundHit(hit) == false) {
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
			Loggers.ElasticSearch.debug("Missing index during Crawler reader: no callbacks", ime);
		} catch (SearchPhaseExecutionException epee) {
			Loggers.ElasticSearch.debug("Problem during Crawler reader (maybe shard failures / no data): no callbacks", epee);
		}
	}
	
}
