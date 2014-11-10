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
 * engine
 * @return object
 */
(function(inputselect) {
	inputselect.engine = function(jquery_startup) {
		/**
		 * User configuration
		 */
		var inputtarget = jquery_startup.data("inputtarget");
		var onlydirectories = jquery_startup.data("onlydirectories");

		/**
		 * All jquery selectors
		 */
		var jquery = {};
		jquery.root = jquery_startup.children("div.mynis").first();
		jquery.inputbox = jquery.root.children("div.inputbox").first();
		jquery.input = jquery.inputbox.children("input").first();
		jquery.dropdownbox = jquery.root.children("div.dropdownbox").first();
		jquery.dropdownmenu = jquery.dropdownbox.children("ul.dropdown-menu").first();
		
		var getAllDropdownLi = function(special_selector) {
			if (special_selector == null) {
				return jquery.dropdownmenu.children("li");
			} else {
				return jquery.dropdownmenu.children(special_selector);
			}
		};
		
		var getAllDropdownA = function(special_selector) {
			if (special_selector == null) {
				return getAllDropdownLi().children("a");
			} else {
				return getAllDropdownLi().children(special_selector);
			}
		};
		
		var cancelEdition = function() {
			jquery.dropdownmenu.css("display", "none");
			jquery.input.val("");
			jquery.input.css("width", 4);
			jquery.input.blur();
		};
		
		/**
		 * Get the current selected link in dropdown
		 */
		var getDropdownSelectedLink = function(special_selector) {
			if (special_selector == null) {
				special_selector = "a.hovered";
			}
			var jqr_selected_link = getAllDropdownA(special_selector);
			if (jqr_selected_link.length > 0) {
				return jqr_selected_link.first();
			} else {
				return null;
			}
		};
		
		return {
			jquery: jquery,
			getAllDropdownLi: getAllDropdownLi,
			getAllDropdownA: getAllDropdownA,
			getDropdownSelectedLink: getDropdownSelectedLink,
			inputtarget: inputtarget,
			onlydirectories: onlydirectories,
			cancelEdition: cancelEdition,
		};
	};
})(window.mydmam.navigator.inputselect);

/**
 * deploy
 * Navigator Inputselect in this jquery item.
 * @return null
 */
(function(inputselect) {
	inputselect.deploy = function() {
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

		var engine = inputselect.engine($(this));
		//$("#" + engine.inputtarget).addClass("mynis"); //TODO set !
		
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
		
		engine.jquery.inputbox.mouseout(unset_mouse_is_inside);
		engine.jquery.dropdownmenu.mouseout(unset_mouse_is_inside);
		
		engine.jquery.inputbox.mouseover(set_mouse_is_inside);
		engine.jquery.dropdownmenu.mouseover(set_mouse_is_inside);

		engine.jquery.dropdownmenu.mouseover(function() {
			engine.getAllDropdownA().removeClass("hovered");
		});
		
		engine.jquery.inputbox.click(function() {
			engine.jquery.input.focus();
			if (engine.jquery.dropdownmenu.css("display") === "none") {
				engine.jquery.dropdownmenu.css("display", "block");
				inputselect.refreshMenu(engine);
			}
			set_mouse_is_inside();
		});

		$(document).click(function() {
			if (mouse_is_inside === false) {
				engine.cancelEdition();
			}
		});

		/**
		 * Manage the inputbox
		 */
		var timeout_action_handle = null;
		var timeout_action = function() {
			inputselect.refreshMenu(engine);
			timeout_action_handle = null;
		};
		
		var inputbox_key_action = inputselect.inputboxKeyActionCreator(engine, function() {
			if (timeout_action_handle != null) {
				clearTimeout(timeout_action_handle);
			}
			timeout_action_handle = setTimeout(timeout_action, 500);
		});
		
		engine.jquery.input.keypress(inputbox_key_action);
		engine.jquery.input.keyup(inputbox_key_action);
		engine.jquery.input.keydown(inputbox_key_action);
		engine.jquery.input.change(inputbox_key_action);
	};
})(window.mydmam.navigator.inputselect);

/**
 * inputboxKeyAction
 * @return function to call
 */
