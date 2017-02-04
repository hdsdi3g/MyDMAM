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

var href_brand = "#"; // @{Application.index()}#
var user_profile_long_name = "USER NAME"; //%{ out.println(hd3gtv.mydmam.web.AJSController.getUserProfileLongName() ) }%
var href_disconnect = "LOGOUT"; //@{Secure.logout()}

var divider_vertical = (<li className="divider-vertical"></li>);

/*
async.HeaderTab = React.createClass({
	onClick: function(e) {
		//e.preventDefault();
		//this.props.onActiveChange(this.props.pos);
		$(React.findDOMNode(this.refs.tab)).blur();
	},
	render: function(){
		var li_class = classNames({
			"active": this.props.href == location.hash
		});

		return (<li className={li_class}>
			<a href={this.props.href} onClick={this.onClick} ref="tab">{i18n(this.props.i18nlabel)}</a>
		</li>);
	},
});
*/

var DropdownMenu = React.createClass({
	render: function() {

		var items = this.props.items;

		var content = [];

		for (pos in items) {
			var item = items[pos];

			if (item.headeri18n) {
				content.push(<li key={pos} className="nav-header">
					{i18n(item.headeri18n)}
				</li>);
			} else {
				content.push(<li key={pos}>
					<a href={item.href}><i className={item.icon}></i> {i18n(item.i18nlabel)}</a>
				</li>);
			}
		}

		return (<li className="dropdown">
			<a className="dropdown-toggle" data-toggle="dropdown" href="#">
				{this.props.label}
				<b className="caret"></b>
			</a>
			<ul className="dropdown-menu">
				{content}
			</ul>
		</li>);
	}
});

async.TopMenu = React.createClass({
	onSearchKeyPress: function(event) {
		if (!event) {
			event = window.event;
		}
    	var keyCode = event.keyCode || event.which;
    	if (keyCode == '13') {
    		var item = React.findDOMNode(this.refs.search);
    		this.props.onDirectSearch(item.value);
      		item.value = "";

			event.preventDefault();
			if (event.stopImmediatePropagation) {
				event.stopImmediatePropagation();
			}
			event.stopPropagation();
    	}
	},
	render: function() {
		var sitesearchbox = null;
		var sitesearchbox_divider = null;
		var navigate_link = null;
		var navigate_link_divider = null;

		if (this.props.display_search_inputbox) {
			sitesearchbox = (<li className="navbar-search">
				<input
					type="text"
					className="search-query span2"
					placeholder={i18n("maingrid.search")}
					onKeyPress={this.onSearchKeyPress}
					ref="search" />
			</li>);
			sitesearchbox_divider = divider_vertical;
		}

		if (mydmam.async.isAvaliable("stat", "cache")) {
			navigate_link = (<li>
				<a href="#navigate">{i18n("application.navigate")}</a>
			</li>);
			navigate_link_divider = divider_vertical;
		}

		var user_dropdown_items = [];
		user_dropdown_items.push({
			href: href_disconnect,
			icon: "icon-off",
			i18nlabel: "maingrid.disconnect",
		});



		var admin_menu_items = [];
		admin_menu_items.push({
			headeri18n: "service.menuinfrastructure",
		});
		admin_menu_items.push({href: "#watchfolders", 		icon: "icon-folder-open",	i18nlabel: "manager.watchfolders.pagename",});
		admin_menu_items.push({href: "#broker", 			icon: "icon-tasks", 		i18nlabel: "manager.jobs.pagename",});
		admin_menu_items.push({href: "#manager/summary", 	icon: "icon-hdd", 			i18nlabel: "manager.pagename",});
		admin_menu_items.push({href: "#ftpserver", 			icon: "icon-random", 		i18nlabel: "ftpserver.pagename",});
		admin_menu_items.push({href: "#auth", 				icon: "icon-user", 			i18nlabel: "auth.pagenamemenu",});

		var admin_menu = <DropdownMenu label={i18n("maingrid.adminbtn")} items={admin_menu_items} />

		return (<div className="navbar navbar-fixed-top">
			<div className="navbar-inner">
			    <div className="container-fluid">
					<a className="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
						<span className="icon-bar"></span>
						<span className="icon-bar"></span>
						<span className="icon-bar"></span>
					</a>
					<a className="brand" href={href_brand}>{i18n("maingrid.brand")}</a>
		 
			 		<ul className="nav pull-right">
						{sitesearchbox}
						{sitesearchbox_divider}
						<DropdownMenu label={user_profile_long_name} items={user_dropdown_items} />
			 		</ul>

					<div className="nav-collapse collapse">
				 		<ul className="nav pull-left navbar-fixed-top-btn">
							{navigate_link}
							{navigate_link_divider}
							{admin_menu}
						</ul>
					</div>
			    </div>
			</div>  
		</div>);
	}
});

/*
	public String getPlayTargetUrl() {
		try {
			return Router.reverse(play_action).url;
		} catch (NoRouteFoundException e) {
			return play_action;
		}
	}
*/