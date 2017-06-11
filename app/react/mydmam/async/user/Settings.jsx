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

var PreferenceEntry = createReactClass({
	propTypes: {
		delete: PropTypes.func.isRequired,
		entry: PropTypes.object.isRequired,
	},
	getInitialState: function() {
		return {
			pending_delete: false,
		};
	},
	deleteEntry: function() {
		this.setState({pending_delete: true});
		this.props.delete(this.props.entry.key);
	},
	render: function() {
		var entry = this.props.entry;

		var value = entry.value;
		if (value == null || value == undefined) {
			value = <mydmam.async.Empty />;
		} else if (typeof value === "object" || Array.isArray(value)) {
			value = (<code className="json">{JSON.stringify(value, null, " ")}</code>);
		} else if (value === "") {
			value = <mydmam.async.Empty />;
		} else {
			value = (<code className="json">{entry.value}</code>);
		}

		var class_names = classNames({
			error: this.state.pending_delete,
		});

		return (<tr className={class_names}>
			<td><button onClick={this.deleteEntry} className="btn btn-mini"><i className="icon-minus-sign"></i></button></td>
			<th>{entry.key}</th>
			<td>{value}</td>
			<td><small>{(new Date(entry.created)).getI18nShortDisplayTime()}</small></td>
			<td><small>{(new Date(entry.modified)).getI18nShortDisplayTime()}</small></td>
		</tr>);
	},
});

var NewPreferenceEntry = createReactClass({
	propTypes: {
		add: PropTypes.func.isRequired,
	},
	addEntry: function() {
		var key = ReactDOM.findDOMNode(this.refs.pref_key).value.trim();
		var value = ReactDOM.findDOMNode(this.refs.pref_value).value.trim();
		if (key == "") {
			return;
		}
		this.props.add(key, value);
		ReactDOM.findDOMNode(this.refs.pref_key).value = null;
		ReactDOM.findDOMNode(this.refs.pref_value).value = null;
	},
	render: function() {
		var input = function(placeholder, ref) {
			return (<input type="text"
				placeholder={i18n(placeholder)}
				ref={ref}
				className="input-block-level"
				style={{marginBottom: 0,}} />);
		};

		return (<tr>
			<td><button onClick={this.addEntry} className="btn btn-mini"><i className="icon-plus-sign"></i></button></td>
			<td>{input("user.prefeditor.placeholder.key", "pref_key")}</td>
			<td>{input("user.prefeditor.placeholder.value", "pref_value")}</td>
			<td>&nbsp;</td>
			<td>&nbsp;</td>
		</tr>);
	},
});

var PreferenceEditor = createReactClass({
	getInitialState: function() {
		var mapper = function(key, value, created, modified) {
			return {
				key: key,
				value: value,
				created: created,
				modified: modified,
			};
		};
		return {
			preferences: mydmam.user.listPreferences(mapper),
		};
	},
	addEntry: function(key, value) {
		mydmam.user.setPreference(key, value, true);
		mydmam.user.pushToServer(function() {
			var dates = mydmam.user.getPreferenceDates(key);
			this.setState(this.getInitialState());
		}.bind(this));
	},
	deleteEntry: function(key) {
		mydmam.user.setPreference(key, null, true);
		mydmam.user.pushToServer(function() {
			this.setState(this.getInitialState());
		}.bind(this));
	},
	render: function() {
		var preferences = this.state.preferences;
		var table_content = [];
		for (var key in preferences) {
			table_content.push(<PreferenceEntry key={key} entry={preferences[key]} delete={this.deleteEntry} />);
		}
		table_content.push(<NewPreferenceEntry key={"__add_pref_entry"} add={this.addEntry} />);

		return (<div>
			<hr />
			<p className="lead">{i18n("user.prefeditor.tablehead.title")}</p>

			<table className="table table-striped table-bordered table-hover table-condensed">
				<thead>
					<tr>
						<td></td>
						<td>{i18n("user.prefeditor.tablehead.key")}</td>
						<td>{i18n("user.prefeditor.tablehead.value")}</td>
						<td>{i18n("user.prefeditor.tablehead.created")}</td>
						<td>{i18n("user.prefeditor.tablehead.updated")}</td>
					</tr>
				</thead>
				<tbody>
					{table_content}
				</tbody>
			</table>
		</div>);
	},
});

