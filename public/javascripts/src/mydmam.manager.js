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
 * PUBLIC USE
 * display(query_destination)
 */
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
				if (rawdata == null | rawdata.length === 0) {
					$('#laststatusworkers').append('<div class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("manager.nodetectedmodule") + '</strong></div>');
					return;
				}
				manager.showFullDisplay(rawdata, query_destination);
			}
		});
	};
})(window.mydmam.manager);

/**
 * hasInstanceAction()
 */
(function(manager) {
	manager.hasInstanceAction = function() {
		if (mydmam.manager.url.instanceaction) {
			return true;
		}
		return false;
	};
})(window.mydmam.manager);

/**
 * getGitHubURL(app_version)
 * getGitHubURL(app_version, path)
 */
(function(manager) {
	manager.getGitHubURL = function(app_version, path) {
		if (path == null) {
			return 'https://github.com/hdsdi3g/MyDMAM/tree/' + app_version.substring(app_version.lastIndexOf(" ") + 1, app_version.length);
		} else {
			return 'https://github.com/hdsdi3g/MyDMAM/tree/' + app_version.substring(app_version.lastIndexOf(" ") + 1, app_version.length) + '/app/' + path;
		}
	};
})(window.mydmam.manager);

/**
 * showFullDisplay(rawdata, query_destination)
 */
(function(manager) {
	manager.showFullDisplay = function(rawdata, query_destination) {
		var content = '';
		if (window.location.hash === '') {
			window.location.hash = "#mgrsummary";
		}
		
		var getActiveClassIfThisTabIsUserSelected = function(target) {
			if (target === window.location.hash) {
				return "active";
			} else {
				return "";
			}
		};
		
		content = content + '<ul class="nav nav-tabs">';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrsummary") + '"><a href="#mgrsummary" class="btnmanager">' + i18n('manager.summary.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrworkers") + '"><a href="#mgrworkers" class="btnmanager">' + i18n('manager.workers.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgruafunctlist") + '"><a href="#mgruafunctlist" class="btnmanager">' + i18n('manager.uafunctlist.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrcyclic") + '"><a href="#mgrcyclic" class="btnmanager">' + i18n('manager.cyclic.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrtriggers") + '"><a href="#mgrtriggers" class="btnmanager">' + i18n('manager.trigger.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrthreads") + '"><a href="#mgrthreads" class="btnmanager">' + i18n('manager.threads.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrclasspaths") + '"><a href="#mgrclasspaths" class="btnmanager">' + i18n('manager.classpaths.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrlog2filters") + '"><a href="#mgrlog2filters" class="btnmanager">' + i18n('manager.log2filters.title') + '</a></li>';

		content = content + '<li class="pull-right"><a href="#mgrsummary" class="btnrefresh"><i class="icon-refresh"></i>' + '</a></li>';
		content = content + '</ul>';
		 
		content = content + '<div class="tab-content">';
		
		/**
		 * Show summary
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrsummary") + '" id="mgrsummary">';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatable">';
		content = content + '<thead>';
		content = content + manager.prepareSummary(null);
		content = content + '</thead>';
		content = content + '<tbody>';
		for (var pos = 0; pos < rawdata.length; pos++) {
			content = content + manager.prepareSummary(rawdata[pos], pos);
		}
		content = content + '</tbody>';
		content = content + '</table>';
		content = content + '</div>'; //tab-pane

		/**
		 * Show threads
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrthreads") + '" id="mgrthreads">';
		content = content + manager.prepareThreadsStackTrace(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show classpaths
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrclasspaths") + '" id="mgrclasspaths">';
		content = content + manager.prepareClasspaths(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Cyclic
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrcyclic") + '" id="mgrcyclic">';
		content = content + manager.jobcreator.prepareCyclic(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Triggers
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrtriggers") + '" id="mgrtriggers">';
		content = content + manager.jobcreator.prepareTriggers(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Prepare block for Useraction Functionality List
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgruafunctlist") + '" id="mgruafunctlist">';
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Log2filters
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrlog2filters") + '" id="mgrlog2filters">';
		content = content + manager.prepareLog2filters(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Workers status
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrworkers") + '" id="mgrworkers">';
		content = content + '</div>'; //tab-pane
		
		$(query_destination).html(content);
		
		manager.workerstatus.refresh("#mgrworkers");
		
		var all_datatables = $(query_destination + ' table.setdatatable').dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
		});
		$(query_destination + ' table.setdatatable').removeClass('setdatatable');
		
		/**
		 * set actions 
		 */
		$(query_destination + ' ul.nav.nav-tabs a.btnmanager').click(function() {
			window.location.hash = $(this).attr("href");
			$(this).tab('show');
			return false;
		});
		
		$(query_destination + ' ul.nav.nav-tabs a.btnrefresh').click(function() {
			manager.display(query_destination);
			return false;
		});
		
		$(query_destination + ' button.btn.btn-mini.btnincollapse').click(function() {
			var collapsetarget = $(this).data("collapsetarget");
			$(query_destination + ' div.collapse.' + collapsetarget).addClass('in');
			$(this).remove();
			return true;
		});
		
		if (manager.hasInstanceAction()) {
			manager.setBtnActionClick(query_destination);
		}
		
		manager.setBtnForLog2filters(query_destination);
		
		manager.drawUseractionFunctionalityList("#mgruafunctlist", rawdata, function() {
			var all_datatables = $(query_destination + ' table.setdatatable').dataTable({
				"bPaginate": false,
				"bLengthChange": false,
				"bSort": true,
				"bInfo": false,
				"bAutoWidth": false,
				"bFilter": true,
			});
			$(query_destination + ' table.setdatatable').removeClass('setdatatable');
			
			if (manager.hasInstanceAction()) {
				manager.setBtnActionClick(query_destination);
			}
		});

	};
})(window.mydmam.manager);

