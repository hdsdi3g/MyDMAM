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
import java.util.List;
import java.util.Map;

import play.cache.Cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

public class RequestResponseCache {
	
	public static final long CACHING_TTL_SEC = 5 * 60;
	public static final String CACHING_TTL_STR = "5mn";
	private static final String CACHE_PREFIX_NAME = RequestResponseCache.class.getSimpleName() + "_";
	private static final boolean DISPLAY_VERBOSE_LOG = true;
	
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
	
	public RequestResponseCache() {
		spie_cache_factory = new SourcePathIndexerElement_CacheFactory();
		count_dir_cache_factory = new CountDirectoryContentElements_CacheFactory();
	}
	
	private boolean isStorageIsExpired(String storage_name) {
		// TODO isStorageAsExpired
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
			return explorer.getelementByIdkeys(cache_reference_tags);
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
		HashMap<String, CountDirectoryItem> items = getItems(_ids, count_dir_cache_factory);
		
		HashMap<String, Long> result = new HashMap<String, Long>();
		for (int pos = 0; pos < _ids.size(); pos++) {
			if (items.containsKey(_ids.get(pos))) {
				result.put(_ids.get(pos), items.get(_ids.get(pos)).count);
			}
		}
		
		return result;
	}
	
	public HashMap<String, DirectoryContent> getDirectoryContentByIdkeys(List<String> _ids, int from, int size, boolean only_directories, String search) {
		// TODO cache
		return explorer.getDirectoryContentByIdkeys(_ids, from, size, only_directories, search);
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
