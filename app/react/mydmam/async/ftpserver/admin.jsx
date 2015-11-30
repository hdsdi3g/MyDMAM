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

ftpserver.BtnAddUserForm = React.createClass({
	render: function() {
		return (<a href="#ftpserver/add" className="btn btn-primary"><i className="icon-plus icon-white"></i> Add user</a>);
	}
});

ftpserver.AddUser = React.createClass({
	getInitialState: function() {
		return {
			groups: null,
			domains: null,
		};
	},
	loadActualConfig: function() {
		mydmam.async.request("ftpserver", "groupdomainlists", {}, function(data) {
			this.setState(data);
		}.bind(this));
	},
	componentWillMount: function() {
		this.loadActualConfig();
	},
	onAddUserBtnClick: function() {
		var user_name = React.findDOMNode(this.refs.user_name).value;
		var password = React.findDOMNode(this.refs.password).value;
		var group = React.findDOMNode(this.refs.group).value;
		var domain = React.findDOMNode(this.refs.domain).value;

		var request = {
			user_name: user_name,
			clear_password: password,
			group_name: group,
			domain: domain,
			operation: "CREATE",
		};

		document.body.style.cursor = 'wait';

		mydmam.async.request("ftpserver", "adminoperationuser", request, function(data) {
			document.body.style.cursor = 'auto';
			React.findDOMNode(this.refs.user_name).value = data.user_name;
			this.setState({done: data.done});
		}.bind(this));
	},
	render: function() {
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

		var FormControlGroup = mydmam.async.FormControlGroup;

		var user_is_created = null;
		var done = this.state.done;
		if (done != null) {
			if (done) {
				user_is_created = (<mydmam.async.AlertInfoBox title="User is created" />);
			} else {
				var ccu = "Can't create user";
				user_is_created = (<mydmam.async.AlertErrorBox title="Warning">{ccu}</mydmam.async.AlertErrorBox>);
			}
		}

		return (<mydmam.async.PageHeaderTitle title="Add FTP user">
			{user_is_created}
			<form className="form-horizontal" onSubmit={this.onAddUserBtnClick}>
				<FormControlGroup label="User name">
					<input type="text" placeholder="User name" ref="user_name" />
				</FormControlGroup>
				<FormControlGroup label="Password">
					<input type="password" placeholder="Password" ref="password" />
				</FormControlGroup>
				<FormControlGroup label="Group">
					<select ref="group">{select_list_group}</select>
				</FormControlGroup>
				<FormControlGroup label="Domain">
					<select ref="domain">{select_list_domain}</select>
				</FormControlGroup>
				<FormControlGroup>
					<button type="submit" className="btn btn-success"><i className="icon-ok icon-white"></i> Create</button>
				</FormControlGroup>
				<FormControlGroup>
					<a type="cancel" className="btn btn-info" href="#ftpserver"><i className="icon-th-list icon-white"></i> Back to the user list</a>
				</FormControlGroup>
			</form>
		</mydmam.async.PageHeaderTitle>);
	}
});

mydmam.routes.push("ftpserver-addUser", "ftpserver/add", ftpserver.AddUser, [
	{name: "ftpserver", verb: "adminoperationuser"},
	{name: "ftpserver", verb: "groupdomainlists"}
]);	
