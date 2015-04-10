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
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

/**
 * Async JS
 * Functions to exchange Jsons with Play direclty.
 */
(function(mydmam) {
	mydmam.async = {};

	/**
	 * @return null
	 */
	mydmam.async.request = function(name, verb, content, response_callback, error_callback) {
		var encoded_request = JSON.stringify({
			name: name,
			verb: verb,
			content: content,
		});
		
		$.ajax({
			url: mydmam.async.url,
			type: "POST",
			dataType: 'json',
			data: {
				request: encoded_request,
			},
			error: function(jqXHR, textStatus, errorThrown) {
				/**
				 * It never throw an error if you respect privileges and request_name/verb.
				 */
				if (error_callback) {
					error_callback();
				}
			},
			success: response_callback,
		});
	};
	
})(window.mydmam);

(function(mydmam) {
	mydmam.async.search = function(results, dom_target) {
		//TODO call react, with results
		console.log(results);
	};
})(window.mydmam);
