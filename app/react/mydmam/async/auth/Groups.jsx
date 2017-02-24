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
			rolelist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "rolelist", null, function(fulllist) {
			this.setState({rolelist: fulllist.roles});
		}.bind(this));
	},
	render: function(){
		var grouplist = auth.grouplist;
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
					<a href={"#auth/group/edit/" + group_key}>{grouplist[group_key].group_name}</a>
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
			<p><a href="#auth/group/create" className="btn btn-small btn-success">{i18n("auth.groupcreate")}</a></p>
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

auth.GroupCreate = React.createClass({
	onAddBtnClick: function(e){
		var new_group_name = React.findDOMNode(this.refs.group_name).value;
		mydmam.async.request("auth", "groupcreate", new_group_name, function(created) {
			mydmam.async.request("auth", "grouplist", null, function(grouplist) {
				auth.grouplist = grouplist.groups;
				window.location = "#auth/groups";
			}.bind(this));
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.groupcreate")} fluid="false">
				<form className="form-horizontal" onSubmit={this.onAddBtnClick}>
					<FormControlGroup label={i18n("auth.groupname")}>
						<input type="text" placeholder={i18n("auth.groupname")} ref="group_name" />
					</FormControlGroup>

					<FormControlGroup>
						<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("auth.create")}</button>
					</FormControlGroup>

					<FormControlGroup>
						<a type="cancel" className="btn btn-info" href="#auth/groups"><i className="icon-chevron-left icon-white"></i> {i18n("auth.goback")}</a>
					</FormControlGroup>
				</form>
			</mydmam.async.PageHeaderTitle>
		);
	},
});

auth.GroupEdit = React.createClass({
	getInitialState: function() {
		return {
			roles_full_list: [],
			roles: [],
			group_name: "",
		};
	},
	onEditBtnClick: function(e){
		var group_key = this.props.params.group_key;

		mydmam.async.request("auth", "groupchangeroles", {group_roles: this.state.roles, group_key: group_key}, function(list) {
			window.location = "#auth/groups";
		}.bind(this));

	},
	onChangeGroup: function(role, present) {
		var new_roles = [];
		if (present) {
			new_roles = this.state.roles.slice(0);
			new_roles.push(role);
		} else {
			for (pos in this.state.roles) {
				if (this.state.roles[pos] != role) {
					new_roles.push(this.state.roles[pos]);
				}
			}
		}

		var group_key = this.props.params.group_key;
		auth.grouplist[group_key].group_roles = new_roles;

		this.setState({roles: new_roles});
	},
	onDeleteBtnClick: function(e){
		var group_key = this.props.params.group_key;
		if (window.confirm(i18n("auth.confirmremove", this.state.group_name))) {
			mydmam.async.request("auth", "groupdelete", group_key, function(list) {
				delete auth.grouplist[group_key];
				window.location = "#auth/groups";
			}.bind(this));
		}
	},
	componentWillMount: function() {
		var group_key = this.props.params.group_key;

		if (auth.grouplist[group_key]) {
			this.setState({group_name: auth.grouplist[group_key].group_name, roles: auth.grouplist[group_key].group_roles});
		} else {
			window.location = "#auth/groups";
		}

		mydmam.async.request("auth", "rolelist", null, function(rolelist) {
			var list = [];
			for (pos in rolelist.roles) {
				list.push({
					name: rolelist.roles[pos].role_name,
					key: rolelist.roles[pos].key
				});
			}
			this.setState({roles_full_list: list.sort(function(a, b) {
				return a.name < b.name;
			})});
		}.bind(this));

	},
	render: function(){
		var group_key = this.props.params.group_key;
		var FormControlGroup = mydmam.async.FormControlGroup;
		var roles_full_list = this.state.roles_full_list;
		var roles = this.state.roles;

		var cb_list = [];

		for (pos in roles_full_list) {
			var role = roles_full_list[pos];
			var is_checked = roles.indexOf(role.key) > -1;
			cb_list.push(<mydmam.async.CheckboxItem key={role.key} checked={is_checked} reference={role.key} onChangeCheck={this.onChangeGroup}>
				{role.name}
			</mydmam.async.CheckboxItem>);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.groupedit", this.state.group_name)} fluid="false">
				<form className="form-horizontal" onSubmit={this.onEditBtnClick}>
					<FormControlGroup label={i18n("auth.remove-label")}>
						<a className="btn btn-danger" onClick={this.onDeleteBtnClick}><i className="icon-remove icon-white"></i> {i18n("auth.remove")}</a>
					</FormControlGroup>
					
					<FormControlGroup label={i18n("auth.roles")}>
						{cb_list}
					</FormControlGroup>

					<FormControlGroup>
						<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("auth.save")}</button>
					</FormControlGroup>

					<FormControlGroup>
						<a type="cancel" className="btn btn-info" href="#auth/groups"><i className="icon-chevron-left icon-white"></i> {i18n("auth.goback")}</a>
					</FormControlGroup>

				</form>
			</mydmam.async.PageHeaderTitle>
		);

	}
});


mydmam.routes.push("auth-group-create", "auth/group/create",			auth.GroupCreate, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-group-edit", "auth/group/edit/:group_key",		auth.GroupEdit, [{name: "auth", verb: "usercreate"}]);	
