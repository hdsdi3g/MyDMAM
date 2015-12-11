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

ftpserver.hasUserAdminRights = function() {
	return mydmam.async.isAvaliable("ftpserver", "adminoperationuser") & mydmam.async.isAvaliable("ftpserver", "groupdomainlists");
};

var generatePassword = function() {
	var generated_password = "";
	var possible = "abcdefghijkmnopqrstuvwxyz23456789";
	for (var i = 0; i < 8; i++) {
		generated_password += possible.charAt(Math.floor(Math.random() * possible.length));
	}
	return generated_password;
}

ftpserver.AddUser = React.createClass({
	getInitialState: function() {
		return {
			groups: null,
			domains: null,
			display_password_generator: false,
			generated_password: null,
			actual_user: null,
		};
	},
	componentWillMount: function() {
		if (this.props.params.userid == null) {
			mydmam.async.request("ftpserver", "groupdomainlists", {}, function(data) {
				this.setState({groups: data.groups, domains: data.domains});
			}.bind(this));
		}
	},
	onAddUserBtnClick: function() {
		var request = null;
		var user_id = this.props.params.userid;
		if (user_id == null) {
			request = {
				user_name: 			React.findDOMNode(this.refs.user_name).value,
				clear_password: 	React.findDOMNode(this.refs.password).value,
				group_name: 		React.findDOMNode(this.refs.group).value,
				domain: 			React.findDOMNode(this.refs.domain).value,
				operation: 			"CREATE",
			};
		} else {
			request = {
				user_id: 			user_id,		
				clear_password: 	React.findDOMNode(this.refs.password).value,
				operation: 			"CH_PASSWORD",
			};
		}

		document.body.style.cursor = 'wait';

		mydmam.async.request("ftpserver", "adminoperationuser", request, function(data) {
			document.body.style.cursor = 'auto';
			if (this.props.params.userid == null) {
				React.findDOMNode(this.refs.user_name).value = data.user_name;
			}
			this.setState({done: data.done});
		}.bind(this));
	},
	toogleBtnDisplayGeneratePasswordForm: function() {
		var generated_password = generatePassword();
		this.setState({display_password_generator: ! this.state.display_password_generator, generated_password: generated_password});
		React.findDOMNode(this.refs.password).value = generated_password;
	},
	generatePasswordBtn: function() {
		var generated_password = generatePassword();
		this.setState({generated_password: generated_password});
		React.findDOMNode(this.refs.inputgeneratedpassword).value = generated_password;
		React.findDOMNode(this.refs.password).value = generated_password;
	},
	render: function() {
		var user_id = this.props.params.userid;

		var FormControlGroup = mydmam.async.FormControlGroup;
		var generate_password_form = null;
		if (this.state.display_password_generator) {
			generate_password_form = (<FormControlGroup label="Generate password">
			    <div className="input-append">
					<input type="text" disabled="disabled" readOnly="readonly" ref="inputgeneratedpassword" defaultValue={this.state.generated_password} />
			    	<button className="btn" type="button" onClick={this.generatePasswordBtn}><i className="icon-repeat"></i></button>
			    </div>
			</FormControlGroup>);
		}
		var btn_display_generate_password_form_classes = classNames({
			btn: true,
			active: this.state.display_password_generator,
		});

		var submit_buttons = function (label_submit) {
			var items = [];
			items.push(<FormControlGroup key="1">
				<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> {label_submit}</button>
			</FormControlGroup>);

			if (user_id != null) {
				items.push(<FormControlGroup key="2">
					<a type="cancel" className="btn btn-info" href="#ftpserver"><i className="icon-th-list icon-white"></i> {i18n("ftpserver.adduser.goback")}</a>
				</FormControlGroup>);
			}
			return items;
		};

		var getAlertInfoBox = function(label_ok, label_fail) {
			if (this.state.done != null) {
				if (this.state.done) {
					return (<mydmam.async.AlertInfoBox title={label_ok} />);
				} else {
					return (<mydmam.async.AlertErrorBox title={i18n("ftpserver.adduser.warning")}>{label_fail}</mydmam.async.AlertErrorBox>);
				}
			}
			return null;
		}.bind(this);

		if (user_id == null) {
			var groups = this.state.groups;
			var domains = this.state.domains;

			if (groups == null | domains == null) {
				return (<mydmam.async.PageLoadingProgressBar />);
			}

			var select_list_group = [];
			for (pos in groups) {
				select_list_group.push(<option key={pos} value={groups[pos]}>{groups[pos]}</option>);
			}

			var select_list_domain = [];
			for (pos in domains) {
				select_list_domain.push(<option key={pos} value={domains[pos]}>{domains[pos]}</option>);
			}

			return (<div>
				{getAlertInfoBox(i18n("ftpserver.adduser.iscreated"), i18n("ftpserver.adduser.cantcreate"))}
				<form className="form-horizontal" onSubmit={this.onAddUserBtnClick}>
					<FormControlGroup label={i18n("ftpserver.adduser.username")}>
						<input type="text" placeholder={i18n("ftpserver.adduser.username")} ref="user_name" />
					</FormControlGroup>
					<FormControlGroup label="Password">
					    <div className="input-append">
					    	<input type="password" placeholder={i18n("ftpserver.adduser.password")} ref="password" defaultValue={this.state.generated_password} />
					    	<button className={btn_display_generate_password_form_classes} type="button" onClick={this.toogleBtnDisplayGeneratePasswordForm}>
					    		<i className="icon-arrow-down"></i>
					    	</button>
					    </div>
					</FormControlGroup>
					{generate_password_form}
					<FormControlGroup label={i18n("ftpserver.adduser.group")}>
						<select ref="group">{select_list_group}</select>
					</FormControlGroup>
					<FormControlGroup label={i18n("ftpserver.adduser.domain")}>
						<select ref="domain">{select_list_domain}</select>
					</FormControlGroup>
					{submit_buttons(i18n("ftpserver.adduser.create"))}
				</form>
			</div>);
		} else {
			return (<mydmam.async.PageHeaderTitle title={i18n("ftpserver.chpassword.title")} fluid="true">
				{getAlertInfoBox(i18n("ftpserver.chpassword.updated"),i18n("ftpserver.chpassword.noupdated"))}
				<form className="form-horizontal" onSubmit={this.onAddUserBtnClick}>
					<FormControlGroup label={i18n("ftpserver.adduser.password")}>
					    <div className="input-append">
					    	<input type="password" placeholder={i18n("ftpserver.adduser.password")} ref="password" defaultValue={this.state.generated_password} />
					    	<button className={btn_display_generate_password_form_classes} type="button" onClick={this.toogleBtnDisplayGeneratePasswordForm}>
					    		<i className="icon-arrow-down"></i>
					    	</button>
					    </div>
					</FormControlGroup>
					{generate_password_form}
					{submit_buttons(i18n("ftpserver.chpassword.update"))}
				</form>
			</mydmam.async.PageHeaderTitle>);
		}
	}
});

mydmam.routes.push("ftpserver-editUser", "ftpserver/edit/:userid", ftpserver.AddUser, [
	{name: "ftpserver", verb: "adminoperationuser"},
	{name: "ftpserver", verb: "groupdomainlists"}
]);	
