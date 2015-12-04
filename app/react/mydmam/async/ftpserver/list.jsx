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

ftpserver.UserList = React.createClass({
	getInitialState: function() {
		return {
			users: null,
			delete_enabled: false,
			sorted_col: null,
			sorted_order: null,
		};
	},
	componentWillMount: function() {
		this.onWantRefreshAll();
	},
	onWantRefreshAll: function() {
		mydmam.async.request("ftpserver", "allusers", {}, function(data) {
			this.setState({users: data.users});
		}.bind(this));
	},
	onUnLockDelete: function() {
		this.setState({delete_enabled: React.findDOMNode(this.refs.cb_unlockdelete).checked});
	},
	onChangeColSort: function(colname, previous_order) {
		var order = null;
		if (previous_order == null) {
			order = "desc";
		} else if (previous_order === "desc") {
			order = "asc";
		}
		this.setState({
			sorted_col: colname,
			sorted_order: order,
		});
	},
	sortUsers: function() {
		return this.state.users.slice().sort(function(a, b) {
			if (this.state.sorted_col == null | this.state.sorted_order == null) {
			    return a.user_name > b.user_name;
			}
			var comp_a;
			var comp_b;
			switch (this.state.sorted_col) {
			    case "user": 		comp_a = a.user_id; 	comp_b = b.user_id;		break;
			    case "group": 		comp_a = a.group_name;	comp_b = b.group_name;	break;
			    case "created": 	comp_a = a.create_date;	comp_b = b.create_date;	break;
			    case "updated": 	comp_a = a.update_date;	comp_b = b.update_date;	break;
			    case "lastlogin": 	comp_a = a.last_login;	comp_b = b.last_login; 	break;
			    case "enabled": 	comp_a = a.enabled;		comp_b = b.enabled; 	break;
			}
			if (this.state.sorted_order === "asc") {
				return comp_a > comp_b;
			} else {
				return comp_a < comp_b;
			}
		}.bind(this));
	},
	getCurrentColSort: function(colname) {
		return this.state.sorted_col === colname ? this.state.sorted_order : null;
	},
	render: function() {
		if (this.state.users == null) {
			return (<mydmam.async.PageHeaderTitle title="FTP user list" fluid="true">
				<mydmam.async.PageLoadingProgressBar />
			</mydmam.async.PageHeaderTitle>);
		}

		var isAdmin = ftpserver.hasUserAdminRights();
		var users = this.sortUsers();

		var table_lines = [];
		for (pos_user in users) {
			table_lines.push(<ftpserver.UserLine key={pos_user} user={users[pos_user]} onWantRefreshAll={this.onWantRefreshAll} delete_enabled={this.state.delete_enabled} />);
		}
		
		var BtnAdduser = null;
		if (isAdmin) {
			BtnAdduser = (<ftpserver.BtnAddUserForm />);
		}

		if (table_lines.length === 0) {
			return (<mydmam.async.PageHeaderTitle title="FTP user list" fluid="true">
				<mydmam.async.AlertInfoBox>No FTP users!</mydmam.async.AlertInfoBox>
				{BtnAdduser}
			</mydmam.async.PageHeaderTitle>);
		}

		var ButtonSort = mydmam.async.ButtonSort;

		return (<mydmam.async.PageHeaderTitle title="FTP user list" fluid="true">
			{BtnAdduser}
			<button className="btn btn-small" onClick={this.onWantRefreshAll} style={{marginLeft: 5}}><i className="icon-refresh"></i></button>
			<label className="checkbox pull-right"><input type="checkbox" onClick={this.onUnLockDelete} ref="cb_unlockdelete" />Unlock delete</label>
			<hr />
			<table className="table table-striped table-hover table-bordered table-condensed">
				<thead>
					<tr>
						<th>
							User
							<ButtonSort onChangeState={this.onChangeColSort} colname="user" order={this.getCurrentColSort("user")} />
						</th>
						<th>
							Group
							<ButtonSort onChangeState={this.onChangeColSort} colname="group" order={this.getCurrentColSort("group")} />
						</th>
						<th>
							Created at
							<ButtonSort onChangeState={this.onChangeColSort} colname="created" order={this.getCurrentColSort("created")} />
						</th>
						<th>
							Updated at
							<ButtonSort onChangeState={this.onChangeColSort} colname="updated" order={this.getCurrentColSort("updated")} />
						</th>
						<th>
							Last login at
							<ButtonSort onChangeState={this.onChangeColSort} colname="lastlogin" order={this.getCurrentColSort("lastlogin")} />
						</th>
						<th>
							<ButtonSort onChangeState={this.onChangeColSort} colname="enabled" order={this.getCurrentColSort("enabled")} />
						</th>
					</tr>
				</thead>
				<tbody>	
					{table_lines}
				</tbody>	
			</table>
		</mydmam.async.PageHeaderTitle>);
	},
});

mydmam.routes.push("ftpserver-ShowUserList", "ftpserver", ftpserver.UserList, [{name: "ftpserver", verb: "allusers"}]);	

ftpserver.UserLine = React.createClass({
	onAction: function(request) {
		mydmam.async.request("ftpserver", "adminoperationuser", request, function(data) {
			if (data.done) {
				this.props.onWantRefreshAll();
			}
		}.bind(this));
	},
	onToogleEnableDisable: function() {
		this.onAction({
			user_id: this.props.user.user_id,
			operation: "TOGGLE_ENABLE",
		});
	},
	onDelete: function() {
		this.onAction({
			user_id: this.props.user.user_id,
			operation: "DELETE",
		});
	},
	render: function() {
		var user = this.props.user;
		var domain = user.domain;
		if (domain === "") {	
			domain = "default";
		}

		var btns_admin = null;
		if (ftpserver.hasUserAdminRights()) {
			btns_admin = (<span style={{marginLeft: 5}}>
				<a className="btn btn-mini" style={{marginRight: 5}} href={"#ftpserver/edit/" + this.props.user.user_id}>Change password</a>
				<mydmam.async.BtnDelete label="Delete" enabled={this.props.delete_enabled} onClickDelete={this.onDelete} reference={user.user_id} />
			</span>);
		}

		return (<tr>
			<td>
				<strong>{user.domain}</strong> :: {user.user_name}
			</td>
			<td>
				<span className="label label-inverse">{user.group_name}</span>
			</td>
			<td><mydmam.async.pathindex.reactDate date={user.create_date} style={{}} /></td>
			<td><mydmam.async.pathindex.reactDate date={user.update_date} style={{}} /></td>
			<td><mydmam.async.pathindex.reactDate date={user.last_login} style={{}} /></td>
			<td><span className="pull-right"><mydmam.async.BtnEnableDisable
				simplelabel={!ftpserver.hasUserAdminRights()}
				enabled={user.enabled}
				labelenabled="Enabled"
				labeldisabled="Disabled"
				iconcircle={true}
				onEnable={this.onToogleEnableDisable}
				onDisable={this.onToogleEnableDisable}
				reference={user.user_id} />
				{btns_admin}
			</span></td>
		</tr>);
	},	
});

