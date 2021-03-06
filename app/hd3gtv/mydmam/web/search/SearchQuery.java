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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.web.search;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FuzzyQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;
import hd3gtv.mydmam.db.ElastisearchStatSearch;
import hd3gtv.mydmam.gson.GsonIgnore;
import hd3gtv.mydmam.gson.GsonKit;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.Importer.SearchPreProcessor;

public final class SearchQuery implements ElastisearchStatSearch {
	
	public enum SearchMode {
		BY_ID, BY_ALL_WORDS, BY_FULL_TEXT, BY_FUZZY;
	}
	
	/**
	 * Also serialize SearchResult
	 * @deprecated
	 */
	public static final SearchQuerySerializer searchQuerySerializer = new SearchQuerySerializer();
	private static final String[] all_search_types;
	private static final int MAX_ELEMENTS_RESPONSE_PAGE_SIZE = 10;
	
	static {
		List<String> module_ES_TYPE_search = new Importer.SearchPreProcessor().getESTypeForUserSearch();
		all_search_types = module_ES_TYPE_search.toArray(new String[module_ES_TYPE_search.size()]);
	}
	
	@GsonIgnore
	private List<SearchResult> results;
	private String q;
	
	/**
	 * This is the page number !
	 */
	private int from;
	
	@SuppressWarnings("unused")
	private boolean timedout = false;
	@SuppressWarnings("unused")
	private long duration = 0;
	@SuppressWarnings("unused")
	private long total_items_count = 0;
	@SuppressWarnings("unused")
	private float max_score = 0f;
	private int pagesize = MAX_ELEMENTS_RESPONSE_PAGE_SIZE;
	@SuppressWarnings("unused")
	private int pagecount = 0;
	@SuppressWarnings("unused")
	private SearchMode mode = null;
	
	public static class SearchQuerySerializer implements JsonSerializer<SearchQuery> {
		public JsonElement serialize(SearchQuery src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
			result.add("results", MyDMAM.gson_kit.getGsonSimple().toJsonTree(src.results, GsonKit.type_ArrayList_SearchResult));
			return result;
		}
	}
	
	/**
	 * @return return null if q is empty or null.
	 */
	public static String cleanUserTextSearch(String q) {
		if (q == null) {
			return null;
		}
		if (q.trim().equals("")) {
			return null;
		}
		StringBuilder cleanquery = new StringBuilder(q.length());
		char current;
		boolean keepchar;
		for (int pos_q = 0; pos_q < q.length(); pos_q++) {
			current = q.charAt(pos_q);
			keepchar = true;
			for (int pos = 0; pos < Elasticsearch.forbidden_query_chars.length; pos++) {
				if (current == Elasticsearch.forbidden_query_chars[pos]) {
					keepchar = false;
					break;
				}
			}
			if (keepchar) {
				cleanquery.append(current);
			} else {
				cleanquery.append(" ");
			}
		}
		return cleanquery.toString();
	}
	
	public SearchQuery() {
	}
	
