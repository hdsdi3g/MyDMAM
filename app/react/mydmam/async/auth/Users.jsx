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
				
				if (pos + 1 < list.length) {
					ctrl_list.push(<span key={pos + "dot"} style={{marginRight: 5}}>&bull;</span>);
				}
			}
			return ctrl_list;
		};


		for (user_key in userlist) {
			var user = userlist[user_key];
			items.push(<tr key={user_key}>
				<td>
					<a href={"#auth/user/edit/" + user_key.replace("%","::")}>{user.fullname}</a>
				</td>
				<td>
					{user.login}&nbsp;
					<small className="muted pull-right">{user.domain}</small>&nbsp;
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
			<p><a href="#auth/user/create" className="btn btn-small btn-success">{i18n("auth.usercreate")}</a></p>
			<table className="table table-bordered table-striped table-condensed">
				<thead>
					<tr>
						<th>{i18n("auth.userlongname")}</th>
						<th>{i18n("auth.username")}
							<span className="pull-right">{i18n("auth.domain")}</span>
						</th>
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


auth.UserCreate = React.createClass({
	getInitialState: function() {
		return {
			locked_account: false,
			domain_list: [],
			user_domain: "local",
			user_groups: [],
			group_full_list: [],
		};
	},
	componentWillMount: function() {
		//var group_key = this.props.params.group_key;

		mydmam.async.request("auth", "domainlist", null, function(domain_list) {
			this.setState({domain_list: domain_list});
		}.bind(this));

		mydmam.async.request("auth", "grouplist", null, function(grouplist) {
			this.setState({group_full_list: grouplist.groups});
		}.bind(this));
	},
	onChangeLockedAccount: function(text, checked){
		this.setState({locked_account: checked});
	},
	onChangeDomain: function(domain, checked){
		if (checked) {
			this.setState({user_domain: domain});
		} else {
			this.setState({user_domain: null});
		}
	},
	onChangeGroup: function(group_key, present) {
		var user_groups = [];
		if (present) {
			user_groups = this.state.user_groups.slice(0);
			user_groups.push(group_key);
		} else {
			for (pos in this.state.user_groups) {
				if (this.state.user_groups[pos] != group_key) {
					user_groups.push(this.state.user_groups[pos]);
				}
			}
		}
		this.setState({user_groups: user_groups});
	},
	onAddBtnClick: function(e) {
		var password  = React.findDOMNode(this.refs.password).value;
		var password2 = React.findDOMNode(this.refs.password2).value;
		if (password != password2) {
			React.findDOMNode(this.refs.password).value = "";
			React.findDOMNode(this.refs.password2).value = "";
			return;
		}

		var new_user = {
			login: React.findDOMNode(this.refs.login).value,
			fullname: React.findDOMNode(this.refs.fullname).value,
			email_addr: React.findDOMNode(this.refs.email_addr).value,
			password: password,
			locked_account: this.state.locked_account,
			domain: this.state.user_domain,
			user_groups: this.state.user_groups,
		}

		mydmam.async.request("auth", "usercreate", new_user, function(new_user) {
			window.location = "#auth/users";
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;
		var CheckboxItem = mydmam.async.CheckboxItem;

		var cb_domain = [];
		var domain_list = this.state.domain_list;
		for (pos in domain_list) {
			var domain = domain_list[pos];
			var is_checked = (domain == this.state.user_domain);
			cb_domain.push(<CheckboxItem key={domain} checked={is_checked} reference={domain} onChangeCheck={this.onChangeDomain}>
				{domain}
			</CheckboxItem>);
		}

		var cb_groups = [];
		var group_full_list = this.state.group_full_list;
		for (group_key in group_full_list) {
			var is_checked = this.state.user_groups.indexOf(group_key) > -1;
			cb_groups.push(<CheckboxItem key={group_key} checked={is_checked} reference={group_key} onChangeCheck={this.onChangeGroup}>
				{group_full_list[group_key].group_name}
			</CheckboxItem>);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.usercreate")} fluid="false">
				<form className="form-horizontal" onSubmit={this.onAddBtnClick}>
					<FormControlGroup label={i18n("auth.fullname")}>
						<input type="text" placeholder={i18n("auth.fullname")} ref="fullname" />
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.email_addr")}>
						<input type="email" placeholder={i18n("auth.email_addr")} ref="email_addr" />
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.login")}>
						<input type="text" placeholder={i18n("auth.login")} ref="login" />
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.password")}>
						<input type="password" placeholder={i18n("auth.password")} ref="password" />
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.password2")}>
						<input type="password" placeholder={i18n("auth.password2")} ref="password2" />
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.locked_account")}>
						<CheckboxItem checked={this.state.locked_account} reference={"locked_account"} onChangeCheck={this.onChangeLockedAccount}>
							{i18n("auth.locked_account_help")}
						</CheckboxItem>
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.domain")}>
						{cb_domain}
					</FormControlGroup>
					<FormControlGroup label={i18n("auth.groups")}>
						{cb_groups}
					</FormControlGroup>

					<FormControlGroup>
						<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("auth.create")}</button>
					</FormControlGroup>

					<FormControlGroup>
						<a type="cancel" className="btn btn-info" href="#auth/users"><i className="icon-chevron-left icon-white"></i> {i18n("auth.goback")}</a>
					</FormControlGroup>
				</form>
			</mydmam.async.PageHeaderTitle>
		);
	},
});

auth.UserEdit = React.createClass({
	getInitialState: function() {
		return {
			user_groups: [],
			group_full_list: [],
			user: null,
		};
	},
	componentWillMount: function() {
		var user_key = this.props.params.user_key.replace("::","%");

		mydmam.async.request("auth", "userget", user_key, function(user) {
			this.setState({user: user, user_groups: user.user_groups});
		}.bind(this));

		mydmam.async.request("auth", "grouplist", null, function(grouplist) {
			this.setState({group_full_list: grouplist.groups});
		}.bind(this));
	},
	onChangeGroup: function(group_key, present) {
		var user_groups = [];
		if (present) {
			user_groups = this.state.user_groups.slice(0);
			user_groups.push(group_key);
		} else {
			for (pos in this.state.user_groups) {
				if (this.state.user_groups[pos] != group_key) {
					user_groups.push(this.state.user_groups[pos]);
				}
			}
		}
		this.setState({user_groups: user_groups});
	},
	onUpdBtnClick: function(e) {
		var password  = React.findDOMNode(this.refs.password).value;
		var password2 = React.findDOMNode(this.refs.password2).value;
		if (password && password2) {
			if (password != password2) {
				React.findDOMNode(this.refs.password).value = "";
				React.findDOMNode(this.refs.password2).value = "";
				return;
			}
		} else {
			password = "";
		}	

		var update_user = {
			user_key: this.props.params.user_key.replace("::","%"),
			new_password: password,
			user_groups: this.state.user_groups,
		}

		mydmam.async.request("auth", "useradminupdate", update_user, function(user) {
			window.location = "#auth/users";
		}.bind(this));
	},
	onDeleteBtnClick: function(e){
		if (window.confirm(i18n("auth.confirmremoveaccount", this.state.user.fullname))) {
			mydmam.async.request("auth", "userdelete", this.props.params.user_key.replace("::","%"), function(list) {
				window.location = "#auth/users";
			}.bind(this));
		}
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;
		var CheckboxItem = mydmam.async.CheckboxItem;
		var user = this.state.user;
		if (user == null) {
			return (
				<mydmam.async.PageHeaderTitle title={i18n("auth.userupdate")} fluid="false">
				<mydmam.async.PageLoadingProgressBar />
			</mydmam.async.PageHeaderTitle>
			);
		}

		var is_local_user = (user.domain == "local");
		var password_form = null;
		if (is_local_user) {
			var password_form = (<span>
				<FormControlGroup label={i18n("auth.password")}>
					<input type="password" placeholder={i18n("auth.password")} ref="password" />
				</FormControlGroup>
				<FormControlGroup label={i18n("auth.password2")}>
					<input type="password" placeholder={i18n("auth.password2")} ref="password2" />
				</FormControlGroup>
			</span>);
		} else {
			/** Domain users can't change passwords here */
			var password_form = (<span>
				<input type="hidden" value="" ref="password" /><input type="hidden" value="" ref="password2" />
			</span>);
		}

		var cb_groups = [];
		var group_full_list = this.state.group_full_list;
		for (group_key in group_full_list) {
			var is_checked = this.state.user_groups.indexOf(group_key) > -1;
			cb_groups.push(<CheckboxItem key={group_key} checked={is_checked} reference={group_key} onChangeCheck={this.onChangeGroup}>
				{group_full_list[group_key].group_name}
			</CheckboxItem>);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.userupdate", user.fullname)} fluid="false">
				<form className="form-horizontal" onSubmit={this.onUpdBtnClick}>
					{password_form}
					<FormControlGroup label={i18n("auth.groups")}>
						{cb_groups}
					</FormControlGroup>

					<FormControlGroup>
						<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {i18n("auth.update")}</button>
					</FormControlGroup>

					<FormControlGroup>
						<a type="cancel" className="btn btn-info" href="#auth/users"><i className="icon-chevron-left icon-white"></i> {i18n("auth.goback")}</a>
					</FormControlGroup>

					<FormControlGroup>
						<a className="btn btn-danger btn-mini" onClick={this.onDeleteBtnClick}><i className="icon-remove icon-white"></i> {i18n("auth.remove")}</a>
					</FormControlGroup>
				</form>
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("auth-user-create", "auth/user/create",			auth.UserCreate, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-user-view", "auth/user/edit/:user_key",	auth.UserEdit, [{name: "auth", verb: "usercreate"}]);	

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
