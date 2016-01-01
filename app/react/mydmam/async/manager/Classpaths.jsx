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

manager.Classpaths = React.createClass({
	getInitialState: function() {
		return {
			list: {},
			instances: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("instances", "allclasspaths", null, function(list) {
			this.setState({list: list});
		}.bind(this));
	},
	render: function() {
		var items = [];
		var declared_classpath = [];
		var instance_refs_to_resolve = [];

		/**
		 * Mergue all CP for create a reference list.
		 */
		var current_classpath;
		for (var instance_ref in this.state.list) {
			current_classpath = this.state.list[instance_ref];
			for (var pos in current_classpath) {
				if (declared_classpath.indexOf(current_classpath[pos]) == -1) {
					declared_classpath.push(current_classpath[pos]);
				}
			}
		}

		for (pos in declared_classpath) {
			for (var instance_ref in this.state.list) {
				current_classpath = this.state.list[instance_ref];
				if (current_classpath.indexOf(declared_classpath[pos]) == -1) {
					var instance_info = instance_ref;
					if (this.state.instances[instance_ref]) {
						if (this.state.instances[instance_ref] !== "nope") {
							var summary = this.state.instances[instance_ref].summary;
							instance_info = summary.instance_name + " (" + summary.app_name + ") " + summary.host_name;
						} else {
							instance_info = i18n("manager.classpath.notfound") + " :: " + instance_ref;
						}
					} else {
						instance_refs_to_resolve.push(instance_ref);
					}

					items.push(<tr key={md5(declared_classpath[pos] + instance_ref)}>
						<td>{declared_classpath[pos]}</td>
						<td>{instance_info}</td>
					</tr>);
				}
			}
		}
		
		if (instance_refs_to_resolve.length > 0) {
			mydmam.async.request("instances", "byrefs", {refs: instance_refs_to_resolve}, function(data) {
				for (var pos in instance_refs_to_resolve) {
					if (data[instance_refs_to_resolve[pos]] == null) {
						data[instance_refs_to_resolve[pos]] = "nope";
					}
				}
				this.setState({instances: jQuery.extend({}, this.state.instances, data)});
			}.bind(this));
		}

		return (
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("manager.classpath.missing")}</th>
						<th>{i18n("manager.classpath.missingin")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		);
	},
});
