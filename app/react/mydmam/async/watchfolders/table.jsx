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
watchfolders.table =  createReactClass({
	loadActualItemList: function() {
		mydmam.async.request("watchfolders", "list", {}, function(data) {
			this.setState({items: data.items, jobs: data.jobs});
		}.bind(this));
	},
	getInitialState: function() {
		return {
			items: [],
			jobs: {},
		};
	},
	componentWillMount: function() {
		this.loadActualItemList();
		this.setState({interval: setInterval(this.loadActualItemList, 1000)});
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	onDelete: function(abstract_founded_file) {
		var storage_name = abstract_founded_file.storage_name;
		var path = abstract_founded_file.path;
		var key = md5(storage_name + ":" + path);

		var pos_state_item = -1;
		for (pos in this.state.items) {
			if (this.state.items[pos].storage_name === storage_name && this.state.items[pos].path === path) {
				pos_state_item = pos;
				break;
			}
		}

		this.state.items.splice(pos_state_item, 1);
		this.setState({items : this.state.items});

		if (this.state.interval) {
			clearInterval(this.state.interval);
		}

		mydmam.async.request("watchfolders", "remove", {key: key}, function(data) {
			this.setState({interval : setInterval(this.loadActualItemList, 1000)});
		}.bind(this));
	},
	render: function() {
		var items = this.state.items;
		var jobs = this.state.jobs;

		var items = items.sort(function(a, b) {
		    return a.last_checked - b.last_checked;
		});

		var table_lines = [];
		for (pos in items) {
			table_lines.push(<watchfolders.AbstractFoundedFile key={pos} abstract_founded_file={items[pos]} jobs={jobs} onDelete={this.onDelete} />);
		}

		return (<table className="table table-striped table-bordered table-hover table-condensed">
			<thead>
				<tr>
					<th>{i18n("manager.watchfolders.table.file")}</th>
					<th>{i18n("manager.watchfolders.table.filedate")}</th>
					<th>{i18n("manager.watchfolders.table.size")}</th>
					<th>{i18n("manager.watchfolders.table.lastchecked")}</th>
					<th>{i18n("manager.watchfolders.table.status")}</th>
					<th>{i18n("manager.watchfolders.table.jobs")}</th>
					<th></th>
				</tr>
			</thead>
			<tbody>	
				{table_lines}
			</tbody>	
		</table>);
	},
});

mydmam.routes.push("watchfolders", "watchfolders", watchfolders.table, [{name: "watchfolders", verb: "list"}]);	
