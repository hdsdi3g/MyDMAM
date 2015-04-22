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
package hd3gtv.mydmam.web.search;

import java.util.Map;

public final class AsyncSearchResult {
	
	@SuppressWarnings("unused")
	private String index;
	
	@SuppressWarnings("unused")
	private String type;
	
	@SuppressWarnings("unused")
	private float score;
	
	private Map<String, Object> content;
	
	@SuppressWarnings("unused")
	private String key;
	
	public AsyncSearchResult(String index, String type, String key, Map<String, Object> content, float score) {
		this.index = index;
		this.content = content;
		this.score = score;
		this.type = type;
		this.key = key;
	}
	
	public void setContent(Map<String, Object> content) {
		this.content = content;
	}
	
	public Map<String, Object> getContent() {
		return content;
	}
}
