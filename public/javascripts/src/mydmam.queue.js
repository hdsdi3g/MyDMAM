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
 * Broker queue operations and display.
 */

/**
 * Prepare queue consts and vars.
 */
(function(mydmam) {
	mydmam.queue = {};
	var queue = mydmam.queue;

	queue.HIGH_PRIORITY = 1000;
	queue.LONG_MAX = 9223372036854776000;
	queue.taskjoblist = [];
	queue.refresh_handle = null;
	queue.since_update_list = 0;
	queue.taskjobstatuslistorder = ["WAITING", "PREPARING", "PROCESSING", "POSTPONED", "STOPPED", "ERROR", "TOO_OLD", "CANCELED", "DONE"];
	queue.taskjobstatuslistcount = [];
	queue.REFRESH_INTERVAL = 10000;
})(window.mydmam);

/**
 * ajaxUpdateLists
 */
(function(queue) {
	queue.ajaxUpdateLists = function(url, success) {
		$.ajax({
			url: url,
			type: "POST",
			async: false,
			data: {
				"since": queue.since_update_list
			},
			beforeSend: function() {
				$('#imgajaxloader').css("display", "inline");
				$("#btnrefresh").css("display", "none");
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$('#queueplaceholder').after('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n('queue.fetcherror') + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
				clearInterval(queue.refresh_handle);
				$('#imgajaxloader').css("display", "none");
			},
			success: function(data) {
				$('#imgajaxloader').css("display", "none");
				$("#btnrefresh").css("display", "inline");
				if (data.result) {
					queue.since_update_list = new Date().getTime();
					success(data);
				} else {
					console.log(data);
				}
			}
		});
	};
})(window.mydmam.queue);

/**
 * updateExcuseTextIfEmpty
 */
(function(queue) {
	queue.updateExcuseTextIfEmpty = function() {
		if(jQuery.isEmptyObject(queue.taskjoblist)) {
			$('#fulltaskjobtable').html('<div class="alert alert-info" style="margin-bottom: 0px;">' + i18n('queue.notasksinbase') + '</div>');
		} else {
			$('#fulltaskjobtable .alert').remove();
		}
	};
})(window.mydmam.queue);

/**
 * getAll : create tables, retrive datas, and populate all
 */
(function(queue) {
	queue.getAll = function() {
		queue.ajaxUpdateLists(url_getall, function(rawdata) {
			queue.taskjoblist = rawdata.tasksandjobs;
			
			// get queue.taskjobstatuslistcount from queue.taskjoblist
			for (var key in queue.taskjoblist) {
				var current_status = queue.taskjoblist[key].status;
				if (queue.taskjobstatuslistcount[current_status] != null) {
					queue.taskjobstatuslistcount[current_status] = queue.taskjobstatuslistcount[current_status] + 1;
				} else {
					queue.taskjobstatuslistcount[current_status] = 1;
				}
			}
			
			// Prepare and display statuses list
			var content = '';
			content = content + '<div class="well span3" style="padding: 8px 0;">';
			content = content + '<ul id="navlisttaskjobstatus" class="nav nav-list">';
			content = content + '<li class="nav-header">' + i18n('queue.tasksstatus') + '</li>';
			var firsttaskjobstatus = null;
			
			for (var pos = 0; pos < queue.taskjobstatuslistorder.length; pos++) {
				var status = queue.taskjobstatuslistorder[pos];
				var count = 0;
				if (queue.taskjobstatuslistcount[status] != null) {
					count = queue.taskjobstatuslistcount[status];
				}
				content = content + '<li id="navlisttaskjobstatus-' + status + '"';
				if (count === 0) {
					content = content + ' style="display:none;"';
				} else {
					if (firsttaskjobstatus == null) {
						content = content + ' class="active"';
						firsttaskjobstatus = status;
					}
				}
				content = content + '><a href="#" data-status="' + status + '">';
				content = content + '<span class="badge" style="margin-right:0.8em;">' + count + '</span>';
				content = content + i18n(status);
				content = content + '</a></li>';
			}
			content = content + '</ul></div>';
			
			content = content + '<div class="span9" id="fulltaskjobtable"></div>';
			$("#tskjob-list").append(content);

			queue.createfulltaskjobtable(queue.selectOnlyThisStatus(firsttaskjobstatus, queue.taskjoblist));
			
			//Create click action for list taskjob status
			$("#navlisttaskjobstatus").data("selectstatus", firsttaskjobstatus);
			
			for (var pos = 0; pos < queue.taskjobstatuslistorder.length; pos++) {
				var status = queue.taskjobstatuslistorder[pos];
				$('#navlisttaskjobstatus-' + status + ' a').click(function() {
					var status = $(this).data("status");
					queue.createfulltaskjobtable(queue.selectOnlyThisStatus(status, queue.taskjoblist));
					$('#navlisttaskjobstatus li').removeClass("active");
					$('#navlisttaskjobstatus-' + status).addClass("active");
					
					$("#navlisttaskjobstatus").data("selectstatus", status);
				});
			}
			
			queue.updateExcuseTextIfEmpty();

			queue.createTableAllEndedJobs(rawdata.allendedjobs, rawdata.activetriggers);
		});
	};
})(window.mydmam.queue);

/**
 * createTaskJobTableElementMaxDate
 */
(function(queue) {
	queue.createTaskJobTableElementMaxDate = function(current_taskjob) {
		var content = "";
		if (current_taskjob.max_date_to_wait_processing < queue.LONG_MAX) {
			content = content + '<br><span class="label">' + i18n('queue.task.deprecatedat', mydmam.format.fulldate(current_taskjob.max_date_to_wait_processing)) + '</span> ';
			var maxdate = Math.round((current_taskjob.max_date_to_wait_processing - (new Date().getTime()))/3600000);
			if (maxdate > 0) {
				content = content + i18n('queue.task.todate', maxdate);
			} else {
				content = content + i18n('queue.task.since', Math.abs(maxdate));
			}
		}
		return content;
	};
})(window.mydmam.queue);

/**
 * fullDisplayContextItem
 */
(function(queue) {
	queue.fullDisplayContextItem = function(context, intable) {
		if (context == null) {
			return "";
		}
		var content = "";
		if (context instanceof Object) {
			if (intable) {
				content = content + '<table class="table table-bordered table-hover table-condensed" style="width:inherit; margin-bottom:inherit;">';
				for (var element in context) {
					content = content + '<tr><th>' + element + '</th><td>' + context[element] + '</td></tr>';
				}
				content = content + '</table>';
			} else {
				for (var element in context) {
					content = content + element + ': ' + context[element] + ', ';
				}
				content = content.substring(0, content.length - 2);
			}
		} else {
			content = content + context;
		}
		return content;
	};
})(window.mydmam.queue);

/**
 * addContextItemsinTaskJobTableElement
 */
(function(queue) {
	queue.addContextItemsinTaskJobTableElement = function(current_taskjob) {
		var content = "";
		for (var keyctx in current_taskjob.context) {
			content = content + '<tr><th>' + keyctx + '</th><td>' + queue.fullDisplayContextItem(current_taskjob.context[keyctx], true) + '</td></tr>';
		}
		return content;
	};
})(window.mydmam.queue);

/**
 * getRequireText
 */
(function(queue) {
	queue.getRequireText = function(task_key_require_done) {
		var content = '';
		if (task_key_require_done !== "") {
			content = content + i18n('queue.task.requires');
			if (queue.taskjoblist[task_key_require_done]) {
				content = content + '<strong>' + queue.taskjoblist[task_key_require_done].name + '</strong> ';
				switch (queue.taskjoblist[task_key_require_done].status) {
				case "WAITING":
					content = content + '<span class="label label-info">' + i18n("WAITING") + '</span>';
					break;
				case "PREPARING":
					content = content + '<span class="label label-warning">' + i18n("PREPARING") + '</span>';
					break;
				case "PROCESSING":
					content = content + '<span class="label label-warning">' + i18n("PROCESSING") + '</span>';
					break;
				case "POSTPONED":
					content = content + '<span class="label">' + i18n("POSTPONED") + '</span>';
					break;
				case "STOPPED":
					content = content + '<span class="badge badge-inverse">' + i18n("STOPPED") + '</span>';
					break;
				case "CANCELED":
					content = content + '<span class="label">' + i18n("CANCELED") + '</span>';
					break;
				case "ERROR":
					content = content + '<span class="label label-important">' + i18n("ERROR") + '</span>';
					break;
				case "TOO_OLD":
					content = content + '<span class="label label-important">' + i18n("TOO_OLD") + '</span>';
					break;
				case "DONE":
					content = content + '<span class="label label-success">' + i18n("DONE") + '</span>';
					break;
				}
			} else {
				content = content + '<code>' + task_key_require_done + '</code>';
			}
			content = content + '<br>';
		}
		return content;
	};
})(window.mydmam.queue);

/**
 * createTaskJobTableElement
 */
(function(queue) {
	queue.createTaskJobTableElement = function(current_taskjob) {
		var key = current_taskjob.key;
		var simplekey = current_taskjob.simplekey;
		var content = "";
		content = content + '<div class="row" id="rowtskjb-' + simplekey + '">';
		
		content = content + '<div class="span4">';
		content = content + '<button type="button" class="btn btn-mini" data-toggle="collapse" data-target="#fullview-' + simplekey + '"><i class="icon-chevron-down"></i></button> ';
		content = content + '<span class="profilecategory">' + current_taskjob.profile_category + '</span> :: ';
		content = content + '<strong><span class="taskjobname">' + current_taskjob.name + '</span></strong></div>';

		content = content + '<div class="span4 blocktaskjobdateedit">';

		if (enable_update_queue) {
			var display; 
			if (current_taskjob.status == "PROCESSING" | current_taskjob.status == "PREPARING") {
				display = ' style="display:none"';
			}
			content = content + '<div class="btn-group" ' + display + '>';
			content = content + '<a class="btn btn-primary dropdown-toggle btn-mini" data-toggle="dropdown" href="#">' + i18n('queue.task.action') +'<span class="caret"></span></a>';
			content = content + '<ul class="dropdown-menu">';
			
			if (current_taskjob.status == "WAITING") {
				display = ' style="display:none"';
			}
			content = content + '<li><a href="#" class="btntskwait" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setto') + ' <strong>' + i18n('WAITING') + '</strong></a></li>';
			display = "";
			
			if (current_taskjob.status == "POSTPONED") {
				display = ' style="display:none"';
			}
			content = content + '<li><a href="#" class="btntskpostponed" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setto') + ' <strong>' + i18n('POSTPONED') + '</strong></a></li>';
			display = "";
			
			if (current_taskjob.status == "CANCELED") {
				display = ' style="display:none"';
			}
			content = content + '<li><a href="#" class="btntskcanceled" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setto') + ' <strong>' + i18n('CANCELED') + '</strong></a></li>';
			display = "";
			
			if (current_taskjob.priority === 0 || current_taskjob.status == "DONE") {
				display = ' style="display:none"';
			} else {
				content = content + '<li class="divider"></li>';
			}
			content = content + '<li><a href="#" class="btntsk0p" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setdefaultpriority') + '</a></li>';
			display = "";
			
			if (current_taskjob.priority == queue.HIGH_PRIORITY || current_taskjob.status == "DONE") {
				display = ' style="display:none"';
			} else {
				content = content + '<li class="divider"></li>';
			}
			content = content + '<li><a href="#" class="btntskhighp" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.sethighpriority') +'</a></li>';
			display = "";
			
			if (current_taskjob.max_date_to_wait_processing == queue.LONG_MAX) {
				display = ' style="display:none"';
			} else {
				content = content + '<li class="divider"></li>';
			}
			content = content + '<li><a href="#" class="btntsknomaxdate" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.unsetmaxdate') +'</a></li>';
			display = "";
			
			if (current_taskjob.status == "DONE") {
				display = ' style="display:none"';
			} else {
				content = content + '<li class="divider"></li>';
			}
			content = content + '<li><a href="#" class="btntsk1hrsmaxdate" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setmaxdateto1hrs') + '</a></li>';
			content = content + '<li><a href="#" class="btntsk1dmaxdate" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setmaxdateto1d') + '</a></li>';
			content = content + '<li><a href="#" class="btntsk1wmaxdate" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setmaxdateto1w') + '</a></li>';
			content = content + '<li><a href="#" class="btntsk30dmaxdate" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setmaxdateto30d') + '</a></li>';
			content = content + '</ul></div> ';
		}
		content = content + '<span class="label tjdateupdated">' + i18n('queue.task.dateupdated', mydmam.format.fulldate(current_taskjob.updatedate)) + '</span></span> ';

		if (current_taskjob.step_count > 0) {
			content = content + '<span class="badge badge-success tjpriority"></span> ';
			content = content + '<strong class="jobstep">' + (current_taskjob.step + 1) + '/' + current_taskjob.step_count + '</strong> ';
			
			content = content + '</div>';
			content = content + '<div class="span2">';
			
			var width = Math.round((current_taskjob.progress * 100) / current_taskjob.progress_size);
			
			var displayclass = '';
			if (current_taskjob.status == 'PROCESSING') {
				displayclass = displayclass + 'progress progress-warning progress-striped active';
			} else if (current_taskjob.status == 'STOPPED') {
				displayclass = displayclass + 'progress progress-info progress-striped';
			} else if (current_taskjob.status == 'ERROR') {
				displayclass = displayclass + 'progress progress-danger progress-striped';
			} else if (current_taskjob.status == 'DONE') {
				if (current_taskjob.progress / current_taskjob.progress_size == 1) {
					displayclass = displayclass + 'hide';
				} else {
					displayclass = displayclass + 'progress progress-success';
				}
			}
			
			if (displayclass !== '') {
				content = content + '<div class="jobprogress ' + displayclass +'"><div class="bar" style="width:' + width + '%;"></div></div> ';
			} else {
				content = content + '<div class="jobprogress">' + width + ' %</div></div> ';
			}
		} else {
			content = content + '<strong class="jobstep"></strong> ';
			content = content + '</div>';
			
			content = content + '<div class="span2">';
			if (current_taskjob.priority > 0) {
				content = content + '<span class="badge badge-success tjpriority">P' + current_taskjob.priority + '</span> ';
			} else {
				content = content + '<span class="badge badge-success tjpriority"></span> ';
			}
			content = content + '<div class="jobprogress"></div> ';
		}
		content = content + '</div>';		
		
		content = content + '</div>';/* row */

		content = content + '<div id="fullview-' + simplekey + '" class="row collapse" data-emkey="' + key + '">';
		content = content + i18n('queue.task.profile') + ' <span class="profilename">' + current_taskjob.profile_name + '</span><br>';
		content = content + '<span class="requirekey">' + queue.getRequireText(current_taskjob.task_key_require_done) + '</span>';
		content = content + '<span class="label tjdatecreate">' + i18n('queue.task.createdat') + " " + mydmam.format.fulldate(current_taskjob.create_date) + '</span> ';
		content = content + i18n('queue.task.createby') + ' <strong class="tjcreatorcname">' + current_taskjob.creator_classname + '</strong> ';
		content = content + i18n('queue.task.createon') + ' <strong class="tjcreatorhname">' + current_taskjob.creator_hostname + '</strong>';
		
		if (current_taskjob.start_date) {
			if (current_taskjob.start_date > 0) {
				content = content + '<br><span class="label jobdatestart">' + i18n('queue.task.startedat') + ' ' + mydmam.format.fulldate(current_taskjob.start_date) + '</span> ';
				if (current_taskjob.end_date > 0) {
					content = content + '<span class="label jobdateend">' + i18n('queue.task.endedat') + ' ' + mydmam.format.date(current_taskjob.end_date) + '</span> ';
				}
			}
		} else {
			content = content + '<span class="maxdate">' + queue.createTaskJobTableElementMaxDate(current_taskjob) + '</span>';
		}
		
		content = content + '<br><br>' + i18n('queue.task.context');
		content = content + '<table class="table table-condensed tblcontext" style="margin-bottom: 0.5em;">' + queue.addContextItemsinTaskJobTableElement(current_taskjob) + '</table>';
		
		if (current_taskjob.delete_after_done) {
			content = content + '<span class="label label-warning jobdeleteafterdone" style="margin-bottom: 1em;">' + i18n('queue.task.deleteafterdone') + '</span>';
		} else {
			content = content + '<span class="label label-warning jobdeleteafterdone"></span>';
		}
		if (current_taskjob.last_message) {
			content = content + '<div class="well well-small joblastmessage">' + current_taskjob.last_message + '</div>';
		} else {
			content = content + '<div class="well well-small joblastmessage" style="display:none"></div>';
		}
		
		content = content + '<div class="taskjobkeyraw">Key: <code>' + key + '</code></div>';
		if (current_taskjob.processing_error) {
			content = content + '<div style="margin-bottom: 1em;" class="joberror">' + i18n('queue.task.error') + '<br><pre>' + current_taskjob.processing_error + '</pre></div>';
		} else {
			content = content + '<div style="margin-bottom: 1em;" class="joberror"></div>';
		}
		
		content = content + '</div>';/** row */
		return content;
	};
})(window.mydmam.queue);

