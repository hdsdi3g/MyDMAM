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
 * Copyright (C) hdsdi3g for hd3g.tv 2015-2016
 * 
*/

metadatas.Audio = React.createClass({
	getInitialState: function() {
		return {currentTime: null, duration: null, transport: null};
	},
	transportStatusChange: function(currentTime, duration, ispaused) {
		this.setState({currentTime: currentTime, duration: duration, transport: null});
	},
	goToNewTime: function(new_time) {
		this.setState({
			transport: {gototime: new_time}
		});
	},
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

		var transport_status = null;
		if (metadatas.hasAudioGraphicDeepAnalyst(previews)) {
			transport_status = this.transportStatusChange;
		}

		return (
			<div style={{marginBottom: "1em"}}>
				<metadatas.Mediaplayer
					transport={this.state.transport}
					transport_status={transport_status}
					audio_only={true}
					cantloadingplayerexcuse={i18n("browser.cantloadingplayer")}
					source_url={url} />
				
				<metadatas.AudioGraphicDeepAnalyst
					previews={previews}
					file_hash={file_hash}
					currentTime={this.state.currentTime}
					duration={this.state.duration} 
					goToNewTime={this.goToNewTime} />

				<metadatas.Image file_hash={file_hash} previews={previews} hide_audio_da_grahic={true} prefered_size="cartridge_thumbnail" />
			</div>
		);
	}
});
