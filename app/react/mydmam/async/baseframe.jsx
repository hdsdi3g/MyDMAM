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

/**
 * Plug an external React input box, and callback if user press enter.
 * Draw nothing.
 * @see https://facebook.github.io/react/tips/use-react-with-other-libraries.html
 * @see https://facebook.github.io/react/tips/dom-event-listeners.html
 */
async.SearchBox = React.createClass({
	getInitialState: function() {
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
	},
	render: function() {
		return null;
	}
});

async.Home = React.createClass({
	render: function() {
		return (<div className="container">
			<div className="hero-unit">
				<h1>{i18n("site.name")}</h1>
				<p>{i18n("site.baseline")}</p>
			</div>
		</div>);
	}
});

/*


			%{	menu_elements = hd3gtv.mydmam.module.MyDMAMModulesManager.getAllUserMenusEntries()
				menu_elements.each() { if (controllers.Secure.checkview(it.privilege)) {
			}%
				#{if it.add_divider}
					<li class="divider-vertical"></li>
				#{/if}
			<li id="&{it.btn_id}" #{if it.subitems}class="dropdown"#{/if}>
				<a href="&{it.getPlayTargetUrl()}" #{if it.subitems}class="dropdown-toggle" data-toggle="dropdown"#{/if}>
					&{it.title}
					#{if it.subitems}<b class="caret"></b>#{/if}
				</a>
				#{if it.subitems}
					<ul class="dropdown-menu">
						%{	
							it.subitems.eachWithIndex() { item, p ->
						}%
							<li id="&{item.btn_id}">
								<a href="&{item.getPlayTargetUrl()}">&{item.title}</a>
								#{if item.add_divider}
									<li class="divider"></li>
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
						<li id="&{adminitem.btn_id}" #{if adminitem.subitems}class="dropdown-submenu"#{/if}>
							<a href="&{adminitem.getPlayTargetUrl()}" #{if adminitem.subitems}tabindex="-1"#{/if}>&{adminitem.title}</a>
							#{if adminitem.subitems}
								<ul class="dropdown-menu">
								%{	
									adminitem.subitems.eachWithIndex() { sitem, i ->
									if (controllers.Secure.checkview(sitem.privilege)) {
								}%
									<li id="&{sitem.btn_id}">
										<a href="&{sitem.getPlayTargetUrl()}">&{sitem.title}</a>
									</li>
									#{if sitem.add_divider}
										<li class="divider"></li>
									#{/if}
								%{
									}}
								}%
								</ul>
							#{/if}
							#{if adminitem.add_divider}
								<li class="divider"></li>
							#{/if}
						</li>
						<li class="divider"></li>
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