/**
 * doAction(target_class_name, target_reference_key, order_key, order_value, beforesend, error, success)
 * setBtnActionClick(query_destination)
 */
(function(manager) {
	manager.doAction = function(target_class_name, target_reference_key, order_key, order_value, beforesend, error, success) {
		var order = {};
		order[order_key] = order_value;
		
		$.ajax({
			url: manager.url.instanceaction,
			type: "POST",
			data: {
				target_class_name: target_class_name,
				target_reference_key: target_reference_key,
				json_order: JSON.stringify(order),
			},
			beforeSend: beforesend,
			error: error,
			success: success,
		});
	};
	
	var push = function() {
		var jquery_btn = $(this);
		
		if (!manager.url.instanceaction) {
			jquery_btn.remove();
			return false;
		}
		
		if (jquery_btn.hasClass('disabled')) {
			return false;
		}
		
		jquery_btn.removeClass("btnmgraction");

		manager.doAction(jquery_btn.data("target_class_name"),
				jquery_btn.data("target_reference_key"),
				jquery_btn.data("order_key"),
				jquery_btn.data("order_value"),
				function() {
					jquery_btn.addClass("disabled");
				},
				function() {
					jquery_btn.removeClass("disabled");
					alert(i18n('manager.action.error'));
				},
				function(rawdata) {
					jquery_btn.removeClass("disabled");
					if (rawdata.length !== 0) {
						console.err(rawdata);
					}
				});
	};
	
	manager.setBtnActionClick = function(query_destination) {
		$(query_destination + ' button.btn.btnmgraction').click(push);
	};
})(window.mydmam.manager);


/**
 * prepareSummary(instance, pos))
 */
