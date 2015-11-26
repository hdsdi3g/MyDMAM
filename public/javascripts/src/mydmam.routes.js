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
 * JS Route System
 */
(function(mydmam) {
	mydmam.routes = {};
	mydmam.routes.base = {};

	/**
	 * @param async_needs [{name: AsyncJSControllerName, verb: AsyncJSControllerVerb}, ], used to check if user can acces to some controllers, via async.isAvaliable().
	 */
	mydmam.routes.push = function(route_name, path, react_top_level_class, async_needs) {
		if (async_needs != null) {
			for (var pos in async_needs) {
				var async_need = async_needs[pos];
				if (async_need.name == null) {
					console.error("Name param missing for async_need", async_need);
				}
				if (async_need.verb == null) {
					console.error("Verb param missing for async_need", async_need);
				}
				if (mydmam.async.isAvaliable(async_need.name, async_need.verb) == false) {
					console.log("No rights for", async_need);
					return;
				}
			}
		}

		mydmam.routes.base[route_name] = {};
		mydmam.routes.base[route_name].path = path;
		mydmam.routes.base[route_name].react_top_level_class = react_top_level_class;
	};

	/**
	 * @param callback, like function(react_top_level_class, rlite.params)
	 */
	mydmam.routes.populate = function(rlite, callback) {
		var base = mydmam.routes.base;
		for (var routename in base) {
			rlite.add(base[routename].path, function () {
				callback(base[routename].react_top_level_class, rlite.params);
			});
		}
	};

})(window.mydmam);
