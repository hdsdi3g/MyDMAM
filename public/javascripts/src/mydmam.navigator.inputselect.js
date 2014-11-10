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
		var onlydirectories = $(this).data("onlydirectories");
		//$("#" + inputtarget).addClass("mynis"); //TODO set !
		//TODO add can select dir and/or files
		
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

		jqo_dropdownmenu.mouseover(function() {
			jqo_dropdownmenu.children("li").children("a").removeClass("hovered");
		});
		
		jqo_inputbox.click(function() {
			jqo_input.focus();
			
			if (jqo_dropdownmenu.css("display") === "none") {
				jqo_dropdownmenu.css("display", "block");
				inputselect.refreshMenu(jqo_dropdownmenu, jqo_input);
			}
			set_mouse_is_inside();
		});

		var cancel = function() {
			jqo_dropdownmenu.css("display", "none");
			jqo_input.val("");
			jqo_input.css("width", 4);
			jqo_input.blur();
		};
		
		$(document).click(function() {
			if (mouse_is_inside === false) {
				cancel();
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
		var inputbox_key_action = function(event) {
			if (event.type === "keypress") {
				if (event.keyCode === 40) {//Down
					var jqr_selected_link = jqo_dropdownmenu.children("li").children("a.hovered");
					if (jqr_selected_link.length > 0) {
						jqr_selected_link.parent().next().children("a").addClass("hovered");
						jqr_selected_link.removeClass("hovered");
					} else {
						jqo_dropdownmenu.first().children("li").children("a").addClass("hovered");
					}
					jqo_dropdownmenu.scrollTop(jqo_dropdownmenu.children("li").children("a.hovered").scrollTop());
					return false;
				}
				if (event.keyCode === 38) {//Up
					var jqr_selected_link = jqo_dropdownmenu.children("li").children("a.hovered");
					if (jqr_selected_link.length > 0) {
						jqr_selected_link.parent().prev().children("a").addClass("hovered");
						jqr_selected_link.removeClass("hovered");
					} else {
						jqo_dropdownmenu.last().children("li").children("a").addClass("hovered");
					}
					jqo_dropdownmenu.scrollTop(jqo_dropdownmenu.children("li").children("a.hovered").scrollTop());
					return false;
				}
				if (event.keyCode === 13) {//Enter
					var jqr_selected_link = jqo_dropdownmenu.children("li").children("a.hovered");
					if (jqr_selected_link.length === 0) {
						jqr_selected_link = jqo_dropdownmenu.children("li").children("a:hover");
						if (jqr_selected_link.length === 0) {
							return;
						}
					}
					var jqr_selected_link_first = jqr_selected_link.first();
					var storagename = jqr_selected_link_first.data("storagename");
					var path = jqr_selected_link_first.data("path");
					jqo_input.data("currentstoragepath", storagename + ":" + path);
					// TODO ... add to input box, refresh dropdown 
					console.log(storagename + ":" + path);
					return;
				}
				
				if (event.keyCode === 8) {//Backspace
					// TODO ... remove last item in input box, refresh dropdown 
					return;
				}
				if (event.keyCode === 27) {//Esc
					cancel();
					return;
				}
				console.log(event);
			}
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
 * @param jqo_input input text for get current query
 * @param onlydirectories search/display only directories (no files).
 * @return null
 */
(function(inputselect) {
	inputselect.refreshMenu = function(jqo_dropdownmenu, jqo_input, onlydirectories) {
		var current_storagepath = jqo_input.data("currentstoragepath");
		if (current_storagepath == null) {
			current_storagepath = ""; 
		}
		
		var searchpath = jqo_input.data("searchpath");
		if (searchpath === jqo_input.val().trim()) {
			return;
		}
		searchpath = jqo_input.val().trim();
		jqo_input.data("searchpath", searchpath);

		jqo_dropdownmenu.html('<li class="nav-header">Loading...</li>');
		
		var stat = window.mydmam.stat;
		var md5_fullpath = md5(current_storagepath);
		var max_item_list = 10;
		
		var search_scope = [stat.SCOPE_DIRLIST, stat.SCOPE_COUNT_ITEMS];
		if (onlydirectories) {
			search_scope.push(stat.SCOPE_ONLYDIRECTORIES);
		}
		var stat_data = stat.query([md5_fullpath], search_scope, [stat.SCOPE_COUNT_ITEMS], 0, max_item_list); //TODO add searchpath

		if (stat_data == null) {
			jqo_dropdownmenu.html('<li class="nav-header">Error during loading !</li>');
			return;
		}
		if (stat_data[md5_fullpath] == null) {
			jqo_dropdownmenu.html('<li class="nav-header">Can\'t found current directory.</li>');
			return;
		}
		if (stat_data[md5_fullpath].items == null) {
			jqo_dropdownmenu.html('<li class="nav-header">Can\'t found current directory content.</li>');
			return;
		}
		
		var content = "";
		if (stat_data[md5_fullpath].items_total > max_item_list) {
			content = content + '<li class="nav-header">Too many items: type for search.</li>';
			content = content + '<li class="divider"></li>';
		}

		var items = stat_data[md5_fullpath].items;
		for (var pathindexkey in items) {
			var pathindexitem = items[pathindexkey];
			var items_total = pathindexitem.items_total;
			var isdirectory = pathindexitem.reference.directory;
			var path = pathindexitem.reference.path;
			var storagename = pathindexitem.reference.storagename;
			
			content = content + '<li>';
			var icon = '<i class="icon-file"></i>';
			if (isdirectory) {
				if (path === '/') {
					icon = '<i class="icon-hdd"></i>';
				} else {
					icon = '<i class="icon-folder-close"></i>';
				}
			}
			content = content + '<a tabindex="-1" href="" class="dropdownitem" data-path="' + path + '" data-storagename="' + storagename + '">';
			content = content + icon + ' ' + storagename + ':' + path; //TODO display just the name
			content = content + '</a>';
			content = content + '</li>';
		}
		
		jqo_dropdownmenu.html(content);
		//TODO add a click action -> see inputbox_key_action = function(event) Enter
	};
})(window.mydmam.navigator.inputselect);
