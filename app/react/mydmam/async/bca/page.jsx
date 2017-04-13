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

bca.CountDown = createReactClass({
	getInitialState: function() {
		return {
			interval: null,
			count_down: null,
		};
	},
	componentDidMount: function() {
		this.updateTime();
		this.setState({interval: setInterval(this.updateTime, 1000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	updateTime: function() {
		var enddate = this.props.enddate;
		var count_down = enddate - mydmam.async.getTime();

		if (count_down < 0) {
			clearInterval(this.state.interval);
			this.setState({interval: null, count_down: 0});
			if (this.props.onDone) {
				this.props.onDone();
			}
			return;
		}
		this.setState({count_down: count_down});
	},
	render: function() {
		var style_progress = this.props.progressbar;
		var initial_duration = this.props.duration;
		var count_down = this.state.count_down;

		var count_down_displayed = null;
		var percent = 0;

		if (count_down == null) {
			count_down_displayed = mydmam.format.msecToHMSms(initial_duration, true, true);
			percent = 0;
		} else if (this.state.count_down <= 0) {
			count_down_displayed = null;
			percent = 101;
		} else {
			count_down_displayed = mydmam.format.msecToHMSms(this.state.count_down + 1000, true, true);
			percent = Math.ceil(100 * (initial_duration - this.state.count_down) / initial_duration);
		}

		if (style_progress) {
			if (percent == 0) {
				return null;
			} else if (percent == 101) {
				return (<div className="progress progress-striped active">
					<div className="bar bar-danger" style={{"width": "100%"}}></div>
				</div>);
			}

			var style_done = {"width": percent + "%"};
			var style_remaining = {"width": (100 - percent) + "%"};

			return (<div className="progress">
				<div className="bar bar-danger" style={style_done}></div>
			</div>);
		} else {
			return (<div>
				{count_down_displayed}
			</div>);
		}
	}
});

var Event = createReactClass({
	getInitialState: function() {
		return {
			event_is_done: this.props.event.enddate < mydmam.async.getTime(),
		};
	},
	onEventSupposedAired: function() {
		if (this.props.event.automation_paused == false) {
			this.setState({event_is_done: true});
			this.props.onAired(this.props.eventkey);
		}
	},
	render: function() {
		var event = this.props.event;
		if (event == null) {
			return null;			
		}

		var label_next_days = <small>&nbsp;</small>;
		var date_event = new Date(event.startdate);
		var event_day = date_event.getDay();
		if (new Date().getDay() != event_day) {
			label_next_days = (<small className="muted" style={{fontFamily: "Verdana", fontSize: 10,}}>{i18n("day." + event_day)}</small>);
		}

		var duration = (<span style={{fontFamily: "Verdana", fontSize: 12,}}>{event.duration}</span>);
		var progressbar = null;

		if (event.startdate < mydmam.async.getTime() && event.enddate > mydmam.async.getTime()) {
			/** ON AIR */
			duration = (<bca.CountDown progressbar={false} duration={event.enddate - event.startdate} enddate={event.enddate} onDone={this.onEventSupposedAired} />);
			progressbar = (<bca.CountDown progressbar={true} duration={event.enddate - event.startdate} enddate={event.enddate} />);
		}

		var label_rec = null;
		if (event.recording) {
			label_rec = (<span className="badge badge-important pull-right" style={{marginTop: "2px"}}>REC</span>);
		}

		var label_paused = null;
		if (event.automation_paused) {
			label_paused = (<i className="icon-pause"></i>);
		}

		var material_and_comment = null;
		if (event.material_type || event.comment) {
			if (event.material_type && event.comment) {
				material_and_comment = event.material_type + " :: " + event.comment;
			} else if (event.material_type) {
				material_and_comment = event.material_type;
			} else {
				material_and_comment = event.comment;
			}
		}

		var other = [];
		if (event.other) {
			for (var o_type in event.other) {
				var o_value = event.other[o_type];
				if (Array.isArray(o_value)) {
					var o_values = [];
					for (var pos in o_value) {
						if (o_values.indexOf(o_value[pos]) == -1) {
							o_values.push(o_value[pos]);
						}
					}
					other.push(<span key={o_type} title={o_type} className="other">{o_values.join(", ")}</span>);
				} else {
					other.push(<span key={o_type} title={o_type} className="other">{o_value}</span>);
				}
			}
		}

		var global_style = {};
		if (this.state.event_is_done && this.props.is_asrun == false) {
			global_style = {color: "#CCC"};
		}

		return (<div className="bcaevent" style={global_style}>
			<div className="row hidden-phone hidden-tablet">
				<div className="span2">
					<span style={{fontFamily: "Verdana", fontSize: 12,}}>{mydmam.format.date(event.startdate)}</span> {label_paused} {label_rec}
				</div>
				<div className="span4">
					<span style={{fontFamily: "Verdana", fontSize: 12,}}>{event.name}</span>
				</div>
				<div className="span1" style={{fontFamily: "Verdana", fontSize: 12,}}>
					{duration}
				</div>
				<div className="span1">
					<span className="label label-info">{event.video_source}</span>
				</div>
				<div className="span4">
					<span className="label label-success">{event.file_id}</span>
					<small>{other}</small>
				</div>
			</div>
			<div className="row hidden-phone hidden-tablet">
				<div className="span1">
					{label_next_days}
				</div>
				<div className="span4 offset1">
					<small className="muted" style={{fontFamily: "Verdana", fontSize: 10,}}>{material_and_comment}</small>
				</div>
				<div className="span1">
					{progressbar}
				</div>
				<div className="span1">
					<small className="muted">{event.automation_id}</small>
				</div>
				<div className="span1">
					<small className="muted">{event.som}</small>
				</div>
			</div>

			<div className="row visible-phone visible-tablet" style={{fontFamily: "Verdana", fontSize: 12,}}>
				<strong>{mydmam.format.date(event.startdate)} <span style={{marginLeft: "10px"}}>{label_paused}</span> {label_next_days}
				<span className="pull-right">{event.duration.substr(0, 8)}</span></strong><br />

				{event.name}<br />
				<small><span className="text-info">{event.video_source}</span> &bull; <span className="text-success">{event.file_id}</span></small>
			</div>
		</div>);
	},
});

bca.Home = createReactClass({
	getInitialState: function() {
		return {
			interval: null,
			events: null,
			playlist_events_keys: null,
			asruns_events_keys: null,
			display_asuns: false,
			display_channel: null,
			clock: null,
		};
	},
	componentWillMount: function() {
		this.getAllEvents();
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
	getUpdateEventKeys: function() {
		mydmam.async.request("bca", "allkeys", {}, function(data) {
			var raw_events_keys = data.items;
			
			var keys_to_get = [];

			for (var event_key in raw_events_keys) {
				if (!this.state.events[event_key]) {
					keys_to_get.push(event_key);
				}
			}

			var mergueKeys = function(resolved_new_keys) {
				var new_list = JSON.parse(JSON.stringify(this.state.events));

				var keys_to_remove = [];

				/** Prepare remove list */
				for (var event_key in new_list) {
					if (!raw_events_keys[event_key]) {
						keys_to_remove.push(event_key);
					}
				}
				/** Remove ols items */
				for (var pos in keys_to_remove) {
					delete new_list[keys_to_remove[pos]];
				}

				/** Add new items */
				for (var event_key in resolved_new_keys) {
					new_list[event_key] = JSON.parse(resolved_new_keys[event_key]);
				}

				/** Block to send to setState() */
				var update = {
					events: {},
					playlist_events_keys: [],
					asruns_events_keys: [],
				}

				/** Events start date filter */
				var searchMin = function(list) {
					var older_date = Number.MAX_VALUE;
					var older_event_key = null;
					for (var event_key in list) {
						if (list[event_key].startdate < older_date) {
							older_event_key = event_key;
							older_date = list[event_key].startdate;
						} 
					}
					return older_event_key;
				}

				/** Filter by older events (ugly) */
				var event_count = Object.keys(new_list).length;
				for (var i = 0; i < event_count; i++) {
					var older_key = searchMin(new_list);
					var event = new_list[older_key];
					delete new_list[older_key];

					/** Send events main list */
					update.events[older_key] = event;

					/** Send events to tables */
					if (event.enddate < mydmam.async.getTime()) {
						/** asrun */
						update.asruns_events_keys.push(older_key);
					} else {
						/** onair or playlist */
						update.playlist_events_keys.push(older_key);
					}
				}

				if (Object.keys(new_list).length > 0) {
					/** All items should be consumed */
					console.error("PROBLEM!", new_list); //XXX remove after tests
				}

				this.setState(update);
			}.bind(this);

			if (keys_to_get.length > 0) {
				mydmam.async.request("bca", "geteventsbykeys", {items: keys_to_get}, function(data2) {
					mergueKeys(data2.items);
				}.bind(this));
			} else {
				mergueKeys({});
			}

			//this.setState(update);
		}.bind(this));
	},
	componentDidMount: function(){
		var last_time = "";
		var updateClock = function(e) {
			var new_time = mydmam.format.date(mydmam.async.getTime());
			if (last_time != new_time) {
				$(ReactDOM.findDOMNode(this.refs.clock)).text(new_time);
			}
		}.bind(this);

		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
		this.setState({clock: setInterval(updateClock, 100), interval: setInterval(this.getUpdateEventKeys, 5000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onSwitchSchList: function(new_type) {
		var change_state = null;
		if (new_type == "_playlist") {
			if (this.state.display_asuns == true) {
				change_state = "pl";
			}
		} else {
			if (this.state.display_asuns == false) {
				change_state = "ar";
			}
		}

		if (change_state != null) {
			if (this.state.interval) {
				clearInterval(this.state.interval);
			}
			setTimeout(this.getUpdateEventKeys, 100);
			this.setState({
				display_asuns: change_state == "ar",
				interval: setInterval(this.getUpdateEventKeys, 5000),
			});
		}
	},
	onSwitchChannel: function(channel) {
		this.setState({display_channel: channel});
	},
	onActualOnAirEventIsAired: function(event_key) {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}

		var refresh = function() {
			this.getAllEvents();
			this.setState({interval: setInterval(this.getUpdateEventKeys, 5000)});
		}.bind(this);
		setTimeout(refresh, 1000);

		var eventtoremove = this.state.playlist_events_keys.indexOf(event_key);
		if (eventtoremove == 0) {
			if (this.state.playlist_events_keys) {
				this.setState({playlist_events_keys: this.state.playlist_events_keys.slice(1, this.state.playlist_events_keys.length)});
			}
		}
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
				if (!channels[event.channel]) {
					channels[event.channel] = [];
				}
				channels[event.channel].push(event_key);

				if (this.state.display_channel) {
					if (event.channel == this.state.display_channel) {
						event_list.push(<Event key={event_key} event={event} eventkey={event_key} onAired={this.onActualOnAirEventIsAired} is_asrun={this.state.display_asuns} />);
					}
				} else {
					event_list.push(<Event key={event_key} event={event} eventkey={event_key} onAired={this.onActualOnAirEventIsAired} is_asrun={this.state.display_asuns} />);
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

			/** Playlist or as-run tab selection */
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

			/** Channel tab selection */
			var channel_list = [];
			for (var channel_name in channels) {
				channel_list.push(channel_name);
			}

			channel_list = channel_list.reverse();

			for (var pos in channel_list) {
				var channel_name = channel_list[pos];
				var li_class_channel = classNames("pull-right", {
					"active": channel_name == this.state.display_channel,
				});

				navtabscontent.push(<li className={li_class_channel} key={"channel_" + channel_name}>
					<mydmam.async.NavTabsLink pos={channel_name} i18nlabel={channel_name} onActiveChange={this.onSwitchChannel} />
				</li>);
			}

			/** Display all events */
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

		var new_title = i18n("site.name");
		if (this.state.display_asuns) {
			new_title = i18n("bca.asruns") + " :: " + new_title;
		} else {
			new_title = i18n("bca.playlist") + " :: " + new_title;
		}
		if (this.state.display_channel) {
			new_title = this.state.display_channel + " :: " + new_title;
		}

		if (document.title != new_title) {
			document.title = new_title
		}

 		return (<div className="container">
 			<p className="lead">
 				{i18n("bca.automation")}
 				<span className="pull-right hidden-phone hidden-tablet" ref="clock">{mydmam.format.date(mydmam.async.getTime())}</span>
 				<span className="pull-right visible-phone visible-tablet">{mydmam.format.date(mydmam.async.getTime())}</span>
 			</p>
			{is_loading}
			{table}
		</div>);
	},
});

mydmam.routes.push("bca-home", bca.link, bca.Home, [{name: "bca", verb: "allevents"}]);
