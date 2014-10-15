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
 * prepareModalHeader()
 * @return html string
 */
(function(creator) {
	creator.prepareModalHeader = function() {
		var content = '';
		content = content + '<div id="uacreationmodal" class="modal hide" tabindex="-1" role="dialog" aria-labelledby="uacreationmodalLabel" aria-hidden="true">';
		content = content + '<div class="modal-header">';
		content = content + '<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>';
		content = content + '<button class="btn btn-primary btn-mini ua-creation-start">' + i18n("useractions.startnewaction") + '</button>';
		content = content + '<h3 id="uacreationmodalLabel">' + i18n("useractions.newaction") + '</h3>';
		content = content + '</div>';
		content = content + '<div class="modal-body ua-creation-box">';
		return content;
	};
})(mydmam.useraction.creator);

/**
 * prepareModalFooter()
 * @return html string
 */
(function(creator) {
	creator.prepareModalFooter = function() {
		var content = '';
		content = content + '</div>';
		content = content + '<div class="modal-footer">';
		content = content + '<button class="btn" data-dismiss="modal" aria-hidden="true">' + i18n("useractions.cancelnewaction") + '</button>';
		content = content + '<button class="btn btn-primary ua-creation-start">' + i18n("useractions.startnewaction") + '</button>';
		content = content + '</div>';
		content = content + '</div>';
		return content;
	};
})(mydmam.useraction.creator);

/**
 * prepareFinisherForm() + getFinisherFromCreator()
 * @return html string + {}
 */
(function(creator) {
	creator.prepareFinisherForm = function(basketname) {
		var content = '';
		content = content + '<div class="control-group">';
		content = content + '<label class="control-label">' + i18n('useractions.newaction.setup.finisher') + '</label>';
		content = content + '<div class="controls">';
		content = content + '<label class="checkbox">';
		content = content + '<input type="checkbox" class="ua-creation-finisher" value="soft_refresh_source_storage_index_item"> ' + i18n("useractions.newaction.setup.finisher.soft_refresh_source_storage_index_item");
		content = content + '</label>';
		content = content + '<label class="checkbox">';
		content = content + '<input type="checkbox" class="ua-creation-finisher" value="force_refresh_source_storage_index_item"> ' + i18n("useractions.newaction.setup.finisher.force_refresh_source_storage_index_item");
		content = content + '</label>';
		content = content + '<label class="checkbox';
		if (basketname === null) {
			content = content + ' hide';
		}
		content = content + '">';
		content = content + '<input type="checkbox" class="ua-creation-finisher" value="remove_user_basket_item"> ' + i18n("useractions.newaction.setup.finisher.remove_user_basket_item");
		content = content + '</label>';
		content = content + '</div>';
		content = content + '</div>';
		return content;
	};
	creator.getFinisherFromCreator = function() {
		var result = {};
		$('input.ua-creation-finisher').each(function() {
			result[$(this).val()] = false;
		});
		$('input.ua-creation-finisher:checked').each(function() {
			result[$(this).val()] = true;
		});
		return result;
	};
})(mydmam.useraction.creator);

/**
 * prepareRangeForm() + getRangeFromCreator()
 * @return html string + String
 */
(function(creator) {
	creator.prepareRangeForm = function(basketname) {
		var content = '';
		content = content + '<div class="control-group hide ua-creation-range-group">';
		content = content + '<label class="control-label">' + i18n('useractions.newaction.setup.range') + '</label>';
		content = content + '<div class="controls">';
		content = content + '<label class="radio">';
		content = content + '<input type="radio" class="ua-creation-range" name="ua-creation-range" checked="checked" value="ONE_USER_ACTION_BY_STORAGE_AND_BASKET"> ' + i18n("useractions.newaction.setup.range.ONE_USER_ACTION_BY_STORAGE_AND_BASKET");
		content = content + '</label>';
		if (basketname) {
			content = content + '<label class="radio">';
			content = content + '<input type="radio" class="ua-creation-range" name="ua-creation-range" value="ONE_USER_ACTION_BY_BASKET_ITEM"> ' + i18n("useractions.newaction.setup.range.ONE_USER_ACTION_BY_BASKET_ITEM");
			content = content + '</label>';
		}
		content = content + '<label class="radio">';
		content = content + '<input type="radio" class="ua-creation-range" name="ua-creation-range" value="ONE_USER_ACTION_BY_FUNCTIONALITY"> ' + i18n("useractions.newaction.setup.range.ONE_USER_ACTION_BY_FUNCTIONALITY");
		content = content + '</label>';
		content = content + '</div>';
		content = content + '</div>';
		return content;
	};
	creator.getRangeFromCreator = function() {
		return $('input.ua-creation-range:radio[name=ua-creation-range]:checked').val();
	};
})(mydmam.useraction.creator);

