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

metadatas.ImageURL = function(file_hash, thumbnail) {
	return metadatas.getFileURL(file_hash, thumbnail.type, thumbnail.file);
};

var chooseTheCorrectImageToDisplay = function(previews, prefered_size) {
	if (prefered_size == null) {
		prefered_size = "full_size_thumbnail";
	}

	if (prefered_size === "full_size_thumbnail") {
		if (previews.full_size_thumbnail) {
			return previews.full_size_thumbnail;
		} else {
			prefered_size = "cartridge_thumbnail";
		}
	}

	if (prefered_size === "cartridge_thumbnail") {
		if (previews.cartridge_thumbnail) {
			return previews.cartridge_thumbnail;
		} else {
			prefered_size = "icon_thumbnail";
		}
	}

	if (prefered_size === "icon_thumbnail") {
		if (previews.icon_thumbnail) {
			return previews.icon_thumbnail;
		}
	}

	return null;
};

metadatas.Image = React.createClass({
	render: function() {
		var file_hash = this.props.file_hash;
		var previews = this.props.previews;
		var prefered_size = this.props.prefered_size;

		var preview = chooseTheCorrectImageToDisplay(previews, prefered_size);

		var url = metadatas.ImageURL(file_hash, preview);
		var width = preview.options.width;
		var height = preview.options.height;
		
		var image = null;
		if ((width > 0) & (height > 0)) {
			image = (
				<img src={url} className="img-polaroid" alt={width + "x" + height} style={{width: width, height: height}} />
			);
		} else {
			image = (
				<img src={url} className="img-polaroid" />
			);
		}

		return (
			<div style={{marginBottom: "1em"}}>
				{image}
			</div>
		);
	}
});