(function(manager) {
	manager.prepareSummary = function(instance, pos) {
		var content = '<tr>';
		if (instance == null) {
			content = content + '<th>' + i18n('manager.summary.instance_name') + '</th>';
			content = content + '<th>' + i18n('manager.summary.app_name') + '</th>';
			content = content + '<th>' + i18n('manager.summary.uptime') + '</th>';
			content = content + '<th>' + i18n('manager.summary.java_version') + '</th>';
			content = content + '<th>' + i18n('manager.summary.app_version') + '</th>';
			content = content + '<th>' + i18n('manager.summary.host') + '</th>';
			content = content + '<th>' + i18n('manager.summary.addresses') + '</th>';
			if (manager.hasInstanceAction()) {
				content = content + '<th>' + i18n('manager.summary.actions') + '</th>';
			}
			return content + '</tr>';
		}
		
		content = content + '<td>' + instance.instance_name + '</td>';
		content = content + '<td><strong>' + instance.app_name + '</strong><br><small>' + instance.instance_name_pid + '</small></td>';
	
		content = content + '<td>';
		content = content + instance.uptime_from;
		if (instance.next_updater_refresh_date > 0) {
			content = content + '<br><small>' + mydmam.format.timeAgo(instance.next_updater_refresh_date, 'manager.summary.next_updater_refresh_date.from', 'manager.summary.next_updater_refresh_date.to') + '</small>';
		}
		content = content + '</td>';
		
		content = content + '<td>';
		if (instance.java_version.startsWith('1.7.0')) {
			content = content + '<a href="http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase7-521261.html#jre-7u';
			content = content +  instance.java_version.substring(instance.java_version.lastIndexOf("_") + 1, instance.java_version.length) + '-oth-JPR">' + instance.java_version + '</a>';
		} else if (instance.java_version.startsWith('1.8.0')) {
			content = content + '<a href="http://www.oracle.com/technetwork/java/javase/downloads/java-archive-javase8-2177648.html#jre-8u';
			content = content +  instance.java_version.substring(instance.java_version.lastIndexOf("_") + 1, instance.java_version.length) + '-oth-JPR">' + instance.java_version + '</a>';
		} else {
			content = content + instance.java_version;
		}
		content = content + '</td>';
		
		content = content + '<td><a href="' + manager.getGitHubURL(instance.app_version) + '">' + instance.app_version + '</a></td>';
		
		content = content + '<td>' + instance.host_name + '</td>';
		content = content + '<td><small>';
		for (var pos = 0; pos < instance.host_addresses.length; pos++) {
			content = content + instance.host_addresses[pos] + '<br/>';
		}
		content = content + '</small></td>';
		
		if (manager.hasInstanceAction()) {
			content = content + '<td>';
			if (instance.brokeralive) {
				content = content + '<button class="btn btn-mini btnmgraction btn-danger" ';
				content = content + 'data-target_class_name="AppManager" ';
				content = content + 'data-order_key="broker" ';
				content = content + 'data-order_value="stop" ';
				content = content + 'data-target_reference_key="' + instance.instance_name_pid + '" ';
				content = content + '><i class="icon-stop icon-white"></i> ' + i18n("manager.summary.broker.stop") ;
			} else {
				content = content + '<button class="btn btn-mini btnmgraction btn-success" ';
				content = content + 'data-target_class_name="AppManager" ';
				content = content + 'data-order_key="broker" ';
				content = content + 'data-order_value="start" ';
				content = content + 'data-target_reference_key="' + instance.instance_name_pid + '" ';
				content = content + '><i class="icon-play icon-white"></i> ' + i18n("manager.summary.broker.start") ;
			}
			content = content + '</button>';
			content = content + '</td>';
		}
		
		return content + '</tr>';
	};
})(window.mydmam.manager);

/**
 * prepareThreadsStackTrace(instances)
 */
