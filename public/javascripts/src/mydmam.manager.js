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

(function(mydmam) {
	mydmam.manager = {};
	mydmam.manager.url = {};
})(window.mydmam);


(function(manager) {
	manager.display = function(query_destination) {
		$.ajax({
			url: mydmam.manager.url.allinstances,
			type: "POST",
			beforeSend: function() {
				$(query_destination).html(i18n('manager.loading'));
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$(query_destination).html('<div class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + textStatus + '</strong></div>');
			},
			success: function(rawdata) {
				if (rawdata == null | rawdata == "null" | rawdata === ""| rawdata == "[]") {
					$('#laststatusworkers').append('<div id="alertnoeventsfound" class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("service.nodetectedmodule") + '</strong></div>');
					return;
				}
				
				$(query_destination).html('<pre>' + rawdata + '</pre>');
				
				var data = rawdata.sort(function(a, b){
					/**
					 * normal sort
					 */
					return a.workername < b.workername ? -1 : 1;
				});
			}
		});
	};
})(window.mydmam.manager);
