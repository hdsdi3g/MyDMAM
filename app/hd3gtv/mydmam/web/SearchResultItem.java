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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.web;

import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.search.SearchHit;

/**
 * @deprecated
 */
public class SearchResultItem implements Log2Dumpable {
	
	public String index;
	public String type;
	public float score;
	public String id;
	public Map<String, Object> source;
	
	static SearchResultItem fromSource(SearchHit hit) {
		SearchResultItem item = new SearchResultItem();
		item.index = hit.getIndex();
		item.type = hit.getType();
		item.score = hit.getScore();
		item.source = hit.getSource();
		item.id = hit.getId();
		return item;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("index", index);
		dump.add("type", type);
		dump.add("score", score);
		dump.add("id", id);
		dump.add("source", source);
		return dump;
	}
	
	public HashMap<String, Object> getArgs() {
		HashMap<String, Object> args = new HashMap<String, Object>();
		args.put("source", source);
		args.put("index", index);
		args.put("score", score);
		args.put("type", type);
		args.put("id", id);
		return args;
	}
	
}
