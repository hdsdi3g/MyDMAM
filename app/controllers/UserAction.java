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
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.useraction.UAFinisherConfiguration;
import hd3gtv.mydmam.useraction.UARange;
import hd3gtv.mydmam.web.UserActionCreator;

import java.lang.reflect.Type;
import java.util.ArrayList;

import models.ACLUser;
import models.UserProfile;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

@With(Secure.class)
public class UserAction extends Controller {
	
	private static final String JSON_OK_RESPONSE = "{\"result\": true}";
	
	private static Gson gson = new Gson();
	private static Type typeOfArrayString = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	/**
	 * @return null if no restrictions.
	 */
	private static ArrayList<String> getUserRestrictedFunctionalities() {
		String username = Secure.connected();
		
		ACLUser acl_user = ACLUser.findById(username);
		String json_functionalities = acl_user.group.role.functionalities;
		if (json_functionalities == null) {
			return null;
		}
		if (json_functionalities.isEmpty()) {
			return null;
		}
		if (json_functionalities.equalsIgnoreCase("[]")) {
			return new ArrayList<String>(1);
		}
		return gson.fromJson(json_functionalities, typeOfArrayString);
	}
	
	@Check("userAction")
	/**
	 * @render Map<String, List<UAFunctionalityDefinintion>> availabilities
	 */
	public static void currentavailabilities() throws Exception {
		renderJSON(IsAlive.getCurrentAvailabilitiesAsJsonString(getUserRestrictedFunctionalities()));
	}
	
	// TODO get user map : Name <-> crypted user key. Lock to a domain ?
	
	@Check("userAction")
	public static void create() throws Exception {
		String[] raw_items = params.getAll("items[]");
		if (raw_items == null) {
			Log2.log.error("No items !", new NullPointerException());
			renderJSON("{}");
		}
		if (raw_items.length == 0) {
			Log2.log.error("No items !", null);
			renderJSON("{}");
		}
		
		ArrayList<SourcePathIndexerElement> items = new ArrayList<SourcePathIndexerElement>(raw_items.length);
		Explorer explorer = new Explorer();
		SourcePathIndexerElement item;
		for (int pos = 0; pos < raw_items.length; pos++) {
			item = explorer.getelementByIdkey(raw_items[pos]);
			if (item != null) {
				items.add(item);
			}
		}
		
		String username = Secure.connected();
		UserProfile userprofile = UserProfile.getORMEngine(username).getInternalElement();// TODO no mail or long name in userprofile
		UserActionCreator creator = new UserActionCreator(items);
		creator.setUserprofile(userprofile);
		creator.setBasket_name(params.get("basket_name"));
		creator.setUsercomment(params.get("comment"));
		
		/**
		 * Not mandatory
		 */
		String[] notification_reasons = params.getAll("notification_reasons[]");
		try {
			creator.addNotificationdestinationForCreator(notification_reasons);
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump();
			dump.add("raw", notification_reasons);
			Log2.log.error("Setup notification destinations for user", e, dump);
		}
		
		String finisher_json = params.get("finisher_json");
		UAFinisherConfiguration finiser_conf = null;
		try {
			finiser_conf = UAFinisherConfiguration.getFinisherFromJsonString(finisher_json);
		} catch (JsonSyntaxException e) {
			Log2.log.error("Bad JSON finisher", e, new Log2Dump("rawcontent", finisher_json));
			renderJSON("{}");
		}
		creator.setRange_Finisher(finiser_conf, UARange.fromString(params.get("range")));
		
		String configured_functionalities_json = params.get("configured_functionalities_json");
		try {
			creator.setConfigured_functionalities(configured_functionalities_json, getUserRestrictedFunctionalities());
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump();
			dump.add("user", Secure.connected());
			dump.add("raw", configured_functionalities_json);
			Log2.log.error("Setup functionalities", e, dump);
			throw e;
		}
		
		// creator.createTasks(); // XXX
		creator.addUALogEntry();
		
		renderJSON(JSON_OK_RESPONSE);
	}
	
	/**
	 * TODO JS/View Useraction publisher in website
	 * - popup method for a basket in baskets list
	 * - special web page, "Useraction creation page", apply to the current basket.
	 */
	
}
