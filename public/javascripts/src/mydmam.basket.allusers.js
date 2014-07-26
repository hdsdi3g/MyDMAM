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
			content = content + '<li id="libtnselectuser' + pos + '" class="libtnselectuser">';
			content = content + '<a href="#" data-userkey="' + allusers.userkeys[pos] + '" data-selectuserpos="' + pos + '" class="btnbasketusername">';
			content = content + allusers.userkeys[pos];
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
		
		content_basketscontent = content_basketscontent + '<table class="table table-hover table-condensed" id="tlbbasketcontent">';
		content_basketscontent = content_basketscontent + '<thead>';
		content_basketscontent = content_basketscontent + '<tr>';
		content_basketscontent = content_basketscontent + '<th>Basketname</th>';
		content_basketscontent = content_basketscontent + '<th>Path</th>';
		content_basketscontent = content_basketscontent + '<th>Type/Size</th>';
		content_basketscontent = content_basketscontent + '<th></th>';
		content_basketscontent = content_basketscontent + '<th>Date</th>';
		content_basketscontent = content_basketscontent + '<th></th>';
		content_basketscontent = content_basketscontent + '<th>Remove</th>';
		content_basketscontent = content_basketscontent + '</tr>';
		content_basketscontent = content_basketscontent + '</thead>';
		content_basketscontent = content_basketscontent + '<tbody id="userbasketscontent">';

		for (var pos_ub in userbaskets) {
			basketname = userbaskets[pos_ub].name;
			basketcontent = userbaskets[pos_ub].content;
			
			content_basketslist = content_basketslist + '<li>';
			content_basketslist = content_basketslist + basketname;
			content_basketslist = content_basketslist + '</li>';
			
			for (var pos_elm in basketcontent) {
				basketelement = basketcontent[pos_elm];
				
				content_basketscontent = content_basketscontent + '<tr>';
				
				content_basketscontent = content_basketscontent + '<td>' + basketname + '</td>';
				
				content_basketscontent = content_basketscontent + '<td>';
				content_basketscontent = content_basketscontent + '<a href="#">' + basketelement + '</a>';
				content_basketscontent = content_basketscontent + '</td>';
				
				content_basketscontent = content_basketscontent + '<td>';
				content_basketscontent = content_basketscontent + '<span class="label label-important">Directory</span>';
				content_basketscontent = content_basketscontent + '<span class="label label-important">447306</span>';
				content_basketscontent = content_basketscontent + '</td>';
				
				content_basketscontent = content_basketscontent + '<td>';
				content_basketscontent = content_basketscontent + '447306';
				content_basketscontent = content_basketscontent + '</td>';
				
				content_basketscontent = content_basketscontent + '<td>';
				content_basketscontent = content_basketscontent + '<span class="label">01/02/2014 17:22:28</span>';
				content_basketscontent = content_basketscontent + '</td>';
				content_basketscontent = content_basketscontent + '<td>';
				content_basketscontent = content_basketscontent + '12434343432';
				content_basketscontent = content_basketscontent + '</td>';

				
				content_basketscontent = content_basketscontent + '<td>';
				content_basketscontent = content_basketscontent + '<button class="btn btn-mini btn-danger" type="button">';
				content_basketscontent = content_basketscontent + '<i class="icon-star icon-white"></i>';
				content_basketscontent = content_basketscontent + '</button>';
				content_basketscontent = content_basketscontent + '</td>';
				
				content_basketscontent = content_basketscontent + '</tr>';
			}
		}
		content_basketscontent = content_basketscontent + '</tbody>';
		content_basketscontent = content_basketscontent + '</table>';
		
		$("#userbasketslist").html(content_basketslist);
		$("#containertlbbasketcontent").html(content_basketscontent);
		
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

		$('#sitesearch').bind('keyup.DT', function(e) {
			var val = this.value==="" ? "" : this.value;
			$('.dataTables_filter input').val(val);
			$('.dataTables_filter input').trigger("keyup.DT");
		});
		
	};
})(window.mydmam.basket.allusers);
