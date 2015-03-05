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
 * isEnabled()
 * 
 * @return boolean
 */
(function(useraction) {
	useraction.isEnabled = function() {
		if (useraction.url.currentavailabilities === null) {
			return false;
		} else {
			return true;
		}
	};
})(window.mydmam.useraction);

/**
 * availabilities.downloadLast()
 * 
 * @param callback
 * @return null
 */
(function(useraction) {
	useraction.availabilities.downloadLast = function(callback) {
		if (useraction.isEnabled() === false) {
			useraction.availabilities.content = {};
			return;
		}
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
 * isStorageAsFunctionalities
 * 
 * @return boolean
 */
(function(useraction) {
	useraction.isStorageAsFunctionalities = function(storagename) {
		if (useraction.availabilities.content == null) {
			useraction.availabilities.downloadLast(useraction.isStorageAsFunctionalities);
		}
		for ( var functionality_class in useraction.availabilities.content) {
			var functionality = useraction.availabilities.content[functionality_class];
			if (functionality.capability.storageindexeswhitelist.length === 0) {
				return true;
			}
			for ( var storagename_pos in functionality.capability.storageindexeswhitelist) {
				if (storagename === functionality.capability.storageindexeswhitelist[storagename_pos]) {
					return true;
				}
			}
		}
		return false;
	};
})(window.mydmam.useraction);

/**
 * populateButtonsCreate()
 * 
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
 * 
 * @param basereference
 *            to set in html attributes.
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
 * 
 * @return list never null
 */
(function(useraction) {
	useraction.getFunctionalityListForItem = function(item_key, is_directory, item_storagename, item_path) {
		var item_functionalities = [];
		var is_rootstorage = (item_path === '/') && is_directory;

		var availabilities_content = useraction.availabilities.content;
		for ( var functionalities_classname in availabilities_content) {
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
				for ( var pos_whitelist in capability.storageindexeswhitelist) {
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
 * drawButtonsCreateContent() &
 * drawButtonsCreateContentItemFunctionality(item_functionalities, item_key,
 * is_directory, item_storagename, item_path) Non-blocking action.
 * 
 * @return null
 */
(function(useraction) {
	useraction.drawButtonsCreateContentItemFunctionality = function(item_functionalities, item_key, is_directory, item_storagename, item_path, indexcreator) {
		var content = '';
		content = content + '<ul class="dropdown-menu">';

		var sections = {};

		var addToList = function(item_functionality) {
			var section_content = sections[item_functionality.section];
			if (section_content == null) {
				section_content = [];
			}
			section_content.push(item_functionality);
			sections[item_functionality.section] = section_content;
		};

		/**
		 * Add simple sections
		 */
		for ( var pos_f in item_functionalities) {
			if (item_functionalities[pos_f].powerful_and_dangerous === false) {
				addToList(item_functionalities[pos_f]);
			}
		}

		/**
		 * Add simple powerful and dangerous sections
		 */
		for ( var pos_f in item_functionalities) {
			if (item_functionalities[pos_f].powerful_and_dangerous) {
				addToList(item_functionalities[pos_f]);
			}
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
				content = content + ' data-item_path="' + item_path + '"';
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
			content = content + '<li class="nav-header">' + i18n('useractions.functionalities.sections.' + section_name) + '</li>';
		};

		for ( var section_name in sections) {
			var section_content = sections[section_name];
			if (section_content.length === 1) {
				addDivider(section_name);
				addLink(section_content[0]);
			} else {
				startSubmenu(section_name);
				for ( var pos_functionality in section_content) {
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
			key: $(this).data('item_key'),
			directory: $(this).data('is_directory'),
			storagename: $(this).data('item_storagename'),
			path: $(this).data('item_path')
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

/**
 * drawBasketListAndAddUA()
 * 
 * @return null
 */
(function(useraction) {
	useraction.drawBasketListAndAddUA = function() {
		/**
		 * Display current basket
		 */
		var content = '';
		content = content + "<ul>";
		current_basket_content = mydmam.basket.content.backend.pullData();
		var pathelementkeys_to_resolve = [];
		for (var pos_b = 0; pos_b < current_basket_content.length; pos_b++) {
			var basket_element_key = current_basket_content[pos_b];
			content = content + "<li>";
			content = content + '<div class="btn-group" style="margin-right: 8pt;">';
			content = content + '<button type="button" class="btn btn-mini active btnbasket" data-toggle="button" data-elementkey="' + basket_element_key + '"><i class="icon-star"></i></button>';
			content = content + '<button type="button" class="btn btn-mini basketpresence" data-elementkey="' + basket_element_key + '"><i class="icon-picture"></i></button>';
			content = content + '</div>';
			content = content + '<span class="pathelement" data-elementkey="' + basket_element_key + '" style="color: #222222;"></span>';
			content = content + "</li>";
			pathelementkeys_to_resolve.push(basket_element_key);
		}
		if (current_basket_content.length === 0) {
			content = content + '<li><p class="muted">' + i18n("userprofile.baskets.empty") + '</p></li>';
		}
		content = content + "</ul>";

		$('div.basketaddua').html(content);

		/**
		 * Actions on displayed basket
		 */
		if (current_basket_content.length === 0) {
			$("#uacreation").empty();
			return;
		}

		var pathelementkeys_resolved = mydmam.basket.showPathIndexKeysForBasketItemsLabel(pathelementkeys_to_resolve);

		mydmam.basket.setSwitchButtonsEvents();
		$('.btnbasket').click(useraction.drawBasketListAndAddUA);
		mydmam.basket.setNavigateButtonsEvents(pathelementkeys_resolved);

		/**
		 * Prepare UA creator
		 */
		var creator = mydmam.useraction.creator;
		// var availabilities = mydmam.useraction.availabilities;

		var items = [];
		var itemskeys = [];
		for (var pathelementkey in pathelementkeys_resolved) {
			var item = pathelementkeys_resolved[pathelementkey].reference;
			item.key = pathelementkey;
			items.push(item);
			itemskeys.push(pathelementkey);
		}
		creator.current = {};
		creator.current.items = items;
		var final_functionalities = creator.mergueAllFunctionalitiesForAllItemsBasket(creator.current.items);

		/**
		 * Display UA creator
		 */
		content = '';
		content = content + '<form class="form-horizontal">';

		content = content + '<p class="lead">' + i18n("useractions.newaction.settings") + '</p>';
		content = content + '<span class="ua-creation-boxaddnewconfigurator"></span>';
		content = content + '<div class="control-group ua-creation-addnewconfigurator-btngroup">';
		content = content + '<div class="btn-group ua-dropdown">';
		content = content + '<button class="btn btn-info" data-toggle="dropdown">';
		content = content + i18n('useractions.newaction.setup.addfirstsetting') + ' <span class="caret">';
		content = content + '</button>';
		content = content + useraction.drawButtonsCreateContentItemFunctionality(final_functionalities, null, null, null, null, 0);
		content = content + '</div>'; // btn-group ua-dropdown
		content = content + '</div>'; // control-group

		content = content + '<p class="lead">' + i18n("useractions.newaction.setup") + '</p>';

		content = content + creator.prepareFinisherForm(basketname);
		content = content + creator.prepareCommentForm();
		content = content + creator.prepareUserNotificationReasonsForm();

		content = content + '</form>';
		content = content + '<button class="btn btn-primary btn-large hide ua-creation-start">' + i18n("useractions.startnewaction") + '</button>';
		$("#uacreation").html(content);

		/**
		 * Add btn handlers
		 */
		creator.addNewConfiguratorFunctionalityHandler('#uacreation');

		$('#uacreation a.btn-ua-dropdown-showcreate').click(function() {
			/**
			 * To remove the protection which block to add UA without add a
			 * functionality.
			 */
			$('#uacreation button.ua-creation-start').removeClass('hide');
		});

		/**
		 * Called by useraction page
		 */
		var onValidationForm = function() {
			var request = {};
			request.items = itemskeys;
			request.basket_name = basketname;
			request.comment = creator.getCommentFromCreator();
			request.notification_reasons = creator.getUserNotificationReasonsFromCreator();
			request.finisher = creator.getFinisherFromCreator();
			request.configured_functionalities = creator.getFunctionalityConfigurationsFromUACreation("#uacreation");

			document.body.style.cursor = 'wait';
			$("#alertcontainer").empty();
			$('html').first().scrollTop(0);

			creator.requestUA(request, function() {
				document.body.style.cursor = 'default';
				var content = "";
				content = content + '<div class="alert alert-info">';
				content = content + '<button type="button" class="close" data-dismiss="alert">&times;</button>';
				content = content + '<h4>' + i18n("useractions.newaction.requestvalid") + '</h4>';
				content = content + i18n("useractions.newaction.requestvalid.text", "userprofile.notifications.pagename");
				content = content + '</div>';
				$('#alertcontainer').html(content);
			}, function() {
				document.body.style.cursor = 'default';
				var content = "";
				content = content + '<div class="alert alert-error">';
				content = content + '<button type="button" class="close" data-dismiss="alert">&times;</button>';
				content = content + '<h4>' + i18n("useractions.newaction.requesterror") + '</h4>';
				content = content + i18n("useractions.newaction.requesterror.text");
				content = content + '</div>';
				$('#alertcontainer').html(content);
			});
		};
		$('#uacreation button.ua-creation-start').click(onValidationForm);
	};
})(window.mydmam.useraction);
