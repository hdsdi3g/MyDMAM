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

import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.useraction.Basket;
import hd3gtv.mydmam.useraction.UACreationRequest;
import hd3gtv.mydmam.useraction.UAManager;
import hd3gtv.mydmam.useraction.UASelectAsyncOptions;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.ACLUser;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.With;

import com.google.gson.reflect.TypeToken;

@With(Secure.class)
public class UserAction extends Controller {
	
	private static final String JSON_OK_RESPONSE = "{\"result\": true}";
	
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
			return new ArrayList<String>(1);
		}
		if (json_functionalities.isEmpty()) {
			return new ArrayList<String>(1);
		}
		if (json_functionalities.equalsIgnoreCase("[]")) {
			return new ArrayList<String>(1);
		}
		return UAManager.getGson().fromJson(json_functionalities, typeOfArrayString);
	}
	
	@Check("userAction")
	/**
	 * @render Map<String, List<UAFunctionalityDefinintion>> availabilities
	 */
	public static void currentavailabilities() throws Exception {
		renderJSON(InstanceStatus.getCurrentAvailabilitiesAsJsonString(getUserRestrictedFunctionalities()));
	}
	
	@Check("userAction")
	public static void create(@Required String uarequest) throws Exception {
		if (Validation.hasErrors()) {
			throw new NullPointerException("request");
		}
		
		ArrayList<String> getuserrestrictedfunctionalities = getUserRestrictedFunctionalities();
		if (getuserrestrictedfunctionalities.isEmpty()) {
			throw new SecurityException("User " + Secure.connected() + " can't use useractions");
		}
		
		UACreationRequest ua_request = UAManager.getGson().fromJson(uarequest, UACreationRequest.class);
		ua_request.setUserprofile(User.getUserProfile());
		ua_request.setUserRestrictedPrivileges(getuserrestrictedfunctionalities);
		ua_request.createJobs();
		
		renderJSON(JSON_OK_RESPONSE);
	}
	
	@Check("userAction")
	public static void index() throws Exception {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("useractions.pagename");
		String currentavailabilities = InstanceStatus.getCurrentAvailabilitiesAsJsonString(getUserRestrictedFunctionalities());
		Basket basket = Basket.getBasketForCurrentPlayUser();
		String currentbasket = basket.getSelectedContentJson();
		String currentbasketname = basket.getSelectedName();
		render(title, currentavailabilities, currentbasket, currentbasketname);
	}
	
	@Check("userAction")
	public static void optionsforselectform() throws Exception {
		String class_referer = params.get("classreferer");
		if (class_referer == null) {
			throw new NullPointerException("classreferer in params");
		}
		Class<?> c_referer = Class.forName(class_referer);
		List<String> list = null;
		
		if (c_referer.isEnum()) {
			Object[] enum_constants = c_referer.getEnumConstants();
			list = new ArrayList<String>(enum_constants.length);
			for (int pos = 0; pos < enum_constants.length; pos++) {
				list.add(((Enum<?>) enum_constants[pos]).name());
			}
		} else if (UASelectAsyncOptions.class.isAssignableFrom(c_referer)) {
			UASelectAsyncOptions selectasyncoptions = (UASelectAsyncOptions) c_referer.newInstance();
			list = selectasyncoptions.getSelectOptionsList();
		} else {
			throw new ClassCastException(class_referer + " is not a valid instance from " + UASelectAsyncOptions.class.getSimpleName() + ".class");
		}
		if (list == null) {
			renderJSON("[]");
		}
		if (list.isEmpty()) {
			renderJSON("[]");
		}
		
		List<Map<String, String>> result = new ArrayList<Map<String, String>>(list.size());
		for (int pos = 0; pos < list.size(); pos++) {
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("value", list.get(pos));
			result.add(item);
		}
		
		renderJSON(result);
	}
	
}
