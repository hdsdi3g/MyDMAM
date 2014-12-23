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
 */
(function(jobs) {
	jobs.list = {};
	jobs.last_refresh = null;
	jobs.last_full_refresh = null;
	jobs.jquery_destination = "";
	jobs.jquery_header = "";
	jobs.datatable = null;
	jobs.datatable_job_pos = {};
	jobs.client_time = 0; 
	jobs.server_time = 0; 
	jobs.refresh_intervaller = null;
	jobs.status_list = ["TOO_OLD", "CANCELED", "POSTPONED", "WAITING", "DONE", "PROCESSING", "STOPPED", "ERROR", "PREPARING", "TOO_LONG_DURATION"];
	jobs.refresh_delay_time = 60000;
})(window.mydmam.manager.jobs);


/**
 * displayClassName(class_name)
 */
(function(jobs) {
	jobs.displayClassName = function(class_name) {
		var simple_name = class_name.substring(class_name.lastIndexOf(".") + 1, class_name.length);
		if (class_name.indexOf("(") > -1) {
			simple_name = class_name.substring(class_name.indexOf("(") + 1, (class_name.indexOf(")")));
			if (simple_name.indexOf(".java") > -1) {
				simple_name = simple_name.substring(0, simple_name.indexOf(".java"));
			}
		}
		return '<i class="icon-book"></i> <abbr title="' + class_name + '">' + simple_name + '</abbr>';
	};
})(window.mydmam.manager.jobs);

/**
 * displayKey(key)
 */
(function(jobs) {
	jobs.displayKey = function(key, ishtml) {
		var short_value = key.substring(key.lastIndexOf(":") + 1, key.lastIndexOf(":") + 9) + '.';
		if (ishtml) {
			return '<abbr title="' + key + '"><code><i class="icon-barcode"></i> ' + short_value + '</code></abbr>';
		} else {
			return short_value;
		}
	};
})(window.mydmam.manager.jobs);


/**
 * drawTable
 */
(function(jobs) {
	jobs.drawTable = function() {
		var content = '';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed" style="margin-bottom: 0px;">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('manager.jobs.th.name') + '</th>';
		content = content + '<th>' + i18n('manager.jobs.th.status') + '</th>';
		content = content + '<th>' + i18n('manager.jobs.th.date') + '</th>';
		content = content + '<th>' + i18n('manager.jobs.th.params') + '</th>';
		content = content + '<th>' + i18n('manager.jobs.th.progress') + '</th>';
		content = content + '<th>' + i18n('manager.jobs.th.action') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';
		content = content + '</tbody>';
		content = content + '</table>';
		
		$(jobs.jquery_destination).html(content);
		datatable_job_pos = {};
		jobs.datatable = $(jobs.jquery_destination + ' table').dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
			"aoColumnDefs": [
				{"sWidth": "4em", "aTargets": [1]},
				{"sWidth": "5em", "aTargets": [2]},
				{"sWidth": "8em", "aTargets": [5]},
			],
		});
	};
})(window.mydmam.manager.jobs);

/**
 * addRow(job)
 */
