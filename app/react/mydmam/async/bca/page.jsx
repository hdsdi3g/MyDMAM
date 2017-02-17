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

		/*if (event.enddate < mydmam.async.getTime()) {
			return (<span />);
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

		var classname = classNames({
			"muted": event.enddate < mydmam.async.getTime(),
		});

		// {mydmam.format.date(event.enddate)} {event.channel} {event.other}

		return (<div className={classname} style={{borderTop: "1px solid #CCC", borderBottom: "1px solid #CCC", marginTop: "2px", paddingTop: "2px", paddingBottom: "2px", marginBottom: "2px"}}>
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

					&bull; {event.som} &bull;
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
			events: null,
			interval: null,
		};
	},
	componentWillMount: function() {
		this.updateAll(); //TODO add <> delta
	},
	updateAll: function() {
		mydmam.async.request("bca", "allevents", {}, function(data) {
			this.setState({events: data.items});
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
	render: function() {
		var is_loading = null;
		var table = null;

		if (this.state.events == null) {
			is_loading = (<div className="alert alert-info">
			      <h4>{i18n("bca.loading")}</h4>
		    </div>);
		} else {
			var event_list = [];
			for (var event_key in this.state.events) {
				var event = this.state.events[event_key];
				event_list.push(<Event key={event_key} event={JSON.parse(event)} />);
			}

			if (event_list.length > 0) {
				table = (<div>Events:<br />
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
