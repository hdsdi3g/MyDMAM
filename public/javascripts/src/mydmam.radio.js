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

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	mydmam.radio = {};
	mydmam.radio.instant_key_content = {};
})(window.mydmam);

(function(radio) {
	radio.instantKeyClick = function(key, item) {
		var id_ref = $(this).data("idref");
		var audio = document.getElementById('instantkeyaudio-' + id_ref);
		var button = $(this).find("button");
		
		// view-source:http://www.w3.org/2010/05/video/mediaevents.html
		if (audio.paused) {
			audio.play();
			button.addClass("btn-danger");
			button.removeClass("btn-success");
		} else {
			audio.pause();
			audio.load();
			button.addClass("btn-success");
			button.removeClass("btn-danger");
		}
		
		
		console.log(audio);
	};
})(mydmam.radio);


/**
 * loadItem
 */
(function(radio) {
	radio.loadItem = function(key, item) {
		if (!item.mtdsummary) {
			return;
		}
		var mtdsummary = item.mtdsummary;
		var reference = item.reference;
		var url = null;
		
		if (mtdsummary.master_as_preview) {
			url = mydmam.metadatas.decodeMasterAsPreview(mtdsummary.mimetype, reference.path, key).master_as_preview_url;
		} else if (!mtdsummary.previews) {
			return;
		} else if (!mtdsummary.previews.audio_pvw) {
			return;
		} else {
			url = mydmam.metadatas.getURL(key, mtdsummary.previews.audio_pvw.type, mtdsummary.previews.audio_pvw.file);
		}
		
		var id_ref = key.substring(0, 6);
		var file_name = reference.path.substring(reference.path.lastIndexOf("/") + 1, reference.path.lastIndexOf("."));
		
		var content = "";
		content = content + '<span id="instantkey-' + id_ref + '" data-idref="' + id_ref + '">';
		content = content + '<audio preload="auto" id="instantkeyaudio-' + id_ref + '">';
		content = content + '<source src="' + url + '" type="' + mtdsummary.mimetype + '" />';
		content = content + '<p>Your user agent does not support the HTML5 Audio element.</p>';
		content = content + '</audio>';
		content = content + '<button class="btn btn-success">' + file_name + '</button>';
		content = content + '</span>';
		
		$("#instantkeys").append(content);
		$("#instantkey-" + id_ref).click(radio.instantKeyClick);
		//console.log(item, url);
	};
})(mydmam.radio);


/**
 * loadAll
 */
(function(radio) {
	radio.loadAll = function() {
		var item;
		for (var key in radio.instant_key_content) {
			item = radio.instant_key_content[key];
			radio.loadItem(key, item);
		}
	};
})(mydmam.radio);
