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
	notification.getAndDisplayTasksJobs = function() {
		//TODO
		//$(this).html('<span class="label label-inverse">' + key + '</span>');
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
