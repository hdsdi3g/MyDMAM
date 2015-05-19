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

search.found = function(results, dom_target) {
	if (!dom_target) {
		return;
	}

	/**
	 * Create React uniq key.
	 */
	var createReactKey = function(result_list) {
		for (var pos in result_list) {
			result_list[pos].reactkey = result_list[pos].index + ":" + result_list[pos].type + ":" + result_list[pos].key;
		};
		console.log(results);
	}

	createReactKey(results.results);

	React.render(
		<search.SearchResultPage results={results} />,
		dom_target
	);
};