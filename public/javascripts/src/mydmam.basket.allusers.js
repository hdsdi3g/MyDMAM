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
	allusers.usersname = {};
	allusers.userkeys = [];
	allusers.pathindexelements = {};
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
	};
})(window.mydmam.basket.allusers);

/**
 * displayUsers
 */
(function(allusers) {
	allusers.displayUsers = function() {
		var content = "";
		for (var pos in allusers.userkeys) {
			content = content + '<li id="libtnselectuser' + pos + '" class="libtnselectuser">';
			content = content + '<a href="#" data-userkey="' + allusers.userkeys[pos] + '" data-selectuserpos="' + pos + '" class="btnbasketusername">';
			content = content + allusers.usersname[allusers.userkeys[pos]];
			content = content + '</a>';
			content = content + '</li>';
		}
		$("#userlist").empty();
		$("#userlist").html(content);
		
		$('a.btnbasketusername').each(function() {
			$(this).click(function() {
				var pos = $(this).data("selectuserpos");
				$('li.libtnselectuser').removeClass("active");
				$('#libtnselectuser' + pos).addClass("active");
				
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
		var basketelement;
		var content_basketslist = "";
		var content_basketscontent = "";
		
		var addBasket = function(basketname, content) {
			content = content + '<tr>';
			content = content + '<td>' + basketname + '</td>';
			content = content + '<td>';
			content = content + '<button class="btn btn-mini" type="button"><i class="icon-align-left"></i> ' + i18n('userprofile.baskets.admin.rawview') + '</button>';
			content = content + ' <button class="btn btn-mini" type="button"><i class="icon-download"></i> ' + i18n('userprofile.baskets.admin.importbasket') + '</button>';
			content = content + ' <button class="btn btn-mini" type="button"><i class="icon-upload"></i> ' + i18n('userprofile.baskets.admin.exportbasket') + '</button>';
			content = content + ' &bull;';
			content = content + ' <button class="btn btn-mini" type="button"><i class="icon-remove-sign"></i> ' + i18n('userprofile.baskets.admin.truncate') + '</button>';
			content = content + ' <button class="btn btn-mini" type="button"><i class="icon-remove"></i> ' + i18n('userprofile.baskets.admin.remove') + '</button>';
			content = content + '</td>';
			content = content + '</tr>';
			return content;
		};

		var addBasketItem = function(basketname, itemelementkey, content) {
			var element = allusers.pathindexelements[itemelementkey];
			element = element.reference;
			if (element) {
				content = content + '<tr>';
				content = content + '<td>' + basketname + '</td>';
				content = content + '<td>';
				content = content + '<span style="font-weight: bold;">' + element.storagename + '</span>';
				content = content + ' :: ' + element.path.substring(0, element.path.lastIndexOf("/") + 1);
				content = content + '<a class="tlbdirlistitem" data-elementkey="' + itemelementkey + '" href="' + mydmam.metadatas.url.navigate + "#" + element.storagename + ":" + element.path + '">';
				content = content + element.path.substring(element.path.lastIndexOf("/") + 1);
				content = content + '</a></td>';
				content = content + '<td>';
				if (element.directory) {
					content = content + '<span class="label label-success">' + i18n('browser.directory') + '</span>';
				}
				if (element.size) {
					content = content + '<span class="label label-important">' + element.size + '</span>';
				}
				content = content + '</td>';
				content = content + '<td>' + element.directory + element.size + '</td>'; //Only for table order functions
				content = content + '<td>';
				if (element.date) {
					content = content + '<span class="label">' + mydmam.format.fulldate(element.date) + '</span>';
				}
				content = content + '</td>';
				content = content + '<td>' + element.date + '</td>';
				content = content + '<td>';
				content = content + '<button class="btn btn-mini" type="button"><i class="icon-minus-sign"></i></button>';
				content = content + '</td>';
				content = content + '</tr>';
			} else {
				content = content + '<tr>';
				content = content + '<td>' + basketname + '</td>';
				content = content + '<td>' + '<a href="#">' + itemelementkey + '</a></td>';
				content = content + '<td></td>';
				content = content + '<td></td>';
				content = content + '<td></td>';
				content = content + '<td></td>';
				content = content + '<td>';
				content = content + '<button class="btn btn-mini" type="button"><i class="icon-minus-sign"></i></button>';
				content = content + '</td>';
				content = content + '</tr>';
			}
			
			return content;
		};
		
		var prepareTableBasketsList = function(content) {
			var content_pre = "";
			content_pre = content_pre + '<table class="table table-hover table-condensed" id="tlbbasketlist">';
			content_pre = content_pre + '<thead>';
			content_pre = content_pre + '<tr>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.basketname') + '</th>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.action') + '</th>';
			content_pre = content_pre + '</tr>';
			content_pre = content_pre + '</thead>';
			content_pre = content_pre + '<tbody>';
			content = content_pre + content + '</tbody></table>';
			return content;
		};
		
		var prepareTableBasketsItems = function(content) {
			var content_pre = "";
			content_pre = content_pre + '<table class="table table-hover table-condensed" id="tlbbasketcontent">';
			content_pre = content_pre + '<thead>';
			content_pre = content_pre + '<tr>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.basketname') + '</th>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.path') + '</th>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.typesize') + '</th>';
			content_pre = content_pre + '<th></th>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.date') + '</th>';
			content_pre = content_pre + '<th></th>';
			content_pre = content_pre + '<th>' + i18n('userprofile.baskets.admin.removeitem') + '</th>';
			content_pre = content_pre + '</tr>';
			content_pre = content_pre + '</thead>';
			content_pre = content_pre + '<tbody>';
			content = content_pre + content + '</tbody></table>';
			return content;
		};
		
		for (var pos_ub in userbaskets) {
			basketname = userbaskets[pos_ub].name;
			basketcontent = userbaskets[pos_ub].content;
			content_basketslist = addBasket(basketname, content_basketslist);
			for (var pos_elm in basketcontent) {
				basketelement = basketcontent[pos_elm];
				content_basketscontent = addBasketItem(basketname, basketelement, content_basketscontent);
			}
		}
		
		$("#containertlbbasketlist").html(prepareTableBasketsList(content_basketslist));
		$("#containertlbbasketcontent").html(prepareTableBasketsItems(content_basketscontent));
		
		$("#tlbbasketcontent").dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
			"aoColumnDefs": [
				{"iDataSort": 3, "aTargets": [2], "bSearchable": false}, //SIZE displayed
				{"bVisible": false, "bSearchable": false, "aTargets": [3]}, //SIZE raw
				{"iDataSort": 5, "aTargets": [4], "bSearchable": false}, //DATE displayed
				{"bVisible": false, "bSearchable": false, "aTargets": [5]}, //DATE raw
				{"bVisible": true, "bSearchable": false, "bSortable": false, "aTargets": [6]}, //Remove
			]
		});

		$("#tlbbasketlist").dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
			"aoColumnDefs": [
				{"bVisible": true, "bSearchable": false, "bSortable": false, "aTargets": [1]}, //Actions
			]
		});
		
		$('#sitesearch').bind('keyup.DT', function(e) {
			var val = this.value==="" ? "" : this.value;
			$('.dataTables_filter input').val(val);
			$('.dataTables_filter input').trigger("keyup.DT");
		});
		
	};
})(window.mydmam.basket.allusers);