(function(jobs) {
	jobs.addRow = function(job, selected_status) {
		if (selected_status.indexOf(job.status) === -1) {
			// return; TODO set !
		}
		var cols = [];
		var content = '';
		
		/**
		 * New col
		 */
		content = content + '<strong>' + job.name + '</strong>';
		content = content + '<button class="btn btn-mini pull-right"><i class="icon-chevron-down"></i></button>'; 
		cols.push(content);
		
		/**
		 * New col
		 */
		content = '';
		var i18n_status = i18n('manager.jobs.status.' + job.status);
		if (job.status === 'WAITING') {
			content = content + '<span class="label">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'PREPARING') {
			content = content + '<span class="label label-warning">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'PROCESSING') {
			content = content + '<span class="label label-warning">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'DONE') {
			content = content + '<span class="label">'               + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'TOO_OLD') {
			content = content + '<span class="label label-info">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'STOPPED') {
			content = content + '<span class="label label-info">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'TOO_LONG_DURATION') {
			content = content + '<span class="label label-info">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'CANCELED') {
			content = content + '<span class="label label-info">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'POSTPONED') {
			content = content + '<span class="label label-info">' + i18n_status + '</span>' + '<br />'; 
		} else if (job.status === 'ERROR') {
			content = content + '<span class="label badge-important">' + i18n_status + '</span>' + '<br />'; 
		} else {
			content = content + '<span class="label label-inverse">' + i18n_status + '</span>' + '<br />'; 
		}
		if (job.priority > 0) {
			content = content + '<span class="badge badge-important">' + job.priority + '</span>' + '<br />';
		}
		if (job.urgent) {
			content = content + '<span class="badge badge-important">' + i18n('manager.jobs.urgent') + '</span>' + '<br />';
		}
		cols.push(content);
		
		/**
		 * New col
		 */
		content = '';
		content = content + '<span class="label">' + i18n('manager.jobs.create_date', mydmam.format.fulldate(job.create_date)) + '</span>' + '<br />';
		content = content + '<span class="label">' + i18n('manager.jobs.start_date', mydmam.format.fulldate(job.start_date)) + '</span>' + '<br />';
		content = content + '<span class="label">' + i18n('manager.jobs.update_date', mydmam.format.fulldate(job.update_date)) + '</span>' + '<br />';
		content = content + '<span class="label">' + i18n('manager.jobs.expiration_date', mydmam.format.fulldate(job.expiration_date)) + '</span>' + '<br />';
		content = content + '<span class="label">' + i18n('manager.jobs.end_date', mydmam.format.fulldate(job.end_date)) + '</span>' + '<br />';
		cols.push(content);
		
		/**
		 * New col
		 */
		content = '';
		content = content + i18n('manager.jobs.createdby') + ' ' + jobs.displayClassName(job.creator);
		content = content + ' <abbr title="' + job.instance_status_creator_key + '">' + job.instance_status_creator_hostname + '</abbr>' + '<br />';
		if (job.require_key) {
			var jobrq = jobs.list[require_key];
			if (jobrq) {
				content = content + '<abbr title="' + jobs.displayKey(jobrq.key, false)  + '">';
				content = content + '<span class="label label-info">';
				content = content + i18n('manager.jobs.requireto') + ' ' + jobrq.name + ' (' + i18n('manager.jobs.status.' + jobrq.status) + ')'; 
				content = content + '</span>'; 
				content = content + '</abbr>'; 
				content = content + '<br />'; 
			} else {
				content = content + 'Rq ' + jobs.displayKey(job.require_key, true) + '' + '<br />'; 
			}
		}
		if (job.max_execution_time < (1000 * 3600 * 24)) {
			if (job.max_execution_time > (3600 * 1000)) {
				content = content + '<span class="label">' + i18n('manager.jobs.max_execution_time_hrs', Math.round((job.max_execution_time / (3600 * 1000)))) + '</span>' + '<br />';
			} else {
				content = content + '<span class="label">' + i18n('manager.jobs.max_execution_time_sec', (job.max_execution_time / 1000)) + '</span>' + '<br />';
			}
		}
		
		if (job.delete_after_completed) {
			content = content + '<span class="label label-inverse">' + i18n("manager.jobs.delete_after_completed") + '</span>' + '<br />'; 
		}
		content = content + '' + jobs.displayClassName(job.context.classname) + '' + '<br />'; 
		if (job.context.neededstorages) {
			content = content + '' + JSON.stringify(job.context.neededstorages) + '' + '<br />'; 
		}
		if (job.context.content) {
			content = content + '<code><i class="icon-indent-left"></i> ' + JSON.stringify(job.context.content) + '</code>' + '<br />'; 
		}
		
		if (job.processing_error) {
			content = content + '<code>' + JSON.stringify(job.processing_error) + '</code>' + '<br />';
		}

		content = content + jobs.displayKey(job.key, true) + '<br />';
		cols.push(content);
		
		/**
		 * New col
		 */
		content = '';
		if (job.progression) {
			var progression = job.progression;
			content = content + '' + jobs.displayClassName(progression.last_caller) + '' + '<br />'; 
			content = content + '<i class="icon-comment"></i> <em>' + progression.last_message + '</em>' + '<br />'; 
			
			content = content + '<strong class="pull-left" style="margin-right: 5px;">'; 
			if (progression.step > progression.step_count) {
				content = content + progression.step; 
			} else {
				content = content + progression.step + ' <i class="icon-arrow-right"></i> ' + progression.step_count; 
			}
			content = content + '</strong>';

			if (job.status === 'DONE') {
				content = content + '<div class="progress progress-success" style="margin-bottom: 5px;">';
			    content = content + '<div class="bar" style="width: 100%;"></div>';
			    content = content + '</div>';
				content = content + '<br />';
			} else {
				var percent = (progression.progress / progression.progress_size) * 100;
				if (job.status === 'PROCESSING') {
					content = content + '<div class="progress progress-striped active" style="margin-bottom: 5px;">';
				} else {
					content = content + '<div class="progress progress-danger progress-striped" style="margin-bottom: 5px;">';
				}
			    content = content + '<div class="bar" style="width: ' + percent + '%;"></div>';
			    content = content + '</div>';
				content = content + ' ' + progression.progress + '/' + progression.progress_size + '<br>';
			}
		}
		content = content + i18n('manager.jobs.worker') + ' ' + jobs.displayClassName(job.worker_class) + ' '; 
		content = content + '(' + jobs.displayKey(job.worker_reference, true) + ')<br>'; 
		content = content + i18n('manager.jobs.instanceexecutor') + ' <abbr title="' + job.instance_status_executor_key + '">' + job.instance_status_executor_hostname + '</abbr>' + '<br />';
		cols.push(content);
		
		/**
		 * New col
		 */
		content = '';
		content = content + '<button class="btn btn-mini"><i class="icon-repeat"></i> ' + i18n('manager.jobs.btn.restart') + '</button><br>'; 
		content = content + '<button class="btn btn-mini"><i class="icon-trash"></i> ' + i18n('manager.jobs.btn.delete') + '</button><br>';
		content = content + '<button class="btn btn-mini"><i class="icon-stop"></i> ' + i18n('manager.jobs.btn.stop') + '</button><br>';
		content = content + '<button class="btn btn-mini"><i class="icon-inbox"></i> ' + i18n('manager.jobs.btn.setinwait') + '</button><br>';
		content = content + '<button class="btn btn-mini"><i class="icon-off"></i> ' + i18n('manager.jobs.btn.cancel') + '</button><br>';
		content = content + '<button class="btn btn-mini"><i class="icon-warning-sign"></i> ' + i18n('manager.jobs.btn.hipriority') + '</button><br>';
		content = content + '<button class="btn btn-mini"><i class="icon-calendar"></i> ' + i18n('manager.jobs.btn.noexpiration') + '</button><br>';
		cols.push(content);

		console.log("add", job, selected_status);
		var pos_new_row = jobs.datatable.fnAddData(cols);
		jobs.datatable_job_pos[job.key] = pos_new_row;
	};
})(window.mydmam.manager.jobs);

