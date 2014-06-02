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
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.indices.IndexMissingException;

import com.google.gson.Gson;

public class Stat {
	
	private ArrayList<String> scopes_element;
	private ArrayList<String> scopes_subelements;
	private LinkedHashMap<String, StatElement> selected_path_elements;
	private List<String> pathelementskeys;
	private int page_from = 0;
	private int page_size = 100;
	private Explorer explorer;
	
	public Stat(String[] pathelementskeys, String[] array_scopes_element, String[] array_scopes_subelements) {
		explorer = new Explorer();
		
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
	
	private void populate_pathinfo() {
		HashMap<String, SourcePathIndexerElement> map_elements_resolved = explorer.getelementByIdkeys(pathelementskeys);
		boolean count_items = scopes_element.contains(StatElement.SCOPE_COUNT_ITEMS);
		
		for (Map.Entry<String, StatElement> entry : selected_path_elements.entrySet()) {
			if (map_elements_resolved.containsKey(entry.getKey()) == false) {
				continue;
			}
			entry.getValue().spie_reference = map_elements_resolved.get(entry.getKey());
			entry.getValue().reference = entry.getValue().spie_reference.toJson();
			if (count_items) {
				entry.getValue().items_total = explorer.countDirectoryContentElements(entry.getKey());
			}
		}
	}
	
	private HashMap<String, HashMap<String, SourcePathIndexerElement>> populate_dirlists() {
		boolean count_items = scopes_subelements.contains(StatElement.SCOPE_COUNT_ITEMS);
		
		HashMap<String, HashMap<String, SourcePathIndexerElement>> map_dir_list = explorer.getDirectoryContentByIdkeys(pathelementskeys, page_from, page_size);
		
		HashMap<String, SourcePathIndexerElement> dir_list;
		for (Map.Entry<String, StatElement> entry : selected_path_elements.entrySet()) {
			if (map_dir_list.containsKey(entry.getKey()) == false) {
				continue;
			}
			dir_list = map_dir_list.get(entry.getKey());
			if (dir_list.isEmpty()) {
				continue;
			}
			
			entry.getValue().items = new HashMap<String, StatElement>(dir_list.size());
			for (Map.Entry<String, SourcePathIndexerElement> dir_list_entry : dir_list.entrySet()) {
				StatElement s_element = new StatElement();
				s_element.spie_reference = dir_list_entry.getValue();
				s_element.reference = s_element.spie_reference.toJson();
				if (count_items) {
					s_element.items_total = explorer.countDirectoryContentElements(s_element.spie_reference.prepare_key());
				}
				entry.getValue().items.put(dir_list_entry.getKey(), s_element);
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
				if (statelement.spie_reference == null) {
					continue;
				}
				pathelements.add(statelement.spie_reference);
			}
			summaries = MetadataCenter.getSummariesByPathElements(pathelements);
		} else {
			summaries = MetadataCenter.getSummariesByPathElementKeys(pathelementskeys);
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
	
	private void populate_dir_list_mtd_summary(HashMap<String, HashMap<String, SourcePathIndexerElement>> map_dir_list) {
		ArrayList<SourcePathIndexerElement> pathelements = new ArrayList<SourcePathIndexerElement>();
		Map<String, Map<String, Object>> summaries = null;
		
		for (Map.Entry<String, HashMap<String, SourcePathIndexerElement>> dir_list : map_dir_list.entrySet()) {
			pathelements.addAll(dir_list.getValue().values());
		}
		summaries = MetadataCenter.getSummariesByPathElements(pathelements);
		
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
			
			HashMap<String, HashMap<String, SourcePathIndexerElement>> map_dir_list = null;
			
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
		
		Gson gson = new Gson();
		return gson.toJson(selected_path_elements);
	}
}
