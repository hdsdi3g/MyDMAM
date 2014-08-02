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

/**
 * showPathIndexKeysForBasketItemsLabel
 * @returns pathelementkeys_resolved
 */
(function(basket) {
	basket.showPathIndexKeysForBasketItemsLabel = function(pathelementkeys_to_resolve) {
		if (pathelementkeys_to_resolve.length === 0) {
			return;
		}
		var pathelementkeys_resolved = mydmam.stat.query(pathelementkeys_to_resolve, mydmam.stat.SCOPE_PATHINFO);
		var display = function() {
			var elementkey = $(this).data('elementkey');
			var element = pathelementkeys_resolved[elementkey];
			if (element.reference) {
				var content = '';
				if (element.reference.directory) {
					content = content + '<span style="margin-right: 4px;"><i class="icon-folder-open"></i></span>';
				} else {
					content = content + '<span style="margin-right: 6px;"><i class="icon-file"></i></span>';
				}

				content = content + '<span style="font-weight: bold;">' + element.reference.storagename + '</span> :: ';
				var path = element.reference.path.split("/");
				for (var pos = 1; pos < path.length; pos++) {
					content = content + "/" + path[pos];
				}
				
				if (element.reference.size) {
					content = content + ' <span class="label label-important" style="margin-left: 1em;">' + element.reference.size + '</span>';
				}
				content = content + ' <span class="label" style="margin-left: 1em;">' + mydmam.format.fulldate(element.reference.date) + '</span>';
				$(this).html(content);
			}
		};
		
		$('span.pathelement').each(display);
		return pathelementkeys_resolved;
	};
})(window.mydmam.basket);

/**
 * setNavigateButtonsEvents
 */
(function(basket) {
	basket.setNavigateButtonsEvents = function(pathelementkeys_resolved) {
		var event = function() {
			var elementkey = $(this).data("elementkey");
			var element = pathelementkeys_resolved[elementkey];
			if (element.reference) {
				$(location).attr('href', mydmam.metadatas.url.navigate + "#" + element.reference.storagename + ":" + element.reference.path);
			}
		};
		$('button.basketpresence').each(function() {
			$(this).click(event);
		});
	};
})(window.mydmam.basket);

/**
 * setSwitchBasketButtonsEvents()
 */
(function(basket) {
	basket.setSwitchBasketButtonsEvents = function() {
		var on_switch_event = function(name, data) {
			document.body.style.cursor = 'default';
			if (name === null) {
				return;
			}
			basket.showAll(null, null);
		};
		
		var event = function() {
			var basketname = $(this).data("basketname");
			document.body.style.cursor = 'wait';
			basket.content.backend.switch_selected(basketname, on_switch_event);
		};
		
		$('input.btnswitchbasket').each(function() {
			$(this).click(event);
		});

	};
})(window.mydmam.basket);


/**
 * setRenameBasketButtonsEvents()
 */
(function(basket) {
	basket.setRenameBasketButtonsEvents = function() {
		var on_rename_event = function(name, newname) {
			if (name === null) {
				return;
			}
			basket.showAll(null, null);
		};
		
		var event_click = function() {
			var basketname = $(this).data("basketname");
			var id = '#inputbasketname' + md5(basketname).substring(0, 6);
			var newname = $(id).val().trim();
			if (newname === "") {
				return;
			}
			basket.content.backend.rename(basketname, newname, on_rename_event);
		};
		
		var event_keyenter = function(event) {
			if (event.which !== 13) {
				return;
			}
			var basketname = $(this).data("basketname");
			var id = '#inputbasketname' + md5(basketname).substring(0, 6);
			var newname = $(id).val().trim();
			if (newname === "") {
				return;
			}
			basket.content.backend.rename(basketname, newname, on_rename_event);
		};
		
		$('button.btnrenamebasket').each(function() {
			$(this).click(event_click);
		});

		$('input.inputbasketname').each(function() {
			$(this).keypress(event_keyenter);
		});
		//" data-basketname="' + current_basket.name
	};
})(window.mydmam.basket);


/**
 * setRemoveBasketButtonsEvents()
 */
(function(basket) {
	basket.setRemoveBasketButtonsEvents = function() {
		var on_remove_event = function(name) {
			if (name === null) {
				return;
			}
			basket.showAll(null, null);
		};
		
		var event = function() {
			var basketname = $(this).data("basketname");
			basket.content.backend.bdelete(basketname, on_remove_event);
		};
		
		$('button.btnremovebasket').each(function() {
			$(this).click(event);
		});
	};
})(window.mydmam.basket);

/**
 * setTruncateBasketButtonsEvents()
 */
