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
 * typeofelement : display translated mime in page.
 */
(function(metadatas) {
	metadatas.typeofelement = function(mtd_element) {
		if (!mtd_element) {
			return "";
		}
		if (!mtd_element.mimetype) {
			return "";
		}
		/**
		 * transform "application/x-dummy" to "application-dummy"
		 */
		var element = mtd_element.mimetype;
		var element_type = element.substr(0, element.indexOf('/'));
		var element_subtype = element.substr(element.indexOf('/') + 1);
		
		if (element_subtype.startsWith("x-")) {
			element_subtype = element_subtype.substr(2);
		}
		element = element_type + "-" + element_subtype;
		
		var translated_element = i18n("mime." + element);
		if (translated_element.startsWith("mime.")) {
			translated_element = translated_element.substr(5);
		}
		return translated_element;
	};
})(window.mydmam.metadatas);

/**
 * displaySummary
 */
(function(metadatas) {
	metadatas.displaySummary = function(mtd_element) {
		var title = "";
		var count = 0;
		for (var analyser in mtd_element) {
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
			title = title + mtd_element[analyser];
		}
		if (count > 0) {
			return '<abbr title="' + title.trim() + '">' + metadatas.typeofelement(mtd_element) + '</abbr>';
		} else {
			return metadatas.typeofelement(mtd_element);
		}
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
		content = content + 'id="jwpvw-' + file_hash.substr(0,8) + '">';
		content = content + i18n('browser.loadingplayer');
		content = content + '</div>';
		return content;
	};
	
	thisview.loadafterdisplay = function() {
		$('div.jwplayer-case').each(function(){
			//console.log($(this));
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
		content = content + 'id="jwpvw-' + file_hash.substr(0,8) + '">';
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
			content = content + '<img id="' + file_hash.substr(0,8) + '" src="' + url + '" class="' + htmlclass + '" alt="' + width + 'x' + height + '" data-src="holder.js/' + width + 'x' + height + '" style="width: ' + width + 'px; height: ' + height + 'px;"/>';
		} else {
			content = content + '<img id="' + file_hash.substr(0,8) + '" src="' + url + '" class="' + htmlclass + '" data-src="holder.js"/>';
		}
		content = content + '</div>';
		return content;
	};
})(window.mydmam.metadatas);

/**
 * display : the code to display in page
 */
(function(metadatas) {
	metadatas.display = function(element, method) {
		if (!element.metadatas) {
			return "";
		}
		var mtd_element = element.metadatas;
		var file_hash = md5(element.storagename + ":" + element.path);
		
		var content = '';
		var master_as_preview_type = '';
		var master_as_preview_url = '';
		
		if (mtd_element.master_as_preview) {
			master_as_preview_type = mtd_element.mimetype.substring(0, mtd_element.mimetype.indexOf("/"));
			var ext = element.path.substring(element.path.lastIndexOf("."), element.path.length);
			master_as_preview_url = this.getURL(file_hash, "master_as_preview", "default" + ext);
		}

		if (method == metadatas.displaymethod.NAVIGATE_SHOW_ELEMENT) {
			if (mtd_element.previews) {
				var previews = mtd_element.previews;
				
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
					content = content + metadatas.view.video.prepare(file_hash, 640, 360, url_image, medias);
				} else if ((previews.audio_pvw != null) | (master_as_preview_type == "audio")) {
					/**
					 * Audio
					 */
					if (master_as_preview_type == "audio") {
						content = content + metadatas.view.audio.prepare(file_hash, master_as_preview_url);
					} else {
						var url = this.getURL(file_hash, previews.audio_pvw.type, previews.audio_pvw.file);
						content = content + metadatas.view.audio.prepare(file_hash, url);
					}
					if (previews.full_size_thumbnail) {
						/**
						 * Display Album artwork
						 */
						var url = this.getURL(file_hash, previews.full_size_thumbnail.type, previews.full_size_thumbnail.file);
						content = content + metadatas.view.image.prepare(file_hash, url, "img-polaroid", 0, 0); //TODO size ?
					}
				} else if (master_as_preview_type == "image") {
					/**
					 * Image
					 */
					content = content + metadatas.view.image.prepare(file_hash, master_as_preview_url, "img-polaroid", 0, 0); //TODO get size ?
				} else if (previews.full_size_thumbnail) {
					var url = this.getURL(file_hash, previews.full_size_thumbnail.type, previews.full_size_thumbnail.file);
					content = content + metadatas.view.image.prepare(file_hash, url, "img-polaroid", 0, 0); //TODO size ?
				}
			} else {
				/**
				 * Only master_as_preview (maybe a small element)
				 */
				if (master_as_preview_type == "video") {
					var media = {};
					media.url = master_as_preview_url;
					media.label = "Original";
					content = content + metadatas.view.video.prepare(file_hash, 640, 360, null, [media]);
				} else if (master_as_preview_type == "audio") {
					content = content + metadatas.view.audio.prepare(file_hash, master_as_preview_url, null);
				} else if (master_as_preview_type == "image") {
					content = content + metadatas.view.image.prepare(file_hash, master_as_preview_url, "img-polaroid", 0, 0); //TODO get size ?
				}
			}
			
			for (var analyser in mtd_element) {
				if ((analyser == "mimetype") | (analyser == "master_as_preview") | (analyser == "previews")) {
					/**
					 * Don't show that
					 */
					continue;
				}
				content = content + '<blockquote style="margin-top:1em;">';
				content = content + '<p>' + mtd_element[analyser] + '</p>';
				content = content + '<small>' + analyser + '</small>';
				content = content + '</blockquote>';
			}
		}
		return content;
	};
})(window.mydmam.metadatas);

