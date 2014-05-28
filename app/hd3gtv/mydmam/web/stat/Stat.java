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

import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class Stat {
	
	private ArrayList<String> scopes_element;
	private ArrayList<String> scopes_subelements;
	private LinkedHashMap<String, StatElement> pathelements;
	private List<String> pathelementskeys;
	private int from = 0;
	private int size = 100;
	
	public Stat(String[] pathelementskeys, String[] array_scopes_element, String[] array_scopes_subelements) {
		if (pathelementskeys != null) {
			if (pathelementskeys.length > 0) {
				pathelements = new LinkedHashMap<String, StatElement>(pathelementskeys.length);
				this.pathelementskeys = new ArrayList<String>(pathelementskeys.length);
				for (int pos_k = 0; pos_k < pathelementskeys.length; pos_k++) {
					pathelements.put(pathelementskeys[pos_k], new StatElement());
					this.pathelementskeys.add(pathelementskeys[pos_k]);
				}
			} else {
				pathelements = new LinkedHashMap<String, StatElement>(1);
				this.pathelementskeys = new ArrayList<String>(1);
			}
		} else {
			pathelements = new LinkedHashMap<String, StatElement>(1);
			this.pathelementskeys = new ArrayList<String>(1);
		}
		
		if (array_scopes_subelements != null) {
			if (array_scopes_subelements.length > 0) {
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
	
	public Stat setFrom(int from) throws IndexOutOfBoundsException {
		if (from < 0) {
			throw new IndexOutOfBoundsException("Too low: " + from);
		}
		this.from = from;
		return this;
	}
	
	public Stat setSize(int size) throws IndexOutOfBoundsException {
		if (size < 1) {
			throw new IndexOutOfBoundsException("Too low: " + size);
		}
		this.size = size;
		return this;
	}
	
	public String toJSONString() {
		
		/*
		TODO scopes_element SCOPE_MTD_SUMMARY
		TODO scopes_element SCOPE_MTD_PREVIEW

		TODO scopes_subelements SCOPE_MTD_SUMMARY
		TODO scopes_subelements SCOPE_MTD_PREVIEW
		* */
		
		Explorer explorer = new Explorer();
		
		if (scopes_element.contains(StatElement.SCOPE_PATHINFO)) {
			HashMap<String, SourcePathIndexerElement> map_elements_resolved = explorer.getelementByIdkeys(pathelementskeys);
			for (Map.Entry<String, StatElement> entry : pathelements.entrySet()) {
				if (map_elements_resolved.containsKey(entry.getKey()) == false) {
					continue;
				}
				entry.getValue().path = map_elements_resolved.get(entry.getKey());
				entry.getValue().countsubelements = explorer.countDirectoryContentElements(entry.getKey());
			}
		}
		
		if (scopes_element.contains(StatElement.SCOPE_DIRLIST)) {
			Map<String, List<SourcePathIndexerElement>> map_dir_list = explorer.getDirectoryContentByIdkeys(pathelementskeys, from, size);
			
			List<SourcePathIndexerElement> dir_list;
			for (Map.Entry<String, StatElement> entry : pathelements.entrySet()) {
				if (map_dir_list.containsKey(entry.getKey()) == false) {
					continue;
				}
				dir_list = map_dir_list.get(entry.getKey());
				if (dir_list.isEmpty()) {
					continue;
				}
				
				entry.getValue().items = new ArrayList<StatElement>(dir_list.size());
				for (int pos = 0; pos < dir_list.size(); pos++) {
					StatElement s_element = new StatElement();
					s_element.path = dir_list.get(pos);
					s_element.countsubelements = explorer.countDirectoryContentElements(s_element.path.prepare_key());
					entry.getValue().items.add(s_element);
				}
			}
		}
		
		/*
		 * 	private int from = 0;
			private int size = 100;
		 * */
		
		/*if (scope.equalsIgnoreCase("pathinfo")) {
			 // Size, date
		} else if (scope.equalsIgnoreCase("dirlist")) {
			 // get dirlist content : size, date, and sub element count
		} else if (scope.equalsIgnoreCase("summary")) {
		} else if (scope.equalsIgnoreCase("")) {
		}*/
		
		Gson gson = new Gson();
		return gson.toJson(pathelements);
	}
}