/**
 * updateTaskJobTableElement
 */
(function(queue) {
	queue.updateTaskJobTableElement = function(current_taskjob) {
		var key = current_taskjob.key;
		var simplekey = current_taskjob.simplekey;
		
		var id_row = 'rowtskjb-' + simplekey;
		var id_rowfullview = 'fullview-' + simplekey;

		$('#' + id_row).animate({
			"opacity": 0.7
		}, 100);
		
		$('#' + id_row + " .profilecategory").html(current_taskjob.profile_category);
		$('#' + id_row + " .taskjobname").html(current_taskjob.name);
		
		if (current_taskjob.status == "WAITING") {
			$('#' + id_row + " .btntskwait").css("display", "none");
		} else {
			$('#' + id_row + " .btntskwait").css("display", "block");
		}
		if (current_taskjob.status == "POSTPONED") {
			$('#' + id_row + " .btntskpostponed").css("display", "none");
		} else {
			$('#' + id_row + " .btntskpostponed").css("display", "block");
		}
		if (current_taskjob.status == "CANCELED") {
			$('#' + id_row + " .btntskcanceled").css("display", "none");
		} else {
			$('#' + id_row + " .btntskcanceled").css("display", "block");
		}
		if (current_taskjob.priority === 0) {
			$('#' + id_row + " .btntsk0p").css("display", "none");
		} else {
			$('#' + id_row + " .btntsk0p").css("display", "block");
		}
		if (current_taskjob.priority == queue.HIGH_PRIORITY) {
			$('#' + id_row + " .btntskhighp").css("display", "none");
		} else {
			$('#' + id_row + " .btntskhighp").css("display", "block");
		}
		if (current_taskjob.max_date_to_wait_processing == queue.LONG_MAX) {
			$('#' + id_row + " .btntsknomaxdate").css("display", "none");
		} else {
			$('#' + id_row + " .btntsknomaxdate").css("display", "block");
		}
		
		$('#' + id_row + " .tjdateupdated").html(i18n('queue.task.updatedat') + mydmam.format.fulldate(current_taskjob.updatedate));

		if (current_taskjob.step_count > 0) {
			$('#' + id_row + " .tjpriority").empty();
			$('#' + id_row + " .jobstep").html((current_taskjob.step + 1) + '/' + current_taskjob.step_count);
			
			var width = Math.round((current_taskjob.progress * 100) / current_taskjob.progress_size);
			
			var displayprogress = false;
			if (current_taskjob.status == 'PROCESSING') {
				$('#' + id_row + " .jobprogress").addClass('progress progress-warning progress-striped active');
				$('#' + id_row + " .jobprogress").removeClass('progress-success progress-info progress-danger hide');
				displayprogress = true;
			} else if (current_taskjob.status == 'STOPPED') {
				$('#' + id_row + " .jobprogress").addClass('progress progress-info progress-striped');
				$('#' + id_row + " .jobprogress").removeClass('progress-success progress-warning progress-danger active hide');
				displayprogress = true;
			} else if (current_taskjob.status == 'ERROR') {
				$('#' + id_row + " .jobprogress").addClass('progress progress-danger progress-striped');
				$('#' + id_row + " .jobprogress").removeClass('progress-info progress-success progress-warning active hide');
				displayprogress = true;
			} else if (current_taskjob.status == 'DONE') {
				if (current_taskjob.progress / current_taskjob.progress_size == 1) {
					$('#' + id_row + " .jobprogress").addClass('hide');
					$('#' + id_row + " .jobprogress").removeClass('progress progress-success progress-danger progress-info progress-warning progress-striped active hide');
				} else {
					$('#' + id_row + " .jobprogress").addClass('progress progress-success');
					$('#' + id_row + " .jobprogress").removeClass('progress-info progress-warning progress-danger progress-striped active hide');
				}
				displayprogress = true;
			}
			
			if (displayprogress) {
				$('#' + id_row + " .jobprogress .bar").css('width', width + '%');
			} else {
				$('#' + id_row + " .jobprogress").html(width + ' %');
			}
		} else {
			$('#' + id_row + " .jobstep").empty();
			
			if (current_taskjob.priority > 0) {
				$('#' + id_row + " .tjpriority").html('P' + current_taskjob.priority);
			} else {
				$('#' + id_row + " .tjpriority").empty();
			}
			$('#' + id_row + " .jobprogress").empty();
		}

		$('#' + id_rowfullview + " .profilename").html(current_taskjob.profile_name);
		$('#' + id_rowfullview + " .requirekey").html(getRequireText(current_taskjob.task_key_require_done));
		$('#' + id_rowfullview + " .tjdatecreate").html(i18n('queue.task.createdat') + ' ' + mydmam.format.fulldate(current_taskjob.create_date));
		$('#' + id_rowfullview + " .tjcreatorcname").html(current_taskjob.creator_classname);
		$('#' + id_rowfullview + " .tjcreatorhname").html(current_taskjob.creator_hostname);
		
		if (current_taskjob.start_date) {
			if (current_taskjob.start_date > 0) {
				$('#' + id_rowfullview + " .jobdatestart").html(i18n('queue.task.startedat') + ' ' + mydmam.format.fulldate(current_taskjob.start_date));
				if (current_taskjob.end_date > 0) {
					$('#' + id_rowfullview + " .jobdateend").html(i18n('queue.task.endedat') + ' ' + mydmam.format.date(current_taskjob.end_date));
				}
			}
		} else {
			$('#' + id_rowfullview + " .maxdate").html(createTaskJobTableElementMaxDate(current_taskjob));
		}
		
		$('#' + id_rowfullview + " .tblcontext").empty();
		$('#' + id_rowfullview + " .tblcontext").append(addContextItemsinTaskJobTableElement(current_taskjob));
		
		
		if (current_taskjob.delete_after_done) {
			$('#' + id_rowfullview + " .jobdeleteafterdone").html(i18n('queue.task.deleteafterdone'));
			$('#' + id_rowfullview + " .jobdeleteafterdone").css("margin-bottom","1em");
		} else {
			$('#' + id_rowfullview + " .jobdeleteafterdone").html("");
			$('#' + id_rowfullview + " .jobdeleteafterdone").css("margin-bottom","0em");
		}
		if (current_taskjob.last_message) {
			$('#' + id_rowfullview + " .joblastmessage").html(current_taskjob.last_message);
			$('#' + id_rowfullview + " .joblastmessage").css("display", "block");
		} else {
			$('#' + id_rowfullview + " .joblastmessage").html("");
			$('#' + id_rowfullview + " .joblastmessage").css("display", "none");
		}

		if (current_taskjob.processing_error) {
			$('#' + id_rowfullview + " .joberror").html(i18n('queue.task.error') + '<br><pre>' + current_taskjob.processing_error + '</pre></div>');
		} else {
			$('#' + id_rowfullview + " .joberror").html('');
		}
		
		$('#' + id_row).animate({
			"opacity": 1
		}, 100);
	};
})(window.mydmam.queue);

