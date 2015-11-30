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

ftpserver.ShowUserList = React.createClass({
	loadActualUsersList: function() {
		mydmam.async.request("ftpserver", "allusers", {}, function(data) {
			this.setState({users: data.users});
		}.bind(this));
	},
	getInitialState: function() {
		return {
			users: null,
		};
	},
	componentWillMount: function() {
		this.loadActualUsersList();
	},
	componentWillUnmount: function() {
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
			table_lines.push(<tr key={pos_user}><td>users[pos_user]</td></tr>);
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
			<div className="pull-right">{BtnAdduser}</div>
			<table className="table table-striped table-bordered table-hover table-condensed">
				<tbody>	
					{table_lines}
				</tbody>	
			</table>
		</mydmam.async.PageHeaderTitle>);
	},
});

mydmam.routes.push("ftpserver-ShowUserList", "ftpserver", ftpserver.ShowUserList, [{name: "ftpserver", verb: "allusers"}]);	
