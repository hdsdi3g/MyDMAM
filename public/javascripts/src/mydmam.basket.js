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
 * setContent
 */
(function(basket) {
	basket.setContent = function(elements) {
		if (elements === null) {
			elements = [];
		}
		basket.content.backend.setContent(elements);
	};
})(window.mydmam.basket);
