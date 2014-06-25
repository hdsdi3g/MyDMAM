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
	
})(window.mydmam);

/**
 * prepareNavigatorButton
 */
(function(basket) {
	basket.prepareNavigatorSwitchButton = function(elementkey) {
		var content = "";
		content = content + '<button type="button" class="btn btn-mini btnbasket btnbasketnav" data-toggle="button" data-elementkey="' + elementkey + '">';
		content = content + '<i class="icon-star"></i>';
		content = content + '</button>';
		return content;
	};
})(window.mydmam.basket);

/**
 * 
 */
(function(basket) {
	basket.setSwitchButtonsEvents = function() {
		$('.btnbasket').click(function() {
			if ($(this).hasClass("active")) {
				console.log("remove from basket for: "+ $(this).data("elementkey"));
			} else {
				console.log("put in basket for: "+ $(this).data("elementkey"));
			}
		});
	};
})(window.mydmam.basket);