/**
 * prepareCommentForm() + getCommentFromCreator()
 * @return html string + String
 */
(function(creator) {
	creator.prepareCommentForm = function() {
		var content = '';
		content = content + '<div class="control-group">';
		content = content + '<label class="control-label" for="uacreationcomment">' + i18n('useractions.newaction.setup.comment') + '</label>';
		content = content + '<div class="controls">';
		content = content + '<input type="text" class="input-xlarge" id="uacreationcomment">';
		content = content + '</div>';
		content = content + '</div>';
		return content;
	};
	creator.getCommentFromCreator = function() {
		return $('#uacreationcomment').val();
	};
})(mydmam.useraction.creator);

/**
 * prepareUserNotificationReasonsForm() + getUserNotificationReasonsFromCreator()
 * @return html string + [String]
 */
(function(creator) {
	creator.prepareUserNotificationReasonsForm = function() {
		var content = '';
		content = content + '<div class="control-group">';
		content = content + '<label class="control-label">' + i18n('useractions.newaction.setup.notification_reasons') + '</label>';
		content = content + '<div class="controls">';
		content = content + '<label class="checkbox">';
		content = content + '<input type="checkbox" class="ua-creation-ntf-reason" value="ERROR" checked="checked"> ' + i18n("useractions.newaction.setup.notification_reasons.ERROR");
		content = content + '</label>';
		content = content + '<label class="checkbox">';
		content = content + '<input type="checkbox" class="ua-creation-ntf-reason" value="DONE"> ' + i18n("useractions.newaction.setup.notification_reasons.DONE");
		content = content + '</label>';
		content = content + '<label class="checkbox hide">';
		content = content + '<input type="checkbox" class="ua-creation-ntf-reason" value="COMMENTED"> ' + i18n("useractions.newaction.setup.notification_reasons.COMMENTED");
		content = content + '</label>';
		content = content + '<label class="checkbox hide">';
		content = content + '<input type="checkbox" class="ua-creation-ntf-reason" value="CLOSED"> ' + i18n("useractions.newaction.setup.notification_reasons.CLOSED");
		content = content + '</label>';
		content = content + '</div>';
		content = content + '</div>';
		return content;
	};
	creator.getUserNotificationReasonsFromCreator = function() {
		var result = [];
		$('input.ua-creation-ntf-reason:checked').each(function() {
			result.push($(this).val());
		});
		return result;
	};
})(mydmam.useraction.creator);

/**
 * prepareConfiguratorForFunctionality() & addNewConfiguratorFunctionality()
 * @param classname java class UA to create
 * @return html string
 */
