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

pathindex.reactMetadataFull = React.createClass({
	render: function() {
		if (!this.props.mtdsummary | !this.props.reference) {
			return null;
		}
		var mtdsummary = this.props.mtdsummary;
		var reference = this.props.reference;
		console.log("TODO", this.props);

		var file_hash = md5(reference.storagename + ":" + reference.path);
		var master_as_preview_type = '';
		var master_as_preview_url = '';

		/*if (mtd_element.master_as_preview) {
			master_as_preview_type = mtd_element.mimetype.substring(0, mtd_element.mimetype.indexOf("/"));
			var ext = reference.path.substring(reference.path.lastIndexOf("."), reference.path.length);
			master_as_preview_url = pathindex.metadatas.getFileURL(file_hash, "master_as_preview", "default" + ext);
		}*/

		if (mtdsummary.previews) {
			var previews = mtdsummary.previews;
			var has_image_thumbnail = (previews.full_size_thumbnail != null) | (previews.cartridge_thumbnail != null) | (previews.icon_thumbnail != null);

			var preview = null;

			if ((previews.video_lq_pvw != null) | (previews.video_sd_pvw != null) | (previews.video_hd_pvw != null) | (master_as_preview_type == "video")) {
				/** Video */
				//var url_image = null;
				if (has_image_thumbnail) {
					//url_image = prepareImage(file_hash, previews, "cartridge_thumbnail", true);
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
					media.url = pathindex.metadatas.getFileURL(file_hash, previews.video_hd_pvw.type, previews.video_hd_pvw.file);
					media.label = "HD";
					medias.push(media);
				}
				if (previews.video_sd_pvw) {
					var media = {};
					media.url = pathindex.metadatas.getFileURL(file_hash, previews.video_sd_pvw.type, previews.video_sd_pvw.file);
					media.label = "SQ";
					medias.push(media);
				}
				if (previews.video_lq_pvw) {
					var media = {};
					media.url = pathindex.metadatas.getFileURL(file_hash, previews.video_lq_pvw.type, previews.video_lq_pvw.file);
					media.label = "LQ";
					medias.push(media);
				}
				//content = content + metadatas.view.video.prepare(file_hash, 640, 360, url_image, medias);
			} else if ((previews.audio_pvw != null) | (master_as_preview_type == "audio")) {
				/** Audio */
				if (master_as_preview_type == "audio") {
					//content = content + metadatas.view.audio.prepare(file_hash, master_as_preview_url);
				} else {
					var url = pathindex.metadatas.getFileURL(file_hash, previews.audio_pvw.type, previews.audio_pvw.file);
					//content = content + metadatas.view.audio.prepare(file_hash, url);
				}
				if (has_image_thumbnail) {
					//content = content + prepareImage(file_hash, previews);
				}
			} else if ((previews.full_size_thumbnail != null) | (previews.cartridge_thumbnail != null) | (previews.icon_thumbnail != null)) {
				/** Image */
				preview = (<pathindex.metadatas.Image file_hash={file_hash} previews={previews} />);
			}
		} else {
			//Only master_as_preview (maybe a small element)
			if (master_as_preview_type == "video") {
				var media = {};
				media.url = master_as_preview_url;
				media.label = "Original";
				//content = content + metadatas.view.video.prepare(file_hash, 640, 360, null, [media]);
			} else if (master_as_preview_type == "audio") {
				//content = content + metadatas.view.audio.prepare(file_hash, master_as_preview_url, null);
			}
			//It never be an image as master, this may be
			//security/confidentiality problems.
		}
		
		var analyser_results = [];
		if (mtdsummary.summaries) {
			for (var analyser in mtdsummary.summaries) {
				analyser_results.push(
					<blockquote key={analyser} style={{marginTop: "1em"}}>
						<p>
							{mtdsummary.summaries[analyser]}
						</p>
						<small>
							{analyser}
						</small>
					</blockquote>
				);
			}
		}

		return (
			<div>
				{preview}
				{analyser_results}
			</div>
		);
	}
});