(function(inputselect) {
	inputselect.inputboxKeyActionCreator = function(engine, f_reset_timeout) {
		return function(event) {
			if (event.type !== "keypress") {
				engine.jquery.input.css("width", 4 + (8 * (engine.jquery.input.val().length + 1)));
				f_reset_timeout();
				return;
			}
			
			var keycodes_to_handle = [keycodemap.down, keycodemap.up, keycodemap.enter, keycodemap.backspace, keycodemap.esc];
			if (keycodes_to_handle.indexOf(event.keyCode) === -1) {
				engine.jquery.input.css("width", 4 + (8 * (engine.jquery.input.val().length + 1)));
				f_reset_timeout();
				console.log(event);
				return true;
			}
			
			/**
			 * Pressed key is a "special" key.
			 */
			
			if (event.keyCode === keycodemap.esc) {
				engine.cancelEdition();
				return false;
			}

			if (event.keyCode === keycodemap.backspace) {
				// TODO ... remove last item in input box, refresh dropdown 
				return false;
			}
			
			var jqr_selected_link = engine.getDropdownSelectedLink();
			
			/**
			 * Enter key is pressed
			 */
			if (event.keyCode === keycodemap.enter) {
				if (jqr_selected_link == null) {
					return false;
				}
				var storagename = jqr_selected_link.data("storagename");
				var path = jqr_selected_link.data("path");
				engine.jquery.input.data("currentstoragepath", storagename + ":" + path);
				// TODO ... add to input box, refresh dropdown 
				console.log(storagename + ":" + path);
				return false;
			}
			
			/**
			 * Down/Up key
			 * 
			 * No actual selected
			 */
			if (jqr_selected_link == null) {
				if (event.keyCode === keycodemap.down) {
					engine.getAllDropdownA().first().addClass("hovered");
				} else if (event.keyCode === keycodemap.up) {
					engine.getAllDropdownA().last().addClass("hovered");
				}
				return false;
			}
			
			var jqr_parent_li = jqr_selected_link.parent();
			if (event.keyCode === keycodemap.down) {
				if (jqr_parent_li.text() !== engine.getAllDropdownLi().last().text()) {
					jqr_parent_li.next().children("a").addClass("hovered");
					jqr_selected_link.removeClass("hovered");
				}
			} else if (event.keyCode === keycodemap.up) {
				if (jqr_parent_li.text() !== engine.getAllDropdownLi().first().text()) {
					jqr_parent_li.prev().children("a").addClass("hovered");
					jqr_selected_link.removeClass("hovered");
				}
			}
			
			jqr_selected_link = engine.getDropdownSelectedLink();
			var top_first_link_offset = engine.getAllDropdownLi().first().offset().top;
			var top_selected_link_offset = jqr_selected_link.offset().top;
			engine.jquery.dropdownmenu.scrollTop(top_selected_link_offset - top_first_link_offset);
			return false;
		};
	};
})(window.mydmam.navigator.inputselect);

/**
 * showMenu
 * @param engine
 * @param onlydirectories search/display only directories (no files).
 * @return null
 */
(function(inputselect) {
	inputselect.refreshMenu = function(engine) {
		var current_storagepath = engine.jquery.input.data("currentstoragepath");
		if (current_storagepath == null) {
			current_storagepath = ""; 
		}
		
		var searchpath = engine.jquery.input.data("searchpath");
		if (searchpath === engine.jquery.input.val().trim()) {
			return;
		}
		searchpath = engine.jquery.input.val().trim();
		engine.jquery.input.data("searchpath", searchpath);

		engine.jquery.dropdownmenu.html('<span><li class="nav-header">Loading...</li></span>');
		
		var stat = window.mydmam.stat;
		var md5_fullpath = md5(current_storagepath);
		var max_item_list = 10;
		
		var search_scope = [stat.SCOPE_DIRLIST, stat.SCOPE_COUNT_ITEMS];
		if (engine.onlydirectories) {
			search_scope.push(stat.SCOPE_ONLYDIRECTORIES);
		}
		var stat_data = stat.query([md5_fullpath], search_scope, [stat.SCOPE_COUNT_ITEMS], 0, max_item_list); //TODO add searchpath

		if (stat_data == null) {
			engine.jquery.dropdownmenu.html('<span><li class="nav-header">Error during loading !</li></span>');
			return;
		}
		if (stat_data[md5_fullpath] == null) {
			engine.jquery.dropdownmenu.html('<span><li class="nav-header">Can\'t found current directory.</li></span>');
			return;
		}
		if (stat_data[md5_fullpath].items == null) {
			engine.jquery.dropdownmenu.html('<span><li class="nav-header">Can\'t found current directory content.</li></span>');
			return;
		}
		
		var content = "";
		if (stat_data[md5_fullpath].items_total > max_item_list) {
			content = content + '<span><li class="nav-header">Too many items: type for search.</li></span>';
			content = content + '<span><li class="divider"></li></span>';
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
		
		engine.jquery.dropdownmenu.html(content);
		//TODO add a click action -> see inputbox_key_action = function(event) Enter
	};
})(window.mydmam.navigator.inputselect);
