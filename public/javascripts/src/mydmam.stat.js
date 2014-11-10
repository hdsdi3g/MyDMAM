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
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	mydmam.stat = {};
	var stat = mydmam.stat;
	stat.DEFAULT_PAGE_SIZE = 500;
	
	/**
	 * Must match with StatElement.java
	 */
	stat.SCOPE_DIRLIST = "dirlist";
	stat.SCOPE_PATHINFO = "pathinfo";
	stat.SCOPE_MTD_SUMMARY = "mtdsummary";
	stat.SCOPE_COUNT_ITEMS = "countitems";
	stat.SCOPE_ONLYDIRECTORIES = "onlydirs";
	
	stat.url = "";
})(window.mydmam);

/**
 * query
 * @param scopes_subelements, page_from and page_size are not mandatory
 * @return stat result (StatElement JSON) or null if error
 */
(function(stat) {
	stat.query = function(fileshashs, scopes_element, scopes_subelements, page_from, page_size) {
		if (!page_from) {
			page_from = 0;
		}
		if (!page_size) {
			page_size = stat.DEFAULT_PAGE_SIZE;
		}
		if (!scopes_subelements) {
			scopes_subelements = [];
		}
		
		var result = null;
		$.ajax({
			url: stat.url,
			type: "POST",
			async: false,
			data: {
				"fileshashs": fileshashs,
				"scopes_element": scopes_element, 
				"scopes_subelements": scopes_subelements,
				"page_from": page_from,
				"page_size": page_size
				//TODO add text search, with JSON.stringify() ?
			},
			success: function(response) {
				result = response;
			},
			error: function(jqXHR, textStatus, errorThrown) {
				console.error(errorThrown);
			}
		});
		return result;
	};
	
})(window.mydmam.stat);
