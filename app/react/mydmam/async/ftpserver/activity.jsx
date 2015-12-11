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
			interval: null,
			max_items: 20,
			searched_text: null,
			searched_action_type: "ALL",
		};
	},
	onWantRefresh: function() {
		var request = {
			max_items: this.state.max_items,
			searched_text: this.state.searched_text,
			searched_action_type: this.state.searched_action_type,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities});
		}.bind(this));
	},
	onWantExpandList: function() {
		var request = {
			max_items: this.state.max_items + 20,
			searched_text: this.state.searched_text,
			searched_action_type: this.state.searched_action_type,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities, max_items: request.max_items});
		}.bind(this));
	},
	componentWillMount: function() {
		this.onWantRefresh();
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.onWantRefresh, 10000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onTextSearch: function(request_searched_text) {
		var searched_text = request_searched_text.trim();
		if (searched_text == "") {
			searched_text = null;
		}
		var request = {
			max_items: this.state.max_items,
			searched_text: searched_text,
			searched_action_type: this.state.searched_action_type,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities, searched_text: request.searched_text});
		}.bind(this));
	},
	onSelectActionTypeChange: function() {
		var request = {
			max_items: this.state.max_items,
			searched_text: this.state.searched_text,
			searched_action_type: React.findDOMNode(this.refs.select_action).value,
		};
		if (this.props.user_id) {
			request.user_session_ref = md5(this.props.user_id);
		}

		mydmam.async.request("ftpserver", "recentactivities", request, function(data) {
			this.setState({activities: data.activities, searched_action_type: request.searched_action_type});
		}.bind(this));
	},
	render: function(){
		var activities = this.state.activities;
		if (activities == null) {
			return (<mydmam.async.PageLoadingProgressBar />);
		}

		var no_activity_label = null;
		if (activities.length === 0) {
			if (this.state.searched_text == null & this.state.searched_action_type == "ALL") {
				return (<mydmam.async.AlertInfoBox>{i18n("ftpserver.activities.emptylist")}</mydmam.async.AlertInfoBox>);
			}
			no_activity_label = (<mydmam.async.AlertInfoBox>{i18n("ftpserver.activities.noresults")}</mydmam.async.AlertInfoBox>);
		}

		var th_display_user = null;
		if (this.props.user_id == null) {
			th_display_user = (<th>{i18n("ftpserver.activities.user")}</th>);
		}

		var lines = [];
		for(var pos in activities) {
			var activity = activities[pos];

			var size_offset = null;
			if (activity.file_size > 0) {
				if (activity.file_offset > 0) {
					size_offset = (<span style={{marginLeft: 5}} className="label label-important pull-right">
						{i18n("ftpserver.activities.bytes", activity.file_offset)} / {i18n("ftpserver.activities.bytes", activity.file_size)}
					</span>);
				} else {
					size_offset = (<span style={{marginLeft: 5}} className="label label-important pull-right">
						{i18n("ftpserver.activities.bytes", activity.file_size)}
					</span>);
				}
			}

			var td_display_user = null;
			if (this.props.user_id == null) {
				td_display_user = (<td>
					<strong>{activity.user_domain}</strong> :: {activity.user_name} <span className="label label-inverse pull-right">{activity.user_group}</span>
				</td>);
			}

			lines.push(<tr key={pos}>
				{td_display_user}
				<td>
					<span style={{backgroundColor: "#" + activity.session_key.substring(0,6), borderRadius: 5, paddingRight: 12, marginRight: 5, border: "1px solid black"}}>&nbsp;</span>
					<mydmam.async.pathindex.reactDate date={activity.activity_date} style={{marginRight: 5}} />
					<mydmam.async.pathindex.reactSinceDate date={activity.activity_date} i18nlabel="ftpserver.activities.since" />
				</td>
				<td>
					<span className="badge badge-info">{i18n("ftpserver.activities.actionenum." + activity.action)}</span>		
				</td>
				<td>
					<code style={{whiteSpace: "normal", color: "#080"}}>{activity.working_directory}</code>
					<code style={{whiteSpace: "normal", color: "black"}}>{activity.argument}</code>
					{size_offset}
				</td>
				<td>
					{activity.client_host}
				</td>
			</tr>);
		}

		var button_show_next_items = null;
		if (activities.length >= 20) {
			if (this.state.max_items < 100) {
				button_show_next_items = (<button className="btn btn-mini btn-block" style={{marginBottom: "2em"}} onClick={this.onWantExpandList}>
					<i className="icon-arrow-down"></i> {i18n("ftpserver.activities.next")} <i className="icon-arrow-down"></i>
				</button>);
			} else {
				button_show_next_items = (<button className="btn btn-mini btn-block disabled" style={{marginBottom: "2em"}}>
					{i18n("ftpserver.activities.nextdisabled", this.state.max_items)}
				</button>);
			}
		}

		return (<div>
			<span className="lead" style={{marginLeft: "0.5em"}}>{i18n("ftpserver.activities.activityandsessions")}</span>
		    <form className="form-search pull-right">
				<select ref="select_action" onChange={this.onSelectActionTypeChange}>
					<option key="ALL" value="ALL">		{i18n("ftpserver.activities.search_by_select.ALL")}</option>
					<option key="IO" value="IO">		{i18n("ftpserver.activities.search_by_select.IO")}</option>
					<option key="STORE" value="STORE">	{i18n("ftpserver.activities.search_by_select.STORE")}</option>
					<option key="RESTOR" value="RESTOR">{i18n("ftpserver.activities.search_by_select.RESTOR")}</option>
					<option key="DELETE" value="DELETE">{i18n("ftpserver.activities.search_by_select.DELETE")}</option>
					<option key="MKDIR" value="MKDIR">	{i18n("ftpserver.activities.search_by_select.MKDIR")}</option>
					<option key="RENAME" value="RENAME">{i18n("ftpserver.activities.search_by_select.RENAME")}</option>
				</select>
				<mydmam.async.SearchInputBox style={{marginLeft: "0.5em"}} onKeyPress={this.onTextSearch} />
		    </form>
			<table style={{marginBottom: "1em"}} className="table table-striped table-hover table-bordered table-condensed">
				<thead>
					<tr>
						{th_display_user}
						<th>{i18n("ftpserver.activities.session.color")} &bull; {i18n("ftpserver.activities.session.date")}</th>
						<th>{i18n("ftpserver.activities.title.action")}</th>
						<th>{i18n("ftpserver.activities.title.directory")} &bull; {i18n("ftpserver.activities.title.name")} &bull; {i18n("ftpserver.activities.title.size")}</th>
						<th>{i18n("ftpserver.activities.title.clientip")}</th>
					</tr>
				</thead>
				<tbody>
					{lines}
				</tbody>
			</table>
			{no_activity_label}
			{button_show_next_items}
		</div>);
	},
});
