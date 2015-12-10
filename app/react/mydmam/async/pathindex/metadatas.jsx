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

if(!pathindex.metadatas){pathindex.metadatas = {};}

var metadatas = pathindex.metadatas;

metadatas.getFileURL = function(file_hash, file_type, file_name) {
	if (!mydmam.metadatas.url.metadatafile) {
		return "";
	}
	return mydmam.metadatas.url.metadatafile.replace("filehashparam1", file_hash).replace("typeparam2", file_type).replace("fileparam3", file_name);
};

/** ================================== IMAGE REAML ================================== */

metadatas.ImageURL = function(file_hash, thumbnail) {
	if (thumbnail == null) {
		return null;
	}
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

metadatas.chooseTheCorrectImageURL = function(file_hash, previews, prefered_size) {
	return metadatas.ImageURL(file_hash, chooseTheCorrectImageToDisplay(previews, prefered_size));
};

metadatas.Image = React.createClass({
	render: function() {
		var file_hash = this.props.file_hash;
		var previews = this.props.previews;
		var prefered_size = this.props.prefered_size;

		var preview = chooseTheCorrectImageToDisplay(previews, prefered_size);

		if (preview === null) {
			return null;
		}

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

/** ================================== VIDEO REAML ================================== */

var QualityTabs = React.createClass({
	handleClickSwitchSize: function(event) {
		event.preventDefault();
		this.props.onSwitchSize(!this.props.isbigsize);
	},
	handleClickSwitchQuality: function(event) {
		event.preventDefault();
		this.props.onChangeQuality($(event.currentTarget).data("qualid"));
	},
	render: function() {
		var switchsize_icon_class = "icon-resize-full";
		if (this.props.isbigsize) {
			switchsize_icon_class = "icon-resize-small";
		}

		var medias = this.props.medias;
		var selectedquality = this.props.selectedquality;

		var libuttons = [];
		for (var i = 0; i < medias.length; i++) {
			var switch_qual_li_classes = classNames({
		    	'active': (i === selectedquality),
			});
			libuttons.push(
				<li key={i} className={switch_qual_li_classes}>
					<a href={medias[i].url} style={{outline: "none"}} onClick={this.handleClickSwitchQuality} data-qualid={i}>
						{medias[i].label}
					</a>
				</li>
			);
		};
		libuttons.push(
			<li key="switchsize"><a href="" style={{outline: "none"}} onClick={this.handleClickSwitchSize}><i className={switchsize_icon_class}></i></a></li>
		);

		return (
			<ul className="nav nav-tabs">
				{libuttons}
			</ul>
		);
	}
});

metadatas.Video = React.createClass({
	getInitialState: function() {
		return {selectedquality: 0, medias: [], isbigsize: false};
	},
	componentDidMount: function() {
		var master_as_preview_url = this.props.master_as_preview_url;
		var file_hash = this.props.file_hash;
		var previews = this.props.mtdsummary.previews;
		//var reference = this.props.reference;
		var medias = [];

		if (master_as_preview_url) {
			var media = {};
			media.url = master_as_preview_url;
			media.label = "Original";
			medias.push(media);
		}
		if (previews) {
			if (previews.video_hd_pvw) {
				var media = {};
				media.url = metadatas.getFileURL(file_hash, previews.video_hd_pvw.type, previews.video_hd_pvw.file);
				media.label = "HD";
				medias.push(media);
			}
			if (previews.video_sd_pvw) {
				var media = {};
				media.url = metadatas.getFileURL(file_hash, previews.video_sd_pvw.type, previews.video_sd_pvw.file);
				media.label = "SQ";
				medias.push(media);
			}
			if (previews.video_lq_pvw) {
				var media = {};
				media.url = metadatas.getFileURL(file_hash, previews.video_lq_pvw.type, previews.video_lq_pvw.file);
				media.label = "LQ";
				medias.push(media);
			}
		}
		this.setState({medias: medias});
	},
	handleChangeQuality: function(selectedquality) {
		this.setState({selectedquality: selectedquality});
		var video = React.findDOMNode(this.refs.videoplayer);
		var current_time = video.currentTime;
		video.load();
		video.play();
		var gototime = function() {
			this.currentTime = current_time;
			video.removeEventListener('loadedmetadata', gototime);
		};
		video.addEventListener('loadedmetadata', gototime, false);
	},
	handleSwitchSize: function(isbigsize) {
		this.setState({isbigsize: isbigsize});
	},
	render: function() {
		var file_hash = this.props.file_hash;
		var previews = this.props.mtdsummary.previews;

		if (this.state.medias.length === 0) {
			return null;
		}

		var url = this.state.medias[this.state.selectedquality].url;
		var poster = metadatas.chooseTheCorrectImageURL(file_hash, previews);
		// http://www.w3.org/2010/05/video/mediaevents.html

		var width = 640;
		var height = 360;
		var className = null;
		var isbigsize = this.state.isbigsize;
		if (isbigsize) {
			width = null;
			height = null;
			className = "container";
		}

		var video = (
			<video ref="videoplayer" controls="controls" className={className} width={width} height={height} preload="auto" poster={poster}>
				{i18n("browser.cantloadingplayer")}
				<source src={url} />
			</video>
		);

		var content = null;
		if (this.state.medias.length > 1) {
			content = (
				<div className="tabbable tabs-below">
					<div className="tab-content">
						{video}
					</div>
					<QualityTabs
						isbigsize={isbigsize}
						medias={this.state.medias}
						selectedquality={this.state.selectedquality}
						onChangeQuality={this.handleChangeQuality}
						onSwitchSize={this.handleSwitchSize} />
				</div>
			);
		} else {
			content = video;
		}

		return (
			<div style={{marginBottom: "1em"}}>
				{content}
			</div>
		);
	}
});

/** ================================== AUDIO REAML ================================== */

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
