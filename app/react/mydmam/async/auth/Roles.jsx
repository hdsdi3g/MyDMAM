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
	var items = [];

	for (pos in list) {
		items.push(list[pos]);
	}
	items.sort();
	for (pos in items) {
		ctrl_list.push(<li key={pos}>{items[pos]}</li>);
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
					<a href={"#auth/role/edit/" + role_key}>{rolelist[role_key].role_name}</a>
				</td>
				<td>
					<ul style={{marginLeft: 0, marginBottom: 0}}>
						{auth.roleList(rolelist[role_key].privileges)}
					</ul>
				</td>
			</tr>);
		}

		return (<div>
			<p><a href="#auth/role/create" className="btn btn-small btn-success">{i18n("auth.rolecreate")}</a></p>
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

auth.RoleCreate = React.createClass({
	onAddBtnClick: function(e){
		var new_role_name = ReactDOM.findDOMNode(this.refs.role_name).value;
		mydmam.async.request("auth", "rolecreate", new_role_name, function(created) {
			window.location = "#auth/roles";
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.rolecreate")} fluid="false">
				<form className="form-horizontal" onSubmit={this.onAddBtnClick}>
					<FormControlGroup label={i18n("auth.rolename")}>
						<input type="text" placeholder={i18n("auth.rolename")} ref="role_name" />
					</FormControlGroup>

					<FormControlGroup>
						<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("auth.create")}</button>
					</FormControlGroup>

					<FormControlGroup>
						<a type="cancel" className="btn btn-info" href="#auth/roles"><i className="icon-chevron-left icon-white"></i> {i18n("auth.goback")}</a>
					</FormControlGroup>
				</form>
			</mydmam.async.PageHeaderTitle>
		);
	},
});

auth.RoleEdit = React.createClass({
	getInitialState: function() {
		return {
			privileges_full_list: [],
			privileges: [],
			role_name: "",
		};
	},
	onEditBtnClick: function(e){
		var role_key = this.props.params.role_key;

		mydmam.async.request("auth", "rolechangeprivilege", {privileges: this.state.privileges, role_key: role_key}, function(list) {
			window.location = "#auth/roles";
		}.bind(this));

	},
	onChangePrivilege: function(privilege, present) {
		var new_privileges = [];
		if (present) {
			new_privileges = this.state.privileges.slice(0);
			new_privileges.push(privilege);
		} else {
			for (pos in this.state.privileges) {
				if (this.state.privileges[pos] != privilege) {
					new_privileges.push(this.state.privileges[pos]);
				}
			}
		}
		this.setState({privileges: new_privileges});
	},
	onDeleteBtnClick: function(e){
		var role_key = this.props.params.role_key;
		if (window.confirm(i18n("auth.confirmremove", this.state.role_name))) {
			mydmam.async.request("auth", "roledelete", role_key, function(list) {
				window.location = "#auth/roles";
			}.bind(this));
		}
	},
	componentWillMount: function() {
		var role_key = this.props.params.role_key;

		mydmam.async.request("auth", "rolelist", null, function(rolelist) {
			if (rolelist.roles[role_key]) {
				this.setState({role_name: rolelist.roles[role_key].rolename, privileges: rolelist.roles[role_key].privileges});
			} else {
				window.location = "#auth/roles";
			}
		}.bind(this));

		mydmam.async.request("auth", "getallprivilegeslist", null, function(privileges_full_list) {
			var list = [];
			for (p in privileges_full_list) {
				list.push(p);
			}
			this.setState({privileges_full_list: list.sort()});
		}.bind(this));

	},
	render: function(){
		var role_key = this.props.params.role_key;
		var FormControlGroup = mydmam.async.FormControlGroup;
		var privileges_full_list = this.state.privileges_full_list;
		var privileges = this.state.privileges;

		var cb_list = [];

		for (pos in privileges_full_list) {
			var p = privileges_full_list[pos];
			var is_checked = privileges.indexOf(p) > -1;
			cb_list.push(<mydmam.async.CheckboxItem key={p} checked={is_checked} reference={p} onChangeCheck={this.onChangePrivilege}>
				{p}
			</mydmam.async.CheckboxItem>);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.roleedit", this.state.role_name)} fluid="false">
				<form className="form-horizontal" onSubmit={this.onEditBtnClick}>
					<FormControlGroup label={i18n("auth.remove-label")}>
						<a className="btn btn-danger" onClick={this.onDeleteBtnClick}><i className="icon-remove icon-white"></i> {i18n("auth.remove")}</a>
					</FormControlGroup>

					<FormControlGroup label={i18n("auth.privileges")}>
						{cb_list}
					</FormControlGroup>

					<FormControlGroup>
						<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("auth.save")}</button>
					</FormControlGroup>

					<FormControlGroup>
						<a type="cancel" className="btn btn-info" href="#auth/roles"><i className="icon-chevron-left icon-white"></i> {i18n("auth.goback")}</a>
					</FormControlGroup>
				</form>
			</mydmam.async.PageHeaderTitle>
		);

	}
});


mydmam.routes.push("auth-role-create", "auth/role/create",			auth.RoleCreate, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-role-edit", "auth/role/edit/:role_key",	auth.RoleEdit, [{name: "auth", verb: "usercreate"}]);	
