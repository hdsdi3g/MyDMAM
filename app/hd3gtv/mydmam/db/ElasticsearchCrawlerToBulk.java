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
package hd3gtv.mydmam.db;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;

import hd3gtv.mydmam.db.ElasticsearchBulkOperation.BulkConfiguration;

public class ElasticsearchCrawlerToBulk {
	
	private ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
	private ElastisearchCrawlerReader crawler = Elasticsearch.createCrawlerReader();
	
	ElasticsearchCrawlerToBulk() {
		bulk = Elasticsearch.prepareBulk();
		crawler = Elasticsearch.createCrawlerReader();
		crawler.setPageSize(bulk.getWindowUpdateSize());
	}
	
	/**
	 * @see ElastisearchCrawlerReader
	 */
	public ElasticsearchCrawlerToBulk setIndices(String... indices) {
		crawler.setIndices(indices);
		return this;
	}
	
	/**
	 * @see ElastisearchCrawlerReader
	 */
	public ElasticsearchCrawlerToBulk setTypes(String... types) {
		crawler.setTypes(types);
		return this;
	}
	
	/**
	 * @see ElastisearchCrawlerReader
	 */
	public ElasticsearchCrawlerToBulk setQuery(QueryBuilder query) {
		crawler.setQuery(query);
		return this;
	}
	
	/**
	 * @see ElastisearchCrawlerReader
	 */
	public ElasticsearchCrawlerToBulk setRetriveTTL(boolean retrive_ttl) {
		crawler.setRetriveTTL(retrive_ttl);
		return this;
	}
	
	public ElastisearchCrawlerReader getCrawler() {
		return crawler;
	}
	
	public BulkConfiguration getBulkConfiguration() {
		return bulk.getConfiguration();
	}
	
	public ElasticsearchBulkOperation getBulk() {
		return bulk;
	}
	
	/**
	 * set for crawler and bulk
	 */
	public ElasticsearchCrawlerToBulk setPageSize(int size) {
		crawler.setPageSize(size);
		bulk.setWindowUpdateSize(size);
		return this;
	}
	
	public void process(final ElasticsearchCrawlerHitToBulk crawler_bulk) throws Exception {
		crawler.allReader(new ElastisearchCrawlerHit() {
			public boolean onFoundHit(SearchHit hit) throws Exception {
				return crawler_bulk.onFoundHit(hit, bulk);
			}
		});
		bulk.terminateBulk();
	}
	
}
