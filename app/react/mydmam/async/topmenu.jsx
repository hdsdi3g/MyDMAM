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

var divider_vertical = (<li className="divider-vertical"></li>);

var TopMenuEntrylink = createReactClass({
	onClick: function(e) {
		$(ReactDOM.findDOMNode(this.refs.thislink)).blur();
	},
	render: function() {
		var href = this.props.href;
		var is_dropdown = this.props.is_dropdown;
		var is_brand = this.props.brand;
		var label = this.props.label;

		if (is_dropdown) {
			return (<a className="dropdown-toggle" data-toggle="dropdown" href="#" ref="thislink" onClick={this.onClick}>
				{label} <b className="caret"></b>
			</a>);
		} else {
			if (is_brand) {
				return (<a href={href} ref="thislink" onClick={this.onClick} className="brand exofont">
					{label}
				</a>);
			} else {
				return (<a href={href} ref="thislink" onClick={this.onClick}>
					{label}
				</a>);
			}
		}
	}
});

var DropdownMenu = createReactClass({
	render: function() {
		var items = this.props.items;
		var content = [];
		var active = this.props.active;

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

		var li_class = classNames({
			"active": active,
			dropdown: true,
		});

		return (<li className={li_class}>
			<TopMenuEntrylink is_dropdown={true} label={this.props.label} />
			<ul className="dropdown-menu">
				{content}
			</ul>
		</li>);
	}
});

var isMenuItemIsActive = function(href_hash_to_handle) {
	if (!location.hash) {
		return false;
	}

	if (Array.isArray(href_hash_to_handle) == false) {
		href_hash_to_handle = [href_hash_to_handle];
	}

	if (href_hash_to_handle.indexOf(location.hash) == -1) {
		for (var pos in href_hash_to_handle) {
			var href = href_hash_to_handle[pos];
			if (location.hash.startsWith(href)) {
				return true;
			}
		}
		return false;
	}
	return true;
}

async.TopMenu = createReactClass({
	onSearchKeyPress: function(event) {
		if (!event) {
			event = window.event;
		}
    	var keyCode = event.keyCode || event.which;
    	if (keyCode == '13') {
    		var item = ReactDOM.findDOMNode(this.refs.search);
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

		if (async.isAvaliable("stat", "cache")) {
			var li_class = classNames({
				"active": isMenuItemIsActive("#navigate"),
			});

			navigate_link = (<li className={li_class}>
				<TopMenuEntrylink label={i18n("application.navigate")} href="#navigate" />
			</li>);
			navigate_link_divider = divider_vertical;
		}

		var bca_link = null;
		if (async.isAvaliable("bca", "allevents")) {
			var li_class = classNames({
				"active": isMenuItemIsActive("#" + async.bca.link),
			});

			bca_link = (<li className={li_class}>
				<TopMenuEntrylink label={i18n("bca.page")} href={"#" + async.bca.link} />
			</li>);
		}

		var user_profile_menu_active = false; // isMenuItemIsActive()

		var user_dropdown_items = [];
		user_dropdown_items.push({
			href: mydmam.routes.reverse("disconnect"),
			icon: "icon-off",
			i18nlabel: "maingrid.disconnect",
		});

		var admin_menu_items = [];

		admin_menu_items.push({
			headeri18n: "service.menuinfrastructure",
		});

		if (mydmam.async.isAvaliable("watchfolders", "list")) {
			admin_menu_items.push({href: "#watchfolders", 		icon: "icon-folder-open",	i18nlabel: "manager.watchfolders.pagename",});
		}
		if (mydmam.async.isAvaliable("broker", "list")) {
			admin_menu_items.push({href: "#broker", 			icon: "icon-tasks", 		i18nlabel: "manager.jobs.pagename",});
		}
		if (mydmam.async.isAvaliable("instances", "allsummaries")) {
			admin_menu_items.push({href: "#manager/summary", 	icon: "icon-hdd", 			i18nlabel: "manager.pagename",	links: ["#manager", "#debugpage"]});
		}
		if (mydmam.async.isAvaliable("ftpserver", "allusers")) {
			admin_menu_items.push({href: "#ftpserver", 			icon: "icon-random", 		i18nlabel: "ftpserver.pagename",});
		}
		if (mydmam.async.isAvaliable("auth", "usercreate")) {
			admin_menu_items.push({href: "#auth/users",			icon: "icon-user", 			i18nlabel: "auth.pagenamemenu",	links: "#auth"});
		}

		var admin_menu = null;
		if (admin_menu_items.length > 1) {
			var admin_menu_href_handle = [];

			for (var pos in admin_menu_items) {
				var item = admin_menu_items[pos];
				if (item.href) {
					admin_menu_href_handle.push(item.href);
				}
				if (item.links) {
					if (Array.isArray(item.links)) {
						admin_menu_href_handle = admin_menu_href_handle.concat(item.links);
					} else {
						admin_menu_href_handle.push(item.links);
					}
				}
			}
			admin_menu = <DropdownMenu label={i18n("maingrid.adminbtn")} items={admin_menu_items} active={isMenuItemIsActive(admin_menu_href_handle)} />
		}

		return (<div className="navbar navbar-fixed-top">
			<div className="navbar-inner">
			    <div className="container-fluid">
					<a className="btn btn-navbar" data-toggle="collapse" data-target=".nav-collapse">
						<span className="icon-bar"></span>
						<span className="icon-bar"></span>
						<span className="icon-bar"></span>
					</a>
					<TopMenuEntrylink label={i18n("maingrid.brand")} brand={true} href={mydmam.routes.reverse("home") + "#"} />
		 
			 		<ul className="nav pull-right">
						{sitesearchbox}
						{sitesearchbox_divider}
						<DropdownMenu label={mydmam.user.long_name} items={user_dropdown_items} active={user_profile_menu_active} />
			 		</ul>

					<div className="nav-collapse collapse">
				 		<ul className="nav pull-left navbar-fixed-top-btn">
							{navigate_link}
							{bca_link}
							{navigate_link_divider}
							{admin_menu}
						</ul>
					</div>
			    </div>
			</div>  
		</div>);
	}
});

