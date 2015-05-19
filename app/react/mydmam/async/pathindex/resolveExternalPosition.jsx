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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/

pathindex.resolveExternalPosition = function(externalpos_request_keys, data_callback) {
	if (!externalpos_request_keys) {
		return;
	}
	if (externalpos_request_keys.length == 0) {
		return;
	}
	$.ajax({
		url: mydmam.metadatas.url.resolvepositions,
		type: "POST",
		data: {
			"keys": externalpos_request_keys,
		},
		success: data_callback,
		error: function(jqXHR, textStatus, errorThrown) {
			console.error(jqXHR, textStatus, errorThrown);
		},
	});
};
