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
import hd3gtv.mydmam.manager.GsonIgnoreStrategy;
import hd3gtv.mydmam.metadata.PreviewType;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.Containers;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.metadata.container.Preview;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.indices.IndexMissingException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Stat {
	
	private ArrayList<String> scopes_element;
	private ArrayList<String> scopes_subelements;
	private LinkedHashMap<String, StatElement> selected_path_elements;
	private List<String> pathelementskeys;
	private int page_from = 0;
	private int page_size = 100;
	private String search;
	private Explorer explorer;
	private Gson gson_simple;
	private Gson gson;
	
	public Stat(String[] pathelementskeys, String[] array_scopes_element, String[] array_scopes_subelements) {
		explorer = new Explorer();
		
		GsonBuilder builder = new GsonBuilder();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		gson_simple = builder.create();
		
		StatElement.Serializer statelement_serializer = new StatElement.Serializer();
		builder.registerTypeAdapter(StatElement.class, statelement_serializer);
		gson = builder.create();
		statelement_serializer.gson = gson;
		statelement_serializer.gson_simple = gson_simple;
		
		if (pathelementskeys != null) {
			if (pathelementskeys.length > 0) {
				selected_path_elements = new LinkedHashMap<String, StatElement>(pathelementskeys.length);
				this.pathelementskeys = new ArrayList<String>(pathelementskeys.length);
				for (int pos_k = 0; pos_k < pathelementskeys.length; pos_k++) {
					selected_path_elements.put(pathelementskeys[pos_k], new StatElement());
					this.pathelementskeys.add(pathelementskeys[pos_k]);
				}
			} else {
				selected_path_elements = new LinkedHashMap<String, StatElement>(1);
				this.pathelementskeys = new ArrayList<String>(1);
			}
		} else {
			selected_path_elements = new LinkedHashMap<String, StatElement>(1);
			this.pathelementskeys = new ArrayList<String>(1);
		}
		
		if (array_scopes_element != null) {
			if (array_scopes_element.length > 0) {
				scopes_element = new ArrayList<String>(array_scopes_element.length);
				for (int pos_sc = 0; pos_sc < array_scopes_element.length; pos_sc++) {
					scopes_element.add(array_scopes_element[pos_sc]);
				}
			} else {
				scopes_element = new ArrayList<String>(1);
			}
		} else {
			scopes_element = new ArrayList<String>(1);
		}
		
		if (array_scopes_subelements != null) {
			if (array_scopes_subelements.length > 0) {
				scopes_subelements = new ArrayList<String>(array_scopes_subelements.length);
				for (int pos_sc = 0; pos_sc < array_scopes_subelements.length; pos_sc++) {
					scopes_subelements.add(array_scopes_subelements[pos_sc]);
				}
			} else {
				scopes_subelements = new ArrayList<String>(1);
			}
		} else {
			scopes_subelements = new ArrayList<String>(1);
		}
	}
	
	public Stat setPageFrom(int page_from) throws IndexOutOfBoundsException {
		if (page_from < 0) {
			throw new IndexOutOfBoundsException("Too low: " + page_from);
		}
		this.page_from = page_from;
		return this;
	}
	
	public Stat setPageSize(int page_size) throws IndexOutOfBoundsException {
		if (page_size < 1) {
			throw new IndexOutOfBoundsException("Too low: " + page_size);
		}
		this.page_size = page_size;
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
	
	private void populate_pathinfo() {
		LinkedHashMap<String, SourcePathIndexerElement> map_elements_resolved = explorer.getelementByIdkeys(pathelementskeys);
		boolean count_items = scopes_element.contains(StatElement.SCOPE_COUNT_ITEMS);
		
		for (Map.Entry<String, StatElement> entry : selected_path_elements.entrySet()) {
			if (map_elements_resolved.containsKey(entry.getKey()) == false) {
				continue;
			}
			entry.getValue().reference = map_elements_resolved.get(entry.getKey());
			if (count_items) {
				entry.getValue().items_total = explorer.countDirectoryContentElements(entry.getKey());
			}
		}
	}
	
	private LinkedHashMap<String, Explorer.DirectoryContent> populate_dirlists() {
		boolean count_items_for_request_dir = scopes_element.contains(StatElement.SCOPE_COUNT_ITEMS);
		boolean count_items = scopes_subelements.contains(StatElement.SCOPE_COUNT_ITEMS);
		boolean only_directories = scopes_subelements.contains(StatElement.SCOPE_ONLYDIRECTORIES);
		
		LinkedHashMap<String, Explorer.DirectoryContent> map_dir_list = explorer.getDirectoryContentByIdkeys(pathelementskeys, page_from, page_size, only_directories, search);
		
		LinkedHashMap<String, SourcePathIndexerElement> dir_list;
		for (Map.Entry<String, StatElement> entry : selected_path_elements.entrySet()) {
			if (map_dir_list.containsKey(entry.getKey()) == false) {
				continue;
			}
			dir_list = map_dir_list.get(entry.getKey()).content;
			if (dir_list.isEmpty()) {
				continue;
			}
			
			entry.getValue().items = new LinkedHashMap<String, StatElement>(dir_list.size());
			for (Map.Entry<String, SourcePathIndexerElement> dir_list_entry : dir_list.entrySet()) {
				StatElement s_element = new StatElement();
				s_element.reference = dir_list_entry.getValue();
				if (count_items) {
					s_element.items_total = explorer.countDirectoryContentElements(s_element.reference.prepare_key());
				}
				entry.getValue().items.put(dir_list_entry.getKey(), s_element);
			}
			
			if (count_items_for_request_dir) {
				entry.getValue().items_total = map_dir_list.get(entry.getKey()).directory_size;
			}
			entry.getValue().items_page_from = page_from;
			entry.getValue().items_page_size = page_size;
		}
		return map_dir_list;
	}
	
	private void populate_mtd_summary(boolean has_pathinfo) {
		Map<String, Map<String, Object>> summaries;
		
		if (has_pathinfo) {
			ArrayList<SourcePathIndexerElement> pathelements = new ArrayList<SourcePathIndexerElement>();
			for (StatElement statelement : selected_path_elements.values()) {
				if (statelement.reference == null) {
					continue;
				}
				pathelements.add(statelement.reference);
			}
			summaries = getSummariesByPathElements(pathelements);
		} else {
			summaries = getSummariesByPathElementKeys(pathelementskeys);
		}
		
		if (summaries != null) {
			for (Map.Entry<String, StatElement> entry : selected_path_elements.entrySet()) {
				if (summaries.containsKey(entry.getKey()) == false) {
					continue;
				}
				entry.getValue().mtdsummary = summaries.get(entry.getKey());
			}
		}
	}
	
	private static Map<String, Map<String, Object>> getSummariesByContainers(Containers containers) {
		if (containers == null) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		if (containers.size() == 0) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		
		Map<String, Map<String, Object>> result = new LinkedHashMap<String, Map<String, Object>>(containers.size());
		
		Container c;
		Map<String, String> summaries;
		LinkedHashMap<String, Object> item;
		HashMap<PreviewType, Preview> previews;
		for (int pos = 0; pos < containers.size(); pos++) {
			c = containers.getItemAtPos(pos);
			if (c.getSummary().getSummaries().isEmpty()) {
				continue;
			}
			item = new LinkedHashMap<String, Object>();
			
			summaries = c.getSummary().getSummaries();
			for (Map.Entry<String, String> entry : summaries.entrySet()) {
				item.put(entry.getKey(), entry.getValue());
			}
			
			previews = c.getSummary().getPreviews();
			item.put("previews", previews);
			item.put("master_as_preview", c.getSummary().master_as_preview);
			item.put("mimetype", c.getSummary().getMimetype());
			
			result.put(c.getOrigin().getKey(), item);
		}
		return result;
	}
	
	private static Map<String, Map<String, Object>> getSummariesByPathElements(List<SourcePathIndexerElement> pathelements) throws IndexMissingException {
		if (pathelements == null) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		if (pathelements.size() == 0) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		return getSummariesByContainers(Operations.getByPathIndex(pathelements, true));
	}
	
	private static Map<String, Map<String, Object>> getSummariesByPathElementKeys(List<String> pathelementkeys) throws IndexMissingException {
		if (pathelementkeys == null) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		if (pathelementkeys.size() == 0) {
			return new LinkedHashMap<String, Map<String, Object>>(1);
		}
		return getSummariesByContainers(Operations.getByPathIndexId(pathelementkeys, true));
	}
	
	private void populate_dir_list_mtd_summary(LinkedHashMap<String, Explorer.DirectoryContent> map_dir_list) {
		ArrayList<SourcePathIndexerElement> pathelements = new ArrayList<SourcePathIndexerElement>();
		Map<String, Map<String, Object>> summaries = null;
		
		for (Map.Entry<String, Explorer.DirectoryContent> dir_list : map_dir_list.entrySet()) {
			pathelements.addAll(dir_list.getValue().content.values());
		}
		summaries = getSummariesByPathElements(pathelements);
		
		Map<String, StatElement> items;
		for (Map.Entry<String, StatElement> entry : selected_path_elements.entrySet()) {
			items = entry.getValue().items;
			if (items == null) {
				/**
				 * If can't found a selected_path_element
				 */
				continue;
			}
			for (Map.Entry<String, StatElement> entry_item : items.entrySet()) {
				if (summaries.containsKey(entry_item.getKey()) == false) {
					continue;
				}
				entry_item.getValue().mtdsummary = summaries.get(entry_item.getKey());
			}
		}
	}
	
	public String toJSONString() {
		try {
			if (scopes_element.contains(StatElement.SCOPE_PATHINFO)) {
				populate_pathinfo();
			}
			
			LinkedHashMap<String, Explorer.DirectoryContent> map_dir_list = null;
			
			if (scopes_element.contains(StatElement.SCOPE_DIRLIST)) {
				map_dir_list = populate_dirlists();
			}
			
			if (scopes_element.contains(StatElement.SCOPE_MTD_SUMMARY)) {
				populate_mtd_summary(scopes_element.contains(StatElement.SCOPE_PATHINFO));
			}
			
			if (scopes_subelements.contains(StatElement.SCOPE_MTD_SUMMARY)) {
				if (map_dir_list == null) {
					map_dir_list = populate_dirlists();
				}
				populate_dir_list_mtd_summary(map_dir_list);
			}
			
		} catch (IndexMissingException e) {
			Log2.log.error("Some ES indexes are missing: database has not items for this", e);
		}
		
		return gson.toJson(selected_path_elements);
	}
}
