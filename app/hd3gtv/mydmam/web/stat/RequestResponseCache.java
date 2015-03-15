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
package hd3gtv.mydmam.web.stat;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.Containers;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Explorer.DirectoryContent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.GsonIgnoreStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import play.cache.Cache;
import play.jobs.JobsPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

public class RequestResponseCache {
	
	public static final long CACHING_TTL_SEC = 5 * 60;
	public static final String CACHING_TTL_STR = "5mn";
	public static final int DEFAULT_DIRLIST_PAGE_SIZE = 500;
	private static final String CACHE_PREFIX_NAME = RequestResponseCache.class.getSimpleName() + "_";
	static final boolean DISPLAY_VERBOSE_LOG = true;
	
	private static Explorer explorer;
	private static JsonParser json_parser;
	private static Gson gson_simple;
	
	static {
		explorer = new Explorer();
		json_parser = new JsonParser();
		
		GsonBuilder builder = new GsonBuilder();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		gson_simple = builder.create();
	}
	
	private SourcePathIndexerElement_CacheFactory spie_cache_factory;
	private CountDirectoryContentElements_CacheFactory count_dir_cache_factory;
	private DirectoryContent_CacheFactory directory_content_cache_factory;
	
	public RequestResponseCache() {
		spie_cache_factory = new SourcePathIndexerElement_CacheFactory();
		count_dir_cache_factory = new CountDirectoryContentElements_CacheFactory();
		directory_content_cache_factory = new DirectoryContent_CacheFactory();
	}
	
	private boolean isStorageIsExpired(String storage_name) {
		// TODO isStorageAsExpired
		// TODO add caching date to all jsons, and check it with cassandra
		return false;
	}
	
	public static <T> HashMap<String, T> getItems(List<String> cache_reference_tags, RequestResponseCacheFactory<T> cache_factory) throws Exception {
		HashMap<String, T> result = new HashMap<String, T>();
		
		String[] request = new String[cache_reference_tags.size()];
		for (int pos = 0; pos < cache_reference_tags.size(); pos++) {
			request[pos] = CACHE_PREFIX_NAME + cache_factory.getLocaleCategoryName() + "_" + cache_reference_tags.get(pos);
		}
		
		ArrayList<String> unset_cache_reference_tags = null;
		
		Map<String, Object> cache_results = Cache.get(request);
		String cache_key;
		String cache_reference_tag;
		T item;
		
		for (int pos = 0; pos < request.length; pos++) {
			cache_key = request[pos];
			cache_reference_tag = cache_reference_tags.get(pos);
			
			if (cache_results.containsKey(cache_key)) {
				if (cache_results.get(cache_key) != null) {
					item = cache_factory.deserializeThis((String) cache_results.get(cache_key));
					if (cache_factory.hasExpired(item) == false) {
						result.put(cache_reference_tag, item);
						continue;
					}
					
					if (DISPLAY_VERBOSE_LOG) {
						Log2.log.debug("Cache force delete", new Log2Dump("cache_key", cache_key));
					}
					Cache.safeDelete(cache_key);
					continue;
				}
			}
			
			if (unset_cache_reference_tags == null) {
				unset_cache_reference_tags = new ArrayList<String>();
			}
			unset_cache_reference_tags.add(cache_reference_tag);
		}
		
		if (unset_cache_reference_tags == null) {
			return result;
		}
		
		/**
		 * Some values are not get from Cache.
		 * So, we create it.
		 */
		HashMap<String, T> missing_values_return = cache_factory.makeValues(unset_cache_reference_tags);
		
		if (missing_values_return.isEmpty()) {
			return result;
		}
		
		/**
		 * And push it in Cache.
		 */
		String cache_value;
		for (Map.Entry<String, T> entry : missing_values_return.entrySet()) {
			cache_key = CACHE_PREFIX_NAME + cache_factory.getLocaleCategoryName() + "_" + entry.getKey();
			cache_value = cache_factory.serializeThis(entry.getValue());
			Cache.set(cache_key, cache_value, CACHING_TTL_STR);
			if (DISPLAY_VERBOSE_LOG) {
				Log2Dump dump = new Log2Dump("cache_key", cache_key);
				dump.add("cache_value", cache_value);
				Log2.log.debug("Cache set", dump);
			}
		}
		
		result.putAll(missing_values_return);
		return result;
	}
	
