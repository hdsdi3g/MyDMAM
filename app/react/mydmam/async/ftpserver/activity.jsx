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

ftpserver.ActivityList = React.createClass({
	getInitialState: function() {
		return {
			activities: null,
			interval: null
		};
	},
	onWantRefresh: function() {
		var request = {
			user_session_ref: md5(this.props.user_id),
			max_items: 20,
		};

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities});
		}.bind(this));
	},
	componentWillMount: function() {
		this.onWantRefresh();
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.onWantRefresh, 1000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	render: function(){
		var activities = this.state.activities;
		if (activities == null) {
			return (<mydmam.async.PageLoadingProgressBar />);
		}

		var lines = [];
		for(var pos in activities) {
			var activity = activities[pos];

			var size_offset = null;
			if (activity.file_size > 0) {
				if (activity.file_offset > 0) {
					size_offset = (<span style={{marginLeft: 5}} className="label label-important">{activity.file_offset} bytes / {activity.file_size} bytes</span>);
				} else {
					size_offset = (<span style={{marginLeft: 5}} className="label label-important">{activity.file_size} bytes</span>);
				}
			}

			lines.push(<tr key={pos}>
				<td>
					<span style={{backgroundColor: "#" + activity.session_key.substring(0,6), borderRadius: 5, paddingRight: 12, marginRight: 5, border: "1px solid black"}}>&nbsp;</span>
					<mydmam.async.pathindex.reactDate date={activity.activity_date} style={{marginRight: 5}} />
					<span className="label">
						<i className="icon-time icon-white" style={{marginTop: 0}}></i> {Math.round((activity.activity_date - activity.login_time) / 1000)} sec
					</span>
				</td>
				<td>
					<span className="badge badge-info">{i18n("ftpserver.activities.actionenum." + activity.action)}</span>		
				</td>
				<td>
					<code style={{whiteSpace: "normal", color: "black"}}>{activity.working_directory}</code>
				</td>
				<td>
					<code style={{whiteSpace: "normal", color: "black"}}>{activity.argument}</code>
					{size_offset}
				</td>
				<td>
					{activity.client_host}
				</td>
			</tr>);
		}

		return (<small><table style={{marginBottom: "2em"}} className="table table-striped table-hover table-bordered table-condensed">
			<thead>
				<tr>
					<th>Session color &bull; session date &bull; since logon time</th>
					<th>Action</th>
					<th>Directory</th>
					<th>Name &bull; size</th>
					<th>Client IP</th>
				</tr>
			</thead>
			<tbody>
				{lines}
			</tbody>
		</table></small>);
	},
});
