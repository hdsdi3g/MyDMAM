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
 * Define stand-alone mydmam functions.
 */
(function(mydmam) {
	mydmam.format = {};
	
	var addZeros = function(text) {
		var returntext = '00' + text;
		return returntext.substr(returntext.length - 2, returntext.length);
	};
	mydmam.format.addzeros = addZeros;

	var formatDate = function(epoch) {
		if (epoch < 1) {
			return "";
		}
		var date = new Date(epoch);
		var returntext = addZeros(date.getHours()) + ':' + addZeros(date.getMinutes()) + ':' + addZeros(date.getSeconds()); 
		return returntext;
	};
	mydmam.format.date = formatDate;
		
	var formatFullDate = function(epoch) {
		if (epoch < 1) {
			return "";
		}
		var date = new Date(epoch);
		var returntext = addZeros(date.getDate()) + '/' + addZeros(date.getMonth() + 1) + '/' + date.getFullYear() + ' ' + addZeros(date.getHours()) + ':' + addZeros(date.getMinutes()) + ':' + addZeros(date.getSeconds()); 
		return returntext;
	};
	mydmam.format.fulldate = formatFullDate;

	var formatDate_JustDate = function(epoch) {
		var date = new Date(epoch);
		var returntext = addZeros(date.getDate()) + '/' + addZeros(date.getMonth() + 1) + '/' + date.getFullYear(); 
		return returntext;
	};
	mydmam.format.justdate = formatDate_JustDate;

	var formatDate_JustDate = function(epoch) {
		var date = new Date(epoch);
		var returntext = addZeros(date.getDate()) + '/' + addZeros(date.getMonth() + 1) + '/' + date.getFullYear(); 
		return returntext;
	};
	
	mydmam.format.timeAgo = function(epoch_since_date) {
		var delta = (new Date().getTime() - epoch_since_date) / 1000;
		if (delta < 60) {
			return i18n('timeunit.sec', Math.round(delta));
		} else if (delta < 3600) {
			return i18n('timeunit.min', Math.round((delta / 60)));
		} else if (delta < (3600 * 24)) {
			return i18n('timeunit.hrs', Math.round((delta / 3600)));
		} else if (delta < (3600 * 24 * 7)) {
			return i18n('timeunit.days', Math.round((delta / (3600 * 24))));
		} else {
			return i18n('timeunit.weeks', Math.round((delta / (3600 * 24 * 7))));
		}
	};

})(window.mydmam);
