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

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class StatResult {
	
	LinkedHashMap<String, StatResultElement> selected_path_elements;// TODO set to private
	transient List<SourcePathIndexerElement> all_path_elements;// TODO set to private
	
	void setReference(String item_key, SourcePathIndexerElement reference) {
		selected_path_elements.get(item_key).reference = reference;
		if (all_path_elements == null) {
			all_path_elements = new ArrayList<SourcePathIndexerElement>();
		}
		all_path_elements.add(reference);
	}
	
	void setItemTotalCount(String item_key, Long count) {
		selected_path_elements.get(item_key).items_total = count;
	}
	
	void setMtdSummary(String item_key, Map<String, Object> mtdsummary) {
		selected_path_elements.get(item_key).mtdsummary = mtdsummary;
	}
	
	List<SourcePathIndexerElement> getAllPathElements() {
		return all_path_elements;
	}
	
	StatResult(List<String> pathelementskeys) {
		selected_path_elements = new LinkedHashMap<String, StatResultElement>();
		for (int pos_k = 0; pos_k < pathelementskeys.size(); pos_k++) {
			selected_path_elements.put(pathelementskeys.get(pos_k), new StatResultElement());
		}
	}
	
}
