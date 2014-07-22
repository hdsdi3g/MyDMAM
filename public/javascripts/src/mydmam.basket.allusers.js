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
 * Set baskets var
 */
(function(allusers) {
	allusers.baskets = {};
	allusers.userkeys = [];
})(window.mydmam.basket.allusers);

/**
 * prepare
 */
(function(allusers) {
	allusers.prepare = function(all_baskets) {
		for (var userkey in allusers.baskets) {
			allusers.userkeys.push(userkey);
		}
		allusers.displayUsers();
		var content = "";
		//TODO resolve all userkey to username 
		//TODO stat pathindexkeys and display it
	};
})(window.mydmam.basket.allusers);

/**
 * displayUsers
 */
(function(allusers) {
	allusers.displayUsers = function() {
		var content = "";
		for (var pos in allusers.userkeys) {
			content = content + '<li class="">';
			content = content + '<a href="#" data-userkey="' + allusers.userkeys[pos] + '" class="btnbasketusername">';
			content = content + allusers.userkeys[pos];
			content = content + '</a>';
			content = content + '</li>';
		}
		$("#userlist").empty();
		$("#userlist").html(content);
		
		$('a.btnbasketusername').each(function() {
			$(this).click(function() {
				allusers.displayBasket($(this).data("userkey"));
			});
		});
	};
})(window.mydmam.basket.allusers);


/**
 * displayBasket
 */
(function(allusers) {
	allusers.displayBasket = function(userkey) {
		var userbaskets = allusers.baskets[userkey].baskets;
		
		var basketname;
		var basketcontent;
		for (var pos in userbaskets) {
			basketname = userbaskets[pos].name;
			basketcontent = userbaskets[pos].content;
			
		}
	};
})(window.mydmam.basket.allusers);