(function(manager) {
	manager.prepareThreadsStackTrace = function(instances) {
		var prepareThisInstanceThreadsStackTrace = function(threadstacktraces, instancepos, app_version) {
			var content = '';
			content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatable">';
			content = content + '<thead>';
			content = content + '<th>' + i18n('manager.threads.tnamestate') + '</th>';
			content = content + '<th>' + i18n('manager.threads.tstacks') + '</th>';
			content = content + '</thead>';
			content = content + '<tbody>';
			
			threadstacktraces = threadstacktraces.sort(function(a, b) {
				return a.id < b.id ? -1 : 1;
			});
			
			getStacktraceline = function(line) {
				if (line.startsWith("at hd3gtv.mydmam") === false) {
					return line;
				}
				if (line.startsWith("at ") === false) {
					line = "at " + line;
				}
				var content = '';
				
				var pos_parL = line.indexOf("(");
				var pos_parR = line.indexOf(")");
				
				var filenameandlinepos = line.substring(pos_parL + 1, pos_parR);
				
				var path = line.substring(3, pos_parL).replace(/\./g, "/");
				path = path.substring(0, path.lastIndexOf("/"));//Remove function name
				path = path.substring(0, path.lastIndexOf("/"));//Remove class name
				path = path + "/" + filenameandlinepos.substring(0, filenameandlinepos.indexOf(":"));
				path = path + "#L" + filenameandlinepos.substring(filenameandlinepos.indexOf(":") + 1, filenameandlinepos.length);
				
				content = content + line.substring(0, pos_parL + 1);
				content = content + '<a href="' + manager.getGitHubURL(app_version, path) + '">';
				content = content + filenameandlinepos;
				content = content + '</a>';
				content = content + ')';
				return content;
			};
			
			for (var pos = 0; pos < threadstacktraces.length; pos++) {
				var threadstacktrace = threadstacktraces[pos];
				
				content = content + '<tr>';
				content = content + '<td>';
				content = content + '<span class="badge badge-info">' + threadstacktrace.id + '</span>';

				if (threadstacktrace.state === "TIMED_WAITING") {
					content = content + ' <span class="label label-warning">TIMED_WAITING</span>';
				} else if (threadstacktrace.state === "RUNNABLE") {
					content = content + ' <span class="label label-important">RUNNABLE</span>';
				} else if (threadstacktrace.state === "TERMINATED") {
					content = content + ' <span class="label label-label">TERMINATED</span>';
				} else {
					content = content + ' <span class="label label-success">' + threadstacktrace.state + '</span>';
				}
				if (threadstacktrace.isdaemon === false) {
					content = content + ' <span class="badge badge-important">Not a daemon</span>';
				}
				content = content + '<br /><small>' + threadstacktrace.name + '</small>';
				content = content + '<br /><small><span class="muted">' + threadstacktrace.classname + '</span></small>';
				content = content + '</td>';
				
				var execpoints = threadstacktrace.execpoint.split("\n");
				content = content + '<td><small>';
				if (execpoints.length > 1) {
					if (execpoints.length > 2) {
						content = content + getStacktraceline(execpoints[0]) + '<br />';
						var cssclassname = 'mgr-' + pos_i + '-' + pos; 
						content = content + '<button class="btn btn-mini btnincollapse" data-collapsetarget="' + cssclassname + '"><i class="icon-chevron-down"></i></button>';
						content = content + '<div class="collapse ' + cssclassname + '">';
						for (var pos_ep = 1; pos_ep < execpoints.length - 1; pos_ep++) {
							var execpoint = execpoints[pos_ep];
							content = content + getStacktraceline(execpoint) + '<br />';
						}
						content = content + '</div>'; //collapse
					} else {
						content = content + getStacktraceline(execpoints[0]) + '<br />';
					}
				}
				content = content + '</small></td>';
				content = content + '</tr>';
			}
			content = content + '</tbody>';
			content = content + '</table>';
			return content;
		};
		
		var content = '';
		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			content = content + '<p>';
			content = content + '<span class="label label-inverse">' + instance.app_name + '</span> &bull; ';
			content = content + instance.instance_name + ' &bull; ';
			content = content + '<code>' + instance.instance_name_pid + '</code>';
			content = content + '</p>';
			content = content + prepareThisInstanceThreadsStackTrace(instance.threadstacktraces, pos_i, instance.app_version);
		}
		
		return content;
	};
})(window.mydmam.manager);

/**
 * prepareClasspaths(instances)
 */