	public SearchQuery search(SearchRequest request) {
		results = new ArrayList<SearchResult>(MAX_ELEMENTS_RESPONSE_PAGE_SIZE);
		
		if (request.q == null) {
			request.q = "";
		}
		q = cleanUserTextSearch(request.q);
		if (q == null) {
			q = "";
		}
		q = q.trim().toLowerCase();
		if (q.equals("")) {
			return this;
		}
		
		if (request.from > 1) {
			this.from = request.from;
		} else {
			this.from = 1;
		}
		
		Operation operation = new Operation(this);
		boolean isvalidmediaid = Importer.getIdExtractorFileName().isValidId(q);
		BoolQueryBuilder querybuilder;
		
		if (isvalidmediaid) {
			/**
			 * ID search
			 */
			querybuilder = QueryBuilders.boolQuery();
			querybuilder.should(QueryBuilders.termQuery("mediaid", q));
			querybuilder.should(QueryBuilders.termQuery("id", q));
			querybuilder.should(QueryBuilders.termQuery("_id", q));
			
			if (operation.doQuery(querybuilder)) {
				mode = SearchMode.BY_ID;
				return this;
			}
		}
		
		if (q.indexOf(' ') > -1) {
			/**
			 * All words exact search
			 */
			String[] query_items = q.split(" ");
			querybuilder = QueryBuilders.boolQuery();
			for (int pos = 0; pos < query_items.length; pos++) {
				querybuilder.must((new QueryStringQueryBuilder(query_items[pos])));
			}
			if (operation.doQuery(querybuilder)) {
				mode = SearchMode.BY_ALL_WORDS;
				return this;
			}
		}
		
		/**
		 * Simple search / 1 word search
		 */
		if (operation.doQuery(new QueryStringQueryBuilder(q))) {
			mode = SearchMode.BY_FULL_TEXT;
			return this;
		}
		
		/**
		 * No fuzzy for Id, and no responses here.
		 */
		if (isvalidmediaid) {
			mode = SearchMode.BY_ID;
			return this;
		}
		
		/**
		 * Fuzzy (stupid) search
		 */
		if (q.indexOf(' ') > -1) {
			/**
			 * All words
			 */
			String[] query_items = q.split(" ");
			querybuilder = QueryBuilders.boolQuery();
			for (int pos = 0; pos < query_items.length; pos++) {
				querybuilder.must(new FuzzyQueryBuilder("_all", query_items[pos]).fuzziness(Fuzziness.AUTO));
			}
			if (operation.doQuery(querybuilder)) {
				mode = SearchMode.BY_FUZZY;
				return this;
			}
		}
		/**
		 * 1 word
		 */
		operation.doQuery((new FuzzyQueryBuilder("_all", q)).fuzziness(Fuzziness.AUTO));
		mode = SearchMode.BY_FUZZY;
		return this;
	}
	
	public String toJsonString() {
		return MyDMAM.gson_kit.getGson().toJson(this);
	}
	
	public String getQ() {
		return q;
	}
	
	public boolean hasResults() {
		if (results == null) {
			return false;
		}
		return results.isEmpty() == false;
	}
	
	/**
	 * Index -> Module
	 */
	private volatile static Map<String, SearchResultPreProcessor> search_engines_pre_processing;
	
	private class Operation implements ElastisearchCrawlerHit {
		
		private SearchQuery reference;
		private ElastisearchCrawlerReader crawl;
		
		Operation(SearchQuery reference) {
			this.reference = reference;
			crawl = Elasticsearch.createCrawlerReader();
			crawl.setTypes(all_search_types).setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
			crawl.setPageSize(MAX_ELEMENTS_RESPONSE_PAGE_SIZE).setMaximumSize(MAX_ELEMENTS_RESPONSE_PAGE_SIZE).setFrom((from - 1) * MAX_ELEMENTS_RESPONSE_PAGE_SIZE);
		}
		
		/**
		 * @return true if hits are founded
		 */
		boolean doQuery(QueryBuilder query) {
			try {
				crawl.setQuery(query).allReader(this, reference);
			} catch (Exception e) {
				Loggers.Play.error("Can't search, q: " + q, e);
			}
			return results.isEmpty() == false;
		}
		
		public boolean onFoundHit(SearchHit hit) throws Exception {
			
			SearchResult result = new SearchResult(hit.getIndex(), hit.getType(), hit.getId(), hit.getSource(), hit.getScore());
			
			if (search_engines_pre_processing == null) {
				search_engines_pre_processing = new HashMap<String, SearchResultPreProcessor>();
				
				/**
				 * Add Pathindex
				 */
				SearchPreProcessor pathindex_preproc = new Importer.SearchPreProcessor();
				List<String> es_type_handled = pathindex_preproc.getESTypeForUserSearch();
				for (int pos_estype = 0; pos_estype < es_type_handled.size(); pos_estype++) {
					search_engines_pre_processing.put(es_type_handled.get(pos_estype), pathindex_preproc);
				}
				
			}
			
			SearchResultPreProcessor pre_processor = search_engines_pre_processing.get(hit.getType());
			if (pre_processor == null) {
				results.add(result);
				return true;
			}
			pre_processor.prepareSearchResult(hit, result);
			
			if (result.getContent() == null) {
				result.setContent(new HashMap<String, Object>());
			}
			results.add(result);
			
			return true;
		}
	}
	
	public boolean onFirstSearch(boolean timedout, long duration, long total_items_count, float max_score) {
		this.timedout = timedout;
		this.duration += duration;
		this.total_items_count = total_items_count;
		this.max_score = max_score;
		pagecount = (int) Math.ceil((double) total_items_count / (double) pagesize);
		return (total_items_count > 0);
	}
	
}