/**
 * addActionsForTaskJobTableElement
 */
(function(queue) {
	queue.addActionsForTaskJobTableElement = function(simplekey) {
		$('#rowtskjb-' + simplekey + ' a.btntskpostponed').click(function() {
			queue.changetaskstatus($(this).data("emkey"), "POSTPONED");
		});
		$('#rowtskjb-' + simplekey + ' a.btntskwait').click(function() {
			queue.changetaskstatus($(this).data("emkey"), "WAITING");
		});
		$('#rowtskjb-' + simplekey + ' a.btntskcanceled').click(function() {
			queue.changetaskstatus($(this).data("emkey"), "CANCELED");
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk0p').click(function() {
			queue.changetaskpriority($(this).data("emkey"), 0);
		});
		$('#rowtskjb-' + simplekey + ' a.btntskhighp').click(function() {
			queue.changetaskpriority($(this).data("emkey"), queue.HIGH_PRIORITY);
		});
		$('#rowtskjb-' + simplekey + ' a.btntsknomaxdate').click(function() {
			queue.changetaskmaxage($(this).data("emkey"), 0);				
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk1hrsmaxdate').click(function() {
			queue.changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600));
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk1dmaxdate').click(function() {
			queue.changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600 * 24));
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk1wmaxdate').click(function() {
			queue.changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600 * 24 * 7));
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk30dmaxdate').click(function() {
			queue.changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600 * 24 * 30));
		});
	};
})(window.mydmam.queue);

