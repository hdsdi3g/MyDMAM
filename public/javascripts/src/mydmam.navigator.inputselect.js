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
 * Navigator input selection functions
 */

/**
 * PUBLIC
 * create
 * Each item found will be deployed with Navigator Inputselect.
 * @param jquery_selector
 * @return null
 */
(function(inputselect) {
	inputselect.create = function(jquery_selector) {
		$(jquery_selector).each(inputselect.deploy);
	};
})(window.mydmam.navigator.inputselect);

/**
 * deploy
 * Navigator Inputselect in this jquery item.
 * @return null
 */
(function(inputselect) {
	inputselect.deploy = function() {
		var inputtarget = $(this).data("inputtarget");
		//$("#" + inputtarget).addClass("mynis"); //TODO set !
		
		var content = '';
		content = content + '<div class="mynis">';
		
		content = content + '<div class="inputbox">';
		content = content + '<input type="text" autocomplete="off" tabindex="" style="width: 4px;">';
		content = content + '</div>';
		
		content = content + '<div class="dropdownbox">';
		content = content + '<ul class="dropdown-menu" role="menu" aria-labelledby="dropdownMenu">';
		content = content + '</ul>';
		content = content + '</div>';
		
		content = content + '</div>';
		$(this).append(content);

		var jqo_inputbox = $(this).children("div.mynis").children("div.inputbox");
		var jqo_input = jqo_inputbox.children("input");
		var jqo_dropdownmenu = $(this).children("div.mynis").children("div.dropdownbox").children("ul.dropdown-menu");
		
		/**
		 * Manage the popup menu show and hide
		 */
		var mouse_is_inside = false;
		var set_mouse_is_inside = function() {
			mouse_is_inside = true;
		};
		var unset_mouse_is_inside = function() {
			mouse_is_inside = false;
		};
		
		jqo_inputbox.mouseout(unset_mouse_is_inside);
		jqo_dropdownmenu.mouseout(unset_mouse_is_inside);
		
		jqo_inputbox.mouseover(set_mouse_is_inside);
		jqo_dropdownmenu.mouseover(set_mouse_is_inside);
		
		jqo_inputbox.click(function() {
			jqo_input.focus();
			
			if (jqo_dropdownmenu.css("display") === "none") {
				jqo_dropdownmenu.css("display", "block");
				inputselect.refreshMenu(jqo_dropdownmenu, jqo_input);
			}
			set_mouse_is_inside();
		});
		
		$(document).click(function() {
			if (mouse_is_inside === false) {
				jqo_dropdownmenu.css("display", "none");
			}
		});

		/**
		 * Manage the inputbox
		 */
		var timeout_action_handle = null;
		var timeout_action = function() {
			inputselect.refreshMenu(jqo_dropdownmenu, jqo_input);
			timeout_action_handle = null;
		};
		var inputbox_key_action = function() {
			jqo_input.css("width", 4 + (8 * (jqo_input.val().length + 1)));
			if (timeout_action_handle != null) {
				clearTimeout(timeout_action_handle);
			}
			timeout_action_handle = setTimeout(timeout_action, 500);
		};
		jqo_input.keypress(inputbox_key_action);
		jqo_input.keyup(inputbox_key_action);
		jqo_input.keydown(inputbox_key_action);
		jqo_input.change(inputbox_key_action);
	};
})(window.mydmam.navigator.inputselect);

/**
 * showMenu
 * @param jqo_dropdownmenu Jquery object target
 * @return function
 */
(function(inputselect) {
	inputselect.refreshMenu = function(jqo_dropdownmenu, jqo_input) {
		var searchpath = jqo_dropdownmenu.data("searchpath");
		if (searchpath === jqo_input.val().trim()) {
			return;
		}
		searchpath = jqo_input.val().trim();
		jqo_dropdownmenu.data("searchpath", searchpath);
		
		var content = '';
		content = content + '<li class="nav-header">Loading...</li>';
		content = content + '<li><a tabindex="-1" href="#">Action ' + (new Date()) + '</a></li>';
		//content = content + '<li class="divider"></li>';
		//for (var i = 0; i < 20; i++) {
		//	content = content + '<li><a tabindex="-1" href="#">Padding ' + (i+1) + '</a></li>';
		//}
		jqo_dropdownmenu.html(content);
	};
})(window.mydmam.navigator.inputselect);
