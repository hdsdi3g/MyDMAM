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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/

package hd3gtv.mydmam.web;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.web.search.AsyncSearchQuery;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;

/**
 * @deprecated
 */
public class SearchResult {
	
	private static String[] search_type;
	
	static {
		List<String> module_ES_TYPE_search = MyDMAMModulesManager.getESTypesForUserSearch();
		
		search_type = new String[module_ES_TYPE_search.size() + 2];
		
		for (int pos = 0; pos < module_ES_TYPE_search.size(); pos++) {
			search_type[pos] = module_ES_TYPE_search.get(pos);
		}
		search_type[search_type.length - 1] = Importer.ES_TYPE_FILE;
		search_type[search_type.length - 2] = Importer.ES_TYPE_DIRECTORY;
	}
	
	private static SearchResponse internalSearch(Client client, QueryBuilder querybuilder, int frompage, int pagesize) throws SearchPhaseExecutionException {
		SearchRequestBuilder searchrequestbuilder = client.prepareSearch();
		
		searchrequestbuilder.setTypes(search_type);
		searchrequestbuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		searchrequestbuilder.setQuery(querybuilder);
		searchrequestbuilder.setFrom(frompage * pagesize);
		searchrequestbuilder.setSize(pagesize);
		searchrequestbuilder.setExplain(false);
		return searchrequestbuilder.execute().actionGet();
	}
	
	private static SearchResponse structuredInternalSearch(Client client, String realquery, int frompage, int pagesize, SearchResult searchresult) {
		SearchResponse response;
		
		String query = realquery.toLowerCase();
		
		boolean isvalidmediaid = MyDMAM.isValidMediaId(query);
		
		if (isvalidmediaid) {
			/**
			 * ID search
			 */
			BoolQueryBuilder querybuilder = QueryBuilders.boolQuery();
			querybuilder.should(QueryBuilders.termQuery("mediaid", query));
			querybuilder.should(QueryBuilders.termQuery("id", query));
			querybuilder.should(QueryBuilders.termQuery("_id", query));
			
			response = internalSearch(client, querybuilder, frompage, pagesize);
			if (response.getHits().getTotalHits() > 0) {
				searchresult.mode = SearchMode.BY_ID;
				return response;
			}
		}
		
		if (query.indexOf(' ') > -1) {
			/**
			 * All words exact search
			 */
			String[] query_items = query.split(" ");
			BoolQueryBuilder querybuilder = QueryBuilders.boolQuery();
			for (int pos = 0; pos < query_items.length; pos++) {
				querybuilder.must((new QueryStringQueryBuilder(query_items[pos])));
			}
			response = internalSearch(client, querybuilder, frompage, pagesize);
			if (response.getHits().getTotalHits() > 0) {
				searchresult.mode = SearchMode.BY_ALL_WORDS;
				return response;
			}
		}
		
		/**
		 * Simple search / 1 word search
		 */
		response = internalSearch(client, new QueryStringQueryBuilder(query), frompage, pagesize);
		if (response.getHits().getTotalHits() > 0) {
			searchresult.mode = SearchMode.BY_FULL_TEXT;
			return response;
		}
		
		/**
		 * No fuzzy for Id
		 */
		if (isvalidmediaid) {
			searchresult.mode = SearchMode.BY_ID;
			return response;
		}
		
		/**
		 * Fuzzy (stupid) search
		 */
		if (query.indexOf(' ') > -1) {
			/**
			 * All words
			 */
			String[] query_items = query.split(" ");
			BoolQueryBuilder querybuilder = QueryBuilders.boolQuery();
			for (int pos = 0; pos < query_items.length; pos++) {
				querybuilder.must(new FuzzyQueryBuilder("_all", query_items[pos]).fuzziness(Fuzziness.AUTO));
			}
			response = internalSearch(client, querybuilder, frompage, pagesize);
			if (response.getHits().getTotalHits() > 0) {
				searchresult.mode = SearchMode.BY_FUZZY;
				return response;
			}
		}
		/**
		 * 1 word
		 */
		response = internalSearch(client, (new FuzzyQueryBuilder("_all", query)).fuzziness(Fuzziness.AUTO), frompage, pagesize);
		searchresult.mode = SearchMode.BY_FUZZY;
		return response;
	}
	
	public static SearchResult search(String query, int frompage, int pagesize) {
		if (pagesize < 1) {
			return null;
		}
		
		Client client = Elasticsearch.getClient();
		SearchResult search_result = new SearchResult();
		
		String clean_query = AsyncSearchQuery.cleanUserTextSearch(query);
		SearchResponse response = structuredInternalSearch(client, clean_query, frompage, pagesize, search_result);
		
		search_result.query = clean_query;
		search_result.results = new ArrayList<SearchResultItem>(pagesize);
		search_result.timedout = response.isTimedOut();
		search_result.duration = response.getTookInMillis();
		search_result.total_items_count = response.getHits().getTotalHits();
		search_result.max_score = response.getHits().getMaxScore();
		search_result.frompage = frompage;
		search_result.pagesize = response.getHits().hits().length;
		search_result.pagecount = (int) Math.ceil((double) search_result.total_items_count / (double) pagesize);
		
		SearchHit[] hits = response.getHits().hits();
		
		for (int pos = 0; pos < hits.length; pos++) {
			search_result.results.add(SearchResultItem.fromSource(hits[pos]));
		}
		
		return search_result;
	}
	
	public List<SearchResultItem> results;
	public boolean timedout;
	public long duration;
	public long total_items_count;
	public float max_score;
	public int frompage;
	public int pagesize;
	public int pagecount;
	public String query;
	public SearchMode mode;
	
	public enum SearchMode {
		BY_ID, BY_ALL_WORDS, BY_FULL_TEXT, BY_FUZZY;
	}
}
