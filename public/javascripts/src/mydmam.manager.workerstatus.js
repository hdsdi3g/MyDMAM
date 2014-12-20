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
 * refresh(query_destination)
 */
(function(workerstatus) {
	
	workerstatus.refresh_intervaller = null;
	
	workerstatus.refresh = function(query_destination) {
		var db = {
			last_workers: {},
			datatable_workers_position: {},
			datatable: null,
		};
		
		var content = '';
		content = content + '<div class="tableworkers"></div>';
		content = content + '<div class="refreshhourglass muted" style="visibility: hidden;"><small>' + i18n('manager.workers.inrefresh') + '</small></div>';
		$(query_destination).html(content);
		var refresh = function() {
			workerstatus.do_refresh(query_destination, db);
		};
		if (workerstatus.refresh_intervaller == null) {
			workerstatus.refresh_intervaller = setInterval(refresh, 5000);
		}
		refresh();
	};
})(window.mydmam.manager.workerstatus);

/**
 * drawTable(query_destination)
 * @return datatable
 */
(function(workerstatus) {
	workerstatus.drawTable = function(query_destination) {
		var content = '';
		content = content + '<table class="table table-striped table-bordered table-hover table-condensed" style="margin-bottom: 0px;">';
		content = content + '<thead>';
		content = content + '<th>' + i18n('instance') + '</th>';
		content = content + '<th>' + i18n('name') + '</th>';
		content = content + '<th>' + i18n('state') + '</th>';
		content = content + '<th>' + i18n('capablities') + '</th>';
		content = content + '</thead>';
		content = content + '<tbody>';
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
		
		return datatable;
	};
})(window.mydmam.manager.workerstatus);

/**
 * do_refresh(query_destination)
 */
(function(workerstatus) {
	workerstatus.do_refresh = function(query_destination, db) {
		var reset = function() {
			db.last_workers = {};
			db.datatable_workers_position = {};
			db.datatable = null;
		};
		
		$.ajax({
			url: mydmam.manager.url.allworkers,
			type: "POST",
			beforeSend: function() {
				$(query_destination + " div.refreshhourglass").css('visibility', 'visible');
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$(query_destination + " div.refreshhourglass").css('visibility', 'hidden');
				$(query_destination + ' div.tableworkers').html('<div class="alert"><strong>' + i18n('manager.workers.refresherror') + '</strong></div>');
				reset();
			},
			success: function(rawdata) {
				$(query_destination + " div.refreshhourglass").css('visibility', 'hidden');
				if (rawdata == null | rawdata.length === 0) {
					$(query_destination + ' div.tableworkers').html('<div class="alert alert-info"><strong>' + i18n("manager.workers.empty") + '</strong></div>');
					reset();
					return;
				}
				workerstatus.update(rawdata, db, query_destination);
			}
		});
	};
})(window.mydmam.manager.workerstatus);

/**
 * update(workers, db, query_destination)
 */
(function(workerstatus) {
	workerstatus.update = function(workers, db, query_destination) {
		if (db.datatable == null) {
			db.datatable = workerstatus.drawTable(query_destination);
		}
		
		var current_workers = {};
		
		for (var pos_wr in workers) {
			var worker = workers[pos_wr];
			var key = worker.reference_key;
			if (!db.last_workers[key]) {
				var new_item_pos = datatable.fnAddData(workerstatus.getColsToAddData(worker, pos_wr));
				db.datatable_workers_position[key] = new_item_pos[0];
			}
			workerstatus.updateStatus(query_destination, worker, pos_wr);
			current_workers[key] = worker;
			db.last_workers[key] = worker;
		}
		
		workerstatus.set_btn_event_in_collapse(query_destination);
		
		for (var key in db.last_workers) {
			if (!current_workers[key]) {
				// console.log('remove', last_workers[key]);
				datatable.fnDeleteRow(db.datatable_workers_position[key]);
				delete db.datatable_workers_position[key];
				delete db.last_workers[key];
			}
		}
		return db;
	};
})(window.mydmam.manager.workerstatus);

/**
 * getColsToAddData(worker, pos_wr)
 */
