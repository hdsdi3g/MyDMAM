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
 * Pre-definited strings functions.
 * Init mydmam global object
 */
(function(window) {
	String.prototype.trim = function(){return(this.replace(/^[\s\xA0]+/, "").replace(/[\s\xA0]+$/, ""));};
	String.prototype.startsWith = function(str){return (this.match("^"+str)==str);};
	String.prototype.endsWith = function(str){return (this.match(str+"$")==str);};
	
	window.mydmam = {};
})(window);

/**
 * Prepare new stat function
 */
(function(mydmam) {
	mydmam.stat = function(fileshashs, scopes_element, scopes_subelements) {
		var result;
		$.ajax({
			url: mydmam.navigator.url.getstat,
			type: "POST",
			async: false,
			data: {
				"fileshashs": fileshashs,
				"scopes_element": scopes_element, 
				"scopes_subelements": scopes_subelements,
				"from": 0,
				"size": 20
			},
			success: function(response) {
				result = response;
			}
		});
		return result;
	};
	
	$(window.document).ready(function() {
		console.log(mydmam.stat(["880e31c8d36dd104973c4c03e56fa804","d41d8cd98f00b204e9800998ecf8427e","fh:3"], ["dirlist", "pathinfo", "mtdsummary", "mtdpreview"], ["mtdsummary", "mtdpreview"]));
	});
	
})(window.mydmam);