(function(creator, availabilities, useraction) {
	creator.prepareConfiguratorForFunctionality = function(classname, index) {
		if (index === null) {
			index = 0;
		}
		if (availabilities.content[classname] === null){
			return "";
		}

		/**
		 * Display form
		 */
		var content = '';
		
		var configurator = availabilities.content[classname].configurator;
		var fields = configurator.fields;
		var messagebasename = availabilities.content[classname].messagebasename;
		
		content = content + '<h5>';
		content = content + i18n('useractions.newaction.settings.item') + " ";
		content = content + i18n('useractions.functionalities.' + availabilities.content[classname].messagebasename + '.name');
		content = content + '</h5>';
		
		for (var field_pos in fields) {
			var field = fields[field_pos];
			var default_value = configurator.object[field.name];
			if (default_value == null) {
				default_value = "";
			}
			
			var translatedfieldname = i18n('useractions.functionalities.' + messagebasename + '.fields.' + field.name);
			if (translatedfieldname === 'useractions.functionalities.' + messagebasename + '.fields.' + field.name) {
				/**
				 * No translation.
				 */
				translatedfieldname = field.name;
			}
			var translatedhelpfieldname = i18n('useractions.functionalities.' + messagebasename + '.fields.' + field.name + '.help');
			if (translatedhelpfieldname === 'useractions.functionalities.' + messagebasename + '.fields.' + field.name + '.help') {
				/**
				 * No translation.
				 */
				translatedhelpfieldname = null;
			}

			var input_id = 'ua_creation_configuratoritem_' + index + '_' + messagebasename + "_" + field.name;
			var input_classname = 'ua-creation-configuratoritem';
			var input_data = 'data-fieldname="' + field.name + '" data-functclassname="' + classname + '" data-functindex="' + index + '"';
			
			content = content + '<div class="control-group">';
			content = content + '<label class="control-label" for="' + input_id + '">' + translatedfieldname + '</label>';
			content = content + '<div class="controls">';
			
			if (field.type === 'text') {
				content = content + '<input id="' + input_id + '" type="text" value="' + default_value + '" ' + input_data + ' class="input-xlarge ' + input_classname + '" />';
			} else if (field.type === 'password') {
				content = content + '<input id="' + input_id + '" type="password" value="' + default_value + '" ' + input_data + ' class="input-xlarge ' + input_classname + '" />';
			} else if (field.type === 'email') {
				content = content + '<input id="' + input_id + '" type="email" value="' + default_value + '" ' + input_data + ' class="input-xlarge ' + input_classname + '" />';
			} else if (field.type === 'longtext') {
				content = content + '<textarea id="' + input_id + '" cols="48" rows="5" ' + input_data + ' class="input-xlarge ' + input_classname + '">' + default_value + '</textarea>';
			} else if (field.type === 'number') {
				content = content + '<input id="' + input_id + '" type="number" value="' + default_value + '" ' + input_data + ' class="input-xlarge ' + input_classname + '" />';
			} else if (field.type === 'date') {
				content = content + '<input id="' + input_id + '" type="email" value="' + default_value + '" ' + input_data + ' class="input-xlarge ' + input_classname + '" />';
			} else if (field.type === 'boolean') {
				content = content + '<label class="checkbox">';
				if (default_value) {
					content = content + '<input id="' + input_id + '" type="checkbox" ' + input_data + ' class="' + input_classname + '" checked="checked" />';
				} else {
					content = content + '<input id="' + input_id + '" type="checkbox" ' + input_data + ' class="' + input_classname + '" />';
				}
				content = content + translatedfieldname;
				content = content + '</label>';
			}
			
			if (translatedhelpfieldname) {
				content = content + '<span class="help-block">' + translatedhelpfieldname + '</span>';
			}
			
			content = content + '</div>';
			content = content + '</div>';
		}
		
		content = content + '<span class="ua-creation-boxaddnewconfigurator"></span>';
		
		/**
		 * Display the add another functionality button
		 */
		content = content + '<div class="control-group ua-creation-addnewconfigurator-btngroup">';
		content = content + '<div class="controls">';
		content = content + '<div class="btn-group ua-dropdown">';
		content = content + '<button class="btn btn-info" data-toggle="dropdown">';
		content = content + i18n('useractions.newaction.setup.addnewsettings') + ' <span class="caret">';
		content = content + '</button>';
		
		var final_functionalities = creator.mergueAllFunctionalitiesForAllItemsBasket(creator.current.items);

		content = content + useraction.drawButtonsCreateContentItemFunctionality(final_functionalities, null, null, null, null, index + 1);
		
		content = content + '</div>'; // btn-group ua-dropdown
		content = content + '</div>'; // controls
		content = content + '</div>'; // control-group
		
		return content;
	};
	
	creator.addNewConfiguratorFunctionalityHandler = function(jquery_selector_base) {
		var onclick = function() {
			var classname = $(this).data("ua-classname");
			var indexcreator = $(this).data("ua-indexcreator");
			var content = creator.prepareConfiguratorForFunctionality(classname, indexcreator);
			
			$(jquery_selector_base + ' div.control-group.ua-creation-addnewconfigurator-btngroup').remove();
			$(jquery_selector_base + ' span.ua-creation-boxaddnewconfigurator:last').after(content);
			creator.addNewConfiguratorFunctionalityHandler(jquery_selector_base);
			
			$(jquery_selector_base + ' div.ua-creation-range-group').removeClass("hide");
		};
		$(jquery_selector_base + ' a.btn-ua-dropdown-showcreate').click(onclick);
	};
})(mydmam.useraction.creator, mydmam.useraction.availabilities, mydmam.useraction);

/**
 * mergueAllFunctionalitiesForAllItemsBasket()
 * @param current_items
 * @return Array of functionalities
 */