/**
 * updateRow(job)
 */
(function(jobs) {
	jobs.updateRow = function(job, selected_status) {
		console.log("update", job);
		/**
		 * TODO if job.delete_after_completed && job.status === 'DONE'
		 * 
		 * TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING, TOO_LONG_DURATION;
		 */
		/**
		 * TODO if job.status is changed, delete row !
			var pos_row = jobs.datatable_job_pos[job.key];
		 */
	};
})(window.mydmam.manager.jobs);

/**
 * clearTable()
 */
(function(jobs) {
	jobs.clearTable = function() {
		jobs.drawHeader();
	};
})(window.mydmam.manager.jobs);

/**
 * hideTable()
 */
(function(jobs) {
	jobs.hideTable = function() {
		$(jobs.jquery_destination + ' table').remove();
		$(jobs.jquery_header + ' ul').remove();
	};
})(window.mydmam.manager.jobs);

/**
 * refreshHeaderCounters()
 */
(function(jobs) {
	jobs.refreshHeaderCounters = function() {
		var counters = {};
		
		for (var pos = 0; pos < jobs.status_list.length; pos++) {
			counters[jobs.status_list[pos]] = 0;
		}
		for (var job_key in jobs.list) {
			counters[jobs.list[job_key].status]++;
		}
		
		var setbadges = function() {
			var value = 0;
			for (var status_name in counters) {
				if ($(this).hasClass('jobswitch' + status_name)) {
					value += counters[status_name];
				}
			}
			if (value > 0) {
				$(this).children("span.badge").html(value);
			} else {
				$(this).children("span.badge").html("");
			}
		};
		
		$(jobs.jquery_header + ' ul.nav.joblistheader a.btnswitchjoblisttable').each(setbadges);
	};
})(window.mydmam.manager.jobs);