(function(manager) {
	manager.prepareClasspaths = function(instances) {
		var all_classpath_items = {};
		/**
		 * Collect all classpath items form instances.
		 */
		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			for (var pos_cp = 0; pos_cp < instance.classpath.length; pos_cp++) {
				var classpath_item = instance.classpath[pos_cp];
				if (all_classpath_items[classpath_item]) {
					continue;
				} else {
					all_classpath_items[classpath_item] = true;
				}
			}
		}
		
		/**
		 * @return true if classpath_item exists in instance.classpath
		 */
		var isInThisClasspath = function(instance, classpath_item) {
			for (var pos_cp = 0; pos_cp < instance.classpath.length; pos_cp++) {
				if (classpath_item === instance.classpath[pos_cp]) {
					return true;
				}
			}
			return false;
		};
		
		/**
		 * Collect all missing classpath_item in instances
		 */
		var missing_instances_classpath_items = {};
		for (var classpath_item in all_classpath_items) {
			for (var pos_i = 0; pos_i < instances.length; pos_i++) {
				var instance = instances[pos_i];
				if (isInThisClasspath(instance, classpath_item) === false) {
					if (missing_instances_classpath_items[classpath_item]) {
						missing_instances_classpath_items[classpath_item].push(instance);
					} else {
						missing_instances_classpath_items[classpath_item] = [instance];
					}
				}
			}
		}

		/**
		 * Draw result table.
		 */
		var content = '';
		content = content + '<blockquote><p>' + i18n('manager.classpaths.warnmessage') + '</p></blockquote>';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatable">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('manager.classpaths.item') + '</th>';
		content = content + '<th>' + i18n('manager.classpaths.missinginstances') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';
		
		for (var classpath_item in all_classpath_items) {
			content = content + '<tr>';
			content = content + '<td>' + classpath_item + '</td>';
			
			content = content + '<td>';
			if (missing_instances_classpath_items[classpath_item]) {
				content = content + '<small>';
				var missing_cp = missing_instances_classpath_items[classpath_item];
				for (var pos_mcp = 0; pos_mcp < missing_cp.length; pos_mcp++) {
					content = content + missing_cp[pos_mcp].instance_name_pid;
					content = content + " (" + missing_cp[pos_mcp].app_name + ")";
					content = content + "<br>";
				}
				content = content + '</small>';
			} else {
				content = content + "<em>" + i18n('manager.classpaths.cpiseverywhere') + "</em>";
			} 
			content = content + '</td>';
			content = content + '</tr>';
		}
		content = content + '</tbody>';
		content = content + '</table>';

		return content;
	};
})(window.mydmam.manager);

/**
 * prepareInstanceNameCell(instances)
 */
(function(manager) {
	manager.prepareInstanceNameCell = function(instance, item_key) {
		var content = '';
		content = content + '<strong>' + instance.app_name + '</strong>&nbsp;&bull; ';
		content = content + instance.instance_name + '<br>';
		content = content + '<small>' + instance.host_name + '</small>';
		content = content + '<span class="pull-right">' + mydmam.manager.jobs.view.displayKey(item_key, true) + '</span>';
		return content;
	};
})(window.mydmam.manager);

/**
 * prepareUseractionFunctionalityList(instances)
 */
(function(manager) {
	manager.drawUseractionFunctionalityList = function(query_destination, instances, callback) {
		
		var prepareUseractionFunctionalityList = function(availabilities) {
			var content = '';
			
			content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatable">';
			content = content + '<thead>';
			content = content + '<th>' + i18n('manager.jobcreator.th.instance') + '</th>';
			content = content + '<th>' + i18n("manager.uafunctlist.functionality") + '</th>';
			content = content + '<th>' + i18n("manager.uafunctlist.functionality") + '</th>';
			content = content + '<th>' + i18n("manager.uafunctlist.capabilities") + '</th>';
			content = content + '<th>' + i18n("manager.uafunctlist.storageindexeswhitelist") + '</th>';
			content = content + '</thead>';
			content = content + '<tbody>';
			
			for (var pos_i = 0; pos_i < instances.length; pos_i++) {
				var instance = instances[pos_i];
				var instance_ref = instance.instance_name_pid;				
				
				if (availabilities[instance_ref]) {
					var useraction_functionality_list = availabilities[instance_ref];
					for (var pos_uafl = 0; pos_uafl < useraction_functionality_list.length; pos_uafl++) {
						var functionality = useraction_functionality_list[pos_uafl];
						
						content = content + '<tr>';

						content = content + '<td>';
						content = content + mydmam.manager.prepareInstanceNameCell(instance, functionality.instance);
						content = content + '</td>';
						
						content = content + '<td>' + functionality.vendor + '<br>';
						content = content + '<span class="label label-inverse">' + i18n('useractions.functionalities.sections.' + functionality.section) + '</span>';
						if (functionality.powerful_and_dangerous) {
							content = content + ' <span class="label label-warning">' + i18n("manager.uafunctlist.powerful_and_dangerous") + '</span>';
						}
						content = content + '</td>';

						content = content + '<td>';
						content = content + functionality.longname + '';
						content = content + '<span class="pull-right">' + mydmam.manager.jobs.view.displayClassName(functionality.classname) + '</span>';
						content = content + '<br><small>' + functionality.description + '</small>';
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
							content = content + '<br><span class="label label-important">' + i18n("manager.uafunctlist.musthavelocalstorageindexbridge") + '</span>';
						}
						content = content + '</td>';

						content = content + '<td><small>';
						for (var pos_wl in functionality.capability.storageindexeswhitelist) {
							var storageindexeswhiteitem = functionality.capability.storageindexeswhitelist[pos_wl];
							if (jQuery.inArray(storageindexeswhiteitem, functionality.worker_capablity_storages) > -1) {
								content = content + storageindexeswhiteitem + ' ';
							} else {
								content = content + '<span class="muted">' + storageindexeswhiteitem + '</span> ';
							}
						}
						content = content + '</small></td>';

						content = content + '</tr>';
					}
				}
			}
			content = content + '</tbody>';
			content = content + '</table>';
			return content;
		};
		
		$.ajax({
			url: mydmam.manager.url.allavailabilities,
			type: "POST",
			async: true,
			beforeSend: function() {
				$(query_destination).html(i18n('manager.loading'));
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$(query_destination).html('<div class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + textStatus + '</strong></div>');
			},
			success: function(rawdata) {
				$(query_destination).html(prepareUseractionFunctionalityList(rawdata));
				callback();
			}
		});
		
	};
	
})(window.mydmam.manager);

