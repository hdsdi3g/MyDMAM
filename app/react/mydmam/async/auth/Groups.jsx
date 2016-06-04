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

auth.Groups = React.createClass({
	getInitialState: function() {
		return {
			grouplist: {},
			rolelist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "grouplist", null, function(grouplist) {
			this.setState({grouplist: grouplist.groups});
		}.bind(this));
		mydmam.async.request("auth", "rolelist", null, function(fulllist) {
			this.setState({rolelist: fulllist.roles});
		}.bind(this));
	},
	render: function(){
		var grouplist = this.state.grouplist;
		var rolelist = this.state.rolelist;

		var items = [];
		var real_role_list = {};

		var toList = function (group_key, list) {
			var ctrl_list = [];
			for (pos in list) {
				var role_key = list[pos];
				if (rolelist[role_key]) {
					var this_role_privileges = rolelist[role_key].privileges;
					for (pos_p in this_role_privileges) {
						if (real_role_list[group_key].indexOf(this_role_privileges[pos_p]) == -1) {
							real_role_list[group_key].push(this_role_privileges[pos_p]);
						}
					}
					ctrl_list.push(<li key={pos}>{rolelist[role_key].role_name}</li>);
				} else {
					ctrl_list.push(<li key={pos}>{list[pos]}</li>);
				}
			}
			return ctrl_list;
		};

		for (group_key in grouplist) {
			real_role_list[group_key] = [];

			items.push(<tr key={group_key}>
				<td>
					{grouplist[group_key].group_name}
				</td>
				<td>
					<ul style={{marginLeft: 0, marginBottom: 0}}>
						{toList(group_key, grouplist[group_key].group_roles)}
					</ul>
				</td>
				<td>
					<ul style={{marginLeft: 0, marginBottom: 0}}>
						{auth.roleList(real_role_list[group_key])}
					</ul>
				</td>
			</tr>);
		}

		return (<div>
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("auth.group")}</th>
						<th>{i18n("auth.roles")}</th>
						<th>{i18n("auth.privileges")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		</div>);
	}
});

//	public static GroupView groupCreate(String new_group_name) throws Exception {
//	public static GroupViewList groupDelete(String key) throws Exception {
//	public static GroupView groupChangeRoles(GroupChRole ch_group) throws Exception {
