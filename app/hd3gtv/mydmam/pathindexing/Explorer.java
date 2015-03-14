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
package hd3gtv.mydmam.pathindexing;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.db.ElasticsearchMultiGetRequest;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;
import hd3gtv.mydmam.web.SearchResult;
import hd3gtv.mydmam.web.stat.Stat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

public class Explorer {
	
	public ArrayList<SourcePathIndexerElement> getByStorageFilenameAndSize(String storagename, final String filename, long size) throws Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("storagename", storagename.toLowerCase())).must(QueryBuilders.termQuery("size", size)));
		
		final ArrayList<SourcePathIndexerElement> result = new ArrayList<SourcePathIndexerElement>();
		request.allReader(new ElastisearchCrawlerHit() {
			public boolean onFoundHit(SearchHit hit) throws Exception {
				SourcePathIndexerElement element = SourcePathIndexerElement.fromESResponse(hit);
				if (element.currentpath.endsWith(filename)) {
					result.add(element);
				}
				return true;
			}
		});
		
		if (result.size() == 0) {
			return null;
		} else {
			return result;
		}
	}
	
	private void crawler(ElastisearchCrawlerReader request, final IndexingEvent found_elements_observer) throws Exception {
		request.allReader(new ElastisearchCrawlerHit() {
			public boolean onFoundHit(SearchHit hit) throws Exception {
				found_elements_observer.onFoundElement(SourcePathIndexerElement.fromESResponse(hit));
				return true;
			}
		});
	}
	
	public void getAllId(String id, IndexingEvent found_elements_observer) throws Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.termQuery("id", id.toLowerCase()));
		request.setSize(100);
		crawler(request, found_elements_observer);
	}
	
	public ArrayList<SourcePathIndexerElement> getAllIdFromStorage(String id, String storagename) throws Exception {
		final ArrayList<SourcePathIndexerElement> result = new ArrayList<SourcePathIndexerElement>();
		
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("id", id.toLowerCase())).must(QueryBuilders.termQuery("storagename", storagename.toLowerCase())));
		request.setSize(100);
		
		crawler(request, new IndexingEvent() {
			public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
				result.add(element);
				return true;
			}
			
			public void onRemoveFile(String storagename, String path) throws Exception {
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
	public void getAllSubElementsFromElementKey(String parentpath_key, final long min_index_date, final IndexingEvent found_elements_observer) throws Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
		
		if (min_index_date == 0) {
			request.setQuery(QueryBuilders.termQuery("parentpath", parentpath_key));
		} else {
			request.setQuery(QueryBuilders.boolQuery().must(QueryBuilders.termQuery("parentpath", parentpath_key))
					.must(QueryBuilders.rangeQuery("dateindex").from(min_index_date).to(System.currentTimeMillis() + 1000000)));
		}
		request.setSize(500);
		
		request.allReader(new ElastisearchCrawlerHit() {
			public boolean onFoundHit(SearchHit hit) throws Exception {
				SourcePathIndexerElement element = SourcePathIndexerElement.fromESResponse(hit);
				if (found_elements_observer.onFoundElement(element) == false) {
					return true;
				}
				if (element.directory) {
					getAllSubElementsFromElementKey(element.prepare_key(), min_index_date, found_elements_observer);
				}
				return true;
			}
		});
		
	}
	
	/**
	 * Not recursive
	 */
	public void getChildElementsFromElementKey(String parentpath_key, final IndexingEvent found_elements_observer, String... types) throws Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(types);
		request.setQuery(QueryBuilders.termQuery("parentpath", parentpath_key));
		request.setSize(500);
		
		request.allReader(new ElastisearchCrawlerHit() {
			public boolean onFoundHit(SearchHit hit) throws Exception {
				return found_elements_observer.onFoundElement(SourcePathIndexerElement.fromESResponse(hit));
			}
		});
	}
	
	public void getAllStorage(String storagename, IndexingEvent found_elements_observer) throws Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE);
		request.setQuery(QueryBuilders.termQuery("storagename", storagename.toLowerCase()));
		request.setSize(500);
		crawler(request, found_elements_observer);
	}
	
	public void getAllDirectoriesStorage(String storagename, IndexingEvent found_elements_observer) throws Exception {
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_DIRECTORY);
		request.setQuery(QueryBuilders.termQuery("storagename", storagename.toLowerCase()));
		request.setSize(500);
		crawler(request, found_elements_observer);
	}
	
	public SourcePathIndexerElement getelementByIdkey(String _id) {
		ArrayList<String> ids = new ArrayList<String>(1);
		ids.add(_id);
		HashMap<String, SourcePathIndexerElement> results;
		try {
			results = getelementByIdkeys(ids);
			if (results.isEmpty()) {
				return null;
			}
			return results.get(_id);
		} catch (Exception e) {
			Log2.log.error("Can't found in ES", e);
			return null;
		}
	}
	
	public LinkedHashMap<String, SourcePathIndexerElement> getelementByIdkeys(List<String> _ids) throws Exception {
		if (_ids == null) {
			return new LinkedHashMap<String, SourcePathIndexerElement>(1);
		}
		if (_ids.size() == 0) {
			return new LinkedHashMap<String, SourcePathIndexerElement>(1);
		}
		LinkedHashMap<String, SourcePathIndexerElement> result = new LinkedHashMap<String, SourcePathIndexerElement>(_ids.size());
		
		ArrayList<String> ids_to_query = new ArrayList<String>(_ids.size());
		SourcePathIndexerElement element;
		for (int pos = 0; pos < _ids.size(); pos++) {
			if (_ids.get(pos).equalsIgnoreCase(SourcePathIndexerElement.ROOT_DIRECTORY_KEY)) {
				/**
				 * Root path ? : return empty.
				 */
				element = new SourcePathIndexerElement();
				element.currentpath = null;
				element.date = 0;
				element.directory = true;
				element.id = null;
				element.parentpath = null;
				element.size = 0;
				element.storagename = null;
				result.put(_ids.get(pos), element);
			} else {
				ids_to_query.add(_ids.get(pos));
			}
		}
		
		if (ids_to_query.size() == 0) {
			return result;
		}
		
		ElasticsearchMultiGetRequest multigetrequestbuilder = Elasticsearch.prepareMultiGetRequest();
		multigetrequestbuilder.add(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, ids_to_query);
		multigetrequestbuilder.add(Importer.ES_INDEX, Importer.ES_TYPE_FILE, ids_to_query);
		
		List<GetResponse> responses = multigetrequestbuilder.responses();
		for (int pos = 0; pos < responses.size(); pos++) {
			result.put(responses.get(pos).getId(), SourcePathIndexerElement.fromESResponse(responses.get(pos)));
		}
		return result;
	}
	
	public List<String> getelementIfExists(List<String> _ids) throws Exception {
		if (_ids == null) {
			return new ArrayList<String>(1);
		}
		if (_ids.size() == 0) {
			return new ArrayList<String>(1);
		}
		List<String> result = new ArrayList<String>(_ids.size());
		
		ElasticsearchMultiGetRequest multigetrequestbuilder = Elasticsearch.prepareMultiGetRequest();
		multigetrequestbuilder.add(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, _ids);
		multigetrequestbuilder.add(Importer.ES_INDEX, Importer.ES_TYPE_FILE, _ids);
		
		List<GetResponse> responses = multigetrequestbuilder.responses();
		for (int pos = 0; pos < responses.size(); pos++) {
			result.add(responses.get(pos).getId());
		}
		return result;
	}
	
	public String getStorageNameFromKey(String _id) throws Exception {
		if (_id == null) {
			throw new NullPointerException("\"_id\" can't to be null");
		}
		
		ElastisearchCrawlerReader request = Elasticsearch.createCrawlerReader();
		request.setIndices(Importer.ES_INDEX);
		request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
		request.setQuery(QueryBuilders.termQuery("_id", _id));
		
		final ArrayList<SearchHit> hits = new ArrayList<SearchHit>(1);
		request.allReader(new ElastisearchCrawlerHit() {
			public boolean onFoundHit(SearchHit hit) throws Exception {
				hits.add(hit);
				return false;
			}
		});
		
		if (hits.isEmpty()) {
			return null;
		}
		
		return Elasticsearch.getJSONFromSimpleResponse(hits.get(0)).get("storagename").getAsString();
	}
	
	public class DirectoryContent {
		public LinkedHashMap<String, SourcePathIndexerElement> directory_content;
		public long directory_size;
		
		private DirectoryContent() {
		}
	}
	
	/**
	 * @param from for each _ids
	 * @param size for each _ids
	 * @param only_directories only search in "directory" type
	 * @param search only search with this text. Can be null.
	 * @return never null, _id parent key > element key > element
	 * @see Stat
	 */
	public LinkedHashMap<String, DirectoryContent> getDirectoryContentByIdkeys(List<String> _ids, int from, int size, boolean only_directories, String search) {
		if (_ids == null) {
			return new LinkedHashMap<String, DirectoryContent>(1);
		}
		if (_ids.size() == 0) {
			return new LinkedHashMap<String, DirectoryContent>(1);
		}
		
		MultiSearchRequestBuilder multisearchrequestbuilder = new MultiSearchRequestBuilder(Elasticsearch.getClient());
		
		for (int pos = 0; pos < _ids.size(); pos++) {
			String _id = _ids.get(pos);
			SearchRequestBuilder request = Elasticsearch.getClient().prepareSearch();
			request.setIndices(Importer.ES_INDEX);
			if (only_directories) {
				request.setTypes(Importer.ES_TYPE_DIRECTORY);
			} else {
				request.setTypes(Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
			}
			
			TermQueryBuilder querybuilder_parent = QueryBuilders.termQuery("parentpath", _id.toLowerCase());
			if (search == null) {
				request.setQuery(querybuilder_parent);
			} else {
				String query = SearchResult.cleanUserTextSearch(search);
				if (query == null) {
					request.setQuery(querybuilder_parent);
				} else if (query.equals("")) {
					request.setQuery(querybuilder_parent);
				} else {
					request.setQuery(QueryBuilders.boolQuery().must(querybuilder_parent).must(QueryBuilders.prefixQuery("_all", search)));
				}
			}
			
			request.setFrom(from * size);
			request.setSize(size);
			request.addSort("directory", SortOrder.DESC);
			request.addSort("idxfilename", SortOrder.ASC);
			multisearchrequestbuilder.add(request);
		}
		
		MultiSearchResponse.Item[] responses = multisearchrequestbuilder.execute().actionGet().getResponses();
		
		LinkedHashMap<String, DirectoryContent> map_dir_list = new LinkedHashMap<String, DirectoryContent>();
		
		SearchResponse response;
		SearchHit[] hits;
		String parent_key;
		SourcePathIndexerElement element;
		for (int pos = 0; pos < responses.length; pos++) {
			response = responses[pos].getResponse();
			hits = response.getHits().hits();
			if (hits.length == 0) {
				continue;
			}
			DirectoryContent directorycontent = new DirectoryContent();
			directorycontent.directory_size = response.getHits().getTotalHits();
			directorycontent.directory_content = new LinkedHashMap<String, SourcePathIndexerElement>(hits.length);
			
			parent_key = null;
			for (int pos_hits = 0; pos_hits < hits.length; pos_hits++) {
				element = SourcePathIndexerElement.fromESResponse(hits[pos_hits]);
				directorycontent.directory_content.put(hits[pos_hits].getId(), element);
				if (pos_hits == 0) {
					parent_key = element.parentpath;
				}
			}
			
			if (parent_key != null) {
				map_dir_list.put(parent_key, directorycontent);
			}
		}
		return map_dir_list;
	}
	
	public long countDirectoryContentElements(String _id) {
		return Elasticsearch.countRequest(Importer.ES_INDEX, QueryBuilders.termQuery("parentpath", _id.toLowerCase()), Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
	}
	
	public long countStorageContentElements(String storage_index_name) {
		return Elasticsearch.countRequest(Importer.ES_INDEX, QueryBuilders.termQuery("storagename", storage_index_name.toLowerCase()), Importer.ES_TYPE_FILE, Importer.ES_TYPE_DIRECTORY);
	}
	
	private static HashMap<String, File> bridge;
	private static ArrayList<String> bridge_list;
	
	private static void populate_bridge() throws NullPointerException {
		if (bridge == null) {
			bridge = new HashMap<String, File>();
			bridge_list = new ArrayList<String>();
			if (Configuration.global.isElementExists("storageindex_bridge") == false) {
				Log2.log.error("No configuration for storageindex_bridge", new NullPointerException());
				return;
			}
			LinkedHashMap<String, String> s_bridge = Configuration.global.getValues("storageindex_bridge");
			for (Map.Entry<String, String> entry : s_bridge.entrySet()) {
				bridge.put(entry.getKey(), (new File(entry.getValue())).getAbsoluteFile());
				bridge_list.add(entry.getKey());
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
		return bridge_list;
	}
	
	private class IndexingDelete implements IndexingEvent {
		
		ElasticsearchBulkOperation bulk_op;
		
		public IndexingDelete(ElasticsearchBulkOperation bulk_op) {
			this.bulk_op = bulk_op;
		}
		
		public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
			if (element.directory) {
				bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, element.prepare_key()));
			} else {
				bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, element.prepare_key()));
			}
			return true;
		}
		
		public void onRemoveFile(String storagename, String path) throws Exception {
		}
		
	}
	
	/**
	 * Don't use Bridge, but use StorageManager and PathScan.
	 * Recursive.
	 */
	public void refreshStoragePath(ElasticsearchBulkOperation bulk_op, List<SourcePathIndexerElement> elements, boolean purge_before) throws Exception {
		PathScan pathscan = new PathScan();
		
		for (int pos = 0; pos < elements.size(); pos++) {
			if (elements.get(pos) == null) {
				continue;
			}
			if (purge_before) {
				if (elements.get(pos).directory) {
					getAllSubElementsFromElementKey(elements.get(pos).prepare_key(), 0, new IndexingDelete(bulk_op));
					bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, elements.get(pos).prepare_key()));
				} else {
					bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, elements.get(pos).prepare_key()));
				}
			}
			if (elements.get(pos).directory) {
				pathscan.refreshIndex(bulk_op, elements.get(pos).storagename, elements.get(pos).currentpath, false);
			} else {
				pathscan.refreshIndex(bulk_op, elements.get(pos).storagename, elements.get(pos).currentpath.substring(0, elements.get(pos).currentpath.lastIndexOf("/")), true);
			}
		}
	}
	
	/**
	 * Non recursive, only for this item.
	 * if purge_before, only do purge for file elements.
	 * Don't use Bridge, but use StorageManager and PathScan.
	 */
	public void refreshCurrentStoragePath(ElasticsearchBulkOperation bulk_op, List<SourcePathIndexerElement> elements, boolean purge_before) throws Exception {
		PathScan pathscan = new PathScan();
		
		for (int pos = 0; pos < elements.size(); pos++) {
			if (elements.get(pos) == null) {
				continue;
			}
			if (elements.get(pos).directory) {
				if (purge_before) {
					getChildElementsFromElementKey(elements.get(pos).prepare_key(), new IndexingDelete(bulk_op), Importer.ES_TYPE_FILE);
				}
				pathscan.refreshIndex(bulk_op, elements.get(pos).storagename, elements.get(pos).currentpath, true);
			} else {
				if (purge_before) {
					bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, elements.get(pos).prepare_key()));
				}
				pathscan.refreshIndex(bulk_op, elements.get(pos).storagename, elements.get(pos).currentpath.substring(0, elements.get(pos).currentpath.lastIndexOf("/")), true);
			}
		}
	}
	
	/**
	 * Recursive, delete only ES pathindex.
	 */
	public void deleteStoragePath(ElasticsearchBulkOperation bulk_op, List<SourcePathIndexerElement> elements) throws Exception {
		for (int pos = 0; pos < elements.size(); pos++) {
			if (elements.get(pos) == null) {
				continue;
			}
			if (elements.get(pos).directory) {
				getAllSubElementsFromElementKey(elements.get(pos).prepare_key(), 0, new IndexingDelete(bulk_op));
				bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_DIRECTORY, elements.get(pos).prepare_key()));
			} else {
				bulk_op.add(bulk_op.getClient().prepareDelete(Importer.ES_INDEX, Importer.ES_TYPE_FILE, elements.get(pos).prepare_key()));
			}
		}
	}
	
}
