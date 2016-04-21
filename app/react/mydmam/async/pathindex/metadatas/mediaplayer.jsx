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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/

metadatas.Mediaplayer = React.createClass({
	propTypes: {
		audio_only: 	React.PropTypes.bool,
		className: 		React.PropTypes.oneOfType([
							React.PropTypes.string,
							React.PropTypes.object,
						]),
		width: 			React.PropTypes.number, 
		height: 		React.PropTypes.number,
		poster: 		React.PropTypes.string,
		cantloadingplayerexcuse: React.PropTypes.string,
		source_url: 	React.PropTypes.string,
		transport: 		React.PropTypes.object,
		transport_status: React.PropTypes.func
	},
	getInitialState: function() {
		return {
			//interval: null,
		};
	},
	componentDidMount: function(){
		//this.setState({interval: setInterval(this.refresh_transport_status, 10000)});
		if (this.props.transport_status) {
			var video = React.findDOMNode(this.refs.videoplayer);
			video.addEventListener('timeupdate', this.refresh_transport_status, false);
		}
	},
	componentWillUnmount: function() {
		/*if (this.state.interval) {
			clearInterval(this.state.interval);
		}*/
		var video = React.findDOMNode(this.refs.videoplayer);
		video.removeEventListener('timeupdate', this.refresh_transport_status);
	},
	refresh_transport_status: function () {
		if (this.props.transport_status == null) {
			return;
		}
		var video = React.findDOMNode(this.refs.videoplayer);
		this.props.transport_status(video.currentTime, video.duration, video.paused);
	},
	componentWillReceiveProps: function(nextprops) {
		var transport = nextprops.transport;
		if (transport == null) {
			return;
		}
		var video = React.findDOMNode(this.refs.videoplayer);

		if (transport.macro) {
			if (transport.macro == "RELOAD_PLAY") {
				/** Player URL has changed. Get current the position, reload, play, and goto to the last position. */

				video.removeEventListener('timeupdate', this.refresh_transport_status);

				var current_time = video.currentTime;
				video.load();
				video.play();
				var refresh_transport_status = this.refresh_transport_status;
				var gototime = function() {
					this.currentTime = current_time;
					video.removeEventListener('loadedmetadata', gototime);
					video.addEventListener('timeupdate', refresh_transport_status, false);
				};
				video.addEventListener('loadedmetadata', gototime, false);
			}
		} else if (transport.gototime != null) {
			//if (video.paused == false) {
				//video.removeEventListener('timeupdate', this.refresh_transport_status);
				video.currentTime = transport.gototime;
				//video.addEventListener('timeupdate', this.refresh_transport_status, false);
			//}
		}
	},
	render: function() {
		// http://www.w3.org/2010/05/video/mediaevents.html
		if (this.props.audio_only) {
			return (<audio ref="videoplayer" controls="controls" preload="auto">
					{this.props.cantloadingplayerexcuse}
				<source src={this.props.source_url} />
			</audio>);
		} else {
			return (<video ref="videoplayer" controls="controls" className={this.props.className} width={this.props.width} height={this.props.height} preload="auto" poster={this.props.poster}>
					{this.props.cantloadingplayerexcuse}
				<source src={this.props.source_url} />
			</video>);
		}
	}
});
