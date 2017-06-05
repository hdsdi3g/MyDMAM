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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import controllers.Secure;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import play.exceptions.NoRouteFoundException;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Router;
import play.vfs.VirtualFile;

public class HeaderJS {
	
	public static final HeaderJS INSTANCE = new HeaderJS();
	
	private LinkedHashMap<String, Entry> entries;
	
	public HeaderJS() {
		entries = new LinkedHashMap<>();
		entries.put("home", new Entry("Application.index"));
		entries.put("disconnect", new Entry("Secure.logout"));
		
		Entry e_github_favicon = new Entry();
		e_github_favicon.setStaticFile("/public/img/github-favicon.ico");
		entries.put("github_favicon", e_github_favicon);
		
		Entry e_asyncjavascript = new Entry();
		e_asyncjavascript.setControler("AsyncJavascript.index").addControlerParam("name", "nameparam1").addControlerParam("verb", "verbparam2").pack();
		entries.put("async", e_asyncjavascript);
		
		Entry e_navigate = new Entry();
		e_navigate.setControler("Application.index").setChecks("navigate").pack("#navigate/");
		entries.put("navigate", e_navigate);
		
		Entry e_metadatafile = new Entry();
		e_metadatafile.setControler("Application.metadatafile").addControlerParam("filehash", "filehashparam1").addControlerParam("type", "typeparam2").addControlerParam("file", "fileparam3");
		e_metadatafile.setChecks("navigate").pack();
		entries.put("metadatafile", e_metadatafile);
		
		Entry e_ftpsessions = new Entry();
		e_ftpsessions.setControler("Manager.ftpserver_export_user_sessions").addControlerParam("user_session_ref", "keyparam1").setChecks("adminFtpServer").pack();
		entries.put("ftpserver_export_user_sessions", e_ftpsessions);
		
	}
	
	private class Entry {
		String controler_name;
		ArrayList<String> checks;
		HashMap<String, Object> controler_args;
		
		String url;
		
		Entry() {
			controler_args = new HashMap<String, Object>();
			checks = new ArrayList<>();
		}
		
		/**
		 * It will be pack()
		 */
		Entry(String controler_name) {
			checks = new ArrayList<>();
			controler_args = new HashMap<String, Object>();
			setControler(controler_name);
			pack();
		}
		
		/**
		 * Needs to pack() after all.
		 */
		Entry setControler(String controler_name) {
			if (controler_name == null) {
				throw new NullPointerException("\"controler_name\" can't to be null");
			}
			this.controler_name = controler_name;
			return this;
		}
		
		/**
		 * Needs to pack() after all.
		 */
		Entry addControlerParam(String name, Object value) {
			controler_args.put(name, value);
			return this;
		}
		
		Entry setChecks(String... checks) {
			if (checks == null) {
			} else {
				if (checks.length == 0) {
					this.checks = new ArrayList<>();
				} else {
					this.checks = new ArrayList<>(Arrays.asList(checks));
				}
			}
			
			return this;
		}
		
		/**
		 * Not need to pack.
		 */
		void setStaticFile(String relative_file) {
			try {
				url = Router.reverse(VirtualFile.fromRelativePath(relative_file));
			} catch (NoRouteFoundException e) {
				Loggers.Play.error("Can't found route for " + relative_file);
				url = "/";
			}
		}
		
		void pack() {
			pack("");
		}
		
		/**
		 * @param hashtag_to_add (should be start with #)
		 */
		void pack(String hashtag_to_add) {
			if (url != null) {
				return;
			}
			if (hashtag_to_add == null) {
				hashtag_to_add = "";
			}
			
			try {
				if (controler_args.isEmpty()) {
					url = Router.reverse(controler_name).url + hashtag_to_add;
				} else {
					url = Router.reverse(controler_name, controler_args).url + hashtag_to_add;
				}
			} catch (NoRouteFoundException e) {
				Loggers.Play.error("Can't found route for " + controler_name);
				return;
			}
		}
		
		public String toString() {
			return "URL: " + url + ", checks=" + checks;
		}
		
	}
	
	public String toString() {
		try {
			LinkedHashMap<String, Object> mydmam = new LinkedHashMap<>(1);
			
			JsonObject async = new JsonObject();
			async.add("controllers", MyDMAM.gson_kit.getGson().toJsonTree(AJSController.getAllControllersVerbsForThisUser()));
			
			if (Secure.getRequestAddress().equals("loopback") == false) {
				async.addProperty("server_time", System.currentTimeMillis());
			}
			
			JsonObject user = new JsonObject();
			user.addProperty("long_name", AJSController.getUserProfileLongName());
			
			mydmam.put("async", async);
			mydmam.put("user", user);
			
			LinkedHashMap<String, LinkedHashMap<String, String>> routes = new LinkedHashMap<>(1);
			LinkedHashMap<String, String> routes_statics = new LinkedHashMap<>(entries.size());
			
			HashSet<String> session_privileges = Secure.getSessionPrivileges();
			for (Map.Entry<String, Entry> entry : entries.entrySet()) {
				Entry route = entry.getValue();
				
				if (route.checks.isEmpty() == false) {
					if (route.checks.stream().anyMatch(p -> {
						return session_privileges.contains(p);
					}) == false) {
						continue;
					}
				}
				
				routes_statics.put(entry.getKey(), route.url);
			}
			
			routes.put("statics", routes_statics);
			mydmam.put("routes", routes);
			
			/**
			 * Inject configuration Messages
			 */
			JsonObject j_i18n = new JsonObject();
			MyDMAM.getconfiguredMessages().forEach((k, v) -> {
				j_i18n.addProperty((String) k, (String) v);
			});
			mydmam.put("i18n", j_i18n);
			
			return MyDMAM.gson_kit.getGsonSimple().toJson(mydmam);
		} catch (DisconnectedUser e) {
			Loggers.Play.warn("User was disconnected: " + e.getMessage());
		} catch (NullPointerException e) {
			Loggers.Play.warn("Troubes during user request", e);
		}
		
		return "null";
	}
	
	public String getMessageForCurrentUserLocale(String key, Object... args) {
		if (MyDMAM.getconfiguredMessages().containsKey(key)) {
			return Messages.formatString(Lang.getLocale(), MyDMAM.getconfiguredMessages().getProperty(key), args);
		}
		return Messages.get(key, args);
	}
	
}
