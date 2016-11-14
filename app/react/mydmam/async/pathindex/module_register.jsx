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

//TODO refactor this !
var externalPos = function(index_name, is_directory, storagename) {
	if (!index_name | !storagename) {
		return false;
	}
	if (index_name !== "pathindex") {
		return false;
	}
	if (is_directory) {
		return false;
	}
	
	/* if (window.list_external_positions_storages == null) {
		return false;
	}
	for (var pos = 0; pos < list_external_positions_storages.length; pos++) {
		if (list_external_positions_storages[pos] === storagename) {
			return true;
		}
	}*/
	return false;
};

var searchResult = function(result) {
	if (result.index !== "pathindex") {
		return null;
	}
	return pathindex.react2lines;
};

/**
 * We don't wait the document.ready because we are sure the mydmam.module.f code is already loaded. 
 */
mydmam.module.register("PathIndexView", {
	processViewSearchResult: searchResult,
	wantToHaveResolvedExternalPositions: externalPos,
});
