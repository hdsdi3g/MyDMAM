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

bca.Home = React.createClass({
	getInitialState: function() {
		return {
			events: null,
		};
	},
	componentWillMount: function() {
		mydmam.async.request("bca", "allevents", {}, function(data) {
			this.setState({events: data.items});
		}.bind(this));
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
				event = JSON.parse(event);
				event_list.push(<div key={event_key}>
					{event.startdate} {event.name} {event.duration} 
				</div>);
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
