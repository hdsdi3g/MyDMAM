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


function addMetadatasToSearchListItems() {
	var external_elements_to_resolve = [];
	var elements_to_get_metadatas = [];
	
	$(".searchresultitem").each(function(index) {

		/**
		 * Transform text path to navigate links on search results
		 */
		var element_storage = $(this).find(".storagename").text();
		var element_path = $(this).find(".path").text();
		$(this).find(".storagename").html('<a href=\"' + url_navigate + "#" + element_storage + ':/\">' + element_storage + '</a>');
		
		var element_subpaths = element_path.split("/");
		var element_path_new = "";
		var currentpath = "";
		var newpath = "";
		for (var pos = 1; pos < element_subpaths.length; pos++) {
			newpath = element_storage + ':' + currentpath + "/" + element_subpaths[pos];
			element_path_new = element_path_new + "/";
			element_path_new = element_path_new + '<a href="' + url_navigate + "#" + newpath + '">';
			element_path_new = element_path_new + element_subpaths[pos];
			element_path_new = element_path_new + "</a>";
			currentpath = currentpath + "/" + element_subpaths[pos];
		}
		$(this).find(".path").html(element_path_new);
		
		/**
		 * Search items for a search archive position
		 */
		for (var pos = 0; pos < list_external_positions_storages.length; pos++) {
			if (list_external_positions_storages[pos] == element_storage) {
				external_elements_to_resolve.push($(this).data('storagekey'));
			}
		}

		elements_to_get_metadatas.push($(this).data('storagekey'));
	});
	
		/**
		 * Add archive position to items
		 */
	if (external_elements_to_resolve.length > 0) {
		getAndAddExternalPosition(external_elements_to_resolve, function(key) {
			$('#sri-' + key).prepend('<span class="label label-success">' + i18n('browser.externalposition.online') + '</span> ');
		}, function(key, barcode) {
			$('#sri-' + key).prepend('<span class="label label-important">' + barcode + '</span> ');
		}, function(key) {
			$('#sri-' + key).prepend('<span class="label label-success">' + i18n('browser.externalposition.nearline') + '</span> ');
		});
	}
	
	if (elements_to_get_metadatas.length > 0) {
		$.ajax({
			url: url_simplemetadatas,
			type: "POST",
			data: {"fileshash" : elements_to_get_metadatas},
			success: function(data) {
				if (data.length === 0) {
					return;
				}
				var mtdengine = new MetadataEngine();
				
				for (var pos_key = 0; pos_key < elements_to_get_metadatas.length; pos_key++) {
					var key = elements_to_get_metadatas[pos_key];
					var metadatas = data[key];
					if (metadatas == null) {
						continue;
					}
					if (metadatas.summary == null) {
						continue;
					}
					
					var count = 0;
					var title = "";
					for (var metadata in metadatas.summary) {
						if (metadata == "mimetype") {
							continue;
						}
						if (metadata == "previews") {
							continue;
						}
						count++;
						if (title !== "") {
							title = title + " - ";
						}
						title = title + metadatas.summary[metadata];
					}
					if (count > 0) {
						$('#mtd-' + key).html('<small>' + mtdengine.typeofelement(metadatas.summary) + ' :: ' + title.trim() + '</small> ');
					} else {
						$('#mtd-' + key).html('<small>' + mtdengine.typeofelement(metadatas.summary) + '</small> ');
					}
				}
			}
		});
	}
}

function getAndAddExternalPosition(external_elements_to_resolve, callback_online, callback_offline, callback_nearline) {
	$.ajax({
		url: url_resolvepositions,
		type: "POST",
		data: {"keys" : external_elements_to_resolve},
		success: function(data) {
			
			var key;
			for (var pos_key = 0; pos_key < external_elements_to_resolve.length; pos_key++) {
				key = external_elements_to_resolve[pos_key];
				if (data.positions[key]) {
					var verbose = true;
					for (var pos = 0; pos < data.positions[key].length; pos++) {
						var tapename = data.positions[key][pos];
						if (tapename == "cache") {
							callback_online(key);
							verbose = false;
							break;
						}
					}
					if (verbose) {
						for (var pos = 0; pos < data.positions[key].length; pos++) {
							var tapename = data.positions[key][pos];
							var location = data.locations[tapename];
							if (location.isexternal) {
								callback_offline(key, location.barcode);
							} else {
								callback_nearline(key);
							}
						}
					}
				}
			}
		}
	});
}