(function(creator, useraction) {
	creator.mergueAllFunctionalitiesForAllItemsBasket = function(current_items) {
		var importitem = function(item) {
			var item_key = item.key;
			var is_directory =  item.directory;
			var item_storagename = item.storagename;
			var item_path = item.path;
			return useraction.getFunctionalityListForItem(item_key, is_directory, item_storagename, item_path);
		};
		
		var items_functionalities = importitem(current_items[0]);
		var final_functionalities = [];
		
		if (current_items.length > 1) {
			for (var pos_functionalities in items_functionalities) {
				/**
				 * for each functionalities from first element
				 */
				var current_global_functionality = items_functionalities[pos_functionalities];
				var founded = false;
	
				for (var pos_items = 1; pos_items < current_items.length; pos_items++) {
					/**
					 * for each items after the first
					 */
					var item = current_items[pos_items];
					var functionalities = importitem(item);
					for (var pos_item_functionality in functionalities) {
						/**
						 * for each functionalities to each items
						 */
						var current_item_functionality = functionalities[pos_item_functionality];
						if (current_global_functionality.classname === current_item_functionality.classname) {
							founded = true;
							break;
						}
					}
					if (founded === false) {
						break;
					}
				}
				
				if (founded) {
					final_functionalities.push(current_global_functionality);
				}
			}
		} else {
			final_functionalities = items_functionalities;
		}

		return final_functionalities;
	};
})(mydmam.useraction.creator, mydmam.useraction);

/**
 * prepareDisplaySelectedItemInList()
 * @param item
 * @return html string
 */
(function(creator) {
	creator.prepareDisplaySelectedItemInList = function(item) {
		content = '';
		if (item.directory) {
			content = content + '<span class="label label-success">' + i18n("useractions.newaction.source.directory") + '</span> ';
		}
		content = content + '<strong>' + item.storagename + '</strong>';
		content = content + ' :: ';
		
		var last_slash_pos = item.path.lastIndexOf("/");
		if (last_slash_pos > 0) {
			content = content + '<span class="text-success">' + item.path.substring(0, last_slash_pos + 1) + '</span>';
			content = content + item.path.substring(last_slash_pos + 1, item.path.length);
		} else {
			content = content + item.path;
		}
		content = content + '';
		content = content + '';
		return content;
	};
})(mydmam.useraction.creator);

/**
 * createModal()
 * public use
 * @param classname java class UA to create
 * @param items Object/Array of objects, item mush have key, directory, storagename and path as content.
 * @param basketname can be null
 * @return null
 */
(function(creator, availabilities) {
	creator.createModal = function(classname, items, basketname) {
		if ($.isArray(items) === false) {
			items = [items];
		}
		
		creator.current = {
			classname: classname,
			items: items
		};
		
		$("#uacreationmodal").remove();

		var content = '';
		content = content + creator.prepareModalHeader();

		content = content + '<p class="lead">' + i18n("Source") + '</p>';
		
		/**
		 * Display basket name
		 **/
		if (basketname) {
			content = content + '<span class="label label-inverse ua-basketname" data-basketname="' + basketname + '">' + i18n("useractions.newaction.sourcefrombasket") + " " + basketname + '</span> ';
		} else {
			content = content + '<span class="hide ua-basketname" data-basketname=""></span>';
		}
		
		/**
		 * Item list
		 **/
		if (items.length > 1) {
			content = content + i18n("useractions.newaction.manyselected");
			content = content + '<ul class="ua-sourcesitems">';
			for (var pos_item in items) {
				content = content + '<li><i class="icon-star"></i> ';
				content = content + '<span class="hide ua-sourcesitem" data-itemkey="' + items[pos_item].key + '"></span>';
				content = content + creator.prepareDisplaySelectedItemInList(items[pos_item]);
				content = content + '</li>';
			}
			content = content + '</ul>';
		} else {
			content = content + i18n("useractions.newaction.oneselected");
			content = content + '<br><i class="icon-star-empty"></i> ';
			content = content + '<span class="hide ua-sourcesitem" data-itemkey="' + items[0].key + '"></span>';
			content = content + creator.prepareDisplaySelectedItemInList(items[0]);
		}

		content = content + '<form class="form-horizontal">';

		content = content + '<p class="lead">' + i18n("useractions.newaction.settings") + '</p>';
		content = content + creator.prepareConfiguratorForFunctionality(classname, 0);
		
		content = content + '<p class="lead">' + i18n("useractions.newaction.setup") + '</p>';
		
		content = content + creator.prepareRangeForm(basketname);
		content = content + creator.prepareFinisherForm(basketname);
		content = content + creator.prepareCommentForm();
		content = content + creator.prepareUserNotificationReasonsForm();
		
		content = content + '</form>';
		content = content + creator.prepareModalFooter();
		$("body").append(content);
		
		$('#uacreationmodal').modal({});
		$('#uacreationmodal div.modal-body.ua-creation-box').scrollTop(0);
		
		$('#uacreationmodal').on('hidden', function () {
			creator.current = null;
		});
		$('#uacreationmodal button.ua-creation-start').click(creator.onValidationForm);
		
		creator.addNewConfiguratorFunctionalityHandler('#uacreationmodal');
	};
})(mydmam.useraction.creator, mydmam.useraction.availabilities);