/**
 * createfulltaskjobtable
 */
(function(queue) {
	queue.createfulltaskjobtable = function(selected_taskjoblist) {
		var table = [];
		for (var key in selected_taskjoblist) {
			table.push(selected_taskjoblist[key]);
		}
		
		var table = table.sort(function(a, b) {
			return a.updatedate > b.updatedate ? -1 : 1;
		});
		
		var content = "";
		for (var pos in table) {
			var current_taskjob = table[pos];
			content = content + queue.createTaskJobTableElement(current_taskjob);
		}
		
		$("#fulltaskjobtable").empty();
		$("#fulltaskjobtable").append(content);
		
		if (enable_update_queue) {
			for (var pos in selected_taskjoblist) {
				queue.addActionsForTaskJobTableElement(selected_taskjoblist[pos].simplekey);
			}
		}
	};
})(window.mydmam.queue);

/**
 * updatenewtaskjobtable
 */
(function(queue) {
	queue.updatenewtaskjobtable = function(selected_taskjoblist) {
		var table = [];
		for (var key in selected_taskjoblist) {
			table.push(selected_taskjoblist[key]);
		}
		
		var table = table.sort(function(a, b) {
			return a.updatedate > b.updatedate ? -1 : 1;
		});
		
		var content = "";
		for (var pos in table) {
			var current_taskjob = table[pos];
			content = content + queue.createTaskJobTableElement(current_taskjob);
		}
		
		$("#fulltaskjobtable").prepend(content);
		
		if (enable_update_queue) {
			for (var pos in selected_taskjoblist) {
				queue.addActionsForTaskJobTableElement(selected_taskjoblist[pos].simplekey);
			}
		}
	};
})(window.mydmam.queue);

/**
 * changetaskstatus, changetaskpriority, changetaskmaxage
 */
(function(queue) {
	queue.changetaskstatus = function(task_key, status) {
		$.ajax({url: url_changetaskstatus, type: "POST", async: false, data:{"task_key": task_key, "status": status}, success: queue.refresh});
	};
	queue.changetaskpriority = function(task_key, priority) {
		$.ajax({url: url_changetaskpriority, type: "POST", async: false, data:{"task_key": task_key, "priority": priority}, success: queue.refresh});
	};
	queue.changetaskmaxage = function(task_key, date_max_age) {
		$.ajax({url: url_changetaskmaxage, type: "POST", async: false, data:{"task_key": task_key, "date_max_age": date_max_age}, success: queue.refresh});
	};
})(window.mydmam.queue);

/**
 * createSimpleKey
 */
(function(queue) {
	queue.createSimpleKey = function(key) {
		return key.substring(7,16);
	};
})(window.mydmam.queue);

/**
 * selectOnlyThisStatus
 */
(function(queue) {
	queue.selectOnlyThisStatus = function(selectedstatus, selectedtaskjoblist) {
		var taskjobsortedlist = [];
		var current;
		for (var key in selectedtaskjoblist) {
			current = selectedtaskjoblist[key];
			if (current.status == selectedstatus) {
				current.key = key;
				current.simplekey = queue.createSimpleKey(key);
				taskjobsortedlist[key] = current;
			}
		}
		return taskjobsortedlist;
	};
})(window.mydmam.queue);

