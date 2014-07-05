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
 * upload
 */
(function(basket, backend) {
	backend.upload = function() {
		$.ajax({
			url: mydmam.basket.url.push,
			type: "POST",
			data: {current : localStorage.getObject(basket.LOCALSTORAGE_CONTENT_KEYNAME)},
			success: function(data) {
				backend.setCacheTimer();
			}
		});
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * download
 */
(function(basket, backend) {
	/**
	 * @param synchronized_request (boolean) default false
	 */
	backend.download = function(synchronized_request) {
		$.ajax({
			url: mydmam.basket.url.pull,
			type: "GET",
			async: (synchronized_request === null),
			success: function(data) {
				localStorage.setObject(basket.LOCALSTORAGE_CONTENT_KEYNAME, data);
				backend.setCacheTimer();
			}
		});
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * setContent
 */
(function(basket, backend) {
	backend.setContent = function(elements) {
		localStorage.setObject(basket.LOCALSTORAGE_CONTENT_KEYNAME, elements);
		backend.setCacheTimer();
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * pullData
 */
(function(basket, backend) {
	backend.pullData = function() {
		if (localStorage.getItem(basket.LOCALSTORAGE_CONTENT_KEYNAME) === null) {
			backend.download(true);
		} else if (backend.isExpiredCache()) {
			backend.download();
		}
		return localStorage.getObject(basket.LOCALSTORAGE_CONTENT_KEYNAME);
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * pushData
 */
(function(basket, backend) {
	backend.pushData = function(data) {
		localStorage.setObject(basket.LOCALSTORAGE_CONTENT_KEYNAME, data);
		backend.upload();
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * setCacheTimer
 */
(function(basket, backend) {
	backend.setCacheTimer = function() {
		localStorage.setItem(basket.LOCALSTORAGE_LASTUPDATE_KEYNAME, new Date().getTime());
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * isExpiredCache
 */
(function(basket, backend) {
	backend.isExpiredCache = function() {
		var date = localStorage.getItem(basket.LOCALSTORAGE_LASTUPDATE_KEYNAME);
		if (date != null) {
			if (((new Date().getTime() - date) / 1000) < basket.CACHE_DURATION) {
				/**
				 * Too recent download, the cache should to be good, don't update.
				 */
				return false;
			}
		}
		return true;
	};
})(window.mydmam.basket, window.mydmam.basket.content.backend);

/**
 * all(result = function(data))
 */
(function(backend) {
	backend.all = function(result) {
		$.ajax({
			url:mydmam.basket.url.all,
			type: "GET",
			success: function(data) {
				result(data);
			}
		});
	};
})(window.mydmam.basket.content.backend);

/**
 * selected(result = function(data.selected))
 */
(function(backend) {
	backend.selected = function(result) {
		$.ajax({
			url:mydmam.basket.url.selected,
			type: "GET",
			success: function(data) {
				result(data.selected);
			}
		});
	};
})(window.mydmam.basket.content.backend);

/**
 * bdelete(name, result = function(name is ok, null if false))
 */
(function(backend) {
	backend.bdelete = function(name, result) {
		$.ajax({
			url:mydmam.basket.url.bdelete,
			type: "POST",
			data: {name : name},
			success: function(data) {
				result(data["delete"]);
			}
		});
	};
})(window.mydmam.basket.content.backend);

/**
 * rename(name, newname, result = function(name, newname is ok / null, null if false))
 */
(function(backend) {
	backend.rename = function(name, newname, result) {
		$.ajax({
			url:mydmam.basket.url.rename,
			type: "POST",
			data: {name : name, newname: newname},
			success: function(data) {
				result(data.rename_from, data.rename_to);
			}
		});
	};
})(window.mydmam.basket.content.backend);

/**
 * create(name, switch_to_selected, result = function(name, switch_to_selected is ok / null, null if false))
 */
(function(backend) {
	backend.create = function(name, switch_to_selected, result) {
		$.ajax({
			url:mydmam.basket.url.create,
			type: "POST",
			data: {name : name, switch_to_selected : switch_to_selected},
			success: function(data) {
				result(data.create, data.switch_to_selected);
			}
		});
	};
})(window.mydmam.basket.content.backend);

/**
 * switch_selected(name, result = function(name, data is ok / null, null if false))
 */
(function(backend, basket) {
	backend.switch_selected = function(name, result) {
		$.ajax({
			url:mydmam.basket.url.switch_selected,
			type: "POST",
			data: {name : name},
			success: function(data) {
				if (data.notselected) {
					result(null, null);
					return;
				}
				result(name, data);
			}
		});
	};
})(window.mydmam.basket.content.backend);
