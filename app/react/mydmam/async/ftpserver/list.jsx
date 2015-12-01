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
	loadActualUsersList: function() {
		mydmam.async.request("ftpserver", "allusers", {}, function(data) {
			this.setState({users: data.users});
		}.bind(this));
	},
	getInitialState: function() {
		return {
			users: null,
			delete_enabled: false,
		};
	},
	componentWillMount: function() {
		this.loadActualUsersList();
	},
	componentWillUnmount: function() {
	},
	onWantRefreshAll: function() {
		mydmam.async.request("ftpserver", "allusers", {}, function(data) {
			this.setState({users: data.users});
		}.bind(this));
	},
	onUnLockDelete: function() {
		this.setState({delete_enabled: React.findDOMNode(this.refs.cb_unlockdelete).checked});
	},
	render: function() {
		var users = this.state.users;

		if (users == null) {
			return (<mydmam.async.PageHeaderTitle title="FTP user list" fluid="true">
				<mydmam.async.PageLoadingProgressBar />
			</mydmam.async.PageHeaderTitle>);
		}

		var isAdmin = ftpserver.hasUserAdminRights();

		var table_lines = [];
		for (pos_user in users) {
			table_lines.push(<ftpserver.UserLine key={pos_user} user={users[pos_user]} onWantRefreshAll={this.onWantRefreshAll} delete_enabled={this.state.delete_enabled} />);
		}
		
		var BtnAdduser = null;
		if (isAdmin) {
			BtnAdduser = (<ftpserver.BtnAddUserForm />);
		}

		/*<thead>
				<tr>
					<th>{i18n("manager.watchfolders.table.file")}</th>
					<th>{i18n("manager.watchfolders.table.filedate")}</th>
					<th>{i18n("manager.watchfolders.table.size")}</th>
					<th>{i18n("manager.watchfolders.table.lastchecked")}</th>
					<th>{i18n("manager.watchfolders.table.status")}</th>
					<th>{i18n("manager.watchfolders.table.jobs")}</th>
					<th></th>
				</tr>
			</thead>*/

		if (table_lines.length === 0) {
			return (<mydmam.async.PageHeaderTitle title="FTP user list" fluid="true">
				<mydmam.async.AlertInfoBox>No FTP users!</mydmam.async.AlertInfoBox>
				{BtnAdduser}
			</mydmam.async.PageHeaderTitle>);
		}

		return (<mydmam.async.PageHeaderTitle title="FTP user list" fluid="true">
			{BtnAdduser}
			<label className="checkbox pull-right"><input type="checkbox" onClick={this.onUnLockDelete} ref="cb_unlockdelete" />Unlock delete</label>
			<hr />
			<table className="table table-striped table-bordered table-hover table-condensed">
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

		var btn_delete = null;
		if (ftpserver.hasUserAdminRights()) {
			btn_delete = (<mydmam.async.BtnDelete label="Delete" enabled={this.props.delete_enabled} onClickDelete={this.onDelete} reference={user.user_id} />);
		}

		return (<tr>
			<td>
				<strong>{user.domain}</strong> :: {user.user_name}
			</td>
			<td>
				<span className="label label-inverse">{user.group_name}</span>
			</td>
			<td><mydmam.async.pathindex.reactDate date={user.create_date} i18nlabel="Cre." /></td>
			<td><mydmam.async.pathindex.reactDate date={user.update_date} i18nlabel="Upd." /></td>
			<td><mydmam.async.pathindex.reactDate date={user.last_login} i18nlabel="LLog." /></td>
			<td><mydmam.async.BtnEnableDisable
				simplelabel={!ftpserver.hasUserAdminRights()}
				enabled={user.enabled}
				labelenabled="Enabled"
				labeldisabled="Disabled"
				iconcircle={true}
				onEnable={this.onToogleEnableDisable}
				onDisable={this.onToogleEnableDisable}
				reference={user.user_id} />
				&nbsp;
				{btn_delete}
			</td>
		</tr>);
	},	
});