	private class SourcePathIndexerElement_CacheFactory implements RequestResponseCacheFactory<SourcePathIndexerElement> {
		
		public HashMap<String, SourcePathIndexerElement> makeValues(List<String> cache_reference_tags) throws Exception {
			HashMap<String, SourcePathIndexerElement> result = explorer.getelementByIdkeys(cache_reference_tags);
			
			ArrayList<String> prefetch_dir_list = new ArrayList<String>();
			for (Map.Entry<String, SourcePathIndexerElement> entry : result.entrySet()) {
				if (entry.getValue().directory) {
					prefetch_dir_list.add(makeCacheKeyForDirlist(entry.getKey(), DEFAULT_DIRLIST_PAGE_SIZE, false));
				}
			}
			if (prefetch_dir_list.isEmpty() == false) {
				JobsPlugin.executor.submit(new RequestResponsePrefetch<DirectoryContent>(prefetch_dir_list, directory_content_cache_factory));
			}
			
			return result;
		}
		
		public String serializeThis(SourcePathIndexerElement item) throws Exception {
			return item.toGson().toString();
		}
		
		public SourcePathIndexerElement deserializeThis(String value) throws Exception {
			return SourcePathIndexerElement.fromJson(json_parser.parse(value).getAsJsonObject());
		}
		
		public boolean hasExpired(SourcePathIndexerElement item) {
			return isStorageIsExpired(item.storagename);
		}
		
		public String getLocaleCategoryName() {
			return "pathindexkeys";
		}
		
	}
	
	private class CountDirectoryItem {
		private String storagename;
		private Long count;
	}
	
	private class CountDirectoryContentElements_CacheFactory implements RequestResponseCacheFactory<CountDirectoryItem> {
		
		public HashMap<String, CountDirectoryItem> makeValues(List<String> cache_reference_tags) throws Exception {
			HashMap<String, CountDirectoryItem> result = new HashMap<String, RequestResponseCache.CountDirectoryItem>();
			CountDirectoryItem item;
			
			HashMap<String, SourcePathIndexerElement> items = getItems(cache_reference_tags, spie_cache_factory);
			
			for (Map.Entry<String, SourcePathIndexerElement> entry : items.entrySet()) {
				item = new CountDirectoryItem();
				item.count = explorer.countDirectoryContentElements(entry.getKey());
				item.storagename = entry.getValue().storagename;
				result.put(entry.getKey(), item);
			}
			return result;
		}
		
		public String serializeThis(CountDirectoryItem item) throws Exception {
			return gson_simple.toJson(item);
		}
		
		public CountDirectoryItem deserializeThis(String value) throws Exception {
			return gson_simple.fromJson(value, CountDirectoryItem.class);
		}
		
		public boolean hasExpired(CountDirectoryItem item) {
			return isStorageIsExpired(item.storagename);
		}
		
		public String getLocaleCategoryName() {
			return "countdirectoryitem";
		}
	}
	
	public HashMap<String, SourcePathIndexerElement> getelementByIdkeys(List<String> _ids) throws Exception {
		return getItems(_ids, spie_cache_factory);
	}
	
	public HashMap<String, Long> countDirectoryContentElements(List<String> _ids) throws Exception {
		HashMap<String, SourcePathIndexerElement> elements = getelementByIdkeys(_ids);
		
		/**
		 * Keep only directories...
		 */
		ArrayList<String> directories_ids = new ArrayList<String>();
		for (Map.Entry<String, SourcePathIndexerElement> entry : elements.entrySet()) {
			if (entry.getValue().directory == false) {
				continue;
			}
			directories_ids.add(entry.getKey());
		}
		
		HashMap<String, CountDirectoryItem> items = getItems(directories_ids, count_dir_cache_factory);
		
		HashMap<String, Long> result = new HashMap<String, Long>();
		for (int pos = 0; pos < directories_ids.size(); pos++) {
			if (items.containsKey(directories_ids.get(pos))) {
				result.put(directories_ids.get(pos), items.get(directories_ids.get(pos)).count);
			}
		}
		
		return result;
	}
	
