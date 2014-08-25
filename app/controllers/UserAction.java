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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package controllers;

import hd3gtv.javasimpleservice.IsAlive;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.useraction.UAFunctionality;
import hd3gtv.mydmam.useraction.UAFunctionalityDefinintion;
import hd3gtv.mydmam.useraction.UAManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;

@With(Secure.class)
public class UserAction extends Controller {
	
	// public static void index() throws Exception {
	// String title = Messages.all(play.i18n.Lang.get()).getProperty("userprofile..pagename");
	// String username = Secure.connected();
	// String key = UserProfile.prepareKey(username);
	// }
	
	@Check({ "adminCrud", "adminUsers" })
	public static void admingroupsrights() throws Exception {
		List<UAFunctionalityDefinintion> full_availabilities = new ArrayList<UAFunctionalityDefinintion>();
		List<String> actual_classes = new ArrayList<String>();
		
		Map<String, List<UAFunctionalityDefinintion>> availabilities = IsAlive.getCurrentAvailabilities();
		
		List<UAFunctionalityDefinintion> current = new ArrayList<UAFunctionalityDefinintion>();
		
		for (Map.Entry<String, List<UAFunctionalityDefinintion>> availability : availabilities.entrySet()) {
			current = availability.getValue();
			for (int pos = 0; pos < current.size(); pos++) {
				if (actual_classes.contains(current.get(pos).classname)) {
					continue;
				}
				actual_classes.add(current.get(pos).classname);
				full_availabilities.add(current.get(pos));
			}
		}
		
		// TODO CRUD Group <-> UAFunctionalityDefinintion (via Availabilities)
		render(full_availabilities); // TODO view
	}
	
	@Check("userAction")
	/**
	 * @render Map<String, List<UAFunctionalityDefinintion>> availabilities
	 */
	public static void currentavailabilities() throws Exception {
		renderJSON(IsAlive.getCurrentAvailabilitiesAsJsonString());// TODO JSON side
	}
	
	private static void internalPrepare(String functionality_name) throws Exception {
		UAFunctionality functionality = UAManager.getByName(functionality_name);
		if (functionality == null) {
			Log2.log.error("Can't found functionality", null, new Log2Dump("name", functionality_name));
			renderJSON("{}");
		}
	}
	
	@Check("userAction")
	public static void prepare(@Required String functionality_name) throws Exception {
		if (Validation.hasErrors()) {
			renderJSON("{}");
			return;
		}
		
		prepare(functionality_name);
		// TODO
		/*
		 * UAConfigurator configurator
		* For display create form in website.
		* @return can be null (if no form or one click UA).
		public abstract UAConfigurator createEmptyConfiguration();
		
		*For display create form in website.
		public abstract boolean hasOneClickDefault();
		
		public abstract UAFinisherConfiguration getFinisherForOneClick();
		
		public abstract UARange getRangeForOneClick();
		
		* For execute an UA.
		public abstract UAConfigurator createOneClickDefaultUserConfiguration();*/
		
		// TODO check if this user can create tasks on this files
		
		renderJSON("{}");
	}
	
	@Check("userAction")
	public static void oneclick(String functionality_name) throws Exception {
		UAFunctionality functionality = UAManager.getByName(functionality_name);
		if (functionality == null) {
			Log2.log.error("Can't found functionality", null, new Log2Dump("name", functionality_name));
			renderJSON("{}");
		}
		
		// TODO
		/*
		 * UAConfigurator configurator
		* For display create form in website.
		* @return can be null (if no form or one click UA).
		public abstract UAConfigurator createEmptyConfiguration();
		
		*For display create form in website.
		public abstract boolean hasOneClickDefault();
		
		public abstract UAFinisherConfiguration getFinisherForOneClick();
		
		public abstract UARange getRangeForOneClick();
		
		* For execute an UA.
		public abstract UAConfigurator createOneClickDefaultUserConfiguration();*/
		
		// TODO check if this user can create tasks on this files
		
		renderJSON("{}");
	}
	
	@Check("userAction")
	public static void create() throws Exception {
		
		String functionality_name = params.get("functionality_name");
		// TODO
		// UAJobContext.createTask(functionality_name);
		/*UAFunctionalityDefinintion definition;
		UAConfigurator configurator;
		UAFinisherConfiguration finisherconf;*/
		
		renderJSON("{}");
	}
	
	/**
	 * TODO Useraction requirement: compute Useractions availabilities with the actual Useraction workers profiles and Storages access
	 * TODO Useraction publisher in website
	 * - popup method for a basket in baskets list
	 * - special web page, "Useraction creation page", apply to the current basket
	 * - popup method for the whole and recursive directory in navigator
	 * - popup method for the current directory in navigator.
	 * - popup method for the current file in navigator.
	 * - nothing in search result
	 * TODO JS Useraction publisher popup menu
	 * - direct display button, async create sub menu content
	 * - retrieve Capabilities and Availabilities from database, and display the correct popup content relative to the creator
	 * - each action link will be targeted to an Useraction creation modal
	 * - or preconfigured one-click action
	 * TODO JS Useraction creator: list options to ask to user in website for create an Useraction. Specific for an Useraction. Declare a Finisher.
	 * TODO JS Useraction creation tasks page/modal by sync
	 * - display current basket, or an anonymous basket with the only one item requested (file, dir, recursive dir)
	 * - select and add an Useraction by Category, and by Long name, following the actual Availabilities.
	 * - add creator configuration form fields, following the Creator declaration.
	 * - add Useraction Range selection
	 * - add basket action after creation (for "by basket" creation): truncate after start, truncate by the finisher, or don't touch.
	 * - add Notification options
	 * - on validation: create task(s) with Task context, finisher(s) and notification(s)
	 * TODO Useraction supervision
	 * - display Capabilities and Availabilities table > in via Service.laststatusworkers page
	 * - admin Usergroups white/black list
	 * - admin Useraction specific params (and published by ORM/form)
	 */
	
}