//$(jobs.jquery_header + ' ul.nav.joblistheader

/**
 * getSelectedHeaderTab()
 * return [job.status]
 */
(function(jobs) {
	jobs.getSelectedStatusHeaderTab = function() {
		var jq_selected_a = $(jobs.jquery_header + ' ul.nav.joblistheader li.active').children("a");
		var result = [];
		for (var pos = 0; pos < jobs.status_list.length; pos++) {
			var status_name = jobs.status_list[pos];
			if (jq_selected_a.hasClass('jobswitch' + status_name)) {
				result.push(status_name);
			}
		}
		return result;
	};
})(window.mydmam.manager.jobs);


/**
 * drawHeader()
 */
(function(jobs) {
	jobs.drawHeader = function() {
		var content = '';
		content = content + '<ul class="nav nav-tabs joblistheader">';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchWAITING">';
		content = content + i18n("manager.jobs.status.WAITING") + ' <span class="badge" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchPREPARING jobswitchPROCESSING">';
		content = content + i18n("manager.jobs.status.PREPARINGPROCESSING") + ' <span class="badge badge-warning" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchDONE">';
		content = content + i18n("manager.jobs.status.DONE") + ' <span class="badge badge-success" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchTOO_OLD">';
		content = content + i18n("manager.jobs.status.TOO_OLD") + ' <span class="badge badge-info" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchSTOPPED jobswitchTOO_LONG_DURATION">';
		content = content + i18n("manager.jobs.status.STOPPEDTOO_LONG_DURATION") + ' <span class="badge badge-info" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchCANCELED jobswitchPOSTPONED">';
		content = content + i18n("manager.jobs.status.CANCELEDPOSTPONED") + ' <span class="badge" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		content = content + '<li><a href="" data-toggle="tab" class="btnswitchjoblisttable jobswitchERROR">';
		content = content + i18n("manager.jobs.status.ERROR") + ' <span class="badge badge-important" style="margin-left: 5px;"></span>';
		content = content + '</a></li>';
		// for the future... content = content + '<li class="pull-right"><a href="" data-toggle="tab" class="btnjoblistaction">Messages2</a></li>';
		content = content + '</ul>';
		$(jobs.jquery_header).html(content);
		
		$(jobs.jquery_header + ' ul.nav.joblistheader a.btnswitchjoblisttable').click(function() {
			$(this).blur();
			$(jobs.jquery_header + ' ul.nav.joblistheader li').removeClass("active");
			$(this).parent().addClass("active");
			jobs.drawTable();
			
			jobs.refreshHeaderCounters();
			var selected_status = jobs.getSelectedStatusHeaderTab();
			
			for (var job_key in jobs.list) {
				jobs.addRow(jobs.list[job_key], selected_status);
			}
			return false;
		});
		
		$(jobs.jquery_header + ' ul.nav.joblistheader a.btnjoblistaction').click(function() {
			$(this).blur();
			console.log('do something action');
			return false;
		});
		
		$(jobs.jquery_header + ' ul.nav.joblistheader a.btnswitchjoblisttable:first').click();
	};
})(window.mydmam.manager.jobs);

/**
 * PUBLIC
 * prepare(inital_job_list, joblist_jquery_destination, divrefresh_jquery_destination)
 */
(function(jobs) {
	jobs.prepare = function(divrefresh_jquery_destination) {
		/**
		 * Draw refresh btn/hourglass
		 */
		$(divrefresh_jquery_destination).addClass("pull-right");
		$(divrefresh_jquery_destination).css("display", "inline");
		$(divrefresh_jquery_destination).append('<button id="btnjobslistrefresh" class="btn btn-mini pull-right"><i class="icon-refresh"></i></button>');
		$(divrefresh_jquery_destination).append('<img id="imgajaxloaderjobslistrefresh" class="pull-right" style="display: none; margin-right: 10px; margin-top: 3px;" src="' + mydmam.urlimgs.ajaxloader + '" />');
		
		var full_refresh = function() {
			jobs.ajaxRefresh(true);
		};
		
		var since_date_refresh = function() {
			jobs.ajaxRefresh(false);
		};
		
		$('#btnjobslistrefresh').click(full_refresh);
		jobs.refresh_intervaller = setInterval(since_date_refresh, jobs.refresh_delay_time);
	
		/**
		 * Display the content provided by the server in web page.
		 */
		var rawdata = jobs.list;
		jobs.list = {};
		jobs.ajaxRefreshOnsuccess(rawdata, true);
	};
})(window.mydmam.manager.jobs);

