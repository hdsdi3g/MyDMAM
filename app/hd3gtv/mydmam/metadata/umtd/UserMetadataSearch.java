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
package hd3gtv.mydmam.metadata.umtd;

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.List;

public class UserMetadataSearch {
	
	private List<UserHashtag> hashtags;
	
	private List<UserBangvalue> bangs;
	
	private String parent_pathindex_key;
	
	private transient SourcePathIndexerElement parent;
	
	private String free_text;
	
	// TODO from and to Json
	
	/**
	 * @return SourcePathIndexerElement keys
	 */
	public List<String> search() {
		// TODO BoolQueryBuilder querybuilder.must()
		// with a must(startsWith(parent.storagename + ":" + parent.currentpath))
		return null;
	}
	
	// TODO HashTag and BangValue search;
	
	// TODO refactor search: if user POST an UserMetadataSearch (to a specific controller), switch the search here.
}
