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

import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.List;

import org.json.simple.JSONObject;

class StatElement {
	
	public static final String SCOPE_DIRLIST = "dirlist";
	public static final String SCOPE_PATHINFO = "pathinfo";
	public static final String SCOPE_MTD_SUMMARY = "mtdsummary";
	public static final String SCOPE_MTD_PREVIEW = "mtdpreview";
	public static final String SCOPE_COUNT_SUB_ELEMENTS = "countsubelements";
	
	public StatElement() {
	}
	
	/**
	 * Bounded by from and size query
	 */
	List<StatElement> items;
	
	/**
	 * Referer to "this" element
	 */
	SourcePathIndexerElement path;
	JSONObject mtdsummary;
	JSONObject mtdpreview;
	
	/**
	 * Total not bounded
	 */
	Long countsubelements;
	
	/**
	 * Bounded values
	 */
	Integer from;
	
	/**
	 * Bounded values
	 */
	Integer size;
	
}
