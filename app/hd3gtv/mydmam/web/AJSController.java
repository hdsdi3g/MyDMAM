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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package hd3gtv.mydmam.web;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import controllers.Secure;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.web.AJSControllerItem.Verb;
import hd3gtv.tools.GsonIgnoreStrategy;
import models.UserProfile;
import play.vfs.VirtualFile;

public class AJSController {
	
	public static final String ASYNC_CLASS_PATH = "/app/controllers/ajs";
	public static final String ASYNC_PACKAGE_NAME = "controllers.ajs";
	public static final Gson gson_simple;
	private static final Gson gson_pretty;
	private static final GsonBuilder gson_builder;
	
	static Gson gson;
	private static final HashMap<String, AJSControllerItem> controllers;
	
	static {
		gson_builder = new GsonBuilder();
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		gson_builder.addDeserializationExclusionStrategy(ignore_strategy);
		gson_builder.addSerializationExclusionStrategy(ignore_strategy);
		gson_builder.serializeNulls();
		gson_builder.registerTypeAdapter(Class.class, new MyDMAM.GsonClassSerializer());
		gson_simple = gson_builder.create();
		gson_pretty = gson_builder.setPrettyPrinting().create();
		gson = gson_builder.create();
		
		/**
		 * Get all class files from all ASYNC_CLASS_PATH directory, module by module.
		 */
		List<VirtualFileModule> main_dirs = JsCompile.getAllfromRelativePath(ASYNC_CLASS_PATH, true, true);
		List<VirtualFile> class_vfiles = new ArrayList<VirtualFile>();
		for (int pos = 0; pos < main_dirs.size(); pos++) {
			class_vfiles.addAll(main_dirs.get(pos).getVfile().list());
		}
		
		/**
		 * Get all AJSController implementations from this classes.
		 */
		controllers = new HashMap<String, AJSControllerItem>();
		Class<?> class_candidate;
		AJSControllerItem item;
		for (int pos = 0; pos < class_vfiles.size(); pos++) {
			String name = class_vfiles.get(pos).getName();
			if (name.endsWith(".java") == false) {
				continue;
			}
			try {
				class_candidate = Class.forName(ASYNC_PACKAGE_NAME + "." + name.substring(0, name.length() - ".java".length()));
			} catch (Exception e) {
				Loggers.Play.warn("Invalid class loading", e);
				continue;
			}
			
			if (AJSController.class.isAssignableFrom(class_candidate)) {
				item = new AJSControllerItem(class_candidate);
				if (item.isEmpty() == false) {
					controllers.put(class_candidate.getSimpleName().toLowerCase(), item);
				}
			} else {
				Loggers.Play.warn("Class declaration is not a valid expected type, java file: " + class_vfiles.get(pos).getRealFile());
				continue;
			}
		}
		
	}
	
	@AJSIgnore
	public static String dumpAll() {
		return gson_pretty.toJson(controllers);
	}
	
	@AJSIgnore
	public static void registerTypeAdapter(Type type, Object typeAdapter) {
		gson_builder.registerTypeAdapter(type, typeAdapter);
		gson = gson_builder.create();
	}
	
	@AJSIgnore
	public static void putAllPrivilegesNames(HashSet<String> mergue_with_list) {
		for (AJSControllerItem item : controllers.values()) {
			item.putAllPrivilegesNames(mergue_with_list);
		}
	}
	
	/**
	 * @return controller -> verbs
	 */
	@AJSIgnore
	public static HashMap<String, ArrayList<String>> getAllControllersVerbsForThisUser() {
		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
		ArrayList<String> session_privileges = Secure.getSessionPrivileges();
		
		String controler_name;
		ArrayList<String> accessible_user_verbs_name;
		
		for (Map.Entry<String, AJSControllerItem> controller : controllers.entrySet()) {
			controler_name = controller.getKey();
			accessible_user_verbs_name = controller.getValue().getAllAccessibleUserVerbsName(session_privileges);
			
			if (accessible_user_verbs_name.isEmpty() == false) {
				result.put(controler_name, accessible_user_verbs_name);
			}
		}
		return result;
	}
	
	@AJSIgnore
	public static UserProfile getUserProfile() throws Exception {
		String username = Secure.connected();
		String key = UserProfile.prepareKey(username);
		CrudOrmEngine<UserProfile> engine = UserProfile.getORMEngine(key);
		return engine.getInternalElement();
	}
	
	@AJSIgnore
	public static String doRequest(String request_name, String verb_name, String request) throws SecurityException, ClassNotFoundException {
		
		AJSControllerItem controller = controllers.get(request_name);
		if (controller == null) {
			throw new ClassNotFoundException("Can't found controller \"" + request_name + "\"");
		}
		
		Verb verb = controller.getVerb(verb_name);
		if (verb == null) {
			throw new ClassNotFoundException("Can't found verb \"" + verb_name + "\" for controller \"" + request_name + "\"");
		}
		
		if (verb.hasMandatoryPrivileges() == false) {
			throw new SecurityException("Missing privileges for this request verb \"" + verb_name + "\" for controller \"" + request_name + "\"");
		}
		
		return verb.invoke(request);
	}
	
}