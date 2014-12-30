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
	String.prototype.append = function(str){return this + str;};
	String.prototype.nl2br = function() { return this.replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + '<br>' + '$2');};

	Storage.prototype.setObject = function(key, obj) {
		return this.setItem(key, JSON.stringify(obj));
	};
	Storage.prototype.getObject = function(key) {
		return JSON.parse(this.getItem(key));
	};
	
	window.mydmam = {};
	
	window.keycodemap = {
		down: 40,
		up: 38,
		enter: 13,
		backspace: 8,
		esc: 27,
	};

	mydmam.urlimgs = {};
	
	if (window.console == null) {
		window.console = {};
		window.console.log = function(){};
		window.console.err = function(){};
	}
})(window);
