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
	mydmam.service = {};
	mydmam.service.url = {};
	
	mydmam.service.showLastStatusWorkers = function() {
		$.ajax({
			url: mydmam.service.url.laststatusworkers,
			type: "POST",
			beforeSend: function() {
				$("#laststatusworkers").empty();
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$('#laststatusworkers').append('<div id="alertnoeventsfound" class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("service.nodetectedmodule") + '</strong></div>');
			},
			success: function(rawdata) {
				if (rawdata == null | rawdata == "null" | rawdata === ""| rawdata == "[]") {
					$('#laststatusworkers').append('<div id="alertnoeventsfound" class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("service.nodetectedmodule") + '</strong></div>');
					return;
				}
	
				var data = rawdata.sort(function(a, b){
					/**
					 * normal sort
					 */
					return a.workername < b.workername ? -1 : 1;
				});
	
				var content = "<tr>";
				var content = content + "<th>" + i18n("service.table.server") + "</th>";
				var content = content + "<th>" + i18n("service.table.service") + "</th>";
				var content = content + "<th>" + i18n("service.table.version") + "</th>";
				var content = content + "<th>" + i18n("service.table.uptime") + "</th>";
				var content = content + "<th>" + i18n("service.table.threads") + "</th>";
				var content = content + "</tr>";
				
				$("#laststatusworkers").append(content);
				
				for (var pos = 0; pos < data.length; pos++) {
					var content = "<tr>";
					content = content + "<td><span class=\"label label-inverse\">" + data[pos].workername + "</span><br/>";
					content = content + "<small>" + data[pos].javaaddress.hostname + "<br/></small>";
					for (var pos_addr = 0; pos_addr < data[pos].javaaddress.address.length; pos_addr++) {
						content = content + "<small>" + data[pos].javaaddress.address[pos_addr] + "<br/></small>";
					}
					content = content + "</td>";
					
					content = content + "<td>" + data[pos].appname + "</td>";
	
					content = content + "<td>" + data[pos].appversion + "<br/>";
					content = content + "<small>JVM : " + data[pos].javaversion + "</small></td>";
					content = content + "<td>" + data[pos].javauptime + "</td>";
					
					content = content + '<td><div class="accordion" id="accordion' + pos + '">';
	
					for (var pos_th = 0; pos_th < data[pos].stacktraces.length; pos_th++) {
	
						content = content + '<div class="accordion-group">';
						content = content + '<div class="accordion-heading">';
						content = content + '<a class="accordion-toggle" data-toggle="collapse" data-parent="#accordion' + pos + '" href="#collapse' + pos + pos_th + '">';
						content = content + data[pos].stacktraces[pos_th].name;
						content = content + " [" + data[pos].stacktraces[pos_th].id + "]";
						
						if (data[pos].stacktraces[pos_th].isdaemon) {
							content = content + ' <span class="badge">daemon</span>';
						}
						
						if (data[pos].stacktraces[pos_th].state == "TIMED_WAITING") {
							content = content + ' <span class="label">';
						} else if (data[pos].stacktraces[pos_th].state == "RUNNABLE") {
							content = content + ' <span class="label label-warning">';
						} else if (data[pos].stacktraces[pos_th].state == "WAITING") {
							content = content + ' <span class="label label-important">';
						} else if (data[pos].stacktraces[pos_th].state == "BLOCKED") {
							content = content + ' <span class="label label-important">';
						} else if (data[pos].stacktraces[pos_th].state == "TERMINATED") {
							content = content + ' <span class="label label-inverse">';
						} else if (data[pos].stacktraces[pos_th].state == "NEW") {
							content = content + ' <span class="label label-inverse">';
						} else {
							content = content + " <span>";
						}
						content = content + data[pos].stacktraces[pos_th].state + "</span>";
						
						content = content + '</a>';
						content = content + '</div>';
						content = content + '<div id="collapse' + pos + pos_th + '" class="accordion-body collapse">';
						content = content + '<div class="accordion-inner">';
						
						content = content + "<dl class=\"dl-horizontal\">";
						content = content + "<dt>class</dt>";
						content = content + "<dd>" + data[pos].stacktraces[pos_th].classname + "</dd>";
						content = content + "<dt>execpoint</dt>";
						content = content + "<dd>" + data[pos].stacktraces[pos_th].execpoint + "</dd>";
						content = content + "</dl>";
	
						content = content + '</div></div></div>';
					}
						
					content = content + "</div></td>";
	
					content = content + "</tr>";
					$("#laststatusworkers").append(content);
				}
				return;
			}
		});
	
	};
	
})(window.mydmam);
