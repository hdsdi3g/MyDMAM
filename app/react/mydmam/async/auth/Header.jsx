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

auth.grouplist = null;

auth.Header = React.createClass({
	getInitialState: function() {
		return {ready: auth.grouplist != null};
	},
	componentWillMount: function() {
		if (!auth.grouplist) {
			mydmam.async.request("auth", "grouplist", null, function(grouplist) {
				auth.grouplist = grouplist.groups;
				this.setState({ready: true});
			}.bind(this));
		}
	},
	render: function(){
		if (this.state.ready == false) {
			return (<mydmam.async.PageHeaderTitle title={i18n("auth.pagename")} fluid="true">
					<mydmam.async.PageLoadingProgressBar />
			</mydmam.async.PageHeaderTitle>);
		}

		var show_this = null;
		if (location.hash.indexOf("#auth/users") == 0) {
			show_this = (<auth.Users />);
		} else if (location.hash.indexOf("#auth/groups") == 0) {
			show_this = (<auth.Groups />);
		} else if (location.hash.indexOf("#auth/roles") == 0) {
			show_this = (<auth.Roles />);
		} else if (location.hash.indexOf("#auth/privileges") == 0) {
			show_this = (<auth.Privileges />);
		} else {
			show_this = (<div>Welcome</div>);
		}

		return (
			<mydmam.async.PageHeaderTitle title={i18n("auth.pagename")} fluid="true">
				<ul className="nav nav-tabs">
					<mydmam.async.HeaderTab href="#auth/users" 	 		i18nlabel="auth.users" />
					<mydmam.async.HeaderTab href="#auth/groups"  		i18nlabel="auth.groups" />
					<mydmam.async.HeaderTab href="#auth/roles"  		i18nlabel="auth.roles" />
					<mydmam.async.HeaderTab href="#auth/privileges" 	i18nlabel="auth.privileges" />
				</ul>
				{show_this}
			</mydmam.async.PageHeaderTitle>
		);
	},
});

mydmam.routes.push("auth", "auth",							auth.Header, [{name: "auth", verb: "usercreate"}]);
mydmam.routes.push("auth-users", "auth/users",				auth.Header, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-groups", "auth/groups",			auth.Header, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-roles", "auth/roles",				auth.Header, [{name: "auth", verb: "usercreate"}]);	
mydmam.routes.push("auth-privileges", "auth/privileges",	auth.Header, [{name: "auth", verb: "usercreate"}]);	
