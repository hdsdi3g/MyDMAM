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
				$('#errorplaceholder').html('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("userprofile.notifications.errorduringsetread") + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
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
		var user_key_list = [];
		var dom_element_list = notification.dom_element_list_for_user_to_resolve;
		var key;
		for (var pos in dom_element_list) {
			key = $(dom_element_list[pos]).text();
			if (user_key_list.indexOf(key) === -1) {
				user_key_list.push(key);
			}
		}
		console.log(user_key_list);
		/*
		TODO...
		var url = url_notificationupdateread.replace("keyparam1", notificationkey);
		
		$.ajax({
			url: url,
			type: "POST",
			async: true,
			data: {
				"list": queue.since_update_list
			},
			error: function(jqXHR, textStatus, errorThrown) {
				$('#errorplaceholder').html('<div class="alert"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>' + i18n("userprofile.notifications.errorduringsetread") + '</strong> ' + textStatus + ', ' + errorThrown +'</div>');
			},
			success: function(data) {
				if (!data.result) {
					console.log(data);
				}
			}
		});*/
	};
})(window.mydmam.notification);

/**
 * getAndDisplayTasksJobs
 */
(function(notification) {
	notification.getAndDisplayTasksJobs = function() {
		//TODO
	};
})(window.mydmam.notification);

/**
 * postProcessPage
 */
(function(notification) {
	notification.postProcessPage = function() {
		$(".btnsetreadnotification").each(function() {
			$(this).click(notification.setReadNotification);
		});

		notification.dom_element_list_for_user_to_resolve = [];
		$(".userprofilekey").each(function() {
			notification.dom_element_list_for_user_to_resolve.push(this);
		});
		notification.getAndDisplayUsers();

		notification.dom_element_list_for_taskjob_resolve = [];
		$(".taskjobsummary").each(function() {
			notification.dom_element_list_for_taskjob_resolve.push(this);
		});
		notification.getAndDisplayTasksJobs();
	};

})(window.mydmam.notification);