/**
 * loadAfterDisplay : call loadafterdisplay() for all metadatas.view.* (if exists).
 */
(function(metadatas) {
	metadatas.loadAfterDisplay = function() {
		for (var viewname in metadatas.view) {
			var viewer =  metadatas.view[viewname];
			if (typeof viewer.loadafterdisplay == 'function') {
				viewer.loadafterdisplay();
			}
		}
	};
})(window.mydmam.metadatas);

/**
 * getAndAddExternalPosition
 */
(function(metadatas) {
	metadatas.getAndAddExternalPosition = function(external_elements_to_resolve, callback_online, callback_offline, callback_nearline) {
		$.ajax({
			url: metadatas.url.resolvepositions,
			type: "POST",
			data: {"keys" : external_elements_to_resolve},
			success: function(data) {
				
				var key;
				for (var pos_key = 0; pos_key < external_elements_to_resolve.length; pos_key++) {
					key = external_elements_to_resolve[pos_key];
					if (data.positions[key]) {
						var verbose = true;
						for (var pos = 0; pos < data.positions[key].length; pos++) {
							var tapename = data.positions[key][pos];
							if (tapename == "cache") {
								callback_online(key);
								verbose = false;
								break;
							}
						}
						if (verbose) {
							for (var pos = 0; pos < data.positions[key].length; pos++) {
								var tapename = data.positions[key][pos];
								var location = data.locations[tapename];
								if (location.isexternal) {
									callback_offline(key, location.barcode);
								} else {
									callback_nearline(key);
								}
							}
						}
					}
				}
			}
		});
	};
})(window.mydmam.metadatas);

/**
 * linkifysearchresultitems
 */
(function(metadatas) {
	metadatas.linkifysearchresultitems = function(external_elements_to_resolve, elements_to_get_metadatas) {
		$(".searchresultitem").each(function(index) {
			// Transform text path to navigate links on search results
			var element_storage = $(this).find(".storagename").text();
			var element_path = $(this).find(".path").text();
			$(this).find(".storagename").html('<a href=\"' + url_navigate + "#" + element_storage + ':/\">' + element_storage + '</a>');
			
			var element_subpaths = element_path.split("/");
			var element_path_new = "";
			var currentpath = "";
			var newpath = "";
			for (var pos = 1; pos < element_subpaths.length; pos++) {
				newpath = element_storage + ':' + currentpath + "/" + element_subpaths[pos];
				element_path_new = element_path_new + "/";
				element_path_new = element_path_new + '<a href="' + url_navigate + "#" + newpath + '">';
				element_path_new = element_path_new + element_subpaths[pos];
				element_path_new = element_path_new + "</a>";
				currentpath = currentpath + "/" + element_subpaths[pos];
			}
			$(this).find(".path").html(element_path_new);
			
			// Search items for a search archive position
			for (var pos = 0; pos < list_external_positions_storages.length; pos++) {
				if (list_external_positions_storages[pos] == element_storage) {
					external_elements_to_resolve.push($(this).data('storagekey'));
				}
			}

			elements_to_get_metadatas.push($(this).data('storagekey'));
		});
	};
})(window.mydmam.metadatas);

/**
 * addMetadatasToSearchListItems
 */
(function(metadatas) {
	metadatas.addMetadatasToSearchListItems = function() {
		var external_elements_to_resolve = [];
		var elements_to_get_metadatas = [];
		
		metadatas.linkifysearchresultitems(external_elements_to_resolve, elements_to_get_metadatas);
		
			/**
			 * Add archive position to items
			 */
		if (external_elements_to_resolve.length > 0) {
			metadatas.getAndAddExternalPosition(external_elements_to_resolve, function(key) {
				$('#sri-' + key).prepend('<span class="label label-success">' + i18n('browser.externalposition.online') + '</span> ');
			}, function(key, barcode) {
				$('#sri-' + key).prepend('<span class="label label-important">' + barcode + '</span> ');
			}, function(key) {
				$('#sri-' + key).prepend('<span class="label label-success">' + i18n('browser.externalposition.nearline') + '</span> ');
			});
		}
		
		if (elements_to_get_metadatas.length > 0) {
			$.ajax({
				url: metadatas.url.simplemetadatas,
				type: "POST",
				data: {"fileshash" : elements_to_get_metadatas},
				success: function(data) {
					if (data.length === 0) {
						return;
					}
					for (var pos_key = 0; pos_key < elements_to_get_metadatas.length; pos_key++) {
						var key = elements_to_get_metadatas[pos_key];
						var mtd_element = data[key];
						if (mtd_element == null) {
							continue;
						}
						if (mtd_element.summary == null) {
							continue;
						}
						
						var count = 0;
						var title = "";
						for (var summary_element in metadatas.summary) {
							if (summary_element == "mimetype") {
								continue;
							}
							if (summary_element == "previews") {
								continue;
							}
							count++;
							if (title !== "") {
								title = title + " - ";
							}
							title = title + mtd_element.summary[summary_element];
						}
						if (count > 0) {
							$('#mtd-' + key).html('<small>' + metadatas.typeofelement(mtd_element.summary) + ' :: ' + title.trim() + '</small> ');
						} else {
							$('#mtd-' + key).html('<small>' + metadatas.typeofelement(mtd_element.summary) + '</small> ');
						}
					}
				}
			});
		}
	};
})(window.mydmam.metadatas);
