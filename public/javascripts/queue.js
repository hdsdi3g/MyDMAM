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

function Queue() {
	var LONG_MAX = 9223372036854776000;
	var HIGH_PRIORITY = 1000;
	
	var taskjoblist = [];
	var refresh_handle;
	var since_update_list = 0;

	var taskjobstatuslistorder = ["WAITING", "PREPARING", "PROCESSING", "POSTPONED", "STOPPED", "ERROR", "TOO_OLD", "CANCELED", "DONE"];
	var taskjobstatuslistcount = [];

	this.prepare = function() {
		getAll();
		$("#btnrefresh").click(refresh);
		refresh_handle = setInterval(refresh, 10000);
	};
	
	ajaxUpdateLists = function(url, success) {
		$.ajax({
			url: url,
			type: "POST",
			async: false,
			data: {
				"since": since_update_list
			},
			beforeSend: function() {
				$('#imgajaxloader').css("display", "inline");
				$("#btnrefresh").css("display", "none");
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$('#queueplaceholder').after('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n('queue.fetcherror') + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
				clearInterval(refresh_handle);
				$('#imgajaxloader').css("display", "none");
			},
			success: function(data) {
				$('#imgajaxloader').css("display", "none");
				$("#btnrefresh").css("display", "inline");
				if (data.result) {
					since_update_list = new Date().getTime();
					success(data);
				} else {
					console.log(data);
				}
			}
		});
	};
	
	updateExcuseTextIfEmpty = function() {
		if(jQuery.isEmptyObject(taskjoblist)) {
			$('#fulltaskjobtable').html('<div class="alert alert-info" style="margin-bottom: 0px;">' + i18n('queue.notasksinbase') + '</div>');
		} else {
			$('#fulltaskjobtable .alert').remove();
		}
	};

	getAll = function() {
		ajaxUpdateLists(url_getall, function(rawdata) {
			taskjoblist = rawdata.tasksandjobs;
			
			/**
			 * get taskjobstatuslistcount from taskjoblist
			*/
			for (var key in taskjoblist) {
				var current_status = taskjoblist[key].status;
				if (taskjobstatuslistcount[current_status] != null) {
					taskjobstatuslistcount[current_status] = taskjobstatuslistcount[current_status] + 1;
				} else {
					taskjobstatuslistcount[current_status] = 1;
				}
			}
			
			/**
			 * Prepare and display statuses list
			 */
			var content = '';
			content = content + '<div class="well span3" style="padding: 8px 0;">';
			content = content + '<ul id="navlisttaskjobstatus" class="nav nav-list">';
			content = content + '<li class="nav-header">' + i18n('queue.tasksstatus') + '</li>';
			var firsttaskjobstatus = null;
			
			for (var pos = 0; pos < taskjobstatuslistorder.length; pos++) {
				var status = taskjobstatuslistorder[pos];
				var count = 0;
				if (taskjobstatuslistcount[status] != null) {
					count = taskjobstatuslistcount[status];
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

			createfulltaskjobtable(selectOnlyThisStatus(firsttaskjobstatus, taskjoblist));
			
			/**
			 * Create click action for list taskjob status
			 */
			$("#navlisttaskjobstatus").data("selectstatus", firsttaskjobstatus);
			
			for (var pos = 0; pos < taskjobstatuslistorder.length; pos++) {
				var status = taskjobstatuslistorder[pos];
				$('#navlisttaskjobstatus-' + status + ' a').click(function() {
					var status = $(this).data("status");
					createfulltaskjobtable(selectOnlyThisStatus(status, taskjoblist));
					$('#navlisttaskjobstatus li').removeClass("active");
					$('#navlisttaskjobstatus-' + status).addClass("active");
					
					$("#navlisttaskjobstatus").data("selectstatus", status);
				});
			}
			
			updateExcuseTextIfEmpty();

			createTableAllEndedJobs(rawdata.allendedjobs, rawdata.activetriggers);
		});

	};

	createTaskJobTableElementMaxDate = function(current_taskjob) {
		var content = "";
		if (current_taskjob.max_date_to_wait_processing < LONG_MAX) {
			content = content + '<br><span class="label">' + i18n('queue.task.deprecatedat', formatFullDate(current_taskjob.max_date_to_wait_processing)) + '</span> ';
			var maxdate = Math.round((current_taskjob.max_date_to_wait_processing - (new Date().getTime()))/3600000);
			if (maxdate > 0) {
				content = content + i18n('queue.task.todate', maxdate);
			} else {
				content = content + i18n('queue.task.since', Math.abs(maxdate));
			}
		}
		return content;
	};

	addContextItemsinTaskJobTableElement = function(current_taskjob) {
		var content = "";
		for (var keyctx in current_taskjob.context) {
			content = content + '<tr><th>' + keyctx + '</th><td>' + current_taskjob.context[keyctx] + '</td></tr>';//TODO raw display
		}
		return content;
	};
	
	getRequireText = function(task_key_require_done) {
		var content = '';
		if (task_key_require_done !== "") {
			content = content + i18n('queue.task.requires');
			if (taskjoblist[task_key_require_done]) {
				content = content + '<strong>' + taskjoblist[task_key_require_done].name + '</strong> ';
				switch (taskjoblist[task_key_require_done].status) {
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
	
	createTaskJobTableElement = function(current_taskjob) {
		var key = current_taskjob.key;
		var simplekey = current_taskjob.simplekey;
		var content = "";
		content = content + '<div class="row" id="rowtskjb-' + simplekey + '">';
		
		content = content + '<div class="span4">';
		content = content + '<button type="button" class="btn btn-mini" data-toggle="collapse" data-target="#fullview-' + simplekey + '"><i class="icon-chevron-down"></i></button> ';
		content = content + '<span class="profilecategory">' + current_taskjob.profile_category + '</span> :: ';
		content = content + '<strong><span class="taskjobname">' + current_taskjob.name + '</span></strong></div>';

		content = content + '<div class="span4">';

		if (enable_update) {
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
			content = content + '<li><a href="#" class="btntskwait" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setto') + '<strong>' + i18n('WAITING') + '</strong></a></li>';
			display = "";
			
			if (current_taskjob.status == "POSTPONED") {
				display = ' style="display:none"';
			}
			content = content + '<li><a href="#" class="btntskpostponed" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setto') + '<strong>' + i18n('POSTPONED') + '</strong></a></li>';
			display = "";
			
			if (current_taskjob.status == "CANCELED") {
				display = ' style="display:none"';
			}
			content = content + '<li><a href="#" class="btntskcanceled" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setto') + '<strong>' + i18n('CANCELED') + '</strong></a></li>';
			display = "";
			
			if (current_taskjob.priority === 0 || current_taskjob.status == "DONE") {
				display = ' style="display:none"';
			} else {
				content = content + '<li class="divider"></li>';
			}
			content = content + '<li><a href="#" class="btntsk0p" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.setdefaultpriority') + '</a></li>';
			display = "";
			
			if (current_taskjob.priority == HIGH_PRIORITY || current_taskjob.status == "DONE") {
				display = ' style="display:none"';
			} else {
				content = content + '<li class="divider"></li>';
			}
			content = content + '<li><a href="#" class="btntskhighp" data-emkey="' + key + '"' + display + '>' + i18n('queue.task.sethighpriority') +'</a></li>';
			display = "";
			
			if (current_taskjob.max_date_to_wait_processing == LONG_MAX) {
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
		content = content + '<span class="label tjdateupdated">' + i18n('queue.task.dateupdated', formatFullDate(current_taskjob.updatedate)) + '</span></span> ';

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
		
		content = content + '</div>';/** row */

		content = content + '<div id="fullview-' + simplekey + '" class="row collapse" data-emkey="' + key + '">';
		content = content + i18n('queue.task.profile') + ' <span class="profilename">' + current_taskjob.profile_name + '</span><br>';
		content = content + '<span class="requirekey">' + getRequireText(current_taskjob.task_key_require_done) + '</span>';
		content = content + '<span class="label tjdatecreate">' + i18n('queue.task.createdat') + " " + formatFullDate(current_taskjob.create_date) + '</span> ';
		content = content + i18n('queue.task.createby') + ' <strong class="tjcreatorcname">' + current_taskjob.creator_classname + '</strong> ';
		content = content + i18n('queue.task.createon') + ' <strong class="tjcreatorhname">' + current_taskjob.creator_hostname + '</strong>';
		
		if (current_taskjob.start_date) {
			if (current_taskjob.start_date > 0) {
				content = content + '<br><span class="label jobdatestart">' + i18n('queue.task.startedat') + ' ' + formatFullDate(current_taskjob.start_date) + '</span> ';
				if (current_taskjob.end_date > 0) {
					content = content + '<span class="label jobdateend">' + i18n('queue.task.endedat') + ' ' + formatDate(current_taskjob.end_date) + '</span> ';
				}
			}
		} else {
			content = content + '<span class="maxdate">' + createTaskJobTableElementMaxDate(current_taskjob) + '</span>';
		}
		
		content = content + '<br><br>' + i18n('queue.task.context');
		content = content + '<table class="table table-condensed tblcontext" style="margin-bottom: 0.5em;">' + addContextItemsinTaskJobTableElement(current_taskjob) + '</table>';
		
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
		
		content = content + '<div>Key: <code>' + key + '</code></div>';
		if (current_taskjob.processing_error) {
			content = content + '<div style="margin-bottom: 1em;" class="joberror">' + i18n('queue.task.error') + '<br><pre>' + current_taskjob.processing_error + '</pre></div>';
		} else {
			content = content + '<div style="margin-bottom: 1em;" class="joberror"></div>';
		}
		
		content = content + '</div>';/** row */
		return content;
	};
	
	updateTaskJobTableElement = function(current_taskjob) {
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
		if (current_taskjob.priority == HIGH_PRIORITY) {
			$('#' + id_row + " .btntskhighp").css("display", "none");
		} else {
			$('#' + id_row + " .btntskhighp").css("display", "block");
		}
		if (current_taskjob.max_date_to_wait_processing == LONG_MAX) {
			$('#' + id_row + " .btntsknomaxdate").css("display", "none");
		} else {
			$('#' + id_row + " .btntsknomaxdate").css("display", "block");
		}
		
		$('#' + id_row + " .tjdateupdated").html(i18n('queue.task.updatedat') + formatFullDate(current_taskjob.updatedate));

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
		$('#' + id_rowfullview + " .tjdatecreate").html(i18n('queue.task.createdat') + ' ' + formatFullDate(current_taskjob.create_date));
		$('#' + id_rowfullview + " .tjcreatorcname").html(current_taskjob.creator_classname);
		$('#' + id_rowfullview + " .tjcreatorhname").html(current_taskjob.creator_hostname);
		
		if (current_taskjob.start_date) {
			if (current_taskjob.start_date > 0) {
				$('#' + id_rowfullview + " .jobdatestart").html(i18n('queue.task.startedat') + ' ' + formatFullDate(current_taskjob.start_date));
				if (current_taskjob.end_date > 0) {
					$('#' + id_rowfullview + " .jobdateend").html(i18n('queue.task.endedat') + ' ' + formatDate(current_taskjob.end_date));
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

	addActionsForTaskJobTableElement = function(simplekey) {
		$('#rowtskjb-' + simplekey + ' a.btntskpostponed').click(function() {
			changetaskstatus($(this).data("emkey"), "POSTPONED");
		});
		$('#rowtskjb-' + simplekey + ' a.btntskwait').click(function() {
			changetaskstatus($(this).data("emkey"), "WAITING");
		});
		$('#rowtskjb-' + simplekey + ' a.btntskcanceled').click(function() {
			changetaskstatus($(this).data("emkey"), "CANCELED");
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk0p').click(function() {
			changetaskpriority($(this).data("emkey"), 0);
		});
		$('#rowtskjb-' + simplekey + ' a.btntskhighp').click(function() {
			changetaskpriority($(this).data("emkey"), HIGH_PRIORITY);
		});
		$('#rowtskjb-' + simplekey + ' a.btntsknomaxdate').click(function() {
			changetaskmaxage($(this).data("emkey"), 0);				
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk1hrsmaxdate').click(function() {
			changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600));
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk1dmaxdate').click(function() {
			changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600 * 24));
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk1wmaxdate').click(function() {
			changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600 * 24 * 7));
		});
		$('#rowtskjb-' + simplekey + ' a.btntsk30dmaxdate').click(function() {
			changetaskmaxage($(this).data("emkey"), (new Date().getTime()) + (1000 * 3600 * 24 * 30));
		});
	};
	
	createfulltaskjobtable = function(selected_taskjoblist) {
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
			content = content + createTaskJobTableElement(current_taskjob);
		}
		
		$("#fulltaskjobtable").empty();
		$("#fulltaskjobtable").append(content);
		
		if (enable_update) {
			for (var pos in selected_taskjoblist) {
				addActionsForTaskJobTableElement(selected_taskjoblist[pos].simplekey);
			}
		}
	};

	updatenewtaskjobtable = function(selected_taskjoblist) {
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
			content = content + createTaskJobTableElement(current_taskjob);
		}
		
		$("#fulltaskjobtable").prepend(content);
		
		if (enable_update) {
			for (var pos in selected_taskjoblist) {
				addActionsForTaskJobTableElement(selected_taskjoblist[pos].simplekey);
			}
		}
	};
	
	changetaskstatus = function(task_key, status) {
		$.ajax({url: url_changetaskstatus, type: "POST", async: false, data:{"task_key": task_key, "status": status}, success: refresh});
	};
	changetaskpriority = function(task_key, priority) {
		$.ajax({url: url_changetaskpriority, type: "POST", async: false, data:{"task_key": task_key, "priority": priority}, success: refresh});
	};
	changetaskmaxage = function(task_key, date_max_age) {
		$.ajax({url: url_changetaskmaxage, type: "POST", async: false, data:{"task_key": task_key, "date_max_age": date_max_age}, success: refresh});
	};

	createSimpleKey = function(key) {
		return key.substring(7,16);
	};

	selectOnlyThisStatus = function(selectedstatus, selectedtaskjoblist) {
		var taskjobsortedlist = [];
		var current;
		for (var key in selectedtaskjoblist) {
			current = selectedtaskjoblist[key];
			if (current.status == selectedstatus) {
				current.key = key;
				current.simplekey = createSimpleKey(key);
				taskjobsortedlist[key] = current;
			}
		}
		return taskjobsortedlist;
	};

	refresh = function() {
		ajaxUpdateLists(url_getupdate, function(rawdata) {
			var newtasksandjobslist = rawdata.tasksandjobs;
			taskjobstatuslistcount = rawdata.counttasksandjobsstatus;
			
			/**
			 * update taskjobstatuslist counts 
			 */
			for (var pos = 0; pos < taskjobstatuslistorder.length; pos++) {
				var status = taskjobstatuslistorder[pos];
				if (taskjobstatuslistcount[status]) {
					$("#navlisttaskjobstatus-" + status + " .badge").html(taskjobstatuslistcount[status]);
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
					if (taskjoblist[key] != null) {
						if (taskjoblist[key].status == selected_status) {
							updatetasksandjobsforselectedstatus[key] = newtaskjob;
						} else {
							newtasksandjobsforselectedstatus[key] = newtaskjob;
						}
					} else {
						newtasksandjobsforselectedstatus[key] = newtaskjob;
					}
				} else {
					$("#rowtskjb-" + createSimpleKey(key)).remove();
					$("#fullview-" + createSimpleKey(key)).remove();
				}
				taskjoblist[key] = newtaskjob;
			}
			
			for (var pos in rawdata.endlife) {
				var key = rawdata.endlife[pos];
				if (taskjoblist[key]) {
					$("#rowtskjb-" + createSimpleKey(key)).remove();
					$("#fullview-" + createSimpleKey(key)).remove();
					delete taskjoblist[key];
				}
			}
			
			/**
			 * Add new tasks/jobs to current table
			 */
			updatenewtaskjobtable(selectOnlyThisStatus(selected_status, newtasksandjobsforselectedstatus));

			/**
			 * Remove old tasks/jobs from current table
			 */
			for (var key in updatetasksandjobsforselectedstatus) {
				var current = updatetasksandjobsforselectedstatus[key];
				current.key = key;
				current.simplekey = createSimpleKey(key);
				updateTaskJobTableElement(current);
			}

			updateExcuseTextIfEmpty();
			updateTableAllEndedJobs(rawdata.allendedjobs, rawdata.activetriggers);
		});
	};

	createTableAllEndedJobs = function(allendedjobs, activetriggers) {
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
			content = content + addRowTableAllEndedJobs(key, allendedjobs[key], activetriggers[key]);
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

	addRowTableAllEndedJobs = function(key, endedjob, activetriggersforprofile) {
		var content = '<tr id="rowendjob-' + key + '">';
		content = content + '<td>' + endedjob.profile_category + ': ' + endedjob.profile_name + '</td>';
		content = content + '<td><strong class="endedjobname">' + endedjob.name + '</strong></td>';
		content = content + '<td><span class="label" class="endedjobenddateformat">' + formatFullDate(endedjob.end_date) + '</span></td>';
		content = content + '<td class="endedjobenddateraw">' + endedjob.end_date + '</td>';
		content = content + '<td><small><ul class="endedjobcontext">';
		for (var keyctx in endedjob.context) {
			content = content + '<li><strong>' + keyctx + '</strong> :: ' + endedjob.context[keyctx] + '</li>';//TODO raw display
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

	updateTableAllEndedJobs = function(allendedjobs, activetriggers) {
		for (var key in allendedjobs) {
			var endedjob = allendedjobs[key];

			if ($('#rowendjob-' + key).length === 0) {
				/**
				 * New element: destroy table, and recreate it.
				 */
				createTableAllEndedJobs(allendedjobs, activetriggers);
				return;
			} else {
				$('#rowendjob-' + key + ' .endedjobname').html(endedjob.name);
				$('#rowendjob-' + key + ' .endedjobenddateformat').html(formatFullDate(endedjob.end_date));
				$('#rowendjob-' + key + ' .endedjobenddateraw').html(endedjob.end_date);
				var content = "";
				for (var keyctx in endedjob.context) {
					content = content + '<li><strong>' + keyctx + '</strong> :: ' + endedjob.context[keyctx] + '</li>'; //TODO raw display
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
}

/*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
*/

function WorkerManager() {
	var workerlist = [];
	var processingtasks = [];

	this.prepare = function() {
		getAll();
		$("#btnrefresh").click(refresh);
		refresh_handle = setInterval(refresh, 10000);
	};

	ajaxUpdateLists = function(url, success) {
		$.ajax({
			url: url,
			type: "POST",
			async: false,
			beforeSend: function() {
				$('#imgajaxloader').css("display", "inline");
				$("#btnrefresh").css("display", "none");
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$('#queueplaceholder').after('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n('queue.fetcherror') + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
				clearInterval(refresh_handle);
				$('#imgajaxloader').css("display", "none");
			},
			success: function(data) {
				$('#imgajaxloader').css("display", "none");
				$("#btnrefresh").css("display", "inline");
				if (data.result) {
					success(data);
				} else {
					console.log(data);
				}
			}
		});
	};
	
	updateExcuseTextIfEmpty = function() {
		if(jQuery.isEmptyObject(workerlist)) {
			$('#wrk-list').html('<div class="alert alert-info" style="margin-bottom: 0px;">' + i18n('queue.workers.noactivity') + '</div>');
		} else {
			$('#wrk-list .alert').remove();
		}
	};

	getAll = function() {
		ajaxUpdateLists(url_getworkers, function(rawdata) {
			workerlist = rawdata.workers;
			processingtasks = rawdata.processingtasks;
			createfullworkertable();
			updateExcuseTextIfEmpty();
		});
	};
	
	createfullworkertable = function() {
		var table = [];
		
		for (var key in workerlist) {
			workerlist[key].key = key;
			table.push(workerlist[key]);
		}

		var table = table.sort(function(a, b) {
			if (a.hostname == b.hostname) {
				if ((a.cyclic & b.cyclic) | (!a.cyclic & !b.cyclic)) {
					return a.long_worker_name < b.long_worker_name ? -1 : 1;
				} else {
					return a.cyclic > b.cyclic ? -1 : 1;
				}
			} else {
				return a.hostname < b.hostname ? -1 : 1;
			}
		});
		
		$('#wrk-list').empty();
		for (var pos in table) {
			addWorker(table[pos].key, table[pos]);
		}
		
	};
	
	changeworkerstate = function(worker_ref, newstate) {
		$.ajax({url: url_changeworkerstate, type: "POST", async: false, data:{"worker_ref": worker_ref, "newstate": newstate}, success: refresh});
	};

	changeworkercyclicperiod = function(worker_ref, period) {
		$.ajax({url: url_changeworkercyclicperiod, type: "POST", async: false, data:{"worker_ref": worker_ref, "period": period}, success: refresh});
	};

	createSimpleKey = function(key) {
		return key.substring(7,16);
	};

	selectOnlyThisStatus = function(selectedstatus, selectedtaskjoblist) {
		var taskjobsortedlist = [];
		var current;
		for (var key in selectedtaskjoblist) {
			current = selectedtaskjoblist[key];
			if (current.status == selectedstatus) {
				current.key = key;
				current.simplekey = createSimpleKey(key);
				taskjobsortedlist[key] = current;
			}
		}
		return taskjobsortedlist;
	};

	refresh = function() {
		ajaxUpdateLists(url_getworkers, function(rawdata) {
			processingtasks = rawdata.processingtasks;

			var mustfullrefreshworkers = false;
			for (var key in rawdata.workers) {
				if (workerlist[key] == null) {
					mustfullrefreshworkers = true;
					break;
				}
			}
			if (mustfullrefreshworkers === false) {
				for (var key in workerlist) {
					if (rawdata.workers[key] == null) {
						mustfullrefreshworkers = true;
						break;
					}
				}
			}

			workerlist = rawdata.workers;
			
			if (mustfullrefreshworkers) {
				createfullworkertable();
			} else {
				for (var key in workerlist) {
					updateWorker(key, workerlist[key]);
				}
			}
			
			updateExcuseTextIfEmpty();
		});
	};

	updateWorkerStatusButton = function(key, worker) {
		if (enable_update === false) {
			return;
		}

		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey + ' .wkrbtnstatus';
		var current_state = $(jqelement).data('currentstate');
		var content = ''; 
		var addaction = false;
		
		if (worker.statuschange == 'ENABLED') {
			if (current_state != 'setENABLED') {
				$(jqelement).data('currentstate', 'setENABLED');
				$(jqelement).html('<button class="btn btn-info btn-mini disabled" disabled="disabled">' + i18n('queue.worker.starting') + '</button>');
			}
		} else if (worker.statuschange == 'DISABLED') {
			if (current_state != 'setDISABLED') {
				$(jqelement).data('currentstate', 'setDISABLED');
				$(jqelement).html('<button class="btn btn-warning btn-mini disabled" disabled="disabled">' + i18n('queue.worker.stopping') + '</button>');
			}
		} else {
			switch (worker.status) {
				case "PROCESSING":
				case "WAITING":
				if (current_state != 'DISABLED') {
					$(jqelement).data('currentstate', 'DISABLED');
					$(jqelement).data('emkey', key);
					$(jqelement).html('<button class="btn btn-danger btn-mini"><i class="icon-stop icon-white"></i> ' + i18n('queue.worker.stopped') + '</button>');
					addaction = true;
				}
				break;
				case "STOPPED":
				case "PENDING_STOP":
				case "PENDING_CANCEL_TASK":
				if (current_state != 'ENABLED') {
					$(jqelement).data('currentstate', 'ENABLED');
					$(jqelement).data('emkey', key);
					$(jqelement).html('<button class="btn btn-success btn-mini"><i class="icon-play icon-white"></i> ' + i18n('queue.worker.started') + '</button>');
					addaction = true;
				}
				break;
			}
		}
		
		if (addaction) {
			$(jqelement + ' button').click(function() {
				var current_state = $(jqelement).data('currentstate');
				var key = $(jqelement).data('emkey');
				$.ajax({url: url_changeworkerstate, type: "POST", async: false, data:{"worker_ref": key, "newstate": current_state}, success: refresh});
			});
		}
		
	};

	updateWorkerStatus = function(worker) {
		switch (worker.status) {
			case "PROCESSING":
			return '<span class="label label-warning">' + i18n("PROCESSING") + '</span>';
			case "WAITING":
			return '<span class="label label-info">' + i18n("WAITING") + '</span>';
			case "STOPPED":
			return '<span class="badge badge-inverse">' + i18n("STOPPED") + '</span>';
			case "PENDING_STOP":
			return '<span class="label label-important">' + i18n("PENDING_STOP") + '</span>';
			case "PENDING_CANCEL_TASK":
			return '<span class="label label-warning">' + i18n("PENDING_CANCEL_TASK") + '</span>';
		}
		return worker.status;
	};

	updateCyclicWorkerCountdown = function(worker) {
		if (worker.status == 'PROCESSING') {
			if (worker.countdown_to_process > 300) {
				return '<span class="label label-info">Dans ' + worker.countdown_to_process + ' sec</span>';
			} else if (worker.countdown_to_process > 20) {
				return '<span class="label label-warning">Dans ' + worker.countdown_to_process + ' sec</span>';
			} else {
				return '<span class="label label-important">Dans ' + worker.countdown_to_process + ' sec</span>';
			}
		} else {
			return updateWorkerStatus(worker);
		}
	};
	
	addActionsForUpdateCyclicWorkerPeriod = function(key, worker) {
		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey + ' .wkrchngcyclprd';
		
		$(jqelement + ' .btnshowperiodchange').click(function() {
			if ($(jqelement + ' .input-append').css("display") == "none") {
				$(jqelement + ' .input-append').css("display", "inline");
			} else {
				$(jqelement + ' .input-append').css("display", "none");
			}
		});
		
		$(jqelement + ' .btnvalidnewperiod').click(function() {
			var period = $(jqelement + ' input').val();
			$.ajax({url: url_changeworkercyclicperiod, type: "POST", async: false, data:{"worker_ref": key, "period": period}, success: refresh});
		});
	};
	
	updateCyclicWorkerPeriod = function(key, worker) {
		if (worker.time_to_sleep === 0) {
			return;
		}
		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey + ' .wkrchngcyclprd';
		
		if (worker.changecyclicperiod) {
			$(jqelement + ' span.label').html(i18n('queue.worker.newperiod', worker.changecyclicperiod));
			$(jqelement + ' .btnshowperiodchange').css("display", "none");
		} else {
			$(jqelement + ' span.label').html(i18n('queue.worker.period', worker.time_to_sleep));
			$(jqelement + ' input').val(worker.time_to_sleep);
			$(jqelement + ' .btnshowperiodchange').css("display", "inline");
		}
	};
	
	addWorker = function(key, worker){
		var simplekey = createSimpleKey(key);
		var content = "";

		content = content + '<div class="row-fluid';
		if (worker.cyclic) {
			content = content + ' wkrcyclic';
		}
		content = content + '" id="wkr-' + simplekey + '" data-emkey="' + key + '">';
		
		content = content + '<div class="span4">';
		if (worker.cyclic) {
			content = content + '<button class="btn btn-mini disabled" disabled="disabled" type="button"><i class="icon-repeat"></i></button> ';
		} else {
			content = content + '<button class="btn btn-mini" data-target="#wkrfullview-' + simplekey + '" data-toggle="collapse" type="button"><i class="icon-chevron-down"></i></button> ';
		}
		
		content = content + '<strong class="wkrworkerlname">' + worker.long_worker_name + '</strong> on ';
		content = content + '<span class="wkrhostname label label-inverse">' + worker.hostname + '/' + worker.instancename + '</span>';
		content = content + '</div>';
		
		if (worker.cyclic) {
			content = content + '<div class="span1">';
			content = content + '<span class="wkrcountdownpro">' + updateCyclicWorkerCountdown(worker) + '</span> ';
			content = content + '</div>';
			content = content + '<div class="span7">';
			content = content + '<span class="wkrbtnstatus"></span> ';
			if (worker.time_to_sleep > 0) {
				content = content + '<span class="wkrchngcyclprd">';
				content = content + '<span class="label"></span> ';
				if (enable_update) {
					content = content + '<button class="btn btn-mini btnshowperiodchange" data-toggle="button" type="button">' + i18n('queue.worker.edit') + '</button> ';
					content = content + '<div class="input-append" style="display:none">';
					content = content + '<input type="number" class="span2"/>';
					content = content + '<button class="btn btnvalidnewperiod btn-warning" type="button">' + i18n('queue.worker.validate') + '</button>';
					content = content + '</div>';
				}
				content = content + '</span>';
			}
			
			content = content + '</div></div>';
		} else {
			content = content + '<div class="span1">';
			content = content + '<span class="wkrstatus">' + updateWorkerStatus(worker) + '</span> ';
			content = content + '</div>';
			content = content + '<div class="span7">';
			content = content + '<span class="wkrbtnstatus"></span> ';
			if (processingtasks[worker.job]) {
				content = content + '<span class="wkrjob">' + i18n('queue.worker.jobworking') + ' <strong>' +  processingtasks[worker.job].name + '</strong></span>';
			} else {
				content = content + '<span class="wkrjob"></span>';
			}
			content = content + '</div></div>';
			
			content = content + '<div id="wkrfullview-' + simplekey + '" class="collapse"><strong>' + i18n('queue.worker.managedprofiles') + '</strong><ul style="list-style-type:disc">';
			for (var pos in worker.managed_profiles) {
				content = content + '<li>' + worker.managed_profiles[pos].category + ': ' + worker.managed_profiles[pos].name + '</li>';
			}
			content = content + '</ul></div>';
		}
		
		$('#wrk-list').append(content);
		
		updateWorkerStatusButton(key, worker);
		
		if (worker.cyclic) {
			if (worker.time_to_sleep > 0) {
				updateCyclicWorkerPeriod(key, worker);
				addActionsForUpdateCyclicWorkerPeriod(key, worker);
			}
		}
	};
	
	updateWorker = function(key, worker){
		var simplekey = createSimpleKey(key);
		var jqelement = '#wkr-' + simplekey;
		var jqelement_full = '#wkrfullview-' + simplekey;
		
		$(jqelement + ' .wkrworkerlname').html(worker.long_worker_name);
		$(jqelement + ' .wkrhostname').html(worker.hostname + '/' + worker.instancename);
				
		if (worker.cyclic) {
			$(jqelement + ' .wkrcountdownpro').html(updateCyclicWorkerCountdown(worker));
		} else {
			$(jqelement + ' .wkrstatus').html(updateWorkerStatus(worker));
			if (processingtasks[worker.job]) {
				$(jqelement + ' .wkrjob').html(i18n('queue.worker.jobworking') + ' <strong>' +  processingtasks[worker.job].name);
			} else {
				$(jqelement + ' .wkrjob').html('');
			}
			
			$(jqelement_full + ' ul').empty();
			var content = '';
			for (var pos in worker.managed_profiles) {
				content = content + '<li>' + worker.managed_profiles[pos].category + ': ' + worker.managed_profiles[pos].name + '</li>';
			}
			$(jqelement_full + ' ul').append(content);
		}
		
		updateWorkerStatusButton(key, worker);
		
		if (worker.cyclic & (worker.time_to_sleep > 0)) {
			updateCyclicWorkerPeriod(key, worker);
		}
	};

}