function displayStoragePathNavigator(domid, fullpath, callback) {
	
	var externalstorage = false;
	
	var currentstorage = fullpath.substring(0, fullpath.indexOf(":"));
	for (var pos = 0; pos < list_external_positions_storages.length; pos++) {
		if (list_external_positions_storages[pos] == currentstorage) {
			externalstorage = true;
		}
	}
	
	$.ajax({
		url: url_stat,
		type: "GET",
		data: {"filehash" : md5(fullpath)},
		beforeSend: function() {
			//$("#waithourglass").show();
		},
		success: function(data) {
			//$("#waithourglass").hide();
			
			var external_elements_to_resolve = [];
			
			var content = '<div class="page-header">';
			var mtdengine = new MetadataEngine(); 
			
			content = content + '<h3>';
			if (data.storagename) {
				var url_goback = url_navigate + "#" + data.storagename + ":" + data.path.substring(0, data.path.lastIndexOf("/"));
				if (data.path == '/') {
					url_goback = url_navigate + "#";
				} else if (data.path.lastIndexOf("/") === 0) {
					url_goback = url_navigate + "#" + data.storagename + ":/";
				}
				content = content + '<a class="btn btn-mini btngoback" style="margin-bottom: 6px;margin-right: 1em;" href="' + url_goback + '" title="' + i18n('browser.goback') + '"><i class="icon-chevron-left"></i></a>';

				if (data.path != "/") {
					content = content + data.path.substring(data.path.lastIndexOf("/") + 1) + " ";
					if (data.metadatas) {
						content = content + '<small>';
						content = content + mtdengine.typeofelement(data.metadatas);
						content = content + '</small>';
					} else {
						if (data.directory) {
							content = content + '<small>' + i18n("browser.directory") + '</small>';
						} else {
							content = content + '<small>' + i18n("browser.file") + '</small>';
						}
					}
				} else {
					content = content + data.storagename + ' <small>' + i18n("browser.storage") + '</small>';
				}
			} else {
				content = content + i18n("browser.storagestitle") + ' <small>' + i18n("browser.storagebaseline") + '</small>';
			}
			content = content + '</h3>';

			if (data.date) {
				var data_date = mydmam.format.fulldate(data.date);
				if (data_date !== "") {
					content = content + '<span class="label">' + i18n("browser.file.modifiedat") + ' ' + data_date + '</span> ';
				}
			}
			if (data.dateindex) {
				var data_date = mydmam.format.fulldate(data.dateindex);
				if (data_date !== "") {
					content = content + '<span class="label">' + i18n("browser.file.indexedat") + ' ' + data_date + '</span> ';
				}
			} else {
				if (data.items) {
					if (data.items.length > 0) {
						/**
						 * Fake (get from the first item), but realist indexdate.
						 */
						if (data.items[0].dateindex) {
							var data_date = mydmam.format.fulldate(data.items[0].dateindex);
							if (data_date !== "") {
								content = content + '<span class="label">' + i18n("browser.file.indexednearat") + ' ' + data_date + '</span> ';
							}
						}
					}
				}
			}
			
			if (data.size) {
				content = content + '<span class="label label-important">' + data.size + '</span> ';
			}
			
			content = content + '</div>';
			
			if (data.metadatas) {
				content = content + '<div>';
				content = content + mtdengine.display(data, mtdengine.NAVIGATE_SHOW_ELEMENT);
				content = content + '</div>';
			}
			
			if (data.items) {
				var dircontent = data.items.sort(function(a, b) {
					if (a.directory & (b.directory === false)) {
						return -1;
					}
					if (b.directory & (a.directory === false)) {
						return 1;
					}
					return a.idxfilename < b.idxfilename ? -1 : 1;
				});

				content = content + '<table class="navdatatable table table-hover table-condensed">';
				
				content = content + '<thead>';
				if (data.storagename) {
					content = content + '<tr><td>&nbsp;</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>';
				} else {
					content = content + '<tr><td>' + i18n("browser.storagelist") + '</td> <td></td> <td></td> <td></td> <td></td> <td></td><td></td></tr>';
				}
				content = content + '</thead>';

				content = content + '<tbody>';
				for (var pos = 0; pos < dircontent.length; pos++) {
					
					var elementkey = md5(data.storagename + ":" + dircontent[pos].path);
					
					if (dircontent[pos].directory === false) {
						external_elements_to_resolve.push(elementkey);
					}
					
					content = content + '<tr>';
					
					if (dircontent[pos].directory) {
						content = content + '<th>';
						if (data.storagename) {
							content = content + '<a class="tlbdirlistitem" href="' + url_navigate + "#" + dircontent[pos].storagename + ":" + dircontent[pos].path + '">';
							content = content + dircontent[pos].path.substring(dircontent[pos].path.lastIndexOf("/") + 1);
							content = content + '</a>';
						} else {
							content = content + '<a class="tlbdirlistitem" href="' + url_navigate + "#" + dircontent[pos].storagename + ':/">';
							content = content + dircontent[pos].storagename ;
							content = content + '</a>';
						}

						if (dircontent[pos].count === 0) {
							content = content + ' <span class="badge badge-success">' + i18n('browser.emptydir') + '</span>';
						}

						content = content + '</th>';
					} else {
						content = content + '<td>';
						if (dircontent[pos].id) {
							content = content + '<span class="label label-info">' + dircontent[pos].id + '</span> ';
						}
						content = content + '<a class="tlbdirlistitem" href="' + url_navigate + "#" + dircontent[pos].storagename + ":" + dircontent[pos].path + '">';
						content = content + dircontent[pos].path.substring(dircontent[pos].path.lastIndexOf("/") + 1);
						content = content + '</a>';
						content = content + '</td>';
					}
					
					if (dircontent[pos].directory) {
							content = content + '<td><span class="label label-success">';
						if (data.storagename != null) {
							content = content + i18n('browser.directorytitle');
						} else {
							content = content + i18n('browser.storagetitle');
						}
						if (dircontent[pos].count != null) {
							if (dircontent[pos].count === 0) {
								content = content + ' ' + i18n('browser.emptydir');
							} else if (dircontent[pos].count == 1) {
								content = content + ' - ' + i18n('browser.oneelement');
							} else {
								content = content + ' - ' + i18n('browser.Nelements', dircontent[pos].count);
							}
							content = content + '</span></td><td>-' + dircontent[pos].count + '</td>';
						} else {
							content = content + '</span></td><td>0</td>';
						}
					} else {
						content = content + '<td><span class="label label-important">' + dircontent[pos].size + '</span></td>';
						var rawsize = '000000000000000' + dircontent[pos].size;
						content = content + '<td>' + rawsize.substring(rawsize.length - 15, rawsize.length) + '</td>';
					}
					
					if (data.storagename != null) {
						content = content + '<td><span class="label">' + mydmam.format.fulldate(dircontent[pos].date) + '</span></td>';
						content = content + '<td>' + dircontent[pos].date + '</td>';
					} else {
						content = content + '<td></td><td>0</td>';
					}

					if (dircontent[pos].directory === false) {
						external_elements_to_resolve.push();
					}
					
					content = content + '<td id="elmextern-' + elementkey + '"></td>';
					
					if (dircontent[pos].metadatas) {
						content = content + '<td>' + mtdengine.displaySummary(dircontent[pos].metadatas) + '</td>';
					} else {
						content = content + '<td></td>';
					}

					content = content + '</tr>';
				}

				
				content = content + '</tbody>';
				content = content + '</table>';

				$("#" + domid).empty();
				$("#" + domid).append(content);
				
				$('.navdatatable').dataTable({
					"bPaginate": false,
					"bLengthChange": false,
					"bSort": true,
					"bInfo": false,
					"bAutoWidth": false,
					"bFilter": true,
					"aoColumnDefs": [
						{"iDataSort": 2, "aTargets": [1], "bSearchable": false}, //SIZE displayed
						{"bVisible": false, "bSearchable": false, "aTargets": [2]}, //SIZE raw
						{"iDataSort": 4, "aTargets": [3], "bSearchable": false}, //DATE displayed
						{"bVisible": false, "bSearchable": false, "aTargets": [4]} //DATE raw
                    ]
				});

				$('#sitesearch').bind('keyup.DT', function(e) {
					var val = this.value==="" ? "" : this.value;
					$('.dataTables_filter input').val(val);
					$('.dataTables_filter input').trigger("keyup.DT");
				});

				// http://www.jstree.com/demo
				//$("#" + domid).append(fullpath + '<br>' + md5(fullpath));
			} else {
				$("#" + domid).empty();
				$("#" + domid).append(content);
				if (data.toomanyitems) {
					$("#" + domid).append(i18n('browser.toomanyitemsindir', data.toomanyitems - 1));
				}
			}
			
			mtdengine.loadAfterDisplay();
			
			var click_navigate = function() {
				displayStoragePathNavigator("storageelem", $(this).context.hash.substring(1), function(storagename, path) {
					addMetadatasToSearchListItems();
					createBreadcrumb("storageelem", storagename, path);
				});
			};

			$("#" + domid + " .tlbdirlistitem").click(click_navigate);
			$("#" + domid + " .btngoback").click(click_navigate);
			
			if (data.storagename) {
				window.location.hash = data.storagename + ':' + data.path;
			} else {
				window.location.hash = '#';
			}
			
			if (externalstorage & (external_elements_to_resolve.length > 0)) {
				getAndAddExternalPosition(external_elements_to_resolve, function(key) {
					$('#elmextern-' + key).append('<span class="label label-success">' + i18n('browser.externalposition.online') + '</span> ');
				}, function(key, barcode) {
					$('#elmextern-' + key).append(barcode + ' ');
				}, function(key) {
					$('#elmextern-' + key).append('<span class="label label-success">' + i18n('browser.externalposition.nearline') + '</span> ');
				});
			}

			if (callback) {
				callback(data.storagename, data.path);
			}
		},
		error: function(jqXHR, textStatus, errorThrown) {
			$("#" + domid).empty();
			//$("#waithourglass").hide();
			console.error(errorThrown);
			window.location.hash = '#';
			/*if (callback) {
				callback();
			}*/
		}
	});
	
}

