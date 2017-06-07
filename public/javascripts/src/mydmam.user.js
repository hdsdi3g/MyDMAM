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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
 * User API
 * Tools for current User in JS side
 */
(function(mydmam) {
	if(!mydmam.user){mydmam.user = {};}
	/**
	Play will push, via HeaderJS some vars in window.mydmam.user:
		long_name
		preferencesdatas (JsonObject)
		domain
		email
		lang
		login
		lasteditdate
		lastlogindate
		lastloginipsource
	*/
})(window.mydmam);

(function(mydmam, user) {
	var datas = user.preferencesdatas;

	var pullFromServer = function(callback_after_pull) {
		mydmam.async.request("auth", "getpreferences", null, function(new_datas) {
			user.preferencesdatas = new_datas;
			datas = new_datas;
			callback_after_pull();
		});
	}

	var pushToServer = function(callback_after_push) {
		mydmam.async.request("auth", "setpreferences", datas, function() {
			if (callback_after_push) {
				callback_after_push();
			}
		});
	}
	user.pushToServer = pushToServer;

	/**
	   datas[key] = {
			val: <my json value>,
			crd: <created, unixtime>,
			upd: <updated, unixtime>
	 * }
	*/

	user.isPreferenceExists = function(key) {
		if (!datas[key]) {
			return false;
		}
		if (datas[key].val === null) {
			return false;
		}
		if (datas[key].val == undefined) {
			delete datas[key];
			pushToServer();
			return false;
		}
		return true;
	};

	user.getPreference = function(key) {
		if (user.isPreferenceExists(key)) {
			return datas[key].val;
		}
		return null;
	};

	/**
	 * Set value = null for remove Preference
	 * return value
	 */
	user.setPreference = function(key, value, not_send_to_server_now) {
		if (value == undefined) {
			return null;
		}

		var exists = user.isPreferenceExists(key);
		if (exists) {
			if (value == null) {
				/** Remove */
				delete datas[key];
				if (!not_send_to_server_now) {
					pushToServer();
				}
				return null;
			} else {
				/** Update */
				var entry = datas[key];
				if (entry.val === value) {
					return value;
				}
				entry.val = value;
				entry.upd = Date.now();
			}
		} else if (value == null) {
			/** Don't create entry with null value */
			return null;
		} else {
			/** Create new entry */
			datas[key] = {
				val: value,
				crd: Date.now(),
				upd: Date.now(),
			};
		}

		if (!not_send_to_server_now) {
			pushToServer();
		}
		return value;
	}

	/**
	 * Get current value for key or create default value for missing key
	 * @return value / default_result / null
	 */
	user.getPreferenceOrDefault = function(key, default_result, not_send_to_server_now) {
		if (user.isPreferenceExists(key)) {
			return user.getPreference(key);
		}
		if (default_result != null) {
			return user.setPreference(key, value, not_send_to_server_now);
		}
		return null;
	};

	/**
	 * item[key] = mapper(key, json_value, created_date, last_modified_date)
	*/
	user.listPreferences = function(mapper) {
		var key_list = [];
		for (var key in datas) {
			key_list.push(key);
		}

		key_list = key_list.sort();
		var result = {};
		for (var pos in key_list) {
			var key = key_list[pos];
			var data = datas[key];
			result[key] = mapper(key, data.val, data.crd, data.upd);
		}
	};

})(window.mydmam, window.mydmam.user);

