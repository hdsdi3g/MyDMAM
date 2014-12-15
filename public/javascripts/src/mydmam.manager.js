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
 * showFullDisplay(rawdata, query_destination)
 */
(function(manager) {
	manager.showFullDisplay = function(rawdata, query_destination) {
		var content = '';
		
		content = content + '<div class="pagination"><ul>';
		content = content + '<li><a href="#summary">' + i18n('manager.summary.title') + '</a></li>';
		content = content + '<ul/></div>';
		
		content = content + '<a name="summary"></a><p class="lead">' + i18n('manager.summary.title') + '</p>';
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
		
		var displayDetailedInformationForInstance = function(instance, pos) {
			var pos = $(this).data('instancepos');
			var instance = rawdata[pos];
			manager.prepareDetailedInformationForInstance(instance, pos);
		};
		
		$(query_destination + ' button.btnshowinstance').click(displayDetailedInformationForInstance);
		
		
	};
})(window.mydmam.manager);

/**
 * getGitHubURL(app_version)
 */
(function(manager) {
	manager.getGitHubURL = function(app_version, path) {
		if (path == null) {
			return 'https://github.com/hdsdi3g/MyDMAM/tree/' + app_version.substring(app_version.lastIndexOf(" ") + 1, app_version.length);
		} else {
			return 'https://github.com/hdsdi3g/MyDMAM/tree/' + app_version.substring(app_version.lastIndexOf(" ") + 1, app_version.length) + '/' + path;
		}
	};
})(window.mydmam.manager);


/**
 * prepareSummary(rawdata, query_destination)
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
		
		content = content + '<td><button class="btn btn-mini btnshowinstance" data-instancepos="' + pos + '"><i class="icon-zoom-in"></i></button>&nbsp;';
		content = content + instance.instance_name + '</td>';
		content = content + '<td><strong>' + instance.app_name + '</strong><br>' + instance.instance_name_pid + '</td>';
		content = content + '<td>' + instance.uptime_from + '</td>';
		
		content = content + '<td>Java: ';
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
		
		content = content + '<td>App: <a href="' + manager.getGitHubURL(instance.app_version) + '">' + instance.app_version + '</a></td>';
		
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
 * prepareSummary(rawdata, query_destination)
 */
(function(manager) {
	manager.prepareDetailedInformationForInstance = function(instance, pos) {
		//TODO display
		console.log(instance.threadstacktraces);
		return ''; //TODO
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