(function(basket) {
	basket.setTruncateBasketButtonsEvents = function() {
		var on_remove_event = function(name) {
			if (name === null) {
				return;
			}
			basket.showAll(null, null);
		};
		
		var event = function() {
			var basketname = $(this).data("basketname");
			basket.content.backend.truncate(basketname, on_remove_event);
		};
		
		$('button.btntruncatebasket').each(function() {
			$(this).click(event);
		});
	};
})(window.mydmam.basket);


/**
 * showAll
 */
(function(basket) {
	basket.showAll = function(actual_content, actual_selected) {
		if (actual_content === null) {
			/**
			 * Fetch via Ajax
			 */
			basket.content.backend.all(function(data) {
				basket.showAll(data, actual_selected);
			});
			return;
		}
		if (actual_selected === null) {
			/**
			 * Fetch via Ajax
			 */
			basket.content.backend.selected(function(data) {
				basket.showAll(actual_content, data);
			});
			return;
		}
		
		var html = '';
		
		html = html + "<ul>";
		var pathelementkeys_to_resolve = [];
		
		var current_basket;
		var current_basket_content;
		var basket_element;
		var is_selected;
		for (var pos = 0; pos < actual_content.length; pos++) {
			current_basket = actual_content[pos];
			is_selected = (actual_selected === current_basket.name);
			
			html = html + '<li style="margin-bottom: 2em;">'; 
			html = html + '<div class="input-prepend input-append">';
			if (is_selected) {
				html = html + '<span class="add-on"><input type="radio" checked="checked"></span>'; 
			} else {
				html = html + '<span class="add-on"><input type="radio" class="btnswitchbasket" data-basketname="' + current_basket.name + '"></span>'; 
			}
			html = html + '<input type="text" id="inputbasketname' + md5(current_basket.name).substring(0, 6) + '" class="span2 inputbasketname" data-basketname="' + current_basket.name + '" placeholder="' + i18n("userprofile.baskets.basketname") + '" value="' + current_basket.name + '" />';
			html = html + '<button class="btn btnrenamebasket" data-basketname="' + current_basket.name + '"><i class="icon-edit"></i></button>';
			html = html + '<button class="btn btntruncatebasket" data-basketname="' + current_basket.name + '"><i class="icon-remove-sign"></i></button>';
			if (is_selected === false) {
				html = html + '<button class="btn btnremovebasket" data-basketname="' + current_basket.name + '"><i class="icon-remove"></i></button>';
			}
			html = html + '</div>'; 
			html = html + "<ul>"; 
			
			current_basket_content = current_basket.content;
			for (var pos_b = 0; pos_b < current_basket_content.length; pos_b++) {
				basket_element_key = current_basket_content[pos_b];
				html = html + "<li>";
				html = html + '<div class="btn-group" style="margin-right: 8pt;">';
				if (is_selected) {
					html = html + '<button type="button" class="btn btn-mini active btnbasket" data-toggle="button" data-elementkey="' + basket_element_key + '"><i class="icon-star"></i></button>';
				} else {
					html = html + '<button type="button" class="btn btn-mini disabled"><i class="icon-star"></i></button>';
				}
				html = html + '<button type="button" class="btn btn-mini basketpresence" data-elementkey="' + basket_element_key + '"><i class="icon-picture"></i></button>';
				html = html + '</div>';
				html = html + '<span class="pathelement" data-elementkey="' + basket_element_key + '" style="color: #222222;"></span>';
				html = html + "</li>";
				pathelementkeys_to_resolve.push(basket_element_key);
			}
			
			if (current_basket_content.length === 0) {
				html = html + '<li><p class="muted">' + i18n("userprofile.baskets.empty") + '</p></li>';
			}
			
			html = html + "</ul></li>"; 
		}
		html = html + "<li>"; 
		html = html + '<div class="input-append">'; 
		html = html + '<input type="text" id="inputnewbasket" class="span2" placeholder="' + i18n("userprofile.baskets.newbasketname") + '" />';
		html = html + '<button type="button" id="createnewbasket" class="btn btn-success"><i class="icon-plus icon-white"></i></button>';
		html = html + '</div>'; 
		html = html + "</li>"; 
		
		html = html + "</ul>";
		$("#basketslist").html(html);
		
		var pathelementkeys_resolved = basket.showPathIndexKeysForBasketItemsLabel(pathelementkeys_to_resolve);
		basket.setSwitchButtonsEvents();
		basket.setNavigateButtonsEvents(pathelementkeys_resolved);
		basket.setSwitchBasketButtonsEvents();
		basket.setTruncateBasketButtonsEvents();
		basket.setRemoveBasketButtonsEvents();
		basket.setRenameBasketButtonsEvents();
		
		$("#createnewbasket").click(function() {
			var newname = $("#inputnewbasket").val().trim();
			if (newname === "") {
				return;
			}
			basket.content.backend.create(newname, true, function(name, switch_to_selected) {
				if (name) {
					basket.showAll(null, name);
				}
			});
		});
		
	};
})(window.mydmam.basket);
