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
/**
 * Navigator functions
 */

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	mydmam.navigator = {};
	var navigator = mydmam.navigator;
	
	navigator.url = {};
})(window.mydmam);

/**
 * displayStoragePathNavigator
 * @param currentpage is default to 0 (the first page)
 */
(function(navigator) {
	navigator.displayStoragePathNavigator = function(domid, fullpath, addmetadatastosearchlistitems, currentpage) {
		var externalstorage = false;
		
		var currentstorage = fullpath.substring(0, fullpath.indexOf(":"));
		for (var pos = 0; pos < list_external_positions_storages.length; pos++) {
			if (list_external_positions_storages[pos] == currentstorage) {
				externalstorage = true;
			}
		}
		
		var stat = window.mydmam.stat;
		var md5_fullpath = md5(fullpath);
		var stat_data = stat.query([md5_fullpath], [stat.SCOPE_DIRLIST, stat.SCOPE_PATHINFO, stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS], [stat.SCOPE_MTD_SUMMARY, stat.SCOPE_COUNT_ITEMS], currentpage);
		var no_result = function() {
			$("#" + domid).empty();
			window.location.hash = '#';
		};
		
		if (!stat_data) {
			no_result();
			return;
		} else if (stat_data[md5_fullpath] == null) {
			no_result();
			return;
		}
		stat_data = stat_data[md5_fullpath];
		
		var external_elements_to_resolve = [];
		
		var items = false;
		if (stat_data.items) {
			items = stat_data.items;
		}
		var reference = stat_data.reference;
		var parentmtdsummary = stat_data.mtdsummary;
		var items_total = stat_data.items_total;
		var items_page_from = stat_data.items_page_from;
		var items_page_size = stat_data.items_page_size;
		
		var show_useraction_button = mydmam.useraction.isEnabled();

		var content = '<div class="page-header">';
		content = content + '<h3>';
		if (reference.storagename) {
			show_useraction_button = mydmam.useraction.isStorageAsFunctionalities(reference.storagename);
			 
			var url_goback = mydmam.metadatas.url.navigate + "#" + reference.storagename + ":" + reference.path.substring(0, reference.path.lastIndexOf("/"));
			if (reference.path == '/') {
				url_goback = mydmam.metadatas.url.navigate + "#";
			} else if (reference.path.lastIndexOf("/") === 0) {
				url_goback = mydmam.metadatas.url.navigate + "#" + reference.storagename + ":/";
			}
			content = content + '<a class="btn btn-mini btngoback" style="margin-bottom: 6px;margin-right: 1em;" href="' + url_goback + '" title="' + i18n('browser.goback') + '"><i class="icon-chevron-left"></i></a>';

			if (reference.path != "/") {
				content = content + reference.path.substring(reference.path.lastIndexOf("/") + 1) + " ";
				if (parentmtdsummary) {
					content = content + '<small>';
					content = content + mydmam.metadatas.typeofelement(parentmtdsummary);
					content = content + '</small>';
				} else {
					if (reference.directory) {
						content = content + '<small>' + i18n("browser.directory") + '</small>';
					} else {
						content = content + '<small>' + i18n("browser.file") + '</small>';
					}
				}
			} else {
				content = content + reference.storagename + ' <small>' + i18n("browser.storage") + '</small>';
			}
		} else {
			content = content + i18n("browser.storagestitle") + ' <small>' + i18n("browser.storagebaseline") + '</small>';
		}
		content = content + '</h3>';

		if (reference.storagename) {
			content = content + mydmam.basket.prepareNavigatorSwitchButton(md5_fullpath);
			if (show_useraction_button) {
				content = content + mydmam.useraction.prepareButtonCreate(md5_fullpath, reference.directory, reference.storagename, reference.path) + " ";
			}
		}
		
		if (reference.date) {
			var data_date = mydmam.format.fulldate(reference.date);
			if (data_date !== "") {
				content = content + '<span class="label">' + i18n("browser.file.modifiedat") + ' ' + data_date + '</span> ';
			}
		}
		if (reference.dateindex) {
			var data_date = mydmam.format.fulldate(reference.dateindex);
			if (data_date !== "") {
				content = content + '<span class="label">' + i18n("browser.file.indexedat") + ' ' + data_date + '</span> ';
			}
		} else {
			if (items) {
				// Fake (get from the first item), but realist indexdate.
				for (var item in items) {
					var newitem = items[item];
					if (newitem.reference) {
						if (newitem.reference.dateindex) {
							var data_date = mydmam.format.fulldate(newitem.reference.dateindex);
							if (data_date !== "") {
								content = content + '<span class="label">' + i18n("browser.file.indexednearat") + ' ' + data_date + '</span> ';
							}
						}
					}
					break;
				}
			}
		}
		
		if (reference.size) {
			content = content + '<span class="label label-important">' + reference.size + '</span> ';
		}
		
		content = content + '</div>';
		
		if (parentmtdsummary) {
			content = content + '<div>';
			content = content + mydmam.metadatas.display(reference, parentmtdsummary, mydmam.metadatas.displaymethod.NAVIGATE_SHOW_ELEMENT);
			content = content + '</div>';
		}
		
		if (items) {
				
			var dircontent = []; 
			for (var item in items) {
				var newitem = items[item];
				newitem.key = item;
				dircontent.push(newitem);
			}

			dircontent = dircontent.sort(function(a, b) {
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
			if (reference.storagename) {
				content = content + '<tr><td>&nbsp;</td><td></td><td></td><td></td><td></td><td></td><td></td></tr>';
			} else {
				content = content + '<tr><td>' + i18n("browser.storagelist") + '</td> <td></td> <td></td> <td></td> <td></td> <td></td><td></td></tr>';
			}
			content = content + '</thead>';
			
			content = content + '<tbody>';
			for (var pos = 0; pos < dircontent.length; pos++) {
				var elementkey = dircontent[pos].key;
				var element = dircontent[pos].reference;
				var element_items_total = dircontent[pos].items_total;
				var element_mtdsummary = dircontent[pos].mtdsummary;

				if (element.directory === false) {
					external_elements_to_resolve.push(elementkey);
				}
				
				content = content + '<tr>';
				
				if (element.directory) {
					content = content + '<th>';
					content = content + mydmam.basket.prepareNavigatorSwitchButton(elementkey);
					
					if (reference.storagename) {
						content = content + '<a class="tlbdirlistitem" href="' + mydmam.metadatas.url.navigate + "#" + element.storagename + ":" + element.path + '">';
						content = content + element.path.substring(element.path.lastIndexOf("/") + 1);
						content = content + '</a>';
					} else {
						content = content + '<a class="tlbdirlistitem" href="' + mydmam.metadatas.url.navigate + "#" + element.storagename + ':/">';
						content = content + element.storagename ;
						content = content + '</a>';
					}

					if (element_items_total === 0) {
						content = content + ' <span class="badge badge-success">' + i18n('browser.emptydir') + '</span>';
					}

					content = content + '</th>';
				} else {
					content = content + '<td>';
					content = content + mydmam.basket.prepareNavigatorSwitchButton(elementkey);
					
					if (element.id) {
						content = content + '<span class="label label-info">' + element.id + '</span> ';
					}
					content = content + '<a class="tlbdirlistitem" href="' + mydmam.metadatas.url.navigate + "#" + element.storagename + ":" + element.path + '">';
					content = content + element.path.substring(element.path.lastIndexOf("/") + 1);
					content = content + '</a>';
					content = content + '</td>';
				}
				
				if (element.directory) {
						content = content + '<td>';
						if (show_useraction_button) {
							content = content + mydmam.useraction.prepareButtonCreate(elementkey, true, element.storagename, element.path);
						}
						content = content + ' <span class="label label-success">';
					if (reference.storagename != null) {
						content = content + i18n('browser.directorytitle');
					} else {
						content = content + i18n('browser.storagetitle');
					}
					if (element_items_total != null) {
						if (element_items_total === 0) {
							content = content + ' ' + i18n('browser.emptydir');
						} else if (element_items_total == 1) {
							content = content + ' - ' + i18n('browser.oneelement');
						} else {
							content = content + ' - ' + i18n('browser.Nelements', element_items_total);
						}
						content = content + '</span>';
						content = content + '</td><td>-' + element_items_total + '</td>';
					} else {
						content = content + '</span></td><td>0</td>';
					}
				} else {
					content = content + '<td>';
					if (show_useraction_button) {
						content = content + mydmam.useraction.prepareButtonCreate(elementkey, false, element.storagename, element.path);
					}
					content = content + ' <span class="label label-important">' + element.size + '</span>';
					content = content + '</td>';
					
					var rawsize = '000000000000000' + element.size;
					content = content + '<td>' + rawsize.substring(rawsize.length - 15, rawsize.length) + '</td>';
				}
				
				if (reference.storagename != null) {
					content = content + '<td><span class="label">' + mydmam.format.fulldate(element.date) + '</span></td>';
					content = content + '<td>' + element.date + '</td>';
				} else {
					content = content + '<td></td><td>0</td>';
				}

				if (element.directory === false) {
					external_elements_to_resolve.push();
				}
				
				content = content + '<td id="elmextern-' + elementkey + '"></td>';
				
				if (element_mtdsummary) {
					content = content + '<td>' + mydmam.metadatas.displaySummary(element_mtdsummary) + '</td>';
				} else {
					content = content + '<td></td>';
				}

				content = content + '</tr>';
			}
			
			content = content + '</tbody>';
			content = content + '</table>';

			var href = function(currentpage, pagecount) {
				return '#' + fullpath;
			};
			content = content +  mydmam.pagination.create(items_page_from, Math.ceil(items_total/items_page_size), href, "navigator-" + md5_fullpath);
			
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
			
			mydmam.pagination.addevents(function(currentpage) {
				return function() {
					mydmam.navigator.displayStoragePathNavigator("storageelem", fullpath, true, currentpage);
				};
			}, "navigator-" + md5_fullpath);
			
		} else {
			$("#" + domid).empty();
			$("#" + domid).append(content);
		}
		mydmam.metadatas.loadAfterDisplay();

		mydmam.useraction.populateButtonsCreate();
		
		var click_navigate = function() {
			mydmam.navigator.displayStoragePathNavigator("storageelem", $(this).context.hash.substring(1), true);
		};

		$("#" + domid + " .tlbdirlistitem").click(click_navigate);
		$("#" + domid + " .btngoback").click(click_navigate);
		
		if (reference.storagename) {
			window.location.hash = reference.storagename + ':' + reference.path;
		} else {
			window.location.hash = '#';
		}
		
		if (externalstorage & (external_elements_to_resolve.length > 0)) {
			mydmam.metadatas.getAndAddExternalPosition(external_elements_to_resolve, function(key) {
				$('#elmextern-' + key).append('<span class="label label-success">' + i18n('browser.externalposition.online') + '</span> ');
			}, function(key, barcode) {
				$('#elmextern-' + key).append(barcode + ' ');
			}, function(key) {
				$('#elmextern-' + key).append('<span class="label label-success">' + i18n('browser.externalposition.nearline') + '</span> ');
			});
		}

		mydmam.navigator.createBreadcrumb(reference.storagename, reference.path);

		if (addmetadatastosearchlistitems) {
			mydmam.metadatas.addMetadatasToSearchListItems();
		}
		
		mydmam.basket.setSwitchButtonsEvents();
	};
})(window.mydmam.navigator);

/**
 * createBreadcrumb
 */
(function(navigator) {
	navigator.createBreadcrumb = function(storagename, path) {
		var domid = "storageelem";
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
				content = content + '<li><span class="divider">/</span><a href="' + mydmam.metadatas.url.navigate + "#" + newpath + '">' + element_subpaths[pos] + '</a></li>';
			} else {
				content = content + '<li class="active"><span class="divider">/</span>' + element_subpaths[pos] + '</li>';
			}
			currentpath = currentpath + "/" + element_subpaths[pos];
		}
		if (content !== "") {
			var header =      '<li><a href="' + mydmam.metadatas.url.navigate + '#">' + i18n('browser.storagestitle') + '</a> <span class="divider">::</span></li>';
			if (path != "/") {
				header = header + '<li><a href="' + mydmam.metadatas.url.navigate + "#" + storagename + ':/">' + storagename + '</a></li>';
			} else {
				header = header + '<li class="active">' + storagename + '</li>';
			}
			content = '<ul class="breadcrumb">' + header + content + '</ul>';
			$("#" + domid).prepend(content);
			$("#" + domid + " .breadcrumb a").click(function() {
				mydmam.navigator.displayStoragePathNavigator("storageelem", $(this).context.hash.substring(1), true);
			});
		}
	};
})(window.mydmam.navigator);