function createBreadcrumb(domid, storagename, path) {
	if (storagename == null) {
		$("#" + domid).prepend('<ul class="breadcrumb"><li class="active">' + i18n('browser.storagestitle') + '</li></ul>');
		return;
	}
	var element_subpaths = path.split("/");
	var content = "";
	var currentpath = "";
	var newpath = "";
	for (var pos = 1; pos < element_subpaths.length; pos++) {
		newpath = storagename + ':' + currentpath + "/" + element_subpaths[pos];
		if (pos + 1 < element_subpaths.length) {
			content = content + '<li><span class="divider">/</span><a href="' + url_navigate + "#" + newpath + '">' + element_subpaths[pos] + '</a></li>';
		} else {
			content = content + '<li class="active"><span class="divider">/</span>' + element_subpaths[pos] + '</li>';
		}
		currentpath = currentpath + "/" + element_subpaths[pos];
	}
	if (content !== "") {
		var header =      '<li><a href="' + url_navigate + '#">' + i18n('browser.storagestitle') + '</a> <span class="divider">::</span></li>';
		if (path != "/") {
			header = header + '<li><a href="' + url_navigate + "#" + storagename + ':/">' + storagename + '</a></li>';
		} else {
			header = header + '<li class="active">' + storagename + '</li>';
		}
		content = '<ul class="breadcrumb">' + header + content + '</ul>';
		$("#" + domid).prepend(content);
		$("#" + domid + " .breadcrumb a").click(function() {
			displayStoragePathNavigator("storageelem", $(this).context.hash.substring(1), function(storagename, path) {
				addMetadatasToSearchListItems();
				createBreadcrumb("storageelem", storagename, path);
			});
		});
	}
}

