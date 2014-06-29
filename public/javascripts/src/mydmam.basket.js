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
 * Prepare consts and vars.
 */
(function(mydmam) {
	mydmam.basket = {};
	mydmam.basket.content = {};
	mydmam.basket.content.backend = {};
	mydmam.basket.LOCALSTORAGE_CONTENT_KEYNAME = "basket-content";
	mydmam.basket.LOCALSTORAGE_LASTUPDATE_KEYNAME = "basket-lastupdate";
	mydmam.basket.CACHE_DURATION = 60; /** in sec */
	mydmam.basket.url = {};
})(window.mydmam);

/**
 * backend
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

	backend.pullData = function() {
		if (localStorage.getItem(basket.LOCALSTORAGE_CONTENT_KEYNAME) === null) {
			backend.download(true);
		} else if (backend.isExpiredCache()) {
			backend.download();
		}
		return localStorage.getObject(basket.LOCALSTORAGE_CONTENT_KEYNAME);
	};
	
	backend.pushData = function(data) {
		localStorage.setObject(basket.LOCALSTORAGE_CONTENT_KEYNAME, data);
		backend.upload();
	};

	backend.setCacheTimer = function() {
		localStorage.setItem(basket.LOCALSTORAGE_LASTUPDATE_KEYNAME, new Date().getTime());
	};

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
 * content
 */
(function(content) {
	var backend = content.backend;

	content.add = function(elementkey) {
		if (content.contain(elementkey) === false) {
			var data = backend.pullData();
			data.push(elementkey);
			backend.pushData(data);
		}
	};

	content.remove = function(elementkey) {
		var data = backend.pullData();
		var pos = $.inArray(elementkey, data);
		if (pos > -1) {
			data.splice(pos, 1);
		}
		backend.pushData(data);	
	};

	content.contain = function(elementkey) {
		var pos = $.inArray(elementkey, backend.pullData());
		return (pos > -1);
	};

})(window.mydmam.basket.content);

/**
 * prepareNavigatorButton
 */
(function(basket) {
	basket.prepareNavigatorSwitchButton = function(elementkey) {
		var content = "";
		var active = "";
		if (basket.isInBasket(elementkey)) {
			active = "active";
		}
		content = content + '<button type="button" class="btn btn-mini btnbasket btnbasketnav ' + active + '" data-toggle="button" data-elementkey="' + elementkey + '">';
		content = content + '<i class="icon-star"></i>';
		content = content + '</button>';
		return content;
	};
})(window.mydmam.basket);

/**
 * addSearchSwitchButtons
 */
(function(basket) {
	basket.addSearchSwitchButtons = function() {
		$('span.searchresultitem').each(function(){
			var elementkey = $(this).data("storagekey");
			var content = "";
			var active = "";
			if (basket.isInBasket(elementkey)) {
				active = "active";
			}
			content = content + '<button type="button" class="btn btn-mini btnbasket ' + active + '" data-toggle="button" data-elementkey="' + elementkey + '">';
			content = content + '<i class="icon-star"></i>';
			content = content + '</button>';
			$(this).before(content);
		});
		basket.setSwitchButtonsEvents();
	};
})(window.mydmam.basket);

/**
 * isInBasket
 * @return boolean
 */
(function(basket) {
	basket.isInBasket = function(elementkey) {
		return basket.content.contain(elementkey);
	};
})(window.mydmam.basket);

/**
 * setSwitchButtonsEvents
 */
(function(basket) {
	basket.setSwitchButtonsEvents = function() {
		$('.btnbasket').click(function() {
			var elementkey = $(this).data("elementkey");
			if ($(this).hasClass("active")) {
				mydmam.basket.content.remove(elementkey);
			} else {
				mydmam.basket.content.add(elementkey);
			}
		});
	};
})(window.mydmam.basket);

/**
 * forceRefresh (async)
 */
(function(basket) {
	basket.forceRefresh = function() {
		basket.content.backend.download();
	};
})(window.mydmam.basket);