/**
 * refresh
 */
(function(queue) {
	queue.refresh = function() {
		queue.ajaxUpdateLists(url_getupdate, function(rawdata) {
			var newtasksandjobslist = rawdata.tasksandjobs;
			queue.taskjobstatuslistcount = rawdata.counttasksandjobsstatus;
			
			/**
			 * update taskjobstatuslist counts 
			 */
			for (var pos = 0; pos < queue.taskjobstatuslistorder.length; pos++) {
				var status = queue.taskjobstatuslistorder[pos];
				if (queue.taskjobstatuslistcount[status]) {
					$("#navlisttaskjobstatus-" + status + " .badge").html(queue.taskjobstatuslistcount[status]);
					$("#navlisttaskjobstatus-" + status).css("display", "block");
				} else {
					$("#navlisttaskjobstatus-" + status).css("display", "none");
				}
			}
			
			var newtasksandjobsforselectedstatus = [];
			var updatetasksandjobsforselectedstatus = [];
			
			/**
			 * Update table for selected status
			 */
			var selected_status = $("#navlisttaskjobstatus").data("selectstatus");
			for (var key in newtasksandjobslist) {
				var newtaskjob = newtasksandjobslist[key];
				if (newtaskjob.status == selected_status) {
					if (queue.taskjoblist[key] != null) {
						if (queue.taskjoblist[key].status == selected_status) {
							updatetasksandjobsforselectedstatus[key] = newtaskjob;
						} else {
							newtasksandjobsforselectedstatus[key] = newtaskjob;
						}
					} else {
						newtasksandjobsforselectedstatus[key] = newtaskjob;
					}
				} else {
					$("#rowtskjb-" + queue.createSimpleKey(key)).remove();
					$("#fullview-" + queue.createSimpleKey(key)).remove();
				}
				queue.taskjoblist[key] = newtaskjob;
			}
			
			for (var pos in rawdata.endlife) {
				var key = rawdata.endlife[pos];
				if (queue.taskjoblist[key]) {
					$("#rowtskjb-" + queue.createSimpleKey(key)).remove();
					$("#fullview-" + queue.createSimpleKey(key)).remove();
					delete queue.taskjoblist[key];
				}
			}
			
			/**
			 * Add new tasks/jobs to current table
			 */
			queue.updatenewtaskjobtable(queue.selectOnlyThisStatus(selected_status, newtasksandjobsforselectedstatus));

			/**
			 * Remove old tasks/jobs from current table
			 */
			for (var key in updatetasksandjobsforselectedstatus) {
				var current = updatetasksandjobsforselectedstatus[key];
				current.key = key;
				current.simplekey = queue.createSimpleKey(key);
				queue.updateTaskJobTableElement(current);
			}

			queue.updateExcuseTextIfEmpty();
			queue.updateTableAllEndedJobs(rawdata.allendedjobs, rawdata.activetriggers);
		});
	};
})(window.mydmam.queue);

