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


auth.Users = createReactClass({
	getInitialState: function() {
		return {
			userlist: {},
		};
	},
	componentWillMount: function() {
		mydmam.async.request("auth", "userlist", null, function(userlist) {
			this.setState({userlist: userlist.users});
		}.bind(this));
	},
	toogleLockUser: function(user_key) {
		mydmam.async.request("auth", "usertooglelock", user_key, function(user) {
			var new_user_list = jQuery.extend({}, this.state.userlist, {});
			new_user_list[user_key] = user;

			this.setState({userlist: new_user_list});
		}.bind(this));
	},
	deleteUser: function(user_key) {
		mydmam.async.request("auth", "userdelete", user_key, function(list) {
			this.setState({userlist: list.users});
		}.bind(this));
	},
	render: function(){
		var items = [];
		var userlist = this.state.userlist;
		var grouplist = auth.grouplist;

		var toList = function (list) {
			var ctrl_list = [];
			for (pos in list) {
				var group_key = list[pos];
				if (grouplist[group_key]) {
					ctrl_list.push(<span key={pos} style={{marginRight: 5}}>{grouplist[group_key].group_name}</span>);
				} else {
					ctrl_list.push(<span key={pos} style={{marginRight: 5}}>{group_key}</span>);
				}
				
				if (pos != list.length - 1) {
					ctrl_list.push(<span key={pos + "dot"} style={{marginRight: 5}}>&bull;</span>);
				}
			}
			return ctrl_list;
		};


		for (user_key in userlist) {
			var user = userlist[user_key];
			items.push(<tr key={user_key}>
				<td>
					<a href={"#auth/user/edit/" + user_key.replace("%","::") + "/groups"}>{user.fullname}</a>
				</td>
				<td>
					{user.login}&nbsp;
					<small className="muted pull-right">{user.domain}</small>&nbsp;
				</td>
				<td>
					{user.language} &bull; <small><a href={"mailto:" + user.email_addr}>{user.email_addr}</a></small>
				</td>
				<td>
					<mydmam.async.pathindex.reactDate date={user.lasteditdate} />
				</td>
				<td>
					<mydmam.async.pathindex.reactDate date={user.lastlogindate} />
					&nbsp;<small>{user.lastloginipsource}</small>
				</td>
				<td>
					<small>{toList(user.user_groups)}</small>
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
				<td>
					<mydmam.async.BtnDelete enabled={true} onClickDelete={this.deleteUser} reference={user_key}  />
				</td>
			</tr>);
		}

		return (<div>
			<p><a href="#auth/user/create" className="btn btn-small btn-success">{i18n("auth.usercreate")}</a></p>
			<table className="table table-bordered table-striped table-condensed table-hover">
				<thead>
					<tr>
						<th>{i18n("auth.userlongname")}</th>
						<th>{i18n("auth.username")}
							<span className="pull-right">{i18n("auth.domain")}</span>
						</th>
						<th>{i18n("auth.lang")} &bull; {i18n("auth.email")}</th>
						<th>{i18n("auth.lasteditdate")}</th>
						<th>{i18n("auth.lastlogin")}</th>
						<th>{i18n("auth.groups")}</th>
						<th>{i18n("auth.useractivated")}</th>
						<th>{i18n("auth.userdelete")}</th>
					</tr>
				</thead>
				<tbody>
					{items}
				</tbody>
			</table>
		</div>);
	}
});