/**
 * switchHourglassRefresh(in_refresh)
 */
(function(jobs) {
	jobs.switchHourglassRefresh = function(in_refresh) {
		if (in_refresh) {
			$('#imgajaxloaderjobslistrefresh').css('display', 'inline');
			$('#btnjobslistrefresh').css('display', 'none');
		} else {
			$('#imgajaxloaderjobslistrefresh').css('display', 'none');
			$('#btnjobslistrefresh').css('display', 'inline');
		}
	};
})(window.mydmam.manager.jobs);

/**
 * 
 */
(function(jobs) {
	jobs.ajaxRefreshOnsuccess = function(rawdata, full_refresh) {
		jobs.switchHourglassRefresh(false);
		if (full_refresh) {
			$(jobs.jquery_destination + ' div.alert').remove();
		}
		jobs.last_refresh = new Date().getTime();
		
		if (full_refresh) {
			jobs.last_full_refresh = jobs.last_refresh; 
		}
		
		if (rawdata == null | rawdata.length === 0) {
			if (full_refresh) {
				$(jobs.jquery_destination).prepend('<div class="alert alert-info"><strong>' + i18n("empty") + '</strong></div>');
				jobs.hideTable();
				jobs.list = {};
			} else {
				return;
			}
		} else {
			if (full_refresh) {
				jobs.list = rawdata;
				jobs.clearTable();
			} else {
				var selected_status = jobs.getSelectedStatusHeaderTab();
				for (var job_update_key in rawdata) {
					if (jobs.list[job_update_key]) {
						jobs.updateRow(rawdata[job_update_key], selected_status);
					} else {
						jobs.addRow(rawdata[job_update_key], selected_status);
					}
					jobs.list[job_update_key] = rawdata[job_update_key];
				}
				jobs.refreshHeaderCounters();
			}
		}
	};
})(window.mydmam.manager.jobs);


/**
 * ajaxRefresh(in_refresh)
 */
(function(jobs) {
	jobs.ajaxRefresh = function(full_refresh) {
		var url_refresh = null;
		var data = null;
		
		/**
		 * Check if server and client Unix time are close.
		 */
		var delta_time = Math.abs(jobs.client_time - jobs.server_time);
		if (delta_time > 5000) {
			full_refresh = true;
			console.err("Check client time !", delta_time, new Date(jobs.server_time), new Date(jobs.client_time));
		}
		
		if (jobs.last_refresh === 0) {
			full_refresh = true;
		}
		
		if (jobs.last_full_refresh + (3600 * 1000) < (new Date().getTime())) {
			full_refresh = true;
		}
		
		if (full_refresh) {
			url_refresh = mydmam.manager.url.alljobs;
			data = null;
		} else {
			url_refresh = mydmam.manager.url.recentupdatedjobs;
			data = {"since": jobs.last_refresh};
		}
		
		var onerror = function(jqXHR, textStatus, errorThrown) {
			jobs.switchHourglassRefresh(false);
			$(jobs.jquery_destination + ' div.alert').remove();
			$(jobs.jquery_destination).prepend('<div class="alert"><strong>' + i18n('error') + '</strong></div>');
			jobs.last_refresh = 0;
			jobs.list = {};
			jobs.hideTable();
		};
		
		$.ajax({
			url: url_refresh,
			type: "POST",
			data: data,
			beforeSend: function() {
				jobs.switchHourglassRefresh(true);
			},
			error: onerror,
			success: function(rawdata) {
				jobs.ajaxRefreshOnsuccess(rawdata, full_refresh);
			},
		});
	};
})(window.mydmam.manager.jobs);
