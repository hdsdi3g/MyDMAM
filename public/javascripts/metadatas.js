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

function MetadataEngine() {

	/**
	 * Declared method, must be match display() function.
	 */
	this.NAVIGATE_SHOW_ELEMENT = 0;
	
	/**
	 * @return metadata file url
	 */
	this.getURL = function(file_hash, file_type, file_name) {
		if (!url_metadatafile) {
			return "";
		}
		return url_metadatafile.replace("filehashparam1", file_hash).replace("typeparam2", file_type).replace("fileparam3", file_name);
	};
	
	this.loadAfterDisplay = function() {
		$('div.jwplayer-case').each(function(){
			//console.log($(this));//TODO
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
			content = content + 'data-mediakind="video" ';
			content = content + 'data-mediakind="audio" ';
			*/
			jwplayer(htmlid).setup({
				playlist: [playlist_item],
				height: dataset.height,
				width: dataset.width
			});
		});
	};
	
	/**
	 * @return the code to display in page.
	 */
	this.display = function(element, method) {
		if (!element.metadatas) {
			return "";
		}
		var metadatas = element.metadatas;
		var file_hash = md5(element.storagename + ":" + element.path);
		
		var content = '';
		var master_as_preview_type = '';
		var master_as_preview_url = '';
		
		if (metadatas.master_as_preview) {
			master_as_preview_type = metadatas.mimetype.substring(0, metadatas.mimetype.indexOf("/"));
			var ext = element.path.substring(element.path.lastIndexOf("."), element.path.length);
			master_as_preview_url = this.getURL(file_hash, "master_as_preview", "default" + ext);
		}

		var display_prepare_video_player = function(width, height, url_image, medias) {
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
			content = content + 'id="jwpvw-' + file_hash.substr(0,8) + '">';
			content = content + i18n('browser.loadingplayer');
			content = content + '</div>';
			return content;
		};

		var display_prepare_audio_player = function(url_audio) {
			var content = '';
			content = content + '<div class="jwplayer-case" ';
			content = content + 'data-fileurl0="' + url_audio + '" ';
			content = content + 'data-filelabel0="Audio" ';
			content = content + 'data-filecount="1" ';
			content = content + 'data-width="640" data-height="50" ';
			content = content + 'data-mediakind="audio" ';
			content = content + 'id="jwpvw-' + file_hash.substr(0,8) + '">';
			content = content + i18n('browser.loadingplayer');
			content = content + '</div>';
			return content;
		};

		var display_prepare_image = function(url, htmlclass, width, height) {
			var content = '';
			content = content + '<div style="margin-bottom: 1em;">';
			content = content + '<img src="' + url + '" class="' + htmlclass + '" alt="' + width + 'x' + height + '" data-src="holder.js/' + width + 'x' + height + '" style="width: ' + width + 'px; height: ' + height + 'px;"/>';
			content = content + '</div>';
			return content;
		};
		
		if (method == this.NAVIGATE_SHOW_ELEMENT) {
			if (metadatas.previews) {
				var previews = metadatas.previews;
				
				if ((previews.video_lq_pvw != null) | (previews.video_sd_pvw != null) | (previews.video_hd_pvw != null) | (master_as_preview_type == "video")) {
					/**
					 * Video
					 */
					var url_image = null;
					if (previews.full_size_thumbnail) {
						url_image = this.getURL(file_hash, previews.full_size_thumbnail.type, previews.full_size_thumbnail.file);
					}

					var medias = [];
					if (master_as_preview_type == "video") {
						var media = {};
						media.url = master_as_preview_url;
						media.label = "Original";
						medias.push(media);
					}
					if (previews.video_hd_pvw) {
						var media = {};
						media.url = this.getURL(file_hash, previews.video_hd_pvw.type, previews.video_hd_pvw.file);
						media.label = "HD";
						medias.push(media);
					}
					if (previews.video_sd_pvw) {
						var media = {};
						media.url = this.getURL(file_hash, previews.video_sd_pvw.type, previews.video_sd_pvw.file);
						media.label = "SQ";
						medias.push(media);
					}
					if (previews.video_lq_pvw) {
						var media = {};
						media.url = this.getURL(file_hash, previews.video_lq_pvw.type, previews.video_lq_pvw.file);
						media.label = "LQ";
						medias.push(media);
					}
					content = content + display_prepare_video_player(640, 360, url_image, medias);
				} else if ((previews.audio_pvw != null) | (master_as_preview_type == "audio")) {
					/**
					 * Audio
					 */
					if (master_as_preview_type == "audio") {
						var url = this.getURL(file_hash, previews.audio_pvw.type, previews.audio_pvw.file);
						content = content + display_prepare_audio_player(master_as_preview_url);
					} else {
						var url = this.getURL(file_hash, previews.audio_pvw.type, previews.audio_pvw.file);
						content = content + display_prepare_audio_player(url);
					}
				} else if (master_as_preview_type == "image") {
					/**
					 * Image
					 */
					content = content + display_prepare_image(master_as_preview_url, "img-polaroid", 768, 432); //TODO get size ?
				} else if (previews.full_size_thumbnail) {
					var url = this.getURL(file_hash, previews.full_size_thumbnail.type, previews.full_size_thumbnail.file);
					content = content + display_prepare_image(url, "img-polaroid", 768, 432); //TODO size ?
				}
			} else {
				/**
				 * Only master_as_preview (maybe a small element) ?
				 */
				if (master_as_preview_type == "video") {
					var media = {};
					media.url = master_as_preview_url;
					media.label = "Original";
					content = content + display_prepare_video_player(640, 360, null, [media]);
				} else if (master_as_preview_type == "audio") {
					content = content + display_prepare_audio_player(master_as_preview_url, null);
				} else if (master_as_preview_type == "image") {
					content = content + display_prepare_image(master_as_preview_url, "img-polaroid", 768, 432); //TODO get size ?
				}
			}
			
			for (var analyser in metadatas) {
				if ((analyser == "mimetype") | (analyser == "master_as_preview") | (analyser == "previews")) {
					/**
					 * Don't show that
					 */
					continue;
				}
				content = content + '<blockquote style="margin-top:1em;">';
				content = content + '<p>' + metadatas[analyser] + '</p>';
				content = content + '<small>' + analyser + '</small>';
				content = content + '</blockquote>';
			}
		}
		
		return content;
	};
	
	this.displaySummary = function(metadatas) {
		var title = "";
		var count = 0;
		for (var analyser in metadatas) {
			if (analyser == "mimetype") {
				continue;
			}
			if (analyser == "master_as_preview") {
				continue;
			}
			if (analyser == "previews") {
				continue;
			}
			count++;
			if (title !== "") {
				title = title + " - ";
			}
			title = title + metadatas[analyser];
		}
		if (count > 0) {
			return '<abbr title="' + title.trim() + '">' + this.typeofelement(metadatas) + '</abbr>';
		} else {
			return this.typeofelement(metadatas);
		}
	};
	
	/**
	 * @return display translated mime in page.
	 */
	this.typeofelement = function(metadatas) {
		if (!metadatas) {
			return "";
		}
		if (!metadatas.mimetype) {
			return "";
		}
		/**
		 * transform "application/x-dummy" to "application-dummy"
		 */
		var element = metadatas.mimetype;
		var element_type = element.substr(0, element.indexOf('/'));
		var element_subtype = element.substr(element.indexOf('/') + 1);
		
		if (element_subtype.startsWith("x-")) {
			element_subtype = element_subtype.substr(2);
		}
		element = element_type + "-" + element_subtype;
		
		translated_element = i18n("mime." + element);
		if (translated_element.startsWith("mime.")) {
			translated_element = translated_element.substr(5);
		}
		return translated_element;
	};
	
	
}