	private class DirectoryContent_CacheFactory implements RequestResponseCacheFactory<DirectoryContent> {
		
		public HashMap<String, DirectoryContent> makeValues(List<String> cache_reference_tags) throws Exception {
			String[] cache_reference_tag;
			/**
			 * Only one size by request.
			 */
			int fetch_size = 0;
			List<String> _ids = new ArrayList<String>();
			boolean only_directories = false;
			for (int pos = 0; pos < cache_reference_tags.size(); pos++) {
				cache_reference_tag = cache_reference_tags.get(pos).split("_");
				_ids.add(cache_reference_tag[0]);
				fetch_size = Integer.parseInt(cache_reference_tag[1]);
				only_directories = Boolean.parseBoolean(cache_reference_tag[2]);
			}
			
			/**
			 * add >>> "_" + size + "_" + only_directories <<< to keys
			 */
			LinkedHashMap<String, DirectoryContent> real_result = explorer.getDirectoryContentByIdkeys(_ids, 0, fetch_size, only_directories, null);
			if (real_result.isEmpty()) {
				return real_result;
			}
			
			HashMap<String, DirectoryContent> result = new HashMap<String, Explorer.DirectoryContent>(real_result.size());
			for (Map.Entry<String, Explorer.DirectoryContent> entry : real_result.entrySet()) {
				result.put(makeCacheKeyForDirlist(entry.getValue().pathindexkey, fetch_size, only_directories), entry.getValue());
			}
			
			return result;
		}
		
		public String serializeThis(DirectoryContent item) throws Exception {
			return item.toJson().toString();
		}
		
		public DirectoryContent deserializeThis(String value) throws Exception {
			return explorer.getDirectoryContentfromJson(json_parser.parse(value).getAsJsonObject());
		}
		
		public boolean hasExpired(DirectoryContent item) {
			if (item.storagename == null) {
				return true;
			}
			return isStorageIsExpired(item.storagename);
		}
		
		public String getLocaleCategoryName() {
			return "directorycontent";
		}
		
	}
	
	private static String makeCacheKeyForDirlist(String id, int fetch_size, boolean only_directories) {
		StringBuilder sb = new StringBuilder();
		sb.append(id);
		sb.append("_");
		sb.append(fetch_size);
		sb.append("_");
		sb.append(only_directories);
		return sb.toString();
	}
	
	public HashMap<String, DirectoryContent> getDirectoryContentByIdkeys(List<String> _ids, int from, int fetch_size, boolean only_directories, String search) throws Exception {
		if (search != null) {
			return explorer.getDirectoryContentByIdkeys(_ids, from, fetch_size, only_directories, search);
		}
		if (from > 0) {
			return explorer.getDirectoryContentByIdkeys(_ids, from, fetch_size, only_directories, search);
		}
		
		ArrayList<String> cache_ref_tags = new ArrayList<String>();
		for (int pos = 0; pos < _ids.size(); pos++) {
			cache_ref_tags.add(makeCacheKeyForDirlist(_ids.get(pos), fetch_size, only_directories));
		}
		
		HashMap<String, DirectoryContent> cache_result = getItems(cache_ref_tags, directory_content_cache_factory);
		if (cache_result.isEmpty()) {
			return cache_result;
		}
		/**
		 * remove "makeCacheKeyForDirlist" from keys
		 */
		HashMap<String, DirectoryContent> result = new HashMap<String, Explorer.DirectoryContent>(cache_result.size());
		for (Map.Entry<String, Explorer.DirectoryContent> entry : cache_result.entrySet()) {
			result.put(entry.getValue().pathindexkey, entry.getValue());
		}
		
		return result;
	}
	
	public Containers getContainersSummariesByPathIndex(List<SourcePathIndexerElement> pathelements) throws Exception {
		// TODO cache
		// ContainerOperations.getGson() Container.getSummary()
		return ContainerOperations.getByPathIndex(pathelements, true);
	}
	
	public Containers getContainersSummariesByPathIndexId(List<String> pathelement_keys) throws Exception {
		// TODO cache
		// ContainerOperations.getGson() Container.getSummary()
		return ContainerOperations.getByPathIndexId(pathelement_keys, true);
	}
	
}