(function(workerstatus) {
	workerstatus.getColsToAddData = function(worker, pos_wr) {
		var cols = [];
		
		var instance = worker.manager_reference.members;
		var row_class = 'wkrsrow-' + pos_wr;
		
		var content = '';
		content = content + '<strong>' +instance.app_name + '</strong>&nbsp;&bull; ';
		content = content + instance.instance_name + ' ';
		content = content + '<button class="btn btn-mini btnincollapse pull-right notyetsetevent"';
		content = content + ' data-collapsetarget="' + row_class + '"';
		//content = content + ' data-workerref="' + worker.reference_key + '"';
		//content = content + ' data-instanceref="' + instance.instance_ref + '"';
		content = content + '>';
		content = content + '<i class="icon-chevron-down"></i>';
		content = content + '</button><br/>';
		content = content + '<div class="collapse notyetcollapsed ' + row_class + '">';
		content = content + '<small>' + instance.host_name + '</small>';
		content = content + '</div>'; // collapse
		cols.push(content);
		
		content = '';
		content = content + worker.long_name;
		content = content + '<div class="collapse notyetcollapsed ' + row_class + '">';
		content = content + '<span class="label label-inverse">' + i18n('manager.workers.category.' + worker.category) + '</span><br/>';
		content = content + '<small>' + worker.vendor_name + '</small><br/>';
		var worker_class_name = worker.worker_class.substring(worker.worker_class.lastIndexOf('.') + 1, worker.worker_class.length);
		content = content + '<abbr title="' + worker.worker_class + '">' + worker_class_name + '.class</abbr>';
		content = content + '</div>'; // collapse
		cols.push(content);

		content = '';
		content = content + '<div class="workerstate ' + row_class + '"></div>';
		content = content + '<div class="collapse notyetcollapsed currentjobkey ' + row_class + '"></div>';
		cols.push(content);
		
		content = '';
		content = content + '<small>';
		var capablities = worker.capablities;
		for (var pos_wcap = 0; pos_wcap < capablities.length; pos_wcap++) {
			var capablity = capablities[pos_wcap];
			var job_context_avaliable_class_name = capablity.job_context_avaliable.substring(capablity.job_context_avaliable.lastIndexOf('.') + 1, capablity.job_context_avaliable.length);
			content = content + '&bull; <abbr title="' + capablity.job_context_avaliable + '">' + job_context_avaliable_class_name + '.class</abbr>';
			
			content = content + '<div class="collapse notyetcollapsed ' + row_class + '">';
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
		content = content + '</small>';
		cols.push(content);
		
		return cols;
	};
})(window.mydmam.manager.workerstatus);


/**
 * updateStatus(worker, pos_wr)
 */
(function(workerstatus) {
	workerstatus.updateStatus = function(query_destination, worker, pos_wr) {
		var row_class = 'wkrsrow-' + pos_wr;
		var div_workerstate = $(query_destination + ' div.tableworkers div.workerstate.' + row_class);
		var div_currentjobkey = $(query_destination + ' div.tableworkers div.currentjobkey.' + row_class);
		
		content = '';
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
		div_workerstate.html(content);
		
		var instance = worker.manager_reference.members;
		content = '';
		if (worker.current_job_key) {
			content = content + '<small>' + i18n('manager.workers.currentjob', worker.current_job_key) + '</small>';
		}
		div_currentjobkey.html(content);
	};
})(window.mydmam.manager.workerstatus);



/**
 * set_btn_event_in_collapse(query_destination)
 */
(function(workerstatus) {
	btn_event_in_collapse = function() {
		var query_destination = $(this).data("query_destination");
		var row_class = $(this).data("collapsetarget");
		
		var all_collapse = $(query_destination + ' div.tableworkers div.collapse.notyetcollapsed.' + row_class);
		all_collapse.addClass('in');
		all_collapse.removeClass('notyetcollapsed');
		$(this).remove();
		return true;
	};
	
	workerstatus.set_btn_event_in_collapse = function(query_destination) {
		$(query_destination + ' div.tableworkers tr button.btn.btn-mini.btnincollapse.notyetsetevent').click(btn_event_in_collapse).data("query_destination", query_destination);
	};
})(window.mydmam.manager.workerstatus);
