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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/

user.settings_link = "settings";

user.Settings = createReactClass({
	onNothing: function(e) {
		e.preventDefault();
	},
	changeEmailAddr: function(e) {
		e.preventDefault();
		//TODO this.refs.email_addr
	},
	testEmailAddr: function(e) {
		e.preventDefault();
		//TODO
	},
	changePassword: function(e) {
		e.preventDefault();
		//TODO
		/*
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
		*/
	},
	render: function() {
		var FormControlGroup = mydmam.async.FormControlGroup;

		var label_style = {paddingTop: "5px", paddingLeft: "5px", cursor: "default"};

		var last_login_time_ago = mydmam.format.msecToHMSms(new Date().getTime() - mydmam.user.lastlogindate, true, true);

		/** Mail addr zone */
		var btn_test_email_addr = (<button className="btn" type="button" onClick={this.testEmailAddr}>
			{<i className="icon-envelope"></i>} {i18n("user.btn_test_email_addr")}
		</button>);
		var email_addr_group = (<label style={label_style}>
			{mydmam.user.email}<br />
			{btn_test_email_addr}
		</label>);
		if (true | mydmam.user.isInDomain() == false) { //TODO true...
			email_addr_group = (<div className="input-append">
				<input type="email" placeholder={i18n("user.email_addr")} defaultValue={mydmam.user.email} ref="email_addr" />
				<button className="btn" type="button" onClick={this.changeEmailAddr}>
					{<i className="icon-pencil"></i>} {i18n("user.btn_update")}
				</button>
				{btn_test_email_addr}
			</div>);
		}

		/** Password zone */
		var password_form1 = null;
		var password_form2 = null;
		if (true | mydmam.user.isInDomain() == false) { //TODO true...
			var password_form1 = (<FormControlGroup label={i18n("user.password")}>
				<input type="password" placeholder={i18n("user.password")} ref="password" />
				<span className="help-block">{i18n("user.password-hint")}</span>
			</FormControlGroup>);
			var password_form2 = (<FormControlGroup label={i18n("user.password2")}>
				<div className="input-append">
					<input type="password" placeholder={i18n("user.password2")} ref="password2" />
					<button className="btn" type="button" onClick={this.changePassword}>
						{<i className="icon-ok"></i>} {i18n("user.password_change")}
					</button>
				</div>
			</FormControlGroup>);
		}

		//TODO preference editor

		return (<mydmam.async.PageHeaderTitle title={i18n("user.pagename")} fluid="false">
			
				<form className="form-horizontal" onSubmit={this.onNothing}>
					<FormControlGroup label={i18n("user.fullname")}>
						<label style={label_style}>
							{mydmam.user.long_name}<br />
							{i18n("user.login", mydmam.user.login)}<br />
							{i18n("user.domain", mydmam.user.domain)}<br />
							{i18n("user.lang", mydmam.user.lang)}
						</label>
					</FormControlGroup>

					<FormControlGroup label={i18n("user.lastlogin")}>
						<label style={label_style}>
							{i18n("user.lastlogindate", (new Date(mydmam.user.lastlogindate)).getI18nFullDisplayTime())} ({last_login_time_ago})<br />
							{i18n("user.lastloginip", mydmam.user.lastloginipsource)}
						</label>
					</FormControlGroup>

					<FormControlGroup label={i18n("user.lasteditdate")}>
						<label style={label_style}>{(new Date(mydmam.user.lasteditdate)).getI18nFullDisplayTime()}</label>
					</FormControlGroup>

					<FormControlGroup label={i18n("user.email_addr")}>
						{email_addr_group}
					</FormControlGroup>

					{password_form1}{password_form2}

				</form>
		</mydmam.async.PageHeaderTitle>);
	},
});

// Only for this user
//	public static void sendTestMail() throws Exception {
//	public static String changeUserMail(String new_mail_addr) throws Exception {
//	public static String(error message | null) changePassword(String new_clear_text_passwd) throws Exception {

//	public static JsonObject getActivities() throws Exception {
//	public static JsonObject basketsList() throws Exception {
//	public static JsonObject basketPush(BasketUpdate update) throws Exception {
//	public static JsonObject basketDelete(String basket_key) throws Exception {
//	public static JsonObject basketRename(BasketRename rename) throws Exception {
//	public static JsonArray notificationsList() throws Exception {
//	public static JsonArray notificationCheck(String notification_key) throws Exception {

mydmam.routes.push("user_Settings", user.settings_link,		user.Settings);	