/**
 * prepareLog2filters(instances)
 * addLog2filterFormItems(baseclassname, level, filtertype)
 */
(function(manager) {
	manager.prepareLog2filters = function(instances) {
		var content = '';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatableAAAAAAAAAAAAAAAAAAAAA">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('manager.log2filters.th.instances') + '</th>';
		content = content + '<th>' + i18n('manager.log2filters.th.filters') + '</th>';
		content = content + '<th>' + i18n('manager.log2filters.th.btncheck') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';

		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			var log2filters = instance.log2filters;
			
			content = content + '<tr>';

			content = content + '<td>';
			content = content + mydmam.manager.prepareInstanceNameCell(instance);
			content = content + '</td>';
			
			content = content + '<td>';
			for (var pos_l2 = 0; pos_l2 < log2filters.length; pos_l2++) {
				var log2filter = log2filters[pos_l2];
				content = content + manager.addLog2filterFormItems(log2filter.baseclassname, log2filter.level, log2filter.filtertype);
				content = content + ' <button class="btn btn-danger btn-mini btnmgrremovelog2filter" style="margin-bottom: 10px;"><i class="icon-minus icon-white"></i></button>';
				content = content + '<br>';
			}

			content = content + manager.addLog2filterFormItems("", "", "");
			content = content + ' <button class="btn btn-success btn-mini btnmgraddlog2filter" style="margin-bottom: 10px;"><i class="icon-plus icon-white"></i></button>';
			
			content = content + '</td>';

			content = content + '<td>';
			
			content = content + '<p><button class="btn btn-mini btn-primary btnsetlog2filters" ';
			content = content + 'data-instanceref="' + instance.instance_name_pid + '" ';
			content = content + '><i class="icon-ok icon-white"></i> ' + i18n("manager.log2filters.validate") + '</button></p>';

			content = content + '<p><button class="btn btn-mini btncopylogfilterconf">';
			content = content + '<i class="icon-download"></i> ' + i18n("manager.log2filters.copyconf") + '</button></p>';

			content = content + '</td>';
			content = content + '</tr>';
		}
		
		content = content + '</tbody>';
		content = content + '</table>';
		return content;
	};
	
	manager.addLog2filterFormItems = function(baseclassname, level, filtertype) {
		var prepare_select_level = function(actual_value) {
			var levels = ["NONE", "DEBUG", "INFO", "ERROR", "SECURITY"];
			var content = '';
			content = content + '<select class="input-small sellog2level">';
			for (var pos = 0; pos < levels.length; pos++) {
				content = content + '<option value="' + levels[pos] + '"';
				if (actual_value === levels[pos]) {
					content = content + ' selected';
				}
				content = content + '>' + i18n('manager.log2filters.level.' + levels[pos]) + '</option>';
			}
			content = content + '</select>';
			return content;
		};
		
		var prepare_select_filtertype = function(actual_value) {
			var levels = ["HIDE", "ONE_LINE", "NO_DUMP", "DEFAULT", "VERBOSE_CALLER"];
			var content = '';
			content = content + '<select class="input-medium sellog2filtertype">';
			for (var pos = 0; pos < levels.length; pos++) {
				content = content + '<option value="' + levels[pos] + '"';
				if (actual_value === levels[pos]) {
					content = content + ' selected';
				}
				content = content + '>' + i18n('manager.log2filters.filtertype.' + levels[pos]) + '</option>';
			}
			content = content + '</select>';
			return content;
		};

		var content = '';
		content = content + '<span class="inputlog2filter">';
		content = content + '<input type="text" spellcheck="false" class="input-xxlarge log2classname" value="' + baseclassname + '" placeholder="' + i18n('manager.log2filters.classnameplaceholder') + '" />';
		content = content + ' ' + prepare_select_level(level) + '';
		content = content + ' ' + prepare_select_filtertype(filtertype) + '';
		content = content + '</span>';
		return content;
	};
})(window.mydmam.manager);

