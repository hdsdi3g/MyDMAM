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

ftpserver.MainPage = React.createClass({
	render: function() {
		var content = [];

		content.push({
			i18nlabel: "ftpserver.tab.activity",
			content: (<ftpserver.ActivityList />)
		});

		content.push({
			i18nlabel: "ftpserver.tab.users",
			content: (<ftpserver.UserList />)
		});

		if (ftpserver.hasUserAdminRights()) {
			content.push({
				i18nlabel: "ftpserver.tab.adduser",
				content: (<ftpserver.AddUser params={{}} />),
			});
		}

		return (<mydmam.async.PageHeaderTitle title={i18n("ftpserver.pagename")} fluid="true" tabs={content} />);
	},
});

mydmam.routes.push("ftpserver-MainPage", "ftpserver", ftpserver.MainPage, [{name: "ftpserver", verb: "allusers"}]);	

ftpserver.UserList = React.createClass({
	getInitialState: function() {
		return {
			users: null,
			delete_enabled: false,
			sorted_col: null,
			sorted_order: null,
			show_last_activity: [],
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
	onLastActivityToogle: function(user) {
		var user_id = user.user_id;
		var current_show_last_activity = this.state.show_last_activity.slice();
		var pos = current_show_last_activity.indexOf(user_id);
		if (pos > -1) {
			current_show_last_activity.splice(pos, 1);
		} else {
			current_show_last_activity.push(user_id);
		}
		this.setState({show_last_activity: current_show_last_activity});
	},
	isPresentInLastActivityToogle: function(user) {
		return this.state.show_last_activity.indexOf(user.user_id) > -1
	},
	render: function() {
		if (this.state.users == null) {
			return (<mydmam.async.PageLoadingProgressBar />);
		}

		var isAdmin = ftpserver.hasUserAdminRights();
		var users = this.sortUsers();

		var table_lines = [];
		for (pos_user in users) {
			var user = users[pos_user];
			var present_in_last_activity_toogle = this.isPresentInLastActivityToogle(user);
			table_lines.push(<ftpserver.UserLine
				key={pos_user}
				user={user}
				onWantRefreshAll={this.onWantRefreshAll}
				delete_enabled={this.state.delete_enabled}
				present_in_last_activity_toogle={present_in_last_activity_toogle}
				onLastActivityToogle={this.onLastActivityToogle} />);

			if (present_in_last_activity_toogle) {
				table_lines.push(<tr key={pos_user + "_lat"}>
					<td colSpan="5"><ftpserver.ActivityList user_id={user.user_id} /></td>
				</tr>);
			}
		}
		
		if (table_lines.length === 0) {
			return (<div>
				<mydmam.async.AlertInfoBox>
					{i18n("ftpserver.userlist.nousers")}
				</mydmam.async.AlertInfoBox>
			</div>);
		}

		var ButtonSort = mydmam.async.ButtonSort;

		return (<div>
			{i18n("ftpserver.userlist.current")}
			<form className="form-inline pull-right">
				<button className="btn btn-small" onClick={this.onWantRefreshAll} style={{marginRight: "1em"}}>
					<i className="icon-refresh"></i> {i18n("ftpserver.userlist.refresh")}
				</button>
				<label className="checkbox"><input type="checkbox" onClick={this.onUnLockDelete} ref="cb_unlockdelete" />
					{i18n("ftpserver.userlist.unlockdelete")}
				</label>
			</form>
			<table className="table table-striped table-hover table-bordered table-condensed">
				<thead>
					<tr>
						<th>
							{i18n("ftpserver.userlist.table.title.user")}
							<ButtonSort onChangeState={this.onChangeColSort} colname="user" order={this.getCurrentColSort("user")} />
						</th>
						<th>
							{i18n("ftpserver.userlist.table.title.group")}
							<ButtonSort onChangeState={this.onChangeColSort} colname="group" order={this.getCurrentColSort("group")} />
						</th>
						<th>
							{i18n("ftpserver.userlist.table.title.createdat")}
							<ButtonSort onChangeState={this.onChangeColSort} colname="created" order={this.getCurrentColSort("created")} />
						</th>
						<th>
							{i18n("ftpserver.userlist.table.title.updatedat")}
							<ButtonSort onChangeState={this.onChangeColSort} colname="updated" order={this.getCurrentColSort("updated")} />
						</th>
						<th>
							{i18n("ftpserver.userlist.table.title.lastloginat")}
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
		</div>);
	},
});

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
			var ftp_export = null;
			var url_export = mydmam.routes.reverse("ftpserver_export_user_sessions");
			if (url_export) {
				var btn_export_classes = classNames("btn", "btn-mini", {
					"disabled": user.last_login == 0,
				});
				if (user.last_login > 0) {
					var url = url_export.replace("keyparam1", md5(user.user_id));
					ftp_export = (<a className={btn_export_classes} style={{marginRight: 5}} href={url}>
						<i className="icon-download"></i>&nbsp;
						{i18n("ftpserver.userlist.table.btnsessions")}
					</a>);
				} else {
					ftp_export = (<button className={btn_export_classes} style={{marginRight: 5}}>
						<i className="icon-download"></i>&nbsp;
						{i18n("ftpserver.userlist.table.btnsessions")}
					</button>);
				}
			}

			btns_admin = (<span style={{marginLeft: 5}}>
				<a className="btn btn-mini btn-warning" style={{marginRight: 5}} href={"#ftpserver/edit/" + this.props.user.user_id}>
					<i className="icon-edit icon-white"></i>&nbsp;
					{i18n("ftpserver.userlist.table.btnchpassword")}
				</a>
				{ftp_export}
				<mydmam.async.BtnDelete label={i18n("ftpserver.userlist.table.delete")} enabled={this.props.delete_enabled} onClickDelete={this.onDelete} reference={user.user_id} />
			</span>);
		}

		var btn_last_activity_toogle = null;
		if (user.last_login > 0) {
			btn_last_activity_toogle = (<span className="pull-right"><ftpserver.BtnLastActivityToogle
				user={user}
				onLastActivityToogle={this.props.onLastActivityToogle}
				present_in_last_activity_toogle={this.props.present_in_last_activity_toogle} />
			</span>);
		}

		var first_case_content = (<span>
			<strong>{user.domain}</strong> :: {user.user_name}
			{btn_last_activity_toogle}
		</span>);

		var first_case = (<td>{first_case_content}</td>);
		if (this.props.present_in_last_activity_toogle) {
			first_case = (<td rowSpan="2">{first_case_content}</td>);
		}

		return (<tr>
			{first_case}
			<td>
				<span className="label label-inverse">{user.group_name}</span>
			</td>
			<td><mydmam.async.pathindex.reactDate date={user.create_date} style={{}} /></td>
			<td><mydmam.async.pathindex.reactDate date={user.update_date} style={{}} /></td>
			<td><mydmam.async.pathindex.reactDate date={user.last_login} style={{}} /></td>
			<td>
				<span className="pull-right">
					<mydmam.async.BtnEnableDisable
						simplelabel={!ftpserver.hasUserAdminRights()}
						enabled={user.enabled}
						labelenabled={i18n("ftpserver.userlist.table.enabled")}
						labeldisabled={i18n("ftpserver.userlist.table.disabled")}
						iconcircle={true}
						onEnable={this.onToogleEnableDisable}
						onDisable={this.onToogleEnableDisable}
						reference={user.user_id} />
					{btns_admin}
				</span>
			</td>
		</tr>);
	},	
});

ftpserver.BtnLastActivityToogle = React.createClass({
	onLastActivityToogle: function() {
		this.props.onLastActivityToogle(this.props.user);
	},
	render: function() {
		var class_name = classNames("btn", "btn-mini", {
			"active": this.props.present_in_last_activity_toogle,
		});

		return (<button className={class_name} onClick={this.onLastActivityToogle}>
			<i className="icon-chevron-down"></i>
		</button>);
	},
});
