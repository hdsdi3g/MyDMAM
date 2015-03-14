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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.web.stat;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerPreview;
import hd3gtv.mydmam.metadata.container.Containers;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.GsonIgnoreStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.indices.IndexMissingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Stat {
	
	public static final String SCOPE_DIRLIST = "dirlist";
	public static final String SCOPE_PATHINFO = "pathinfo";
	public static final String SCOPE_MTD_SUMMARY = "mtdsummary";
	public static final String SCOPE_COUNT_ITEMS = "countitems";
	public static final String SCOPE_ONLYDIRECTORIES = "onlydirs";
	
	static Gson gson_simple;
	static Gson gson;
	
	@Deprecated
	static Explorer explorer;
	static RequestResponseCache request_response_cache;
	
	static {
		request_response_cache = new RequestResponseCache();
		
		GsonBuilder builder = new GsonBuilder();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Serializer());
		builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Deserializer());
		gson_simple = builder.create();
		
		builder.registerTypeAdapter(StatResult.class, new StatResult.Serializer());
		builder.registerTypeAdapter(StatResultElement.class, new StatResultElement.Serializer());
		builder.registerTypeAdapter(StatResultSubElement.class, new StatResultSubElement.Serializer());
		gson = builder.create();
	}
	
	private StatResult result;
	
	private boolean request_dir_count_items = false;
	private boolean sub_items_count_items = false;
	private boolean sub_items_only_directories = false;
	private boolean request_dir_pathinfo = false;
	private boolean request_dir_mtd_summary = false;
	private boolean request_dir_dir_list = false;
	private boolean sub_items_mtd_summary = false;
	
	/**
	 * Never empty
	 */
	private List<String> pathelementskeys;
	private int page_from = 0;
	private int page_size = 100;
	private String search;
	
	public Stat(String[] request_pathelementskeys, String[] request_scopes_element, String[] request_scopes_subelements) throws IndexOutOfBoundsException, NullPointerException {
		if (request_scopes_element != null) {
			List<String> scopes_element = Arrays.asList(request_scopes_element);
			request_dir_count_items = scopes_element.contains(SCOPE_COUNT_ITEMS);
			request_dir_pathinfo = scopes_element.contains(SCOPE_PATHINFO);
			request_dir_dir_list = scopes_element.contains(SCOPE_DIRLIST);
			request_dir_mtd_summary = scopes_element.contains(SCOPE_MTD_SUMMARY);
		}
		
		if (request_scopes_subelements != null) {
			List<String> scopes_subelements = Arrays.asList(request_scopes_subelements);
			sub_items_count_items = scopes_subelements.contains(SCOPE_COUNT_ITEMS);
			sub_items_only_directories = scopes_subelements.contains(SCOPE_ONLYDIRECTORIES);
			sub_items_mtd_summary = scopes_subelements.contains(SCOPE_MTD_SUMMARY);
		}
		
		if (request_pathelementskeys != null) {
			if (request_pathelementskeys.length == 0) {
				throw new IndexOutOfBoundsException("request_pathelementskeys");
			}
			pathelementskeys = Arrays.asList(request_pathelementskeys);
		} else {
			throw new NullPointerException("request_pathelementskeys");
		}
		
		result = new StatResult(pathelementskeys);
	}
	
	public Stat setPageFrom(int page_from) throws IndexOutOfBoundsException {
		if (page_from < 0) {
			throw new IndexOutOfBoundsException("Too low: " + page_from);
		}
		this.page_from = page_from;
		result.setPage_from(page_from);
		return this;
	}
	
	public Stat setPageSize(int page_size) throws IndexOutOfBoundsException {
		if (page_size < 1) {
			throw new IndexOutOfBoundsException("Too low: " + page_size);
		}
		this.page_size = page_size;
		result.setPage_size(page_size);
		return this;
	}
	
	public Stat setJsonSearch(String json_search) {
		if (json_search == null) {
			return this;
		}
		search = gson.fromJson(json_search, String.class);
		if (search.trim().equals("")) {
			search = null;
		}
		return this;
	}
	
	private static Map<String, Map<String, Object>> getSummariesByContainers(Containers containers) {
		if (containers.size() == 0) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>(containers.size());
		
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
			
			result.put(c.getOrigin().getKey(), item);
		}
		return result;
	}
	
	public String toJSONString() {
		try {
			if (request_dir_pathinfo) {
				/**
				 * populate pathinfo
				 */
				LinkedHashMap<String, SourcePathIndexerElement> map_elements_resolved = request_response_cache.getelementByIdkeys(pathelementskeys);
				String item_key;
				for (int pos = 0; pos < pathelementskeys.size(); pos++) {
					item_key = pathelementskeys.get(pos);
					
					if (map_elements_resolved.containsKey(item_key) == false) {
						continue;
					}
					result.setReference(item_key, map_elements_resolved.get(item_key));
					if (request_dir_count_items) {
						result.setItemTotalCount(item_key, request_response_cache.countDirectoryContentElements(item_key));
					}
				}
			}
			
			LinkedHashMap<String, Explorer.DirectoryContent> map_dir_list = null;
			
			if (request_dir_dir_list | sub_items_mtd_summary) {
				map_dir_list = request_response_cache.getDirectoryContentByIdkeys(pathelementskeys, page_from, page_size, sub_items_only_directories, search);
				result.populateDirListsForItems(map_dir_list, sub_items_count_items, request_dir_count_items);
			}
			
			Map<String, Map<String, Object>> summaries = null;
			
			if (request_dir_mtd_summary) {
				if (request_dir_pathinfo) {
					summaries = getSummariesByContainers(request_response_cache.getContainersByPathIndex(result.getAllPathElements(), true));
				} else {
					summaries = getSummariesByContainers(request_response_cache.getContainersByPathIndexId(pathelementskeys, true));
				}
				
				if (summaries.isEmpty() == false) {
					for (Map.Entry<String, Map<String, Object>> entry : summaries.entrySet()) {
						result.setMtdSummary(entry.getKey(), entry.getValue());
					}
				}
			}
			
			if (sub_items_mtd_summary) {
				ArrayList<SourcePathIndexerElement> pathelements = new ArrayList<SourcePathIndexerElement>();
				for (Map.Entry<String, Explorer.DirectoryContent> dir_list : map_dir_list.entrySet()) {
					pathelements.addAll(dir_list.getValue().directory_content.values());
				}
				if (pathelements.isEmpty() == false) {
					summaries = getSummariesByContainers(request_response_cache.getContainersByPathIndex(pathelements, true));
					result.populateSummariesForItems(summaries);
				}
			}
			
		} catch (IndexMissingException e) {
			Log2.log.error("Some ES indexes are missing: database has not items for this", e);
		} catch (Exception e) {
			Log2.log.error("General error", e);
		}
		return gson.toJson(result);
	}
	
}
