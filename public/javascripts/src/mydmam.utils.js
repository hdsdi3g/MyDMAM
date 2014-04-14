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
 * Define stand-alone mydmam functions.
 */
(function(mydmam) {
	mydmam.utils = {};
	
	/**
	 * Create a click, ajax and call back to a button.
	 * @param id String. Object must exists
	 * @param url_target String
	 * @param loading_label Localised String
	 * @param params Simple object (keys:values)
	 * @param callback Function to call with data param (JQueryObject response)
	 * @returns null
	 */
	var addActionToSimpleButton = function(id, url_target, loading_label, params, callback) {
		$("#" + id).click(function() {
			if ($("#" + id).hasClass("disabled")) {
				return;
			}
			var original_text = $("#" + id).text();
			$.ajax({
				url: url_target,
				type: "POST",
				data: params,
				beforeSend: function() {
					$("#" + id).addClass("disabled");
					$("#" + id).text(loading_label);
				},
				error: function(jqXHR, textStatus, errorThrown) {
					$("#" + id).removeClass("disabled");
					$("#" + id).text(original_text);
					showSimpleModalBox("Erreur", i18n("browser.versatileerror"));
				},
				success: function(rawdata) {
					if (rawdata == null | rawdata == "null" | rawdata === "") {
						$("#" + id).removeClass("disabled");
						$("#" + id).text(original_text);
						showSimpleModalBox("Erreur", i18n("browser.versatileerror"));
						return;
					}
					if (rawdata.btnlabel != null) {
						$("#" + id).text(rawdata.btnlabel);
					} else {
						$("#" + id).text(original_text);
					}
					if (rawdata.canreclick) {
						$("#" + id).removeClass("disabled");
					}
					callback(rawdata);
				}
			});
		});
	};
	mydmam.utils.addActionToSimpleButton = addActionToSimpleButton;

})(window.mydmam);
