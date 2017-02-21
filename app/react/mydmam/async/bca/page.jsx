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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/

bca.link = "broadcastautomation";

var Event = React.createClass({ //TODO manage planned -> onair -> asrun
	render: function() {
		var event = this.props.event;
		if (event == null) {
			return null;			
		}
		/*if (event.enddate < mydmam.async.getTime()) {
		}*/

		//<code>{event_key}</code> 
		
		var label_rec = null;
		if (event.recording) {
			label_rec = (<span className="badge badge-important">RECORD</span>);
		}

		var label_paused = null;
		if (event.automation_paused) {
			label_paused = (<span className="badge badge-inverse">HOLD</span>);
		}

		/*var classname = classNames({
			"muted": event.enddate < mydmam.async.getTime(),
		}); className={classname} */

		// {mydmam.format.date(event.enddate)} {event.channel} {event.other}

		return (<div style={{borderTop: "1px solid #CCC", borderBottom: "1px solid #CCC", marginTop: "2px", paddingTop: "2px", paddingBottom: "2px", marginBottom: "2px"}}>
			<div className="row">
				<div className="span1">
					<span style={{fontFamily: "Verdana", fontSize: 12,}}>{mydmam.format.date(event.startdate)}</span>
				</div>
				<div className="span1">
					{label_paused}
				</div>
				<div className="span4">
					<span style={{fontFamily: "Verdana", fontSize: 12,}}>{event.name}</span>
				</div>
				<div className="span1">
					<span style={{fontFamily: "Verdana", fontSize: 12,}}>{event.duration}</span>
				</div>
				<div className="span1">
					<span className="label label-info">{event.video_source}</span>
				</div>
				<div className="span4">
					<span className="label label-success">{event.file_id}</span>
					{event.som}
				</div>
			</div>
			<div className="row">
				<div className="span4 offset2">
					<small className="muted" style={{fontFamily: "Verdana", fontSize: 10,}}>{event.material_type} :: {event.comment} :: {event.automation_id}</small>
				</div>
				<div className="span1 offset1">
					{label_rec}
				</div>
			</div>
		</div>);
		//TODO if start / end is not the same date -> display new date in duration
	},
});

bca.Home = React.createClass({
	getInitialState: function() {
		return {
			interval: null,
			events: null,
			playlist_events_keys: null,
			asruns_events_keys: null,
			display_asuns: false,
			display_channel: null,
		};
	},
	componentWillMount: function() {
		this.getAllEvents(); //TODO add <> delta
	},
	getAllEvents: function() {
		mydmam.async.request("bca", "allevents", {}, function(data) {
			var rawevents = data.items;
			var update = {
				events: {},
				playlist_events_keys: [],
				asruns_events_keys: [],
			}

			for (var event_key in rawevents) {
				var event = JSON.parse(rawevents[event_key]);
				update.events[event_key] = event;

				if (event.enddate < mydmam.async.getTime()) {
					/** asrun */
					update.asruns_events_keys.push(event_key);
				} else {
					/** onair or playlist */
					update.playlist_events_keys.push(event_key);
				}
			}

			this.setState(update);
		}.bind(this));
	},
	componentDidMount: function(){
		//this.setState({interval: setInterval(this.updateAll, 10000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onSwitchSchList: function(new_type) {
		if (new_type == "_playlist") {
			if (this.state.display_asuns == true) {
				this.setState({display_asuns: false});
			}
		} else {
			if (this.state.display_asuns == false) {
				this.setState({display_asuns: true});
			}
		}
	},
	onSwitchChannel: function(channel) {
		console.log(channel);
	},
	render: function() {
		var is_loading = null;
		var table = null;

		if (this.state.events == null) {
			is_loading = (<div className="alert alert-info">
			      <h4>{i18n("bca.loading")}</h4>
		    </div>);
		} else {
			var navtabscontent = [];
			var event_list = [];
			var channels = {};

			var add = function(event_key) {
				var event = this.state.events[event_key];
				event_list.push(<Event key={event_key} event={event} />);
				if (!channels[event.channel]) {
					channels[event.channel] = [];
				}
				channels[event.channel].push(event_key);

				if (this.state.display_channel) {
					//TODO display all / by channel
				}
			}.bind(this);

			if (this.state.display_asuns) {
				for (var pos in this.state.asruns_events_keys) {
					add(this.state.asruns_events_keys[pos]);
				}
			} else {
				for (var pos in this.state.playlist_events_keys) {
					add(this.state.playlist_events_keys[pos]);
				}
			}

			var li_class_playlist = classNames({
				"active": 		!this.state.display_asuns,
			});
			var li_class_asruns = classNames({
				"active": 		this.state.display_asuns,
			});

			navtabscontent.push(<li className={li_class_playlist} key={"_playlist"}>
				<mydmam.async.NavTabsLink pos={"_playlist"} i18nlabel={"bca.playlist"} icon={"icon-road"} onActiveChange={this.onSwitchSchList} />
			</li>);
			navtabscontent.push(<li className={li_class_asruns} key={"_asruns"}>
				<mydmam.async.NavTabsLink pos={"_asruns"} i18nlabel={"bca.asruns"} icon={"icon-time"} onActiveChange={this.onSwitchSchList} />
			</li>);
			navtabscontent.push(<li style={{marginLeft: 30}} key={"_spacer"} />);

			var channel_list = [];
			for (var channel_name in channels) {
				channel_list.push(channel_name);
			}

			channel_list = channel_list.reverse()

			for (var pos in channel_list) {
				var channel_name = channel_list[pos];
				var li_class_channel = classNames("pull-right", {
					"active": false,
				});
				navtabscontent.push(<li className={li_class_channel} key={"channel_" + channel_name}>
					<mydmam.async.NavTabsLink pos={channel_name} i18nlabel={channel_name} onActiveChange={this.onSwitchChannel} />
				</li>);
			}

			if (event_list.length > 0) {
				table = (<div>
					<ul className="nav nav-tabs">
						{navtabscontent}
					</ul>
					{event_list}
				</div>);
			} else {
				table = (<div className="alert alert-block">
				      <h4>{i18n("bca.noeventstodisplay")}</h4>
			    </div>);
			}
		}

 		return (<mydmam.async.PageHeaderTitle title={i18n("bca.page")} fluid="true">
			{is_loading}
			{table}
		</mydmam.async.PageHeaderTitle>);
	},
});

mydmam.routes.push("bca-home", bca.link, bca.Home, [{name: "bca", verb: "allevents"}]);
