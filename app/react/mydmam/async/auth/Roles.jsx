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

auth.roleList = function (list) {
	var ctrl_list = [];
	for (pos in list) {
		ctrl_list.push(<li key={pos}>{list[pos]}</li>);
	}
	return ctrl_list;
};


auth.Roles = React.createClass({
	getInitialState: function() {
		return {
			rolelist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "rolelist", null, function(fulllist) {
			this.setState({rolelist: fulllist.roles});
		}.bind(this));
	},
	render: function(){
		var rolelist = this.state.rolelist;
		var items = [];


		for (role_key in rolelist) {
			items.push(<tr key={role_key}>
				<td>
					{rolelist[role_key].role_name}
				</td>
				<td>
					<ul style={{marginLeft: 0, marginBottom: 0}}>
						{auth.roleList(rolelist[role_key].privileges)}
					</ul>
				</td>
			</tr>);
		}

		return (<div>
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("auth.role")}</th>
						<th>{i18n("auth.role.privileges")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		</div>);
	}
});

//	public static RoleView roleCreate(String new_role_name) throws Exception {
//	public static RoleViewList roleDelete(String key) throws Exception {
//	public static RoleView roleChangePrivilege(RoleChPrivileges new_privileges) throws Exception {
