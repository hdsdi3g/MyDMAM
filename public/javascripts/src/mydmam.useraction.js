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
 * availabilities.downloadLast()
 * @param callback
 * @return null
 */
(function(useraction) {
	useraction.availabilities.downloadLast = function(callback) {
		$.ajax({
			url: mydmam.useraction.url.currentavailabilities,
			type: "GET",
			async: false,
			data: {},
			success: function(data) {
				useraction.availabilities.content = data;
				callback();
			}
		});
	};
})(window.mydmam.useraction);

/**
 * populateButtonsCreate()
 * @return null
 */
(function(useraction) {
	useraction.populateButtonsCreate = function() {
		if (useraction.availabilities.content == null) {
			useraction.availabilities.downloadLast(useraction.drawButtonsCreateContent);
		} else {
			useraction.drawButtonsCreateContent();
		}
	};
})(window.mydmam.useraction);

/**
 * prepareButtonCreate()
 * @param basereference to set in html attributes.
 * @return simple html content to draw
 */
(function(useraction) {
	useraction.prepareButtonCreate = function(item_key, is_directory, item_storagename, item_path) {
		var content = '';
		content = content + '<div class="btn-group ua-dropdown"';
		content = content + ' data-item_key="' + item_key + '"';
		content = content + ' data-is_directory="' + is_directory + '"';
		content = content + ' data-item_storagename="' + item_storagename + '"';
		content = content + ' data-item_path="' + item_path + '"';
		content = content + '>';
		content = content + '<button id="btn_ua_dropdown_' + item_key + '" class="btn btn-mini disabled" data-toggle="dropdown">';
		content = content + i18n('useractions.buttonactionlabel') + ' <span class="caret">';
		content = content + '</button>';
		content = content + '</div>';
		return content;
	};
	
})(window.mydmam.useraction);

/**
 * getFunctionalitiesForItem()
 * @return list never null
 */
(function(useraction) {
	useraction.getFunctionalityListForItem = function(item_key, is_directory, item_storagename, item_path) {
		var item_functionalities = [];
		var is_rootstorage = (item_path === '/') && is_directory;
		
		var availabilities_content = useraction.availabilities.content;
		for (var functionalities_classname in availabilities_content) {
			var functionality = availabilities_content[functionalities_classname];
			var messagebasename = functionality.messagebasename;
			
			if (functionality.capability === null) {
				continue;
			}
			var capability = functionality.capability;
			if ((capability.fileprocessing_enabled === false) & (is_directory === false)) {
				continue;
			}
			if ((capability.directoryprocessing_enabled === false) & is_directory) {
				continue;
			}
			if ((capability.rootstorageindexprocessing_enabled === false) & is_rootstorage) {
				continue;
			}
			if (capability.storageindexeswhitelist.length > 0) {
				var presence = false;
				for (var pos_whitelist in capability.storageindexeswhitelist) {
					var storage_whitelist = capability.storageindexeswhitelist[pos_whitelist];
					if (storage_whitelist === item_storagename) {
						presence = true;
						break;
					}
				}
				if (presence === false) {
					continue;
				}
			}
			var item_functionality = {};
			item_functionality.messagebasename = messagebasename;
			item_functionality.section = functionality.section;
			item_functionality.powerful_and_dangerous = functionality.powerful_and_dangerous;
			item_functionality.classname = functionalities_classname;
			item_functionalities.push(item_functionality);
		}
		return item_functionalities;
	};
})(window.mydmam.useraction);

/**
 * drawButtonsCreateContent() & drawButtonsCreateContentItemFunctionality(item_functionalities, item_key, is_directory, item_storagename, item_path)
 * Non-blocking action.
 * @return null
 */
(function(useraction) {
	
	useraction.drawButtonsCreateContentItemFunctionality = function(item_functionalities, item_key, is_directory, item_storagename, item_path, indexcreator) {
		var content = '';
		content = content + '<ul class="dropdown-menu">';
		
		var sections = {};
		for (var pos_f in item_functionalities) {
			var item_functionality = item_functionalities[pos_f];
			var section_content = sections[item_functionality.section];
			if (section_content == null) {
				section_content = [];
			}
			section_content.push(item_functionality);
			sections[item_functionality.section] = section_content;
		}
		
		var addLink = function(functionality) {
			content = content + '<li>';
			content = content + '<a class="btn-ua-dropdown-showcreate"';
			content = content + ' data-ua-classname="' + functionality.classname + '"';
			if (item_key) {
				content = content + ' data-item_key="' + item_key + '"';
			}
			if (is_directory != null) {
				content = content + ' data-is_directory="' + is_directory + '"';
			}
			if (item_storagename) {
				content = content + ' data-item_storagename="' + item_storagename + '"';
			}
			if (item_path) {
				content = content + ' data-item_path="' + item_path  + '"';
			}
			if (indexcreator != null) {
				content = content + ' data-ua-indexcreator="' + indexcreator + '"';
			} else {
				content = content + ' data-toggle="modal"';
			}
			content = content + '>';
			content = content + i18n('useractions.functionalities.' + functionality.messagebasename + '.name');
			if (functionality.powerful_and_dangerous) {
				content = content + ' <i class="icon-warning-sign"></i>';
			}
			content = content + '</a>';
			content = content + '</li>';
		};

		var startSubmenu = function(section_name) {
			content = content + '<li class="dropdown-submenu">';
			content = content + '<a tabindex="-1">' + i18n('useractions.functionalities.sections.' + section_name) + '</a>';
			content = content + '<ul class="dropdown-menu">';
		};
		
		var endSubmenu = function() {
			content = content + '</ul>';
			content = content + '</li>';
		};

		var addDivider = function(section_name) {
			//content = content + '<li class="divider"></li>';
			content = content + '<li class="nav-header">' + i18n('useractions.functionalities.sections.' + section_name) + '</li>';
		};
		
		for (var section_name in sections) {
			var section_content = sections[section_name];
			if (section_content.length === 1) {
				addDivider(section_name);
				addLink(section_content[0]);
			} else {
				startSubmenu(section_name);
				for (var pos_functionality in section_content) {
					var functionality = section_content[pos_functionality];
					addLink(functionality);
				}
				endSubmenu();
			}
		}

		content = content + '</ul>';
		return content;
	};
	
	var each_dropdown = function() {
		var item_key = $(this).data('item_key');
		var is_directory = $(this).data('is_directory');
		var item_storagename = $(this).data('item_storagename');
		var item_path = $(this).data('item_path');
		var item_functionalities = useraction.getFunctionalityListForItem(item_key, is_directory, item_storagename, item_path);
		
		if (item_functionalities.length === 0) {
			return;
		}
		
		$('#btn_ua_dropdown_' + item_key).addClass('dropdown-toggle').removeClass('disabled');
		
		$(this).append(useraction.drawButtonsCreateContentItemFunctionality(item_functionalities, item_key, is_directory, item_storagename, item_path));
	};

	var btn_ua_dropdown_showcreate_click = function() {
		var item = {
			key : $(this).data('item_key'),
			directory : $(this).data('is_directory'),
			storagename : $(this).data('item_storagename'),
			path : $(this).data('item_path')
		};
		var classname = $(this).data('ua-classname');
		
		useraction.creator.createModal(classname, item, null);
		
		$('#btn_ua_dropdown_' + item.key).dropdown('toggle').blur();
		return false;
	};
	
	useraction.drawButtonsCreateContent = function() {
		$('div.btn-group.ua-dropdown').each(each_dropdown);
		$("a.btn-ua-dropdown-showcreate").click(btn_ua_dropdown_showcreate_click);
	};
})(window.mydmam.useraction);
