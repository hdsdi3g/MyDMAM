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
	render: function() {

		var search = null;
		if (mydmam.async.isAvaliable("search", "query")) {
			//search = (<mydmam.async.SearchBox onValidation={this.doDirectSearch} />);
		}

		 //#{secure.check 'navigate'}
		var sitesearchbox = (<li className="navbar-search">
			<input type="text" className="search-query span2" placeholder={i18n("maingrid.search")} name="q" />
		</li>);
		var sitesearchbox_divider = divider_vertical;
		var navigate_link = (<li>
			<a href="#navigate">{i18n("application.navigate")}</a>
		</li>);
		var navigate_link_divider = divider_vertical;

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


async.SearchBox = React.createClass({
	/*getInitialState: function() {
		return {
			inputbox: $("#sitesearch")[0],
		};
	},
	componentDidMount: function() {
		this.state.inputbox.value = "";
		this.state.inputbox.addEventListener('keypress', this.onkeyPress);
		this.state.inputbox.style.display = "block";
	},
	componentWillUnmount: function() {
		this.state.inputbox.removeEventListener('keypress', this.onkeyPress);
		this.state.inputbox.style.display = "none";
	},
	shouldComponentUpdate: function(nextProps, nextState) {
	    return false;
	},
	onkeyPress: function(event) {
		if (!event) {
			event = window.event;
		}
    	var keyCode = event.keyCode || event.which;
    	if (keyCode == '13') {
      		this.props.onValidation(this.state.inputbox.value);
      		this.state.inputbox.value = "";

			event.preventDefault();
			event.stopImmediatePropagation();
			event.stopPropagation();
    	}
	},*/
	render: function() {
		return null;
	}
});


/*


			%{	menu_elements = hd3gtv.mydmam.module.MyDMAMModulesManager.getAllUserMenusEntries()
				menu_elements.each() { if (controllers.Secure.checkview(it.privilege)) {
			}%
				#{if it.add_divider}
					<li className="divider-vertical"></li>
				#{/if}
			<li id="&{it.btn_id}" #{if it.subitems}className="dropdown"#{/if}>
				<a href="&{it.getPlayTargetUrl()}" #{if it.subitems}className="dropdown-toggle" data-toggle="dropdown"#{/if}>
					&{it.title}
					#{if it.subitems}<b className="caret"></b>#{/if}
				</a>
				#{if it.subitems}
					<ul className="dropdown-menu">
						%{	
							it.subitems.eachWithIndex() { item, p ->
						}%
							<li id="&{item.btn_id}">
								<a href="&{item.getPlayTargetUrl()}">&{item.title}</a>
								#{if item.add_divider}
									<li className="divider"></li>
								#{/if}
							</li>
						%{
							}
						}%
					</ul>
				#{/if}
			</li>
			%{
				} }
			}%

*/



/*

					%{	adminmenu_elements = hd3gtv.mydmam.module.MyDMAMModulesManager.getAllAdminMenusEntries()
						adminmenu_elements.eachWithIndex() { adminitem, p ->
						if (controllers.Secure.checkview(adminitem.privilege)) {
					}%
						<li id="&{adminitem.btn_id}" #{if adminitem.subitems}className="dropdown-submenu"#{/if}>
							<a href="&{adminitem.getPlayTargetUrl()}" #{if adminitem.subitems}tabindex="-1"#{/if}>&{adminitem.title}</a>
							#{if adminitem.subitems}
								<ul className="dropdown-menu">
								%{	
									adminitem.subitems.eachWithIndex() { sitem, i ->
									if (controllers.Secure.checkview(sitem.privilege)) {
								}%
									<li id="&{sitem.btn_id}">
										<a href="&{sitem.getPlayTargetUrl()}">&{sitem.title}</a>
									</li>
									#{if sitem.add_divider}
										<li className="divider"></li>
									#{/if}
								%{
									}}
								}%
								</ul>
							#{/if}
							#{if adminitem.add_divider}
								<li className="divider"></li>
							#{/if}
						</li>
						<li className="divider"></li>
					%{
						}}
					}%



	public String getPlayTargetUrl() {
		try {
			return Router.reverse(play_action).url;
		} catch (NoRouteFoundException e) {
			return play_action;
		}
	}

*/