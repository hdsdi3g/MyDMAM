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
package hd3gtv.mydmam.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.tools.GsonIgnoreStrategy;

public class HeaderJS {
	
	public static final HeaderJS INSTANCE = new HeaderJS();
	
	private final Gson simple_gson;
	private LinkedHashMap<String, Entry> entries;
	
	public HeaderJS() {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		MyDMAM.registerBaseSerializers(builder);
		
		builder.setPrettyPrinting(); // TODO remove !
		
		simple_gson = builder.create();
		
		entries = new LinkedHashMap<>();
		entries.put("home", new Entry("Application.index"));
		entries.put("disconnect", new Entry("Secure.logout"));
		entries.put("github_favicon", new Entry("'/public/img/github-favicon.ico'"));
		entries.put("async", new Entry("AsyncJavascript.index(name='nameparam1',verb='verbparam2')"));
		entries.put("navigate", new Entry("Application.index()", "navigate")); // #navigate/
		entries.put("metadatafile", new Entry("Application.metadatafile(filehash='filehashparam1',type='typeparam2',file='fileparam3')", "navigate"));
		entries.put("ftpserver_export_user_sessions", new Entry("Manager.ftpserver_export_user_sessions(user_session_ref='keyparam1')", "adminFtpServer"));
	}
	
	private class Entry {
		String url;
		ArrayList<String> checks;
		
		Entry(String controler) {
			this(controler, null);
		}
		
		Entry(String controler, String... checks) {
			if (controler == null) {
				throw new NullPointerException("\"controler\" can't to be null");
			}
			// TODO import
		}
		
	}
	
	/*
	public String getPlayTargetUrl() {
	try {
		return Router.reverse(play_action).url;
	} catch (NoRouteFoundException e) {
		return play_action;
	}
	*/
	
	public String toString() {
		// TODO make tree
		LinkedHashMap<String, Object> mydmam = new LinkedHashMap<>(1);
		
		JsonObject async = new JsonObject();
		async.add("controllers", AppManager.getGson().toJsonTree(AJSController.getAllControllersVerbsForThisUser()));
		
		JsonObject user = new JsonObject();
		user.addProperty("long_name", AJSController.getUserProfileLongName());
		
		mydmam.put("async", async);
		mydmam.put("user", user);
		
		return simple_gson.toJson(mydmam);
	}
	
}
