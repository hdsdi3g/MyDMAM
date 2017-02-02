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

async.TopMenu = React.createClass({
	render: function() {

		return (
			<h1>TOP MENU</h1>
		);
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