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

metadatas.Audio = React.createClass({
	render: function() {
		var file_hash = this.props.file_hash;
		var previews = this.props.mtdsummary.previews;
		var mimetype = this.props.mtdsummary.mimetype;
		var reference = this.props.reference;
		var master_as_preview_url = this.props.master_as_preview_url;

		var url = null;
		if (master_as_preview_url) {
			url = master_as_preview_url;
		} else if (previews) {
			url = metadatas.getFileURL(file_hash, previews.audio_pvw.type, previews.audio_pvw.file);
		}

		if (url == null) {
			return null;
		}

		return (
			<div style={{marginBottom: "1em"}}>
				<audio controls="controls" preload="auto">
					{i18n("browser.cantloadingplayer")}
					<source src={url} />
				</audio>
				<div className="pull-right">
					<metadatas.Image file_hash={file_hash} previews={previews} prefered_size="cartridge_thumbnail" />
				</div>
			</div>
		);
	}
});
