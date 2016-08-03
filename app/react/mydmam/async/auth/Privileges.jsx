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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/

auth.Privileges = React.createClass({
	getInitialState: function() {
		return {
			fulllist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "getallprivilegeslist", null, function(rawfulllist) {
			var fulllist = {};
			for (p in rawfulllist) {
				fulllist[p] = rawfulllist[p].sort();
			}
			this.setState({fulllist: fulllist});
		}.bind(this));
	},
	render: function(){
		var fulllist = this.state.fulllist;
		var items = [];

		var toList = function (list) {
			var ctrl_list = [];
			for (pos in list) {
				ctrl_list.push(<div key={pos}>{list[pos]}</div>);
			}
			return ctrl_list;
		};

		for (privilege_name in fulllist) {
			items.push(<tr key={privilege_name}>
				<td>{privilege_name}</td>
				<td>{toList(fulllist[privilege_name])}</td>
			</tr>);
		}

		return (<div>
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("auth.privilege")}</th>
						<th>{i18n("auth.privilege.controllers")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		</div>);
	}
});
