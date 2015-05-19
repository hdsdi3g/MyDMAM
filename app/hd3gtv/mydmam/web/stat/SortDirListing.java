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

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

public class SortDirListing {
	
	public enum Col {
		sortedfilename, date, directory, size
	};
	
	public Col colname;
	public SortOrder order;
	
	public SortDirListing() {
	}
	
	public SortDirListing(Col colname, SortOrder order) {
		this.colname = colname;
		this.order = order;
	}
	
	SortBuilder getESSort() {
		return SortBuilders.fieldSort(colname.name().toLowerCase()).order(order);
	}
	
	/**
	 * @return null if sort is null or empty
	 */
	static List<SortBuilder> mergue(List<SortDirListing> sort) {
		ArrayList<SortBuilder> result = new ArrayList<SortBuilder>();
		if (sort == null) {
			return null;
		}
		if (sort.isEmpty()) {
			return null;
		}
		for (int pos = 0; pos < sort.size(); pos++) {
			result.add(sort.get(pos).getESSort());
		}
		return result;
	}
	
}
