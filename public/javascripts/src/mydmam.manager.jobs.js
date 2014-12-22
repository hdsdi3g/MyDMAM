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
	jobs.client_time = 0; 
	jobs.server_time = 0; 
	jobs.refresh_intervaller = null;
	jobs.status_list = ["TOO_OLD", "CANCELED", "POSTPONED", "WAITING", "DONE", "PROCESSING", "STOPPED", "ERROR", "PREPARING", "TOO_LONG_DURATION"];
})(window.mydmam.manager.jobs);

/**
 * drawTable
 */
(function(jobs) {
	jobs.drawTable = function() {
		var content = '';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed" style="margin-bottom: 0px;">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('A') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';
		content = content + '</tbody>';
		content = content + '</table>';
		
		$(jobs.jquery_destination).html(content);
		jobs.datatable = $(jobs.jquery_destination + ' table').dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
		});
	};
})(window.mydmam.manager.jobs);

/**
 * addRow(job)
 */
(function(jobs) {
	jobs.addRow = function(job) {
		console.log("add", job);
		jobs.refreshHeaderCounters();
	};
})(window.mydmam.manager.jobs);

/**
 * updateRow(job)
 */
(function(jobs) {
	jobs.updateRow = function(job) {
		console.log("update", job);
		/**
		 * TODO if job.delete_after_completed && job.status === 'DONE'
		 * 
		 * TOO_OLD, CANCELED, POSTPONED, WAITING, DONE, PROCESSING, STOPPED, ERROR, PREPARING, TOO_LONG_DURATION;
		 */
		jobs.refreshHeaderCounters();
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
		content = content + i18n("manager.jobs.status.CANCELEDPOSTPONED") + ' <span class="badge badge-info" style="margin-left: 5px;"></span>';
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
			
			for (var job_key in jobs.list) {
				jobs.addRow(jobs.list[job_key]);
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
		jobs.refresh_intervaller = setInterval(since_date_refresh, 5000);
	
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
				for (var job_update_key in rawdata) {
					if (jobs.list[job_update_key]) {
						jobs.updateRow(rawdata[job_update_key]);
					} else {
						jobs.addRow(rawdata[job_update_key]);
					}
					jobs.list[job_update_key] = rawdata[job_update_key];
				}
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
