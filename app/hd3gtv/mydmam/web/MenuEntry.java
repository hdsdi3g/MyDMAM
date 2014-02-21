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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.web;

import java.util.List;

import play.exceptions.NoRouteFoundException;
import play.mvc.Router;

public class MenuEntry {
	
	public String play_action;
	public String btn_id;
	public String title;
	public boolean add_divider;
	public int order = 0;
	public List<MenuEntry> subitems;
	public String privilege = "";
	
	/**
	 * @param play_action "Mycontroller.function" like for "@{Mycontroller.function()}", use Router.reverse(action)
	 * @param btn_id HTML button id to set
	 * @param title for translated by messages file
	 */
	public MenuEntry(String play_action, String btn_id, String title, boolean add_divider) {
		this.play_action = play_action;
		this.btn_id = btn_id;
		this.title = title;
		this.add_divider = add_divider;
	}
	
	/**
	 * @param btn_id HTML button id to set
	 * @param title for translated by messages file
	 * @param subitems for a submenu
	 */
	public MenuEntry(String btn_id, String title, boolean add_divider, List<MenuEntry> subitems) {
		this.play_action = "";
		this.btn_id = btn_id;
		this.title = title;
		this.add_divider = add_divider;
		this.subitems = subitems;
	}
	
	public String getPlayTargetUrl() {
		try {
			return Router.reverse(play_action).url;
		} catch (NoRouteFoundException e) {
			return play_action;
		}
	}
	
	/**
	 * Default 0; Min values push to right/bottom, Max to left/top
	 */
	public MenuEntry setOrder(int order) {
		this.order = order;
		return this;
	}
	
	public MenuEntry setPrivilege(String privilege) {
		this.privilege = privilege;
		return this;
	}
}
