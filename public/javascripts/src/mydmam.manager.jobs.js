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
	jobs.view = {};
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
	jobs.refresh_delay_time = 5000;
	jobs.last_full_refresh_delay_time = 3600 * 1000;
	
	jobs.default_max_execution_time = 1000 * 3600 * 24; // 1 day
	jobs.default_max_expiration_time = jobs.default_max_execution_time * 7; //Java is 10 days
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
		if (mydmam.manager.url.jobaction) {
			content = content + '<th>' + i18n('manager.jobs.th.action') + '</th>';
		}
		content = content + '</thead>';
		content = content + '<tbody>';
		content = content + '</tbody>';
		content = content + '</table>';
		
		var colsdef = [
			{"sWidth": "4em", "aTargets": [1]},
			{"sWidth": "5em", "aTargets": [2]},
		];
		if (mydmam.manager.url.jobaction) {
			colsdef.push({"sWidth": "8em", "aTargets": [5]});
		}
		
		$(jobs.jquery_destination).html(content);
		jobs.datatable = $(jobs.jquery_destination + ' table').dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
			"aoColumnDefs": colsdef,
		});
		$(jobs.jquery_destination + ' table td.dataTables_empty').html('<em>' + i18n('manager.jobs.tableisempty') + '</em>');
	};
})(window.mydmam.manager.jobs);

/**
 * addRow(job)
 */
(function(jobs, view) {
	jobs.addRow = function(job, selected_status) {
		if (job.isThisStatus(selected_status) === false) {
			return;
		}
		var cols = [];
		var content = '';
		
		content = content + view.getNameCol(job);
		content = content + '<button class="btn btn-mini pull-right btnshowcollapse"><i class="icon-chevron-down"></i></button>'; 
		cols.push(content);
		
		cols.push(view.getStatusCol(job));
		cols.push(view.getDateCol(job));
		cols.push(view.getParamCol(job));
		cols.push(view.getProgressionCol(job));
		
		if (mydmam.manager.url.jobaction) {
			cols.push(view.getButtonsCol(job));
		}
		
		var datatable = jobs.datatable;
		
		job.web = {
			datatablerowpos: datatable.fnAddData(cols)[0],
			jqueryrow: datatable.$('tr:last'),
			status: job.status,
		};
		
		job.web.jqueryrow.find('button.btnshowcollapse').click(function() {
			job.web.jqueryrow.find('div.collapse').addClass('in');
			$(this).remove();
		});
		
		if (mydmam.manager.url.jobaction) {
			var all_jquery_btnjobaction = job.web.jqueryrow.find('button.btnjobaction');
			all_jquery_btnjobaction.click(function() {
				var jquery_button = $(this);
				var order = jquery_button.data("target-order");
				var key = jquery_button.data("target-key");
				
				var requestactions = {
					order: order,
					jobs_keys: [key],
				};
				jobs.doAction(requestactions, all_jquery_btnjobaction);
			});
		}
	};
})(window.mydmam.manager.jobs, window.mydmam.manager.jobs.view);

/**
 * doAction(requestactions, all_jquery_btnjobaction)
 */
(function(jobs) {
	jobs.doAction = function(requestactions, all_jquery_btnjobaction) {
		$.ajax({
			url: mydmam.manager.url.jobaction,
			type: "POST",
			data: {
				requestactions : JSON.stringify(requestactions),
			},
			beforeSend: function() {
				all_jquery_btnjobaction.addClass('disabled');
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$(jobs.jquery_destination + ' div.alert').remove();
				$(jobs.jquery_destination).prepend('<div class="alert"><strong>' + i18n('error') + '</strong></div>');
			},
			success: function(rawdata) {
				if (rawdata[0]) {
					$(jobs.jquery_destination + ' div.alert').remove();
					$(jobs.jquery_destination).prepend('<div class="alert"><strong>' + i18n('error') + '</strong></div>');
					return;
				}
				
				if (requestactions.order === "delete") {
					for (var pos_jb = 0; pos_jb < requestactions.jobs_keys.length; pos_jb++) {
						var job_key = requestactions.jobs_keys[pos_jb];
						var job_to_delete = jobs.list[job_key];
						if (job_to_delete) {
							jobs.deleteRow(job_to_delete);
							delete jobs.list[job_key];
						}
					}
				}
				
				all_jquery_btnjobaction.removeClass('disabled');
				jobs.ajaxRefreshOnsuccess(rawdata, false);
			},
		});
	};
})(window.mydmam.manager.jobs, window.mydmam.manager.jobs.view);

