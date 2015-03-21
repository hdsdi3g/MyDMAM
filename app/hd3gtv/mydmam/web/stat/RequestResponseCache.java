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
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.ContainerOrigin;
import hd3gtv.mydmam.metadata.container.ContainerPreview;
import hd3gtv.mydmam.metadata.container.Containers;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Explorer.DirectoryContent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.pathindexing.WebCacheInvalidation;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import play.cache.Cache;
import play.jobs.JobsPlugin;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class RequestResponseCache {
	
	public static final int DEFAULT_DIRLIST_PAGE_SIZE = 500;
	private static final String CACHE_PREFIX_NAME = RequestResponseCache.class.getSimpleName() + "_";
	static final boolean DISPLAY_VERBOSE_LOG = true;
	
	private static Explorer explorer;
	
	static {
		explorer = new Explorer();
	}
	
	private SourcePathIndexerElement_CacheFactory spie_cache_factory;
	private CountDirectoryContentElements_CacheFactory count_dir_cache_factory;
	private DirectoryContent_CacheFactory directory_content_cache_factory;
	private ContainersSummaries_CacheFactory containers_summaries_cache_factory;
	
	public RequestResponseCache() {
		spie_cache_factory = new SourcePathIndexerElement_CacheFactory();
		count_dir_cache_factory = new CountDirectoryContentElements_CacheFactory();
		directory_content_cache_factory = new DirectoryContent_CacheFactory();
		containers_summaries_cache_factory = new ContainersSummaries_CacheFactory();
	}
	
	public static <E> HashMap<String, E> getItems(List<String> cache_reference_tags, RequestResponseCacheFactory<E> cache_factory) throws Exception {
		HashMap<String, E> result = new HashMap<String, E>();
		
		String[] request = new String[cache_reference_tags.size()];
		for (int pos = 0; pos < cache_reference_tags.size(); pos++) {
			request[pos] = CACHE_PREFIX_NAME + cache_factory.getClass().getSimpleName() + "_" + cache_reference_tags.get(pos);
		}
		
		ArrayList<String> unset_cache_reference_tags = null;
		
		Map<String, Object> cache_results = Cache.get(request);
		String cache_key;
		String cache_reference_tag;
		RequestResponseCacheExpirableItem<E> item;
		
		for (int pos = 0; pos < request.length; pos++) {
			cache_key = request[pos];
			cache_reference_tag = cache_reference_tags.get(pos);
			
			if (cache_results.containsKey(cache_key)) {
				if (cache_results.get(cache_key) != null) {
					item = cache_factory.deserializeThis((String) cache_results.get(cache_key));
					if (item.hasExpired() == false) {
						result.put(cache_reference_tag, item.getItem());
						continue;
					}
					
					if (DISPLAY_VERBOSE_LOG) {
						Log2.log.debug("Cache force delete", new Log2Dump("cache_key", cache_key));
					}
					Cache.safeDelete(cache_key);
					// continue;
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
		HashMap<String, RequestResponseCacheExpirableItem<E>> missing_values_return = cache_factory.makeValues(unset_cache_reference_tags);
		
		if (missing_values_return.isEmpty()) {
			return result;
		}
		
		/**
		 * And push it in Cache.
		 */
		String cache_value;
		for (Map.Entry<String, RequestResponseCacheExpirableItem<E>> entry : missing_values_return.entrySet()) {
			cache_key = CACHE_PREFIX_NAME + cache_factory.getClass().getSimpleName() + "_" + entry.getKey();
			cache_value = cache_factory.serializeThis(entry.getValue());
			Cache.set(cache_key, cache_value, WebCacheInvalidation.CACHING_CLIENT_TTL);
			if (DISPLAY_VERBOSE_LOG) {
				Log2Dump dump = new Log2Dump("cache_key", cache_key);
				dump.add("cache_value", cache_value);
				Log2.log.debug("Cache set", dump);
			}
		}
		
		for (Map.Entry<String, RequestResponseCacheExpirableItem<E>> entry : missing_values_return.entrySet()) {
			result.put(entry.getKey(), entry.getValue().getItem());
		}
		
		return result;
	}
	
	private class SourcePathIndexerElement_CacheFactory extends RequestResponseCacheFactory<SourcePathIndexerElement> {
		
		protected JsonElement toJson(SourcePathIndexerElement item) throws Exception {
			return item.toGson();
		}
		
		protected SourcePathIndexerElement fromJson(JsonElement value) throws Exception {
			return SourcePathIndexerElement.fromJson(value.getAsJsonObject());
		}
		
		HashMap<String, RequestResponseCacheExpirableItem<SourcePathIndexerElement>> makeValues(List<String> cache_reference_tags) throws Exception {
			HashMap<String, SourcePathIndexerElement> values = explorer.getelementByIdkeys(cache_reference_tags);
			
			HashMap<String, RequestResponseCacheExpirableItem<SourcePathIndexerElement>> result = new HashMap<String, RequestResponseCacheExpirableItem<SourcePathIndexerElement>>();
			for (Map.Entry<String, SourcePathIndexerElement> entry : values.entrySet()) {
				result.put(entry.getKey(), new RequestResponseCacheExpirableItem<SourcePathIndexerElement>(entry.getValue(), entry.getValue().storagename));
			}
			return result;
		}
	}
	
	private class CountDirectoryContentElements_CacheFactory extends RequestResponseCacheFactory<Long> {
		
		public HashMap<String, RequestResponseCacheExpirableItem<Long>> makeValues(List<String> cache_reference_tags) throws Exception {
			HashMap<String, RequestResponseCacheExpirableItem<Long>> result = new HashMap<String, RequestResponseCacheExpirableItem<Long>>();
			HashMap<String, SourcePathIndexerElement> items = getItems(cache_reference_tags, spie_cache_factory);
			
			for (Map.Entry<String, SourcePathIndexerElement> entry : items.entrySet()) {
				result.put(entry.getKey(), new RequestResponseCacheExpirableItem<Long>(explorer.countDirectoryContentElements(entry.getKey()), entry.getValue().storagename));
			}
			return result;
		}
		
		protected JsonElement toJson(Long item) throws Exception {
			return new JsonPrimitive(item);
		}
		
		protected Long fromJson(JsonElement value) throws Exception {
			return value.getAsLong();
		}
	}
	
	public HashMap<String, SourcePathIndexerElement> getelementByIdkeys(List<String> _ids) throws Exception {
		return getItems(_ids, spie_cache_factory);
	}
	
	public HashMap<String, Long> countDirectoryContentElements(List<String> _ids) throws Exception {
		HashMap<String, SourcePathIndexerElement> elements = getItems(_ids, spie_cache_factory);
		
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
		
		return getItems(directories_ids, count_dir_cache_factory);
	}
	
	private class DirectoryContent_CacheFactory extends RequestResponseCacheFactory<DirectoryContent> {
		
		public HashMap<String, RequestResponseCacheExpirableItem<DirectoryContent>> makeValues(List<String> cache_reference_tags) throws Exception {
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
			
			HashMap<String, RequestResponseCacheExpirableItem<DirectoryContent>> result = new HashMap<String, RequestResponseCacheExpirableItem<DirectoryContent>>();
			
			/**
			 * add >>> "_" + size + "_" + only_directories <<< to keys
			 */
			LinkedHashMap<String, DirectoryContent> real_result = explorer.getDirectoryContentByIdkeys(_ids, 0, fetch_size, only_directories, null);
			if (real_result.isEmpty()) {
				return result;
			}
			
			for (Map.Entry<String, Explorer.DirectoryContent> entry : real_result.entrySet()) {
				result.put(makeCacheKeyForDirlist(entry.getValue().pathindexkey, fetch_size, only_directories),
						new RequestResponseCacheExpirableItem<DirectoryContent>(entry.getValue(), entry.getValue().storagename));
			}
			
			return result;
		}
		
		protected JsonElement toJson(DirectoryContent item) throws Exception {
			return item.toJson();
		}
		
		protected DirectoryContent fromJson(JsonElement value) throws Exception {
			return explorer.getDirectoryContentfromJson(value.getAsJsonObject());
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
	
	public HashMap<String, DirectoryContent> getDirectoryContentByIdkeys(List<String> _ids, int from, int fetch_size, boolean only_directories, String search, boolean prefetch) throws Exception {
		HashMap<String, DirectoryContent> result;
		if (search != null) {
			result = explorer.getDirectoryContentByIdkeys(_ids, from, fetch_size, only_directories, search);
		} else if (from > 0) {
			result = explorer.getDirectoryContentByIdkeys(_ids, from, fetch_size, only_directories, search);
		} else {
			ArrayList<String> cache_ref_tags = new ArrayList<String>();
			for (int pos = 0; pos < _ids.size(); pos++) {
				cache_ref_tags.add(makeCacheKeyForDirlist(_ids.get(pos), fetch_size, only_directories));
			}
			
			HashMap<String, DirectoryContent> cache_result = getItems(cache_ref_tags, directory_content_cache_factory);
			
			if (cache_result.isEmpty()) {
				result = cache_result;
			} else {
				/**
				 * remove "makeCacheKeyForDirlist" from keys
				 */
				result = new HashMap<String, Explorer.DirectoryContent>(cache_result.size());
				for (Map.Entry<String, Explorer.DirectoryContent> entry : cache_result.entrySet()) {
					result.put(entry.getValue().pathindexkey, entry.getValue());
				}
			}
		}
		if (prefetch) {
			ArrayList<String> prefetch_dir_list = new ArrayList<String>();
			Map<String, SourcePathIndexerElement> item;
			for (Map.Entry<String, DirectoryContent> entry_dir_content : result.entrySet()) {
				item = entry_dir_content.getValue().directory_content;
				for (Map.Entry<String, SourcePathIndexerElement> entry : item.entrySet()) {
					if (entry.getValue().directory) {
						prefetch_dir_list.add(makeCacheKeyForDirlist(entry.getKey(), fetch_size, only_directories));
					}
				}
			}
			
			if (prefetch_dir_list.isEmpty() == false) {
				JobsPlugin.executor.submit(new RequestResponseCachePrefetch<DirectoryContent>(prefetch_dir_list, directory_content_cache_factory));
			}
		}
		
		return result;
	}
	
	public Map<String, Map<String, Object>> getContainersSummariesByPathIndexId(List<String> pathelement_keys) throws Exception {
		return getContainersSummariesByPathIndex(getItems(pathelement_keys, spie_cache_factory).values());
	}
	
	public Map<String, Map<String, Object>> getContainersSummariesByPathIndex(Collection<SourcePathIndexerElement> pathelements) throws Exception {
		ArrayList<String> cache_ref_tags = new ArrayList<String>();
		for (SourcePathIndexerElement item : pathelements) {
			cache_ref_tags.add(ContainerOrigin.getUniqueElementKey(item));
		}
		
		Map<String, ContainersSummaryCachedItem> raw_results = getItems(cache_ref_tags, containers_summaries_cache_factory);
		Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();
		
		for (ContainersSummaryCachedItem entry : raw_results.values()) {
			if (entry.summary == null) {
				continue;
			}
			result.put(entry.pathindexkey, entry.summary);
		}
		
		return result;
	}
	
	private static Type map_string_object_typeOfT = new TypeToken<Map<String, Object>>() {
	}.getType();
	
	private class ContainersSummaries_CacheFactory extends RequestResponseCacheFactory<ContainersSummaryCachedItem> {
		
		public HashMap<String, RequestResponseCacheExpirableItem<ContainersSummaryCachedItem>> makeValues(List<String> cache_reference_tags) throws Exception {
			HashMap<String, RequestResponseCacheExpirableItem<ContainersSummaryCachedItem>> result = new HashMap<String, RequestResponseCacheExpirableItem<ContainersSummaryCachedItem>>();
			
			Containers containers = ContainerOperations.multipleGetInMetadataBase(cache_reference_tags, EntrySummary.type);
			
			if (containers.size() == 0) {
				return result;
			}
			
			Container c;
			Map<String, String> summaries;
			LinkedHashMap<String, Object> item;
			HashMap<PreviewType, ContainerPreview> previews;
			for (int pos = 0; pos < containers.size(); pos++) {
				c = containers.getItemAtPos(pos);
				
				item = new LinkedHashMap<String, Object>();
				previews = c.getSummary().getPreviews();
				item.put("previews", previews);
				item.put("master_as_preview", c.getSummary().master_as_preview);
				item.put("mimetype", c.getSummary().getMimetype());
				
				summaries = c.getSummary().getSummaries();
				for (Map.Entry<String, String> entry : summaries.entrySet()) {
					item.put(entry.getKey(), entry.getValue());
				}
				
				ContainersSummaryCachedItem csc_item = new ContainersSummaryCachedItem();
				csc_item.summary = item;
				csc_item.pathindexkey = c.getOrigin().getKey();
				
				result.put(c.getMtd_key(), new RequestResponseCacheExpirableItem<ContainersSummaryCachedItem>(csc_item, c.getOrigin().getStorage()));
			}
			
			return result;
		}
		
		protected JsonElement toJson(ContainersSummaryCachedItem item) throws Exception {
			return Stat.gson_simple.toJsonTree(item);
		}
		
		protected ContainersSummaryCachedItem fromJson(JsonElement value) throws Exception {
			ContainersSummaryCachedItem result = Stat.gson_simple.fromJson(value, ContainersSummaryCachedItem.class);
			result.summary = Stat.gson_simple.fromJson(value.getAsJsonObject().get("summary"), map_string_object_typeOfT);
			return result;
		}
		
	}
	
	private class ContainersSummaryCachedItem {
		Map<String, Object> summary;
		String pathindexkey;
	}
	
}
