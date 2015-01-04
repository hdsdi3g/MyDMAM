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
		 * Show Useraction Functionality List
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
		
	};
})(window.mydmam.manager);

/**
 * setBtnActionClick(query_destination)
 */
(function(manager) {
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
		
		var order_key = jquery_btn.data("order_key");
		var order = {};
		order[order_key] = jquery_btn.data("order_value");
		
		$.ajax({
			url: manager.url.instanceaction,
			type: "POST",
			data: {
				target_class_name: jquery_btn.data("target_class_name"),
				target_reference_key: jquery_btn.data("target_reference_key"),
				json_order: JSON.stringify(order),
			},
			beforeSend: function() {
				jquery_btn.addClass("disabled");
			},
			error: function() {
				jquery_btn.removeClass("disabled");
				alert(i18n('manager.action.error'));
			},
			success: function(rawdata) {
				jquery_btn.removeClass("disabled");
				if (rawdata.length !== 0) {
					console.err(rawdata);
				}
			},
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
				/* LEGACY CODE :
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
								content = content + '<small>' + functionality.section + '</small></td>';

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
				 * 
				 * */
			}
		}
		return content;
	};
})(window.mydmam.manager);


