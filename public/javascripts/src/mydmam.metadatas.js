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
 * getURL
 */
(function(metadatas) {
	metadatas.getURL = function(file_hash, file_type, file_name) {
		if (!metadatas.url.metadatafile) {
			return "";
		}
		return metadatas.url.metadatafile.replace("filehashparam1", file_hash).replace("typeparam2", file_type).replace("fileparam3", file_name);
	};
})(window.mydmam.metadatas);

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
 * Prepare view image
 */
(function(metadatas) {
	metadatas.view.image = {};
	metadatas.view.image.prepare = function(file_hash, url, htmlclass, width, height) {
		var content = '';
		content = content + '<div style="margin-bottom: 1em;">';
		if ((width > 0) & (height > 0)) {
			content = content + '<img id="' + file_hash.substr(0, 8) + '" src="' + url + '" class="' + htmlclass + '" alt="' + width + 'x' + height + '" data-src="holder.js/' + width + 'x' + height + '" style="width: ' + width + 'px; height: ' + height + 'px;"/>';
		} else {
			content = content + '<img id="' + file_hash.substr(0, 8) + '" src="' + url + '" class="' + htmlclass + '" data-src="holder.js"/>';
		}
		content = content + '</div>';
		return content;
	};
})(window.mydmam.metadatas);

/**
 * display : the code to display in page
 */
(function(metadatas) {

	var prepareImage = function(file_hash, previews, _prefered_size, just_url) {
		var prefered_size = _prefered_size;
		var getReturn = function(thumbnail) {
			var url = metadatas.getURL(file_hash, thumbnail.type, thumbnail.file);
			if (just_url) {
				return url;
			}
			return metadatas.view.image.prepare(file_hash, url, "img-polaroid", thumbnail.options.width, thumbnail.options.height);
		};

		if (prefered_size == null) {
			prefered_size = "full_size_thumbnail";
		}

		if (prefered_size === "full_size_thumbnail") {
			if (previews.full_size_thumbnail) {
				return getReturn(previews.full_size_thumbnail);
			} else {
				prefered_size = "cartridge_thumbnail";
			}
		}

		if (prefered_size === "cartridge_thumbnail") {
			if (previews.cartridge_thumbnail) {
				return getReturn(previews.cartridge_thumbnail);
			} else {
				prefered_size = "icon_thumbnail";
			}
		}

		if (prefered_size === "icon_thumbnail") {
			if (previews.icon_thumbnail) {
				return getReturn(previews.icon_thumbnail);
			}
		} else {
			return null;
		}
	};

	metadatas.display = function(reference, mtd_element, method) {
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

