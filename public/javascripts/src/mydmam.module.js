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
 * Let Play/MyDMAM modules declare functions and callbacks.
 */
(function(mydmam) {
	mydmam.module = {};
})(window.mydmam);

/**
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
		console.log(modules);
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
	
})(window.mydmam.module);
