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
 * Copyright (C) hdsdi3g for hd3g.tv 2014-2015
 * 
 */
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

/**
 * Init mydmam global object.
 */
if(!mydmam){mydmam = {};}
if(!mydmam.module){mydmam.module = {};}
if(!mydmam.routes){mydmam.routes = {};}
if(!mydmam.urlimgs){mydmam.urlimgs = {};}

/**
 * Pre-definited strings functions. Init mydmam global object.
 */
(function(window) {
	String.prototype.trim = function() {
		return (this.replace(/^[\s\xA0]+/, "").replace(/[\s\xA0]+$/, ""));
	};
	String.prototype.startsWith = function(str) {
		return (this.match("^" + str) == str);
	};
	String.prototype.endsWith = function(str) {
		return (this.match(str + "$") == str);
	};
	String.prototype.append = function(str) {
		return this + str;
	};
	String.prototype.nl2br = function() {
		return this.replace(/([^>\r\n]?)(\r\n|\n\r|\r|\n)/g, '$1' + '<br>' + '$2');
	};

	Storage.prototype.setObject = function(key, obj) {
		return this.setItem(key, JSON.stringify(obj));
	};
	Storage.prototype.getObject = function(key) {
		return JSON.parse(this.getItem(key));
	};

	window.keycodemap = {
		down : 40,
		up : 38,
		enter : 13,
		backspace : 8,
		esc : 27,
	};

	if (window.console == null) {
		window.console = {};
		window.console.log = function() {
			/**
			 * No activity, but no bugs.
			 */
		};
		window.console.err = function() {
			/**
			 * No activity, but no bugs.
			 */
		};
	}
	
})(window);

/**
 * Let Play/MyDMAM modules declare functions and callbacks.
 * 
 * register()
 * @param module_name
 * @param declarations: {functionToCallback: function, ...}
 * @return null
 * 
 * dumpList()
 * @return null
 * 
 * module.getCallbacks(callback_name)
 * @return [{name: "Module name", callback: function}, ]
 */
(function(module) {
	
	/**
	 * {functionToCallback: [{name: "Module name", callback: function}, ], ...}
	 */
	var modules = {};
	
	module.register = function(module_name, declarations) {
		if (!module_name) {
			throw "No module name for register !";
		}
		if (!declarations) {
			throw "No correct declaration for register module: " + module_name;
		}
		for (var callback_name in declarations) {
			if (!modules[callback_name]) {
				modules[callback_name] = [];
			}
			modules[callback_name].push({
				name: module_name,
				callback: declarations[callback_name],
			});
		}
	};
	
	module.dumpList = function() {
		console.log("JS modules dump list", modules);
	};
	
	module.getCallbacks = function(callback_name) {
		if (modules[callback_name]) {
			return modules[callback_name];
		} else {
			return [];
		}
	};

	var multipleCallbacks = function(callback_name) {
		return function(args) {
			var callbacks = module.getCallbacks(callback_name);
			var result;
			var callback;
			var module_name;
			var module_response_callback = {};
			
			for (var pos in callbacks) {
				callback = callbacks[pos];
				module_name = callback.name;
				try {
					var result = callback.callback.apply(this, arguments);
					module_response_callback[module_name] = result;
				} catch (err) {
					console.error(err, module_name, callback_name);
				}
			}
			return module_response_callback;
		};
	};
	
	var firstValidCallback = function(callback_name) {
		return function(args) {
			var callbacks = module.getCallbacks(callback_name);
			var result;
			var callback;
			
			for (var pos in callbacks) {
				callback = callbacks[pos];
				try {
					var result = callback.callback.apply(this, arguments);
					if (result) {
						return result;
					}
				} catch (err) {
					console.error(err, callback.name, callback_name);
				}
			}
			return null;
		};
	};

	/**
	 * Functions and callbacks declarations.
	 * Call with module.f.MyFunction(param1, param2, ...).
	 * @return {module_name: Result, ...} OR
	 * @return FirstValidResponse
	 */
	
	module.f = {};
	module.f.helloworld = multipleCallbacks("helloworld");
	module.f.processViewSearchResult = firstValidCallback("processViewSearchResult");
	module.f.wantToHaveResolvedExternalPositions = firstValidCallback("wantToHaveResolvedExternalPositions");
	module.f.i18nExternalPosition = firstValidCallback("i18nExternalPosition");
	module.f.managerInstancesItems = firstValidCallback("managerInstancesItems");
	
})(window.mydmam.module);

/**
 * JS Route System
 */
(function(routes) {
	var base = {};

	/**
	 * @param async_needs [{name: AsyncJSControllerName, verb: AsyncJSControllerVerb}, ], used to check if user can acces to some controllers, via async.isAvaliable().
	 */
	routes.push = function(route_name, path, react_top_level_class, async_needs) {
		base[route_name] = {};
		base[route_name].path = path;
		base[route_name].react_top_level_class = react_top_level_class;
		if (async_needs) {
			base[route_name].async_needs = async_needs;
		}
	};

	var callbackFactory = function(callback, route_name) {
		return function(r) {
			callback(route_name, r.params);
		};
	};

	/**
	 * @param callback, like function(route_name, rlite.params)
	 */
	routes.populate = function(rlite, callback) {
		for (var route_name in base) {
			var async_needs = base[route_name].async_needs;
			if (async_needs != null) {
				for (var pos in async_needs) {
					var async_need = async_needs[pos];
					if (async_need.name == null) {
						console.error("Name param missing for async_need", async_need);
					}
					if (async_need.verb == null) {
						console.error("Verb param missing for async_need", async_need);
					}
				}
				if (mydmam.async.isAvaliable(async_need.name, async_need.verb) == false) {
					// console.log("No rights for", async_need);
					continue;
				}
			}
			rlite.add(base[route_name].path, callbackFactory(callback, route_name));
		}
	};

	routes.getReactTopLevelClassByRouteName = function(route_name) {
		if (base[route_name]) {
			return base[route_name].react_top_level_class;
		}
		return null;
	};
	
})(window.mydmam.routes);