/**
 * getFunctionalityConfigurationsFromUACreation()
 * @return never null: [{functionality_classname: java class name, raw_associated_user_configuration: configured object}]
 */
(function(creator, availabilities) {
	creator.getFunctionalityConfigurationsFromUACreation = function(jquery_selector_base) {
		var result = [];
		var extractConfiguration = function() {
			var fieldname = $(this).data("fieldname");
			var classname = $(this).data("functclassname");
			var index = $(this).data("functindex");
			
			var item = null;
			item = result[index];
			
			if (item == null) {
				result[index] = {
					functionality_classname: classname,
					raw_associated_user_configuration: {}
				};
			}
			result[index].raw_associated_user_configuration[fieldname] = $(this).val();
		};
		$(jquery_selector_base + ' .ua-creation-configuratoritem').each(extractConfiguration);
		return result;
	};
})(mydmam.useraction.creator, mydmam.useraction.availabilities);

/**
 * getItemsFormCreator()
 * @return never null: [item key]
 */
(function(creator, availabilities) {
	creator.getItemsFormCreator = function(jquery_selector_base) {
		var result = [];
		var extract = function() {
			var itemkey = $(this).data("itemkey");
			if (itemkey === null) {
				return;
			}
			for (var pos_result in result) {
				if (result[pos_result] === itemkey) {
					return;
				}
			}
			result.push(itemkey);
		};
		$(jquery_selector_base + ' .ua-sourcesitem').each(extract);
		return result;
	};
})(mydmam.useraction.creator, mydmam.useraction.availabilities);

/**
 * getBasketNameFromCreator()
 * @return never null: String
 */
(function(creator, availabilities) {
	creator.getBasketNameFromCreator = function(jquery_selector_base) {
		return $(jquery_selector_base + ' .ua-basketname').data("basketname");
	};
})(mydmam.useraction.creator, mydmam.useraction.availabilities);

/**
 * onValidationForm()
 * @return null
 */
(function(creator) {
	creator.onValidationForm = function() {
		var request = {};
		request.items = creator.getItemsFormCreator("#uacreationmodal");
		request.basket_name = creator.getBasketNameFromCreator("#uacreationmodal");
		request.comment = creator.getCommentFromCreator();
		request.notification_reasons = creator.getUserNotificationReasonsFromCreator();
		request.finisher_json = JSON.stringify(creator.getFinisherFromCreator());
		request.range = creator.getRangeFromCreator();
		request.configured_functionalities_json = JSON.stringify(creator.getFunctionalityConfigurationsFromUACreation("#uacreationmodal"));
		
		document.body.style.cursor = 'wait';
		creator.requestUA(request, function() {
			document.body.style.cursor = 'default';
			$('#uacreationmodal').modal('hide');
		}, function() {
			document.body.style.cursor = 'default';
			var content = "";
			content = content + '<div class="alert alert-error" style="margin-top: 1em;">';
			content = content + '<button type="button" class="close" data-dismiss="alert">&times;</button>';
			content = content + '<h4>' + i18n("useractions.newaction.requesterror") + '</h4>';
			content = content + i18n("useractions.newaction.requesterror.text");
			content = content + '</div>';
			$('div.ua-creation-box').prepend(content);
			$('#uacreationmodal div.modal-body.ua-creation-box').scrollTop(0);
		});
		
	};
})(mydmam.useraction.creator);

/**
 * availabilities.downloadLast()
 * @param request Object
 * @param callback function()
 * @param callback_error function()
 * @return null
 */
(function(creator) {
	creator.requestUA = function(request, callback, callback_error) {
		$.ajax({
			url: mydmam.useraction.url.create,
			type: "POST",
			async: true,
			data: request,
			error: callback_error,
			success: function(data) {
				if (data.result) {
					callback();
				} else {
					callback_error();
				}
			}
		});
	};
})(window.mydmam.useraction.creator);

