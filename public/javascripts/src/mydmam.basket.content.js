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
 * add
 */
(function(content) {
	var backend = content.backend;

	content.add = function(elementkey) {
		if (content.contain(elementkey) === false) {
			var data = backend.pullData();
			data.push(elementkey);
			backend.pushData(data);
		}
	};
})(window.mydmam.basket.content);

/**
 * remove
 */
(function(content) {
	var backend = content.backend;
	content.remove = function(elementkey) {
		var data = backend.pullData();
		var pos = $.inArray(elementkey, data);
		if (pos > -1) {
			data.splice(pos, 1);
		}
		backend.pushData(data);	
	};
})(window.mydmam.basket.content);

/**
 * contain
 */
(function(content) {
	var backend = content.backend;
	content.contain = function(elementkey) {
		var pos = $.inArray(elementkey, backend.pullData());
		return (pos > -1);
	};
})(window.mydmam.basket.content);

