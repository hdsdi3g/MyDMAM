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

auth.Users = React.createClass({
	getInitialState: function() {
		return {
			userlist: {},
			grouplist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "userlist", null, function(userlist) {
			this.setState({userlist: userlist.users});
		}.bind(this));
		mydmam.async.request("auth", "grouplist", null, function(grouplist) {
			this.setState({grouplist: grouplist.groups});
		}.bind(this));
	},
	toogleLockUser: function(user_key) {
		mydmam.async.request("auth", "usertooglelock", user_key, function(user) {
			var new_user_list = jQuery.extend({}, this.state.userlist, {});
			new_user_list[user_key] = user;

			this.setState({userlist: new_user_list});
		}.bind(this));
	},
	render: function(){
		var items = [];
		var userlist = this.state.userlist;
		var grouplist = this.state.grouplist;

		var toList = function (list) {
			var ctrl_list = [];
			for (pos in list) {
				var group_key = list[pos];
				if (grouplist[group_key]) {
					ctrl_list.push(<span key={pos} style={{marginRight: 5}}>{grouplist[group_key].group_name}</span>);
				} else {
					ctrl_list.push(<span key={pos} style={{marginRight: 5}}>{group_key}</span>);
				}
			}
			return ctrl_list;
		};


		for (user_key in userlist) {
			var user = userlist[user_key];
			items.push(<tr key={user_key}>
				<td>
					{user.fullname}
				</td>
				<td>
					{user.login}&nbsp;
					<small className="muted">%{user.domain}</small>&nbsp;
				</td>
				<td>
					{user.language}
				</td>
				<td>
					{user.email_addr}
				</td>
				<td>
					<mydmam.async.pathindex.reactDate date={user.lasteditdate} />
				</td>
				<td>
					<mydmam.async.pathindex.reactDate date={user.lastlogindate} />
					&nbsp;<small>{user.lastloginipsource}</small>
				</td>
				<td>
					{toList(user.user_groups)}
				</td>
				<td>
					<mydmam.async.BtnEnableDisable
						simplelabel={false}
						enabled={user.locked_account == false} 
						labelenabled={""}
						labeldisabled={""}
						onEnable={this.toogleLockUser}
						onDisable={this.toogleLockUser}
						reference={user_key}
						iconcircle={true} />
				</td>
			</tr>);
		}

		return (<div>
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("auth.userlongname")}</th>
						<th>{i18n("auth.username")} / {i18n("auth.domain")}</th>
						<th>{i18n("auth.lang")}</th>
						<th>{i18n("auth.email")}</th>
						<th>{i18n("auth.lasteditdate")}</th>
						<th>{i18n("auth.lastlogin")}</th>
						<th>{i18n("auth.groups")}</th>
						<th>{i18n("auth.useractivated")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		</div>);
	}
});

//	public static UserViewList userDelete(String key) throws Exception {
//	public static UserView userCreate(NewUser newuser) throws Exception {
//	public static UserView userGet(String key) throws Exception {
//	public static UserView userChangePassword(UserChPassword chpassword) throws Exception {
//	public static UserView userChangeGroup(UserChGroup chgroup) throws Exception {

// All	
//	public static JsonObject getPreferencies() throws Exception {
//	public static UserView changePassword(String new_clear_text_passwd) throws Exception {
//	public static void sendTestMail() throws Exception {
//	public static UserView changeUserMail(String new_mail_addr) throws Exception {
//	public static JsonObject getActivities() throws Exception {
//	public static JsonObject basketsList() throws Exception {
//	public static JsonObject basketPush(BasketUpdate update) throws Exception {
//	public static JsonObject basketDelete(String basket_key) throws Exception {
//	public static JsonObject basketRename(BasketRename rename) throws Exception {
//	public static JsonArray notificationsList() throws Exception {
//	public static JsonArray notificationCheck(String notification_key) throws Exception {