/**
 * setBtnForLog2filters()
 */
(function(manager) {
	manager.setBtnForLog2filters = function(query_destination) {
		
		var getFilters = function(jq_tr) {
			var jq_all_spaninput = jq_tr.find("span.inputlog2filter");
			var filters = [];
			jq_all_spaninput.each(function() {
				var filter = {
					baseclassname: $(this).children("input.log2classname").val().trim(), 
					level: $(this).children("select.sellog2level").val(),
					filtertype:	$(this).children("select.sellog2filtertype").val()
				};
				if (filter.baseclassname === '') {
					return;
				}
				filters.push(filter);
			});
			return filters;
		};
		
		var btnsetlog2filters_click = function() {
			var button = $(this);
			if (button.hasClass("disabled")) {
				return;
			}
			var jq_tr = button.parent().parent().parent();// p > td > tr
			var beforesend = function() {
				button.addClass("disabled");
			};
			var error = function() {
			};
			var success = function() {
				button.removeClass("disabled");
			};
			manager.doAction("AppManager", $(this).data("instanceref"), "log2filters", getFilters(jq_tr), beforesend, error, success);
		};
		
		var btncopylogfilterconf_click = function() {
			$(query_destination + ' textarea.log2filterconf').remove();
			
			var filters = getFilters($(this).parent().parent().parent()); // p > td > tr
			
			var content = '';
			content = content + '<textarea class="input-block-level log2filterconf" style="margin-top: 1em; font-size: 8px; line-height: 1.3em; height: 15em; overflow: none;" spellcheck="false">';
			content = content + 'log2:' + "\n";
			content = content + '    filter:' + "\n";
			for (var pos_f = 0; pos_f < filters.length; pos_f++) {
				content = content + '        -' + "\n";
				content = content + '            for: ' + filters[pos_f].baseclassname + "\n";
				content = content + '            level: ' + filters[pos_f].level + "\n";
				content = content + '            type: ' + filters[pos_f].filtertype + "\n";
			}
			content = content + '</textarea>';
			$(this).after(content);
			$(this).next().select();
		};
		
		var btnmgrremovelog2filter_click = function(){
			$(this).prev().remove(); //span
			$(this).next().remove(); //br
			$(this).remove(); // button
		};
		
		var btnmgraddlog2filter_click = function(){
			var content = '';
			content = content + ' <button class="btn btn-danger btn-mini btnmgrremovelog2filter" style="margin-bottom: 10px;"><i class="icon-minus icon-white"></i></button>';
			content = content + '<br>';
			content = content + manager.addLog2filterFormItems("", "", "") + " ";
			$(this).before(content);
			manager.setBtnForLog2filters(query_destination);
		};
		
		$(query_destination + ' button.btnsetlog2filters').click(btnsetlog2filters_click).removeClass("btnsetlog2filters");
		$(query_destination + ' button.btncopylogfilterconf').click(btncopylogfilterconf_click).removeClass("btncopylogfilterconf");
		$(query_destination + ' button.btnmgrremovelog2filter').click(btnmgrremovelog2filter_click).removeClass("btnmgrremovelog2filter");
		$(query_destination + ' button.btnmgraddlog2filter').click(btnmgraddlog2filter_click).removeClass("btnmgraddlog2filter");
	};
})(window.mydmam.manager);
