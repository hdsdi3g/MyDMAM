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
	if(!mydmam.async){mydmam.async = {};}
	if(!mydmam.async.controllers){mydmam.async.controllers = {};}

	/**
	 * @return null
	 */
	mydmam.async.request = function(_name, _verb, content, response_callback, error_callback) {
		var name = _name.toLowerCase();
		var verb = _verb.toLowerCase();
		
		if (mydmam.async.isAvaliable(name, verb) === false) {
			if (error_callback) {
				error_callback();
			}
			return;
		}

		$.ajax({
			url: mydmam.routes.reverse("async").replace("nameparam1", name).replace("verbparam2", verb),
			type: "POST",
			dataType: 'json',
			data: {
				jsonrq: JSON.stringify(content)
			},
			error: function(jqXHR, textStatus, errorThrown) {
				if (jqXHR.status == 403) {
					/**
					 * Disconnected
					 */
					if (jqXHR.responseJSON.redirect) {
						window.location.href = jqXHR.responseJSON.redirect;
					} else {
						window.location.href = "/";
					}
					return;
				}

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
	
	/**
	 * If you outrepassing this, Play will block you, because it do also this check.
	 * @param verb is not mandatory
	 * @return boolean
	 */
	mydmam.async.isAvaliable = function(name, verb) {
		if ((verb != null) & (mydmam.async.controllers[name] != null)) {
			return (mydmam.async.controllers[name].indexOf(verb) > -1);
		}
		return (mydmam.async.controllers[name] != null);
	};

})(window.mydmam);

(function(async) {

	/** In ms */
	var actual_drift = 0;

	if (async.server_time) {
		actual_drift = Date.now() - async.server_time;
		if (window.performance) {
			if (window.performance.timing) {
				if (window.performance.timing.responseStart) {
					actual_drift = window.performance.timing.responseStart - async.server_time;
				}
			}
		}
	}

	/**
	 * @return long (UNIX Time in ms)
	 */
	async.getTime = function() {
		return Date.now() - actual_drift;
	}

	//console.log(actual_drift);

})(window.mydmam.async);
