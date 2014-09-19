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
	
				/**
				 * laststatusworkers
				 * */
				var content = "<tr>";
				var content = content + "<th>" + i18n("service.table.server") + "</th>";
				var content = content + "<th>" + i18n("service.table.service") + "</th>";
				var content = content + "<th>" + i18n("service.table.version") + "</th>";
				var content = content + "<th>" + i18n("service.table.uptime") + "</th>";
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
					content = content + "</tr>";
					$("#laststatusworkers").append(content);
				}
				
				/**
				 * laststatusworkersfunctionalities
				 */
					var functionality;
					var presence_functionality = false;
					for (var pos = 0; pos < data.length; pos++) {
						if (data[pos].useraction_functionality_list.length) {
							presence_functionality = true;
							var content = "";
							content = content + '<tr><td colspan="5">';
							content = content + '<span class="text-info"><strong>' + data[pos].workername + '</strong> ' + data[pos].javaaddress.hostname;
							for (var pos_addr = 0; pos_addr < data[pos].javaaddress.address.length; pos_addr++) {
								content = content + " &bull; " + data[pos].javaaddress.address[pos_addr];
							}
							content = content + '</span></td></tr>';

							content = content + '<tr>';
							content = content + '<th colspan="2">' + i18n("service.functionalitieslist.functionality") + '</th>';
							content = content + '<th>' + i18n("service.functionalitieslist.profiles") + '</th>';
							content = content + '<th>' + i18n("service.functionalitieslist.capabilities") + '</th>';
							content = content + '<th>' + i18n("service.functionalitieslist.storageindexeswhitelist") + '</th>';
							content = content + '</tr>';

							for (var pos_th = 0; pos_th < data[pos].useraction_functionality_list.length; pos_th++) {
								functionality = data[pos].useraction_functionality_list[pos_th];
								content = content + '<tr>';
								
								content = content + '<td>' + functionality.vendor + '<br>';
								content = content + '<small>' + functionality.section + '</small><br>';
								content = content + '<small>' + functionality.reference + '</small></td>';

								//content = content + '<td>' + functionality.instance + '</td>';
								
								content = content + '<td>' + functionality.longname + '<br>';
								content = content + '<small>' + functionality.description + '</small><br>';
								content = content + '<small>' + functionality.classname + '</small></td>';

								content = content + '<td>';
								for (var pos_pf in functionality.profiles) {
									content = content + '<strong>' + functionality.profiles[pos_pf].category + '</strong> :: ';
									content = content + functionality.profiles[pos_pf].name;
									content = content + '<br>';
								}
								content = content + '</td>';
								
								content = content + '<td>';
								if (functionality.capability.fileprocessing_enabled) {
									content = content + '<span class="label label-success">File</span>';
								}
								if (functionality.capability.directoryprocessing_enabled) {
									content = content + '<span class="label label-success">Directory</span>';
								}
								if (functionality.capability.rootstorageindexprocessing_enabled) {
									content = content + '<span class="label label-success">Root storage</span>';
								}
								if (functionality.capability.musthavelocalstorageindexbridge) {
									content = content + '<br><span class="label label-important">' + i18n("service.functionalitieslist.musthavelocalstorageindexbridge") + '</span>';
								}
								content = content + '</td>';

								content = content + '<td>';
								for (var pos_wl in functionality.capability.storageindexeswhitelist) {
									content = content + functionality.capability.storageindexeswhitelist[pos_wl] + '<br>';
								}
								content = content + '</td>';

								content = content + '</tr>';
							}
							$("#laststatusworkersfunctionalities").append(content);
						}
					}

					if (presence_functionality === false) {
						$("#laststatusworkersfunctionalities_title").remove();
					}

				/**
				 * laststatusworkersthreads
				 */
				for (var pos = 0; pos < data.length; pos++) {
					if (data[pos].stacktraces.length) {
						var content = "";
						content = content + '<tr><td colspan="6">';
						content = content + '<span class="text-info"><strong>' + data[pos].workername + '</strong> ' + data[pos].javaaddress.hostname;
						for (var pos_addr = 0; pos_addr < data[pos].javaaddress.address.length; pos_addr++) {
							content = content + " &bull; " + data[pos].javaaddress.address[pos_addr];
						}
						content = content + '</span></td></tr>';
						
						for (var pos_th = 0; pos_th < data[pos].stacktraces.length; pos_th++) {
							content = content + '<tr>';
	
							content = content + "<td>" + data[pos].stacktraces[pos_th].id + "</td>";
							content = content + "<td>" + data[pos].stacktraces[pos_th].name + "</td>";
							
							content = content + '<td>';
							if (data[pos].stacktraces[pos_th].state == "TIMED_WAITING") {
								content = content + '<span class="label">';
							} else if (data[pos].stacktraces[pos_th].state == "RUNNABLE") {
								content = content + '<span class="label label-warning">';
							} else if (data[pos].stacktraces[pos_th].state == "WAITING") {
								content = content + '<span class="label label-important">';
							} else if (data[pos].stacktraces[pos_th].state == "BLOCKED") {
								content = content + '<span class="label label-important">';
							} else if (data[pos].stacktraces[pos_th].state == "TERMINATED") {
								content = content + '<span class="label label-inverse">';
							} else if (data[pos].stacktraces[pos_th].state == "NEW") {
								content = content + '<span class="label label-inverse">';
							} else {
								content = content + "<span>";
							}
							content = content + data[pos].stacktraces[pos_th].state;
							content = content + "</span></td>";
							
							content = content + '<td class="text-info"><small>';
							if (data[pos].stacktraces[pos_th].isdaemon) {
								content = content + 'Daemon';
							}
							content = content + '</small></td>';
							
							content = content + '<td><small>';
							content = content + data[pos].stacktraces[pos_th].classname;
							content = content + ' (';
							if (data[pos].stacktraces[pos_th].execpoint.startsWith("java.lang.Thread.")) {
								content = content + data[pos].stacktraces[pos_th].execpoint.substring("java.lang.Thread.".length, data[pos].stacktraces[pos_th].execpoint.length);
							} else {
								content = content + data[pos].stacktraces[pos_th].execpoint;
							}
							content = content + ')';
							content = content + '</small></td>';
							
							content = content + '</tr>';
						}
							
						content = content + "</tr>";
						$("#laststatusworkersthreads").append(content);
					} else {
						$("#laststatusworkersthreads_title").remove();
					}
				}
				
				return;
			}
		});
	
	};
	
})(window.mydmam);