user.Settings = createReactClass({
	getInitialState: function() {
		return {
			flash_ok: null,
			flash_error: null,
			display_preference_editor: false,
		};
	},
	displayPreferenceEditor: function(e) {
		e.preventDefault();
		this.setState({display_preference_editor: true});
	},
	flash: function(text, is_error) {
		if (is_error) {
			this.setState({
				flash_ok: null,
				flash_error: text,
			});
		} else {
			this.setState({
				flash_ok: text,
				flash_error: null,
			});
		}
	},
	closeFlash: function() {
		this.setState({flash_ok: null, flash_error: null,});
	},
	onNothing: function(e) {
		e.preventDefault();
	},
	changeEmailAddr: function(e) {
		e.preventDefault();
		var new_email_addr = ReactDOM.findDOMNode(this.refs.email_addr).value.trim();
		var actual_email_addr = mydmam.user.email;
		if (new_email_addr == "") {
			this.flash(i18n("user.empty_new_email_addr"), true);
			return;
		}
		if (new_email_addr == actual_email_addr) {
			return;
		}
		mydmam.async.request("auth", "changeUserMail", new_email_addr, function(new_mail) {
			if (new_mail == null) {
				this.flash(i18n("user.error_during_change_email_addr"), true);
			} else if (new_mail == "") {
				this.flash(i18n("user.error_during_change_email_addr"), true);
			} else {
				mydmam.user.email = new_mail;
				this.flash(i18n("user.changing_email_addr_done"));
			}
		}.bind(this));
	},
	testEmailAddr: function(e) {
		e.preventDefault();
		mydmam.async.request("auth", "sendTestMail", null, function() {
			this.flash(i18n("user.test_email_is_sended"));
		}.bind(this));
	},
	changePassword: function(e) {
		e.preventDefault();
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

		mydmam.async.request("auth", "changePassword", password, function(error_message) {
			ReactDOM.findDOMNode(this.refs.password).value = "";
			ReactDOM.findDOMNode(this.refs.password2).value = "";

			if (error_message) {
				this.flash(error_message, true);
			}
		}.bind(this));
	},
	render: function() {
		var FormControlGroup = mydmam.async.FormControlGroup;

		var label_style = {paddingTop: "5px", paddingLeft: "5px", cursor: "default"};

		var last_login_time_ago = mydmam.format.msecToHMSms(new Date().getTime() - mydmam.user.lastlogindate, true, true);

		/** Mail addr zone */
		var email_addr_group = (<label style={label_style}>
			{mydmam.user.email}
			<a className="btn btn-mini" type="button" onClick={this.testEmailAddr} href="#" style={{marginLeft: "10px"}}>
				{<i className="icon-envelope"></i>} {i18n("user.btn_test_email_addr")}
			</a>
		</label>);
		if (mydmam.user.isInDomain() == false) {
			email_addr_group = (<div className="input-append">
				<input type="email" placeholder={i18n("user.email_addr")} defaultValue={mydmam.user.email} ref="email_addr" />
				<button className="btn" type="button" onClick={this.changeEmailAddr}>
					{<i className="icon-pencil"></i>} {i18n("user.btn_update")}
				</button>
				<button className="btn" type="button" onClick={this.testEmailAddr}>
					{<i className="icon-envelope"></i>} {i18n("user.btn_test_email_addr")}
				</button>
			</div>);
		}

		/** Password zone */
		var password_form1 = null;
		var password_form2 = null;
		if (mydmam.user.isInDomain() == false) {
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

		var flash = null;
		if (this.state.flash_ok) {
			flash = (<mydmam.async.AlertInfoBox onClose={this.closeFlash}>{this.state.flash_ok}</mydmam.async.AlertInfoBox>);
		} else if (this.state.flash_error) {
			flash = (<mydmam.async.AlertErrorBox onClose={this.closeFlash}>{this.state.flash_error}</mydmam.async.AlertErrorBox>);
		}

		var preference_editor = null;
		if (this.state.display_preference_editor) {
			preference_editor = <PreferenceEditor />;
		} else {
			preference_editor = (<a href="#" onClick={this.displayPreferenceEditor}>{i18n("user.prefeditor.tablehead.display")}</a>);
		}

		return (<mydmam.async.PageHeaderTitle title={i18n("user.pagename")} fluid="false">
				{flash}
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

				{preference_editor}
		</mydmam.async.PageHeaderTitle>);
	},
});

// Only for this user
//	public static JsonObject getActivities() throws Exception {
//	public static JsonObject basketsList() throws Exception {
//	public static JsonObject basketPush(BasketUpdate update) throws Exception {
//	public static JsonObject basketDelete(String basket_key) throws Exception {
//	public static JsonObject basketRename(BasketRename rename) throws Exception {
//	public static JsonArray notificationsList() throws Exception {
//	public static JsonArray notificationCheck(String notification_key) throws Exception {

mydmam.routes.push("user_Settings", user.settings_link, user.Settings);	
