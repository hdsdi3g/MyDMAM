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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
/*jshint eqnull:true, loopfunc:true, shadow:true, jquery:true */

(function(mydmam) {
	if(!mydmam.notification){mydmam.notification = {};}
	if(!mydmam.notification.url){mydmam.notification.url = {};}
})(window.mydmam);

/**
 * ajaxError
 */
(function(notification) {
	notification.ajaxError = function(title, textStatus, errorThrown) {
		$('#errorplaceholder').append('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + title + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
	};
})(window.mydmam.notification);


/**
 * setReadNotification
 */
(function(notification) {
	notification.setReadNotification = function() {
		if ($(this).hasClass("btnsetreadnotification") === false) {
			return;
		}
		$(this).removeClass("btnsetreadnotification");
		var notificationkey = $(this).data("notificationkey");
		var url = notification.url.notificationupdateread.replace("keyparam1", notificationkey);
		
		$.ajax({
			url: url,
			type: "GET",
			async: true,
			error: function(jqXHR, textStatus, errorThrown) {
				notification.ajaxError(i18n("userprofile.notifications.errorduringsetread"), textStatus, errorThrown);
			},
			success: function(data) {
				if (!data.result) {
					console.log(data);
				}
			}
		});
	};
})(window.mydmam.notification);

/**
 * getAndDisplayUsers
 */
(function(notification) {
	notification.getAndDisplayUsers = function() {

		/**
		 * displayUser
		 * Transform cyphed user key by a link to user
		 */
		var displayUser = function(dom_element, ajaxdata) {
			var key = $(dom_element).text();
			if(ajaxdata[key]) {
				var content = '<a href="mailto:' + ajaxdata[key].name + '<' + ajaxdata[key].mail + '>">' + ajaxdata[key].name + '</a>';
				$(dom_element).html(content);
			} else {
				$(dom_element).html(i18n("userprofile.notifications.unknown"));
			}
		};

		var key;
		var users_crypted_keys = [];
		var dom_element_list = notification.dom_element_list_for_user_to_resolve;
		for (var pos in dom_element_list) {
			key = $(dom_element_list[pos]).text();
			if (users_crypted_keys.indexOf(key) === -1) {
				users_crypted_keys.push(key);
			}
		}

		$.ajax({
			url: notification.url.notificationresolveusers,
			type: "POST",
			async: false,
			data: {
				"users_crypted_keys": users_crypted_keys
			},
			error: function(jqXHR, textStatus, errorThrown) {
				notification.ajaxError(i18n("userprofile.notifications.errorduringgetusers"), textStatus, errorThrown);
			},
			success: function(data) {
				$(".userprofilekey").each(function() {
					displayUser(this, data);
				});
			}
		});
	};
})(window.mydmam.notification);

/**
 * displayStatus
 */
(function(notification) {
	notification.displayStatus = function(status) {
		var clazz = "";
		if (status === "WAITING") {
			clazz = "badge-info";
		} else if (status === "PREPARING") {
			clazz = "badge-warning";
		} else if (status === "PROCESSING") {
			clazz = "badge-warning";
		} else if (status === "POSTPONED") {
			clazz = "badge-info";
		} else if (status === "STOPPED") {
			clazz = "badge-inverse";
		} else if (status === "CANCELED") {
			clazz = "badge-important";
		} else if (status === "ERROR") {
			clazz = "badge-important";
		} else if (status === "TOO_OLD") {
			clazz = "badge-important";
		} else if (status === "DONE") {
			clazz = "badge-success";
		}
		return '<span class="badge ' + clazz + '">' + i18n('manager.jobs.status.' + status) + '</span>';
	};
})(window.mydmam.notification);

/**
 * getAndDisplayJobs
 */
(function(notification, mydmam) {
	notification.getAndDisplayJobs = function() {
		
		var view = mydmam.manager.jobs.view;
		
		/**
		 * displayJob
		 * @param ajaxdata is {jobkey: jobng, }
		 * Transform Job keys to an informative cartridge
		 */
		var displayJob = function(dom_element, ajaxdata) {
			var key = $(dom_element).text();
			if(ajaxdata[key]) {
				var job = ajaxdata[key];
				
				var content = '';
				content = content + '<div class="row-fluid">';
				
				content = content + '<div class="span2">';
				content = content + view.getNameCol(job);
				content = content + '</div>';
				
				content = content + '<div class="span2">';
				content = content + view.getStatusCol(job);
				content = content + '</div>';
				
				content = content + '<div class="span3">';
				content = content + view.getDateCol(job);
				content = content + '</div>';
				
				content = content + '<div class="span5">';
				content = content + view.getProgressionCol(job);
				content = content + view.getParamCol(job);
				content = content + '</div>';
				
				content = content + '</div>'; //row-fluid
				
				$(dom_element).html(content);
				$(dom_element).find('div.collapse').removeClass('collapse');
				console.log(ajaxdata[key]);
			} else {
				$(dom_element).html('<p>' + i18n("userprofile.notifications.cantfoundjob", key) + '</p>');
			}
			$(dom_element).find(".jobkeyraw").remove();
		};

		var key;
		var job_keys = [];
		var dom_element_list = notification.dom_element_list_for_job_resolve;
		for (var pos in dom_element_list) {
			key = $(dom_element_list[pos]).text();
			if (job_keys.indexOf(key) === -1) {
				job_keys.push(key);
			}
		}

		$.ajax({
			url: notification.url.associatedjobs,
			type: "POST",
			async: true,
			data: {
				"job_keys": job_keys
			},
			error: function(jqXHR, textStatus, errorThrown) {
				notification.ajaxError(i18n("userprofile.notifications.errorduringgetjobs"), textStatus, errorThrown);
			},
			success: function(data) {
				/*
				 * data is {jobkey: jobng, }
				 * */
				$(".jobsummary").each(function() {
					displayJob(this, data);
				});
			}
		});
	};
})(window.mydmam.notification, window.mydmam);

/**
 * postProcessPage
 */
(function(notification) {
	notification.postProcessPage = function(isadmin) {
		$(".redrawstatus").each(function() {
			$(this).html(notification.displayStatus($(this).data("status")));
		});
		if (isadmin === false) {
			$(".btnsetreadnotification").each(function() {
				$(this).click(notification.setReadNotification);
			});
		}
		notification.dom_element_list_for_user_to_resolve = [];
		$(".userprofilekey").each(function() {
			notification.dom_element_list_for_user_to_resolve.push(this);
		});
		notification.getAndDisplayUsers();

		if (isadmin === false) {
			notification.dom_element_list_for_job_resolve = [];
			$(".jobsummary").each(function() {
				notification.dom_element_list_for_job_resolve.push(this);
			});
			notification.getAndDisplayJobs();
		}
		
		if (isadmin) {
			$('.notificationsadmindatatable').dataTable({
				"bPaginate": false,
				"bLengthChange": false,
				"bSort": false,
				"bInfo": false,
				"bAutoWidth": false,
				"bFilter": true,
				"aoColumnDefs": [
					{"bSortable": false, "aTargets": [0,1,2,3,4,5]},
					{"bSearchable": true, "aTargets": [1,2,3,4]},
					{"bSearchable": false, "aTargets": [0,5]}
				]
			});

			$('#sitesearch').bind('keyup.DT', function(e) {
				var val = this.value==="" ? "" : this.value;
				$('.dataTables_filter input').val(val);
				$('.dataTables_filter input').trigger("keyup.DT");
			});
		}
	};

})(window.mydmam.notification);

