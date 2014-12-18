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
 * refreshWorkersStatus(query_destination)
 */
(function(manager) {
	manager.refreshWorkersStatus = function(query_destination) {

		var set_btn_event_in_collapse = function() {
			var tr_class = $(this).data("collapsetarget");
			var all_collapse = $(query_destination + ' div.tableworkers tr.' + tr_class + ' div.collapse.notyetcollapsed');
			all_collapse.addClass('in');
			all_collapse.removeClass('notyetcollapsed');
			$(this).remove();
			return true;
		};
		
		var drawTable = function(workers) {
			var content = '';
			content = content + '<table class="table table-striped table-bordered table-hover table-condensed">';
			content = content + '<thead>';
			content = content + '<th>' + i18n('instance') + '</th>';
			content = content + '<th>' + i18n('name') + '</th>';
			content = content + '<th>' + i18n('state') + '</th>';
			content = content + '<th>' + i18n('capablities') + '</th>';
			content = content + '</thead>';
			content = content + '<tbody>';
			/*for (var pos_wr = 0; pos_wr < workers.length; pos_wr++) {
				var worker = workers[pos_wr];
				var instance = worker.manager_reference.members;
				var tr_class = 'wkrsrow-' + pos_wr;
				
				content = content + '<tr class="' + tr_class + '" data-workerref="' + worker.reference_key + '" data-instanceref="' + instance.instance_ref + '">';
				content = content + '<td>';
				content = content + '<strong>' +instance.app_name + '</strong>&nbsp;&bull; ';
				content = content + instance.instance_name + ' ';
				content = content + '<button class="btn btn-mini btnincollapse pull-right notyetsetevent" data-collapsetarget="' + tr_class + '"><i class="icon-chevron-down"></i></button><br/>';
				content = content + '<div class="collapse notyetcollapsed">';
				content = content + '<small>' + instance.host_name + '</small>';
				content = content + '</div>'; // collapse
				content = content + '</td>';
				
				content = content + '<td>';
				content = content + worker.long_name;
				content = content + '<div class="collapse notyetcollapsed">';
				content = content + '<span class="label label-inverse">' + i18n('manager.workers.category.' + worker.category) + '</span><br/>';
				content = content + '<small>' + worker.vendor_name + '</small><br/>';
				var worker_class_name = worker.worker_class.substring(worker.worker_class.lastIndexOf('.') + 1, worker.worker_class.length);
				content = content + '<abbr title="' + worker.worker_class + '">' + worker_class_name + '.class</abbr>';
				content = content + '</div>'; // collapse
				content = content + '</td>';
				
				content = content + '<td>';
				if (worker.state === 'WAITING') {
					content  = content + '<span class="label label-success">' + i18n('manager.workers.state.' + worker.state) + '</span>';
				} else if (worker.state === 'PROCESSING') {
					content  = content + '<span class="label label-warning">' + i18n('manager.workers.state.' + worker.state) + '</span>';
				} else if (worker.state === 'STOPPED') {
					content  = content + '<span class="label label-info">' + i18n('manager.workers.state.' + worker.state) + '</span>';
				} else if (worker.state === 'PENDING_STOP') {
					content  = content + '<span class="label label-important">' + i18n('manager.workers.state.' + worker.state) + '</span>';
				} else {
					content = content + worker.state;
				}
				
				content = content + '<div class="collapse notyetcollapsed">';
				if (worker.current_job_key) {
					content = content + '<small>' + i18n('manager.workers.currentjob', worker.current_job_key) + '</small>';
				}
				content = content + '</div>'; // collapse
				
				content = content + '</td>';

				content = content + '<td><small>';
				var capablities = worker.capablities;
				for (var pos_wcap = 0; pos_wcap < capablities.length; pos_wcap++) {
					var capablity = capablities[pos_wcap];
					var job_context_avaliable_class_name = capablity.job_context_avaliable.substring(capablity.job_context_avaliable.lastIndexOf('.') + 1, capablity.job_context_avaliable.length);
					content = content + '&bull; <abbr title="' + capablity.job_context_avaliable + '">' + job_context_avaliable_class_name + '.class</abbr>';
					
					content = content + '<div class="collapse notyetcollapsed">';
					var storages_available = capablity.storages_available;
					if (storages_available) {
						content = content + i18n('manager.workers.capablitystorages') + ' ';
						for (var pos_sa = 0; pos_sa < storages_available.length; pos_sa++) {
							var storage_available = storages_available[pos_sa];
							content = content + storage_available;
							if (pos_sa + 1 !== storages_available.length) {
								content = content + ', ';
							}
						}
					}
					content = content + '</div>'; // collapse
					if (pos_wcap + 1 !== capablities.length) {
						content = content + '<br/>';
					}
				}
				content = content + '</small></td>';
				
				content = content + '</tr>';
			}*/
			content = content + '</tbody>';
			content = content + '</table>';
			
			$(query_destination + ' div.tableworkers').html(content);
			datatable = $(query_destination + ' div.tableworkers table').dataTable({
				"bPaginate": false,
				"bLengthChange": false,
				"bSort": true,
				"bInfo": false,
				"bAutoWidth": false,
				"bFilter": true,
			});
			
			$(query_destination + ' div.tableworkers tr button.btn.btn-mini.btnincollapse.notyetsetevent').click(set_btn_event_in_collapse);
			
			return datatable;
		};
		
		var last_workers = {};
		var datatable_workers_position = {};
		var datatable = null;
		
		var update = function(workers) {
			if (datatable == null) {
				datatable = drawTable(workers);
				/*for (var pos_wr = 0; pos_wr < workers.length; pos_wr++) {
					last_workers[workers[pos_wr].reference_key] = workers[pos_wr];
				}*/
			}
			
			var current_workers = {};
			
			for (var pos_wr in workers) {
				var worker = workers[pos_wr];
				var key = worker.reference_key;
				if (last_workers[key]) {
					//TODO update ?
					//console.log('update', worker);
					//datatable.fnUpdate(["DDD<strong>sds</strong>","Bdsd","Csdsd","Dsdsd"], datatable_workers_position[key]);
				} else {
					//TODO add
					//console.log('add', worker);
					var new_item_pos = datatable.fnAddData(["A<strong>BB</strong>","B","C","D"]);
					datatable_workers_position[key] = new_item_pos[0];
					//content = content + '<tr class="' + tr_class + '" data-workerref="' + worker.reference_key + '" data-instanceref="' + instance.instance_ref + '">';
					//content = content + '<td>';
					//datatable_workers_position
				}
				current_workers[key] = worker;
				last_workers[key] = worker;
			}
			
			for (var key in last_workers) {
				if (!current_workers[key]) {
					//TODO remove
					// console.log('remove', last_workers[key]);
					//datatable.fnDeleteRow(0);
					delete last_workers[key];
				}
			}
		};
		
		var do_refresh = function() {
			$.ajax({
				url: mydmam.manager.url.allworkers,
				type: "POST",
				beforeSend: function() {
					$(query_destination + " div.refreshhourglass").css('visibility', 'visible');
				},
				error: function(jqXHR, textStatus, errorThrown) {
					$(query_destination + " div.refreshhourglass").css('visibility', 'hidden');
					$(query_destination + ' div.tableworkers').html('<div class="alert"><strong>' + i18n('manager.workers.refresherror') + '</strong></div>');
					last_workers = {};
					datatable = null;
				},
				success: function(rawdata) {
					$(query_destination + " div.refreshhourglass").css('visibility', 'hidden');
					if (rawdata == null | rawdata.length === 0) {
						$(query_destination + ' div.tableworkers').html('<div class="alert alert-info"><strong>' + i18n("manager.workers.empty") + '</strong></div>');
						last_workers = {};
						datatable = null;
						return;
					}
					update(rawdata);
				}
			});
		};

		var content = '';
		content = content + '<div class="tableworkers"></div>';
		content = content + '<div class="refreshhourglass muted" style="visibility: hidden;"><small>' + i18n('manager.workers.inrefresh') + '</small></div>';
		$(query_destination).html(content);
		do_refresh();
		setInterval(do_refresh, 5000);
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
		content = content + manager.prepareCyclic(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Triggers
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrtriggers") + '" id="mgrtriggers">';
		content = content + manager.prepareTriggers(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Triggers
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgruafunctlist") + '" id="mgruafunctlist">';
		content = content + manager.prepareUseractionFunctionalityList(rawdata);
		content = content + '</div>'; //tab-pane
		
		/**
		 * Show Workers status
		 */
		content = content + '<div class="tab-pane ' + getActiveClassIfThisTabIsUserSelected("#mgrworkers") + '" id="mgrworkers">';
		content = content + '</div>'; //tab-pane
		
		$(query_destination).html(content);
		
		manager.refreshWorkersStatus("#mgrworkers");
		
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
			return content + '</tr>';
		}
		
		content = content + '<td>' + instance.instance_name + '</td>';
		content = content + '<td><strong>' + instance.app_name + '</strong><br><small>' + instance.instance_name_pid + '</small></td>';
		content = content + '<td>' + instance.uptime_from + '</td>';
		
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
 * prepareCyclic(instances)
 */
(function(manager) {
	manager.prepareCyclic = function(instances) {
		var content = 'C';
		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			var declared_cyclics = instance.declared_cyclics;
			for (var pos_cy = 0; pos_cy < declared_cyclics.length; pos_cy++) {
				var declared_cyclic = declared_cyclics[pos_cy];
				//TODO display cyclics
			}
		}
		/*content = content + '<blockquote><p>' + i18n('manager.classpaths.warnmessage') + '</p></blockquote>';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed setdatatable">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('manager.classpaths.item') + '</th>';
		content = content + '<th>' + i18n('manager.classpaths.missinginstances') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';*/

		return content;
	};
})(window.mydmam.manager);

/**
 * prepareTriggers(instances)
 */
(function(manager) {
	manager.prepareTriggers = function(instances) {
		var content = 'T';
		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			var declared_triggers = instance.declared_triggers;
			for (var pos_tr = 0; pos_tr < declared_triggers.length; pos_tr++) {
				var declared_trigger = declared_triggers[pos_tr];
				//TODO display trigger
			}
		}
		return content;
	};
})(window.mydmam.manager);

/**
 * prepareUseractionFunctionalityList(instances)
 */
(function(manager) {
	manager.prepareUseractionFunctionalityList = function(instances) {
		var content = 'UA';
		for (var pos_i = 0; pos_i < instances.length; pos_i++) {
			var instance = instances[pos_i];
			var useraction_functionality_list = instance.useraction_functionality_list;
			for (var pos_uafl = 0; pos_uafl < useraction_functionality_list.length; pos_uafl++) {
				var useraction_functionality = useraction_functionality_list[pos_uafl];
				//TODO display useraction_functionality
			}
		}
		return content;
	};
})(window.mydmam.manager);