/**
 * createTableAllEndedJobs
 */
(function(queue) {
	queue.createTableAllEndedJobs = function(allendedjobs, activetriggers) {
		var content = '<table id="allendedjobs-tlb" class="table table-bordered table-hover table-condensed">';
		content = content + '<thead><tr>';
		content = content + '<th>' + i18n('queue.endedjobs.profile') + '</th>';
		content = content + '<th>' + i18n('queue.endedjobs.name') + '</th>';
		content = content + '<th>' + i18n('queue.endedjobs.date') + '</th><td></td>';
		content = content + '<th>' + i18n('queue.endedjobs.context') + '</th>';
		content = content + '<th>' + i18n('queue.endedjobs.message') + '</th>';
		content = content + '<th>' + i18n('queue.endedjobs.observers') + '</th>';
		content = content + '</tr></thead>';

		content = content + '<tbody>';
		for (var key in allendedjobs) {
			content = content + queue.addRowTableAllEndedJobs(key, allendedjobs[key], activetriggers[key]);
		}
		content = content + '</tbody>';
		content = content + "</table>";
		$("#allendedjobs-list").html(content);

		$("#allendedjobs-tlb").dataTable({
			"bPaginate": false,
			"bLengthChange": false,
			"bSort": true,
			"bInfo": false,
			"bAutoWidth": false,
			"bFilter": true,
			"aoColumnDefs": [
				{"iDataSort": 3, "aTargets": [2], "bSearchable": false}, //date
				{"bVisible": false, "bSearchable": false, "aTargets": [3]}, //date
			]
		});
	};
})(window.mydmam.queue);