/**
 * deleteRow(job)
 */
(function(jobs) {
	jobs.deleteRow = function(job) {
		var datatablerowpos = job.web.datatablerowpos;
		for (var job_key in jobs.list) {
			var thisjob = jobs.list[job_key];
			if (!thisjob.web) {
				continue;
			}
			if (datatablerowpos < thisjob.web.datatablerowpos) {
				/**
				 * Re-order row indexes before deleting...
				 */
				thisjob.web.datatablerowpos--;
			}
		}
		jobs.datatable.fnDeleteRow(datatablerowpos);
		job.web = null;
		$(jobs.jquery_destination + ' table td.dataTables_empty').html('<em>' + i18n('manager.jobs.tableisempty') + '</em>');
	};
})(window.mydmam.manager.jobs);

/**
 * updateRow(job)
 */
(function(jobs, view) {
	jobs.updateRow = function(job, selected_status) {
		if (job.isThisStatus(selected_status) === false) {
			/** 
			 * this status is not for this table.
			 */ 
			if (job.web) {
				/**
				 * but job is in this table: status are changed, remove row
				 */
				jobs.deleteRow(job);
			}
			return;
		}
		if (job.web == null) {
			/**
			 * not added in table
			 */
			jobs.addRow(job, selected_status);
		}
		
		var datatable = jobs.datatable;
		if (job.delete_after_completed && job.isThisStatus('DONE')) {
			/**
			 * job will be deleted by some brokers, delete it here.
			 */
			jobs.deleteRow(job);
			return;
		}
		view.update(job);
	};
})(window.mydmam.manager.jobs, window.mydmam.manager.jobs.view);

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
	jobs.drawHeader = function(setselected) {
		var tabindex = 0;
		if (setselected) {
			var selected = setselected[0];
			if (selected === "PREPARING") {
				tabindex = 1;
			} else if (selected === "PROCESSING") {
				tabindex = 1;
			} else if (selected === "DONE") {
				tabindex = 2;
			} else if (selected === "TOO_OLD") {
				tabindex = 3;
			} else if (selected === "STOPPED") {
				tabindex = 4;
			} else if (selected === "TOO_LONG_DURATION") {
				tabindex = 4;
			} else if (selected === "CANCELED") {
				tabindex = 5;
			} else if (selected === "POSTPONED") {
				tabindex = 5;
			} else if (selected === "ERROR") {
				tabindex = 6;
			}
		}
		
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
		
		/*$(jobs.jquery_header + ' ul.nav.joblistheader a.btnjoblistaction').click(function() {
			$(this).blur();
			console.log('do something action');
			return false;
		});*/
		
		$(jobs.jquery_header + ' ul.nav.joblistheader a.btnswitchjoblisttable:eq(' + tabindex + ')').click();
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
 * ajaxRefreshOnsuccess(rawdata, full_refresh)
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
			for (var job_key in rawdata) {
				rawdata[job_key].isThisStatus = function(status) {
					if (Array.isArray(status)) {
						return status.indexOf(this.status) > -1;
					} else {
						return status === this.status;
					}
				};
			}
			
			var selected_status = jobs.getSelectedStatusHeaderTab();
			if (full_refresh) {
				jobs.list = rawdata;
				jobs.drawHeader(selected_status);
			} else {
				for (var job_update_key in rawdata) {
					var actual_job = jobs.list[job_update_key];
					var updated_job = rawdata[job_update_key];
					if (actual_job) {
						if (actual_job.web) {
							updated_job.web = actual_job.web;
						}
						jobs.updateRow(updated_job, selected_status);
					} else {
						jobs.addRow(updated_job, selected_status);
					}
					jobs.list[job_update_key] = updated_job;
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
			console.error("Check client time !", delta_time, new Date(jobs.server_time), new Date(jobs.client_time));
		}
		
		if (jobs.last_refresh === 0) {
			full_refresh = true;
		}
		
		if (jobs.last_full_refresh + jobs.last_full_refresh_delay_time < (new Date().getTime())) {
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
