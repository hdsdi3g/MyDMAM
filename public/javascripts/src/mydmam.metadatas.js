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
 * Metadata engine
 */

/**
 * Prepare consts and vars.
 */
(function(mydmam) {
	mydmam.metadatas = {};
	var metadatas = mydmam.metadatas;

	metadatas.displaymethod = {};
	metadatas.displaymethod.NAVIGATE_SHOW_ELEMENT = 0;

	metadatas.url = {};
})(window.mydmam);

/**
 * Prepare view function for video, audio and image.
 */
(function(metadatas) {
	metadatas.view = {};
})(window.mydmam.metadatas);

/**
 * Prepare view video
 */
(function(metadatas) {
	metadatas.view.video = {};
	var thisview = metadatas.view.video;

	thisview.prepare = function(file_hash, width, height, url_image, medias) {
		var content = '';
		content = content + '<div class="jwplayer-case" ';
		for (var pos = 0; pos < medias.length; pos++) {
			content = content + 'data-fileurl' + (pos) + '="' + medias[pos].url + '" ';
			content = content + 'data-filelabel' + (pos) + '="' + medias[pos].label + '" ';
		}
		content = content + 'data-filecount="' + medias.length + '" ';
		if (url_image) {
			content = content + 'data-image="' + url_image + '" ';
		}
		content = content + 'data-width="' + width + '" data-height="' + height + '" ';
		content = content + 'data-mediakind="video" ';
		content = content + 'id="jwpvw-' + file_hash.substr(0, 8) + '">';
		content = content + i18n('browser.loadingplayer');
		content = content + '</div>';
		return content;
	};

	thisview.loadafterdisplay = function() {
		$('div.jwplayer-case').each(function() {
			// console.log($(this));
			var htmlid = $(this).context.id;
			var dataset = $(this).context.dataset;

			var playlist_item = {};
			if (dataset.image) {
				playlist_item.image = dataset.image;
			}

			var playlist_item_sources = [];
			for (var pos = 0; pos < dataset.filecount; pos++) {
				var source = {};
				source.file = dataset["fileurl" + pos];
				source.label = dataset["filelabel" + pos];
				playlist_item_sources.push(source);
			}
			playlist_item.sources = playlist_item_sources;

			/*
			 * content = content + 'data-mediakind="video" '; content = content +
			 * 'data-mediakind="audio" ';
			 */
			jwplayer(htmlid).setup({
				playlist: [playlist_item],
				height: dataset.height,
				width: dataset.width
			});
		});
	};

})(window.mydmam.metadatas);

/**
 * Prepare view audio
 */
(function(metadatas) {
	metadatas.view.audio = {};
	metadatas.view.audio.prepare = function(file_hash, url_audio) {
		var content = '';
		content = content + '<div class="jwplayer-case" ';
		content = content + 'data-fileurl0="' + url_audio + '" ';
		content = content + 'data-filelabel0="Audio" ';
		content = content + 'data-filecount="1" ';
		content = content + 'data-width="640" data-height="50" ';
		content = content + 'data-mediakind="audio" ';
		content = content + 'id="jwpvw-' + file_hash.substr(0, 8) + '">';
		content = content + i18n('browser.loadingplayer');
		content = content + '</div>';
		return content;
	};
})(window.mydmam.metadatas);

/**
 * loadAfterDisplay : call loadafterdisplay() for all metadatas.view.* (if
 * exists).
 */
(function(metadatas) {
	metadatas.loadAfterDisplay = function() {
		for ( var viewname in metadatas.view) {
			var viewer = metadatas.view[viewname];
			if (typeof viewer.loadafterdisplay == 'function') {
				viewer.loadafterdisplay();
			}
		}
	};
})(window.mydmam.metadatas);

