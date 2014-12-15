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
				if (rawdata == null | rawdata == "null" | rawdata === ""| rawdata == "[]") {
					$('#laststatusworkers').append('<div class="alert alert-info"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("manager.nodetectedmodule") + '</strong></div>');
					return;
				}
				manager.showFullDisplay(rawdata, query_destination);
			}
		});
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
 * getStacktraceline(line, app_version)
 * Add GitHub MyDMAM url java code if match. 
 */
(function(manager) {
	manager.getStacktraceline = function(line, app_version) {
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
})(window.mydmam.manager);

/**
 * showFullDisplay(rawdata, query_destination)
 */
(function(manager) {
	manager.showFullDisplay = function(rawdata, query_destination) {
		var content = '';
		if (window.location.hash === '') {
			window.location.hash = "#summary";
		}
		
		var getActiveClassIfThisTabIsUserSelected = function(target) {
			if (target === window.location.hash) {
				return "active";
			} else {
				return "";
			}
		};
		
		content = content + '<ul class="nav nav-tabs manager">';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrsummary") + '"><a href="#mgrsummary">' + i18n('manager.summary.title') + '</a></li>';
		content = content + '<li class="' + getActiveClassIfThisTabIsUserSelected("#mgrthreads") + '"><a href="#mgrthreads">' + i18n('manager.threads.title') + '</a></li>';
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
		content = content + '</table>';
		
		content = content + '</div>'; //tab-pane
		
		$(query_destination).html(content);
		
		$(query_destination + ' table.setdatatable').dataTable({
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
		$(query_destination + ' ul.nav.nav-tabs.manager a').click(function() {
			$(this).tab('show');
			return true;
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
						content = content + manager.getStacktraceline(execpoints[0], app_version) + '<br />';
						var cssclassname = 'mgr-' + pos_i + '-' + pos; 
						content = content + '<button class="btn btn-mini btnincollapse" data-collapsetarget="' + cssclassname + '"><i class="icon-chevron-down"></i></button>';
						content = content + '<div class="collapse ' + cssclassname + '">'; //in
						for (var pos_ep = 1; pos_ep < execpoints.length - 1; pos_ep++) {
							var execpoint = execpoints[pos_ep];
							content = content + manager.getStacktraceline(execpoint, app_version) + '<br />';
						}
						content = content + '</div>'; //collapse
					} else {
						content = content + manager.getStacktraceline(execpoints[0], app_version) + '<br />';
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

//TODO display instance.classpath
//TODO display useraction_functionality_list
//TODO display declared_cyclics
//TODO display declared_triggers

/*
var data = rawdata.sort(function(a, b){
	 * normal sort
	return a.workername < b.workername ? -1 : 1;
});
* */
