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
		$('div.jwplayer-video').each(function(){
			jwplayer($(this).context.id).setup({
				playlist: [{
					image: $(this).context.dataset.image,
					sources: [
						{ file: $(this).context.dataset.file, label: "360p" }, //TODO switch quality
						{ file: $(this).context.dataset.file, label: "720p HD" },
						{ file: $(this).context.dataset.file, label: "1080p HD" }
					]
				}],
				height: $(this).context.dataset.height,
				width: $(this).context.dataset.width
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
		
		var content = '';
	
		if (method == this.NAVIGATE_SHOW_ELEMENT) {
			if (element.metadatas.previews) {
				var previews = element.metadatas.previews;
				//TODO test audio ? video ? image ? with mime
				if (previews.video_sd_pvw) {
					var url_file = this.getURL(md5(element.storagename + ":" + element.path), previews.video_sd_pvw.type, previews.video_sd_pvw.file);
					var url_image = "";
					if (previews.full_size_thumbnail) {
						url_image = this.getURL(md5(element.storagename + ":" + element.path), previews.full_size_thumbnail.type, previews.full_size_thumbnail.file);
					}
					content = content + '<div class="jwplayer-video" ';
					content = content + 'data-file="' + url_file + '" ';
					content = content + 'data-image="' + url_image + '" ';
					content = content + 'data-width="640" data-height="360" ';
					content = content + 'id="jwpvw-' + md5(element.storagename + ":" + element.path).substr(0,8) + '">';
					content = content + i18n('browser.loadingplayer');
					content = content + '</div>';
				} else if (previews.full_size_thumbnail) {
					var url = this.getURL(md5(element.storagename + ":" + element.path), previews.full_size_thumbnail.type, previews.full_size_thumbnail.file);
					content = content + '<div style="margin-bottom: 1em;">';
					content = content + '<img src="' + url + '" class="img-polaroid" alt="768x432" data-src="holder.js/768x432" style="width: 768px; height: 432px;"/>';
					content = content + '</div>';
				}
			
				/*if ((preview_type == "full_size") & element.metadatas.master_as_preview) {
					file_type = "master_as_preview";
					file_name = "default";
					file_hash = md5(element.storagename + ":" + element.path);
					url = this.getURL(file_hash, file_type, file_name);
					
					content = content + '<div style="margin-bottom: 1em;">';
					content = content + '<audio controls="controls">';//TODO add jwplayer
					content = content + 'Votre navigateur ne supporte pas lélément <code>audio</code> element.';
					content = content + '<source src="' + url + '" type="' + element.metadatas.mimetype + '">';
					content = content + '</audio>';
					content = content + '</div>';
					
				}*/
			}
			
			for (var analyser in element.metadatas) {
				if ((analyser == "mimetype") | (analyser == "master_as_preview") | (analyser == "previews")) {
					/**
					 * Don't show that
					 */
					continue;
				}
				content = content + '<blockquote style="margin-top:1em;">';
				content = content + '<p>' + element.metadatas[analyser] + '</p>';
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