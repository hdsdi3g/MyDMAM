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
	mydmam.notification = {};
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
		var url = url_notificationupdateread.replace("keyparam1", notificationkey);
		
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
			url: url_notificationresolveusers,
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
 * getAndDisplayTasksJobs
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
		return '<span class="badge ' + clazz + '">' + i18n(status) + '</span>';
	};
})(window.mydmam.notification);

/**
 * getAndDisplayTasksJobs
 */
(function(notification, mydmam) {
	notification.getAndDisplayTasksJobs = function() {
		
		/**
		 * displayTaskJob
		 * Transform Task/Job keys to an informative cartridge
		 */
		var displayTaskJob = function(dom_element, ajaxdata) {
			var key = $(dom_element).text();
			if(ajaxdata[key]) {
				ajaxdata[key].key = key;
				ajaxdata[key].simplekey = mydmam.queue.createSimpleKey(key);
				$(dom_element).html(mydmam.queue.createTaskJobTableElement(ajaxdata[key]));
				$(dom_element).find(".blocktaskjobdateedit").prepend(notification.displayStatus(ajaxdata[key].status) + ' ');
				$(dom_element).find(".taskjobkeyraw").remove();

			} else {
				$(dom_element).html('<strong>' + i18n("userprofile.notifications.cantfoundtaskjob") + '</strong>');
			}
		};

		var key;
		var tasksjobs_keys = [];
		var dom_element_list = notification.dom_element_list_for_taskjob_resolve;
		for (var pos in dom_element_list) {
			key = $(dom_element_list[pos]).text();
			if (tasksjobs_keys.indexOf(key) === -1) {
				tasksjobs_keys.push(key);
			}
		}

		$.ajax({
			url: url_queuegettasksjobs,
			type: "POST",
			async: true,
			data: {
				"tasksjobs_keys": tasksjobs_keys
			},
			error: function(jqXHR, textStatus, errorThrown) {
				notification.ajaxError(i18n("userprofile.notifications.errorduringgettasksjobs"), textStatus, errorThrown);
			},
			success: function(data) {
				$(".taskjobsummary").each(function() {
					displayTaskJob(this, data);
				});
				/* sort T/J ?
				var data = [];
				for (var key in rawdata) {
					rawdata[key].key = key;
					data.push(rawdata[key]);
				}
				data = data.sort(function(a, b) {
					return a.updatedate > b.updatedate ? -1 : 1;
				});
				 * */
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
			notification.dom_element_list_for_taskjob_resolve = [];
			$(".taskjobsummary").each(function() {
				notification.dom_element_list_for_taskjob_resolve.push(this);
			});
			notification.getAndDisplayTasksJobs();
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