/**
 * addRowTableAllEndedJobs
 */
(function(queue) {
	queue.addRowTableAllEndedJobs = function(key, endedjob, activetriggersforprofile) {
		var content = '<tr id="rowendjob-' + key + '">';
		content = content + '<td>' + endedjob.profile_category + ': ' + endedjob.profile_name + '</td>';
		content = content + '<td><strong class="endedjobname">' + endedjob.name + '</strong></td>';
		content = content + '<td><span class="label" class="endedjobenddateformat">' + mydmam.format.fulldate(endedjob.end_date) + '</span></td>';
		content = content + '<td class="endedjobenddateraw">' + endedjob.end_date + '</td>';
		content = content + '<td><small><ul class="endedjobcontext">';
		for (var keyctx in endedjob.context) {
			content = content + '<li><strong>' + keyctx + '</strong> :: ' + queue.fullDisplayContextItem(endedjob.context[keyctx]) + '</li>';
		}
		content = content + '</ul></small></td>';
		content = content + '<td class="endedjobendlastm">' + endedjob.last_message + '</td>';
		content = content + '<td><div class="activetriggers">';
		if (activetriggersforprofile) {
			for (var triggerkey in activetriggersforprofile) {
				content = content + '<i class="icon-magnet"></i>&nbsp;' + activetriggersforprofile[triggerkey].longname + '<br>';
			}
		}
		content = content + '</div></td>';
		content = content + '</tr>';
		return content;
	};
})(window.mydmam.queue);

/**
 * updateTableAllEndedJobs
 */
(function(queue) {
	queue.updateTableAllEndedJobs = function(allendedjobs, activetriggers) {
		for (var key in allendedjobs) {
			var endedjob = allendedjobs[key];

			if ($('#rowendjob-' + key).length === 0) {
				/**
				 * New element: destroy table, and recreate it.
				 */
				queue.createTableAllEndedJobs(allendedjobs, activetriggers);
				return;
			} else {
				$('#rowendjob-' + key + ' .endedjobname').html(endedjob.name);
				$('#rowendjob-' + key + ' .endedjobenddateformat').html(mydmam.format.fulldate(endedjob.end_date));
				$('#rowendjob-' + key + ' .endedjobenddateraw').html(endedjob.end_date);
				var content = "";
				for (var keyctx in endedjob.context) {
					content = content + '<li><strong>' + keyctx + '</strong> :: ' + this.fullDisplayContextItem(endedjob.context[keyctx]) + '</li>';
				}
				$('#rowendjob-' + key + ' .endedjobcontext').html(content);
				$('#rowendjob-' + key + ' .endedjobendlastm').html(endedjob.last_message);

				content = '';
				if (activetriggers[key]) {
					for (var triggerkey in activetriggers[key]) {
						content = content + '<i class="icon-magnet"></i>&nbsp;' + activetriggers[key][triggerkey].longname + '<br>';
					}
					$('#rowendjob-' + key + ' .activetriggers').html(content);
				} else {
					$('#rowendjob-' + key + ' .activetriggers').empty();
				}
			}
		}

	};
})(window.mydmam.queue);

/**
 * display
 */
(function(queue) {
	queue.display = function() {
		queue.getAll();
		$("#btnrefresh").click(queue.refresh);
		queue.refresh_handle = setInterval(queue.refresh, queue.REFRESH_INTERVAL);
	};
	
})(window.mydmam.queue);
