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
watchfolders.table =  React.createClass({
	loadActualItemList: function() {
		mydmam.async.request("watchfolders", "list", {}, function(data) {
				this.setState({items: data});
		}.bind(this));
	},
	getInitialState: function() {
		return {
			items: [],
		};
	},
	componentDidMount: function() {
		this.loadActualItemList();
		//this.state.interval = setInterval(this.loadActualItemList, 1000); //TODO remetre !
	},
	componentWillUnmount: function() {
		if (this.state.interval) {
			clearInterval(this.state.interval);
		}
	},
	render: function() {
		var items = this.state.items;
		var items = items.sort(function(a, b) {
		    return a.last_checked - b.last_checked;
		});

		var table_lines = [];
		for (pos in items) {
			var abstract_founded_file = items[pos];

			var tr_classes = classNames({
			    'error': abstract_founded_file.status === "ERROR",
			    'warning': abstract_founded_file.status === "IN_PROCESSING",
			    'info': abstract_founded_file.status === "DETECTED",
			    /*'success': abstract_founded_file.status === "PROCESSED",*/
			});

			table_lines.push(<tr key={pos} className={tr_classes}>
				<td>
					<mydmam.async.pathindex.reactStoragePathLink
						storagename={abstract_founded_file.storage_name}
						path={abstract_founded_file.path}
						add_link={abstract_founded_file.status !== "PROCESSED"} />
				</td>
				<td><mydmam.async.pathindex.reactDate date={abstract_founded_file.date} /></td>
				<td><mydmam.async.pathindex.reactFileSize size={abstract_founded_file.size} /></td>
				<td><mydmam.async.pathindex.reactDate date={abstract_founded_file.last_checked} /></td>
				<td>{abstract_founded_file.status}</td>
			</tr>);
		}
		if (table_lines.length === 0){
			table_lines.push(<tr key="0"><td>No items</td></tr>);
		}
		return (<table className="table table-striped table-bordered table-hover table-condensed">
			<thead>
				<tr>
					<th>F</th>
					<th>D</th>
					<th>S</th>
					<th>LC</th>
					<th>St</th>
				</tr>
			</thead>
			<tbody>	
				{table_lines}
			</tbody>	
		</table>);
	},
});