auth.UserCreate = createReactClass({
	getInitialState: function() {
		return {
			locked_account: false,
			domain_list: [],
			user_domain: "local",
			user_groups: [],
		};
	},
	componentWillMount: function() {
		//var group_key = this.props.params.group_key;
		mydmam.async.request("auth", "domainlist", null, function(domain_list) {
			this.setState({domain_list: domain_list});
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
		var password  = ReactDOM.findDOMNode(this.refs.password).value;
		var password2 = ReactDOM.findDOMNode(this.refs.password2).value;
		if (password != password2) {
			ReactDOM.findDOMNode(this.refs.password).value = "";
			ReactDOM.findDOMNode(this.refs.password2).value = "";
			return;
		}

		var new_user = {
			login: ReactDOM.findDOMNode(this.refs.login).value,
			fullname: ReactDOM.findDOMNode(this.refs.fullname).value,
			email_addr: ReactDOM.findDOMNode(this.refs.email_addr).value,
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
		var group_full_list = auth.grouplist;
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
						<span className="help-block">{i18n("auth.password-hint")}</span>
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

var GroupPane = createReactClass({
	getInitialState: function() {
		return {
			user_groups: this.props.user.user_groups,
		};
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
		var update_user = {
			user_key: this.props.user_key,
			user_groups: this.state.user_groups,
		}

		mydmam.async.request("auth", "useradminupdate", update_user, function(user) {
			this.props.onsave();
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;
		var CheckboxItem = mydmam.async.CheckboxItem;

		var cb_groups = [];
		var group_full_list = auth.grouplist;
		for (group_key in group_full_list) {
			var is_checked = this.state.user_groups.indexOf(group_key) > -1;
			cb_groups.push(<CheckboxItem key={group_key} checked={is_checked} reference={group_key} onChangeCheck={this.onChangeGroup}>
				{group_full_list[group_key].group_name}
			</CheckboxItem>);
		}

		return (
				<div>
					<FormControlGroup label={i18n("auth.groups")}>
						{cb_groups}
					</FormControlGroup>
					<FormControlGroup>
						<a className="btn btn-success" onClick={this.onUpdBtnClick}><i className="icon-ok icon-white"></i> {i18n("auth.update")}</a>
					</FormControlGroup>
				</div>
		);
	},
});

var PasswordPane = createReactClass({
	onUpdBtnClick: function(e) {
		var password  = ReactDOM.findDOMNode(this.refs.password).value;
		var password2 = ReactDOM.findDOMNode(this.refs.password2).value;
		if (password && password2) {
			if (password != password2) {
				ReactDOM.findDOMNode(this.refs.password).value = "";
				ReactDOM.findDOMNode(this.refs.password2).value = "";
				return;
			}
		} else {
			password = "";
		}	

		var update_user = {
			user_key: this.props.user_key,
			new_password: password,
		}

		mydmam.async.request("auth", "useradminupdate", update_user, function(user) {
			//T O D O done action with user
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;

		return (<div>
				<FormControlGroup label={i18n("auth.password")}>
					<input type="password" placeholder={i18n("auth.password")} ref="password" />
					<span className="help-block">{i18n("auth.password-hint")}</span>
				</FormControlGroup>
				<FormControlGroup label={i18n("auth.password2")}>
					<input type="password" placeholder={i18n("auth.password2")} ref="password2" />
				</FormControlGroup>
				<FormControlGroup>
					<a className="btn btn-success" onClick={this.onUpdBtnClick}><i className="icon-ok icon-white"></i> {i18n("auth.update")}</a>
				</FormControlGroup>
		</div>);
	},
});

var DeletePane = createReactClass({
	onDeleteBtnClick: function(e){
		mydmam.async.request("auth", "userdelete", this.props.user_key, function(list) {
			window.location = "#auth/users";
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;

		return (
					<FormControlGroup label={i18n("auth.remove-label")}>
						<a className="btn btn-danger" onClick={this.onDeleteBtnClick}><i className="icon-remove icon-white"></i> {i18n("auth.remove")}</a>
					</FormControlGroup>
		);
	},
});

var PropertiesPane = createReactClass({
	onSaveBtnClick: function(e){
		var new_value = ReactDOM.findDOMNode(this.refs.data).value;

		var update_user = {
			user_key: this.props.user_key,
			properties: new_value,
		}

		mydmam.async.request("auth", "useradminupdate", update_user, function(user) {
			this.props.onsave();
		}.bind(this));
	},
	render: function(){
		return (<div>
			<p>
				<strong className="text-warning">{i18n("auth.warnbeforechange")}</strong>
			</p>
			<textarea
				rows="8"
				className="input-xxlarge"
				defaultValue={this.props.datas}
				ref="data"
				style={{fontFamily: "monospace", fontSize: 12,}}
				autoComplete="off" autoCorrect="off" autoCapitalize="off" spellCheck="false" />
			<div>
				<a className="btn btn-primary" onClick={this.onSaveBtnClick}><i className="icon-ok icon-white"></i> {i18n("auth.save")}</a>
			</div>
		</div>);
	},
});

var PreferencesPane = createReactClass({
	render: function(){
		return (<div>
			<mydmam.async.JsonCode i18nlabel="auth.preferences.last" json={this.props.preferences} />
		</div>);
	},
});


auth.UserEdit = createReactClass({
	getInitialState: function() {
		return {
			user: null,
		};
	},
	componentWillMount: function() {
		this.onRefreshUser();
	},
	onRefreshUser: function() {
		var user_key = this.props.params.user_key.replace("::","%");

		mydmam.async.request("auth", "userget", user_key, function(user) {
			this.setState({user: user});
		}.bind(this));
	},
	render: function(){
		var FormControlGroup = mydmam.async.FormControlGroup;
		var CheckboxItem = mydmam.async.CheckboxItem;
		var user = this.state.user;
		if (user == null) {
			return (<mydmam.async.PageHeaderTitle title={i18n("auth.loading")} fluid="false" go_back_url={"#auth/users"}>
				<mydmam.async.PageLoadingProgressBar />
			</mydmam.async.PageHeaderTitle>);
		}

		var user_key = this.props.params.user_key.replace("::","%");
		var base_url = "#auth/user/edit/" + user_key.replace("%","::");

		var show_this = null;
		if (location.hash.indexOf(base_url + "/groups") == 0) {
			show_this = (<GroupPane user={this.state.user} user_key={user_key} onsave={this.onRefreshUser} />);
		} else if (location.hash.indexOf(base_url + "/password") == 0) {
			show_this = (<PasswordPane user_key={user_key} />);
		} else if (location.hash.indexOf(base_url + "/remove") == 0) {
			show_this = (<DeletePane user_key={user_key} />);
		} else if (location.hash.indexOf(base_url + "/properties") == 0) {
			show_this = (<PropertiesPane user_key={user_key} datas={user.properties} onsave={this.onRefreshUser} />);
		} else if (location.hash.indexOf(base_url + "/preferences") == 0) {
			show_this = (<PreferencesPane preferences={user.preferences} />);
		} else {
			show_this = (<mydmam.async.PageLoadingProgressBar />);
		}

		var tab_password = null;
		if (user.domain == "local") {
			/** Domain users can't change passwords (here) */
			tab_password = (<mydmam.async.HeaderTab href={base_url + "/password"}	i18nlabel="auth.password" />);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.userupdate", user.fullname)} fluid="false" go_back_url={"#auth/users"}>
				<ul className="nav nav-tabs">
					<mydmam.async.HeaderTab href={base_url + "/groups"}		i18nlabel="auth.groups" />
					{tab_password}
					<mydmam.async.HeaderTab href={base_url + "/properties"}	i18nlabel="auth.properties" />
					<mydmam.async.HeaderTab href={base_url + "/preferences"}	i18nlabel="auth.preferences" />
					<mydmam.async.HeaderTab href={base_url + "/remove"}		i18nlabel="auth.remove" />
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("auth-user-create", "auth/user/create",		auth.UserCreate, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-user-view-groups",		"auth/user/edit/:user_key/groups",		auth.UserEdit, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-user-view-remove",		"auth/user/edit/:user_key/remove",		auth.UserEdit, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-user-view-password",	"auth/user/edit/:user_key/password",	auth.UserEdit, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-user-view-properties",	"auth/user/edit/:user_key/properties",	auth.UserEdit, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-user-view-preferences","auth/user/edit/:user_key/preferences",	auth.UserEdit, [{name: "auth", verb: "usercreate"}]);	
