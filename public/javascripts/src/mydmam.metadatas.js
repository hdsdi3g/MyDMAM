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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
 */
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */
/**
 * Metadata engine
 */

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	if(!mydmam.metadatas){mydmam.metadatas = {};}
	if(!mydmam.metadatas.url){mydmam.metadatas.url = {};}
	if(!mydmam.metadatas.displaymethod){mydmam.metadatas.displaymethod = {};}

	var metadatas = mydmam.metadatas;

	metadatas.displaymethod.NAVIGATE_SHOW_ELEMENT = 0;

})(window.mydmam);

/**
 * Prepare view function for video, audio and image.
 */
(function(metadatas) {
	metadatas.view = {};
})(window.mydmam.metadatas);

/**
 * loadAfterDisplay : call loadafterdisplay() for all metadatas.view.* (if
 * exists).
 */
(function(metadatas) {
	metadatas.loadAfterDisplay = function() {
		for ( var viewname in metadatas.view) {
			var viewer = metadatas.view[viewname];
			if (typeof viewer.loadafterdisplay == 'function') {
				viewer.loadafterdisplay();
			}
		}
	};
})(window.mydmam.metadatas);

