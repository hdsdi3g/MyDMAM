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
package hd3gtv.mydmam.pathindexing;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.db.Elasticsearch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;

public class Explorer {
	
	private Client client;
	
	public Explorer(Client client) {
		this.client = client;
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
	}
	
	public SourcePathIndexerElement getByFullPathFile(String storagename, String pathfilename) {
		SearchRequestBuilder request = client.prepareSearch();
		request.setTypes(Importer.ES_TYPE_FILE);
		
		QueryStringQueryBuilder sqqb = new QueryStringQueryBuilder("\"" + pathfilename + "\"");
		sqqb.defaultField("path");
		request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("storagename", storagename.toLowerCase())).must(sqqb));
		request.setFrom(0);
		request.setSize(1);
		
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().hits();
		if (hits.length > 0) {
			return SourcePathIndexerElement.fromESResponse(hits[0]);
		}
		
		return null;
	}
	
	public ArrayList<SourcePathIndexerElement> getByStorageFilenameAndSize(String storagename, String filename, long size) {
		SearchRequestBuilder request = client.prepareSearch();
		request.setTypes(Importer.ES_TYPE_FILE);
		
		request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("storagename", storagename.toLowerCase())).must(QueryBuilders.termQuery("size", size)));
		request.setFrom(0);
		
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().hits();
		if (hits.length == 0) {
			return null;
		}
		
		ArrayList<SourcePathIndexerElement> result = new ArrayList<SourcePathIndexerElement>();
		for (int pos = 0; pos < hits.length; pos++) {
			SourcePathIndexerElement element = SourcePathIndexerElement.fromESResponse(hits[pos]);
			if (element.currentpath.endsWith(filename)) {
				result.add(element);
			}
		}
		
		if (result.size() == 0) {
			return null;
		} else {
			return result;
		}
	}
	
	private void crawler(SearchRequestBuilder request, IndexingEvent found_elements_observer) throws Exception {
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().hits();
		int count_remaining = (int) response.getHits().getTotalHits();
		int totalhits = count_remaining;
		
		boolean can_continue = true;
		while (can_continue) {
			for (int pos = 0; pos < hits.length; pos++) {
				can_continue = found_elements_observer.onFoundElement(SourcePathIndexerElement.fromESResponse(hits[pos]));
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
	}
	
	public void getAllId(String id, IndexingEvent found_elements_observer) throws Exception {
		SearchRequestBuilder request = client.prepareSearch();
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.termQuery("id", id.toLowerCase()));
		request.setFrom(0);
		request.setSize(100);
		crawler(request, found_elements_observer);
	}
	
	public ArrayList<SourcePathIndexerElement> getAllIdFromStorage(String id, String storagename) throws Exception {
		final ArrayList<SourcePathIndexerElement> result = new ArrayList<SourcePathIndexerElement>();
		
		SearchRequestBuilder request = client.prepareSearch();
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("id", id.toLowerCase())).must(QueryBuilders.termQuery("storagename", storagename.toLowerCase())));
		request.setFrom(0);
		request.setSize(100);
		
		crawler(request, new IndexingEvent() {
			public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
				result.add(element);
				return true;
			}
		});
		return result;
	}
	
	public static String getElementKey(String storagename, String currentpath) {
		SourcePathIndexerElement rootelement = new SourcePathIndexerElement();
		rootelement.storagename = storagename;
		rootelement.currentpath = currentpath;
		return rootelement.prepare_key();
	}
	
	/**
	 * @param min_index_date set 0 for all
	 */
	public void getAllSubElementsFromElementKey(String parentpath_key, long min_index_date, IndexingEvent found_elements_observer) throws Exception {
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
		
		if (min_index_date == 0) {
			request.setQuery(QueryBuilders.termQuery("parentpath", parentpath_key));
		} else {
			request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("parentpath", parentpath_key))
					.must(QueryBuilders.rangeQuery("dateindex").from(min_index_date).to(System.currentTimeMillis() + 1000000)));
		}
		
		request.setFrom(0);
		request.setSize(500);
		
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().hits();
		int count_remaining = (int) response.getHits().getTotalHits();
		int totalhits = count_remaining;
		
		while (hits.length > 0) {
			for (int pos = 0; pos < hits.length; pos++) {
				count_remaining--;
				SourcePathIndexerElement element = SourcePathIndexerElement.fromESResponse(hits[pos]);
				if (element.directory) {
					getAllSubElementsFromElementKey(element.prepare_key(), min_index_date, found_elements_observer);
				} else {
					if (found_elements_observer.onFoundElement(element) == false) {
						return;
					}
				}
			}
			request.setFrom(totalhits - count_remaining);
			response = request.execute().actionGet();
			hits = response.getHits().hits();
		}
	}
	
	public void getAllStorage(String storagename, IndexingEvent found_elements_observer) throws Exception {
		SearchRequestBuilder request = client.prepareSearch();
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.termQuery("storagename", storagename.toLowerCase()));
		request.setFrom(0);
		request.setSize(500);
		crawler(request, found_elements_observer);
	}
	
	public void getAllDirectoriesStorage(String storagename, IndexingEvent found_elements_observer) throws Exception {
		SearchRequestBuilder request = client.prepareSearch();
		request.setTypes(Importer.ES_TYPE_DIRECTORY);
		request.setQuery(QueryBuilders.termQuery("storagename", storagename.toLowerCase()));
		request.setFrom(0);
		request.setSize(500);
		crawler(request, found_elements_observer);
	}
	
	public SourcePathIndexerElement getelementByIdkey(String _id) {
		SourcePathIndexerElement result;
		
		if (SourcePathIndexerElement.hashThis("").equals(_id.toLowerCase())) {
			/**
			 * Root path ? : return empty.
			 */
			result = new SourcePathIndexerElement();
			result.currentpath = null;
			result.date = 0;
			result.directory = true;
			result.id = null;
			result.parentpath = null;
			result.size = 0;
			result.storagename = null;
			return result;
		}
		
		GetResponse response = client.get(new GetRequest(Importer.ES_INDEX, Importer.ES_TYPE_FILE, _id)).actionGet();
		result = SourcePathIndexerElement.fromESResponse(response);
		if (result != null) {
			return result;
		}
		response = client.get(new GetRequest(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, _id)).actionGet();
		result = SourcePathIndexerElement.fromESResponse(response);
		return result;
	}
	
	public String getStorageNameFromKey(String _id) {
		if (_id == null) {
			throw new NullPointerException("\"_id\" can't to be null");
		}
		
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
		request.setQuery(QueryBuilders.termQuery("_id", _id));
		
		SearchResponse response = request.execute().actionGet();
		
		if (response.getHits().totalHits() == 0) {
			return null;
		}
		
		SearchHit[] hits = response.getHits().hits();
		
		JSONObject jo = Elasticsearch.getJSONFromSimpleResponse(hits[0]);
		return (String) jo.get("storagename");
	}
	
	/**
	 * @return never null
	 * @throws IndexOutOfBoundsException if subelement count is > max_size
	 */
	public List<SourcePathIndexerElement> getDirectoryContentByIdkey(String _id, int max_size) throws IndexOutOfBoundsException {
		SearchRequestBuilder request = client.prepareSearch();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
		request.setQuery(QueryBuilders.termQuery("parentpath", _id.toLowerCase()));
		request.setFrom(0);
		request.setSize(max_size + 1);
		
		SearchResponse response = request.execute().actionGet();
		SearchHit[] hits = response.getHits().hits();
		ArrayList<SourcePathIndexerElement> result = new ArrayList<SourcePathIndexerElement>(hits.length);
		
		if (hits.length > max_size) {
			throw new IndexOutOfBoundsException(String.valueOf(hits.length));
		}
		
		for (int pos = 0; pos < hits.length; pos++) {
			result.add(SourcePathIndexerElement.fromESResponse(hits[pos]));
		}
		return result;
	}
	
	public long countDirectoryContentElements(String _id) {
		CountRequestBuilder request = new CountRequestBuilder(client);
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
		request.setQuery(QueryBuilders.termQuery("parentpath", _id.toLowerCase()));
		CountResponse response = request.execute().actionGet();
		return response.getCount();
	}
	
	private static HashMap<String, File> bridge;
	
	private static void populate_bridge() throws NullPointerException {
		if (bridge == null) {
			if (Configuration.global.isElementExists("storageindex_bridge") == false) {
				throw new NullPointerException("No configuration for storageindex_bridge");
			}
			bridge = new HashMap<String, File>();
			LinkedHashMap<String, String> s_bridge = Configuration.global.getValues("storageindex_bridge");
			for (Map.Entry<String, String> entry : s_bridge.entrySet()) {
				bridge.put(entry.getKey(), (new File(entry.getValue())).getAbsoluteFile());
			}
		}
	}
	
	public static File getLocalBridgedElement(SourcePathIndexerElement element) {
		if (element == null) {
			return null;
		}
		populate_bridge();
		
		File base_path = bridge.get(element.storagename);
		if (base_path == null) {
			return null;
		}
		return new File(base_path.getPath() + element.currentpath);
	}
	
	public static ArrayList<String> getBridgedStoragesName() {
		populate_bridge();
		ArrayList<String> list = new ArrayList<String>();
		for (Map.Entry<String, File> entry : bridge.entrySet()) {
			list.add(entry.getKey());
		}
		return list;
	}
	
	public DeleteRequestBuilder deleteRequestFileElement(String _id) {
		return client.prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, _id);
	}
	
}
