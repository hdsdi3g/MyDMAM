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

manager.Summaries = React.createClass({
	getInitialState: function() {
		return {
			list: {},
			interval: null,
		};
	},
	componentWillMount: function() {
		this.refresh();
	},
	refresh: function() {
		mydmam.async.request("instances", "allsummaries", null, function(list) {
			this.setState({list: list});
		}.bind(this));
	},
	componentDidMount: function(){
		this.setState({interval: setInterval(this.refresh, 10000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	render: function() {
		var items = [];
		for (var instance_ref in this.state.list) {
			items.push(<manager.InstanceSummary key={instance_ref} instance={this.state.list[instance_ref]} />);
		}
		return (
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("manager.instance.host")}</th>
						<th>{i18n("manager.instance.manager")}</th>
						<th>{i18n("manager.instance.version")}</th>
						<th>{i18n("manager.instance.uptime")}</th>
						<th>{i18n("manager.instance.jvm")}</th>
						<th>{i18n("manager.instance.addr")}</th>
						<th>{i18n("manager.instance.os")}</th>
						<th>{i18n("manager.instance.user")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		);
	},
});

manager.InstanceSummary = React.createClass({
	render: function() {

		var addr = [];
		for (var pos in this.props.instance.host_addresses) {
			addr.push(<span key={pos}>&bull; {this.props.instance.host_addresses[pos]}<br /></span>);
		}

		var arch = null;
		if (this.props.instance.os_arch != "x86_64") {
			arch = this.props.instance.os_arch;
		}

		return (<tr>
			<td>{this.props.instance.host_name}<br />
				<small className="muted">P:{this.props.instance.pid} C:{this.props.instance.cpucount}</small>
			</td>
			<td>
				{this.props.instance.instance_name}<br />
				<em>{this.props.instance.app_name}</em>
			</td>
			<td>
				{this.props.instance.app_version}
			</td>
			<td>
				<mydmam.async.pathindex.reactSinceDate i18nlabel="manager.instance.uptime" date={this.props.instance.starttime} />
			</td>
			<td>
				{this.props.instance.java_version}<br />
				<small className="muted">
					{this.props.instance.java_vendor}
				</small>
			</td>
			<td>{addr}</td>
			<td>
				<small>
					{this.props.instance.os_name}<br />
					{this.props.instance.os_version} {arch}
				</small>
			</td>
			<td>
				<small>
					{this.props.instance.user_name} &bull; {this.props.instance.user_language}_{this.props.instance.user_country}<br />
					{this.props.instance.user_timezone}
				</small>
			</td>
		</tr>);
	},
});

