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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import controllers.Secure;
import ext.Bootstrap;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.web.AJSControllerItem.Verb;
import hd3gtv.tools.GsonIgnoreStrategy;
import play.Play;
import play.vfs.VirtualFile;

public class AJSController {
	
	public static final String ASYNC_CLASS_PATH = "/app/controllers/ajs";
	public static final String ASYNC_PACKAGE_NAME = "controllers.ajs";
	public static final Gson gson_simple;
	// private static final Gson gson_pretty;
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
		MyDMAM.registerJsonArrayAndObjectSerializer(gson_builder);
		gson_simple = gson_builder.create();
		// gson_pretty = gson_builder.setPrettyPrinting().create();
		gson = gson_builder.create();
		
		/**
		 * Get all class files from all ASYNC_CLASS_PATH directory, module by module.
		 */
		List<VirtualFileModule> main_dirs = getAllfromRelativePath(ASYNC_CLASS_PATH, true, true);
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
				if (isControllerIsEnabled(class_candidate) == false) {
					Loggers.Play.debug("Controller " + class_candidate.getName() + " is currently disabled.");
					continue;
				}
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
	
	public static Gson getGson() {
		return gson;
	}
	
	/**
	 * Get all items named and on this path, from all modules, and not only the first.
	 */
	@AJSIgnore
	public static List<VirtualFileModule> getAllfromRelativePath(String path, boolean must_exists, boolean must_directory) {
		List<VirtualFileModule> file_list = new ArrayList<VirtualFileModule>();
		
		LinkedHashMap<VirtualFile, String> path_modules = new LinkedHashMap<VirtualFile, String>();
		for (VirtualFile vfile : Play.roots) {
			/**
			 * 1st pass : add all paths (main and modules).
			 */
			path_modules.put(vfile, "internal");
		}
		for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
			/**
			 * 2nd pass : overload enties with modules names.
			 */
			path_modules.put(entry.getValue(), entry.getKey());
		}
		
		VirtualFile child;
		for (Map.Entry<VirtualFile, String> entry : path_modules.entrySet()) {
			child = entry.getKey().child(path);
			if (must_exists & (child.exists() == false)) {
				continue;
			}
			if (must_directory & (child.isDirectory() == false)) {
				continue;
			}
			file_list.add(new VirtualFileModule(child, entry.getValue()));
		}
		
		Collections.sort(file_list, new Comparator<VirtualFileModule>() {
			public int compare(VirtualFileModule o1, VirtualFileModule o2) {
				return o1.getVfile().getName().compareToIgnoreCase(o2.getVfile().getName());
			}
		});
		
		return file_list;
	}
	
	private static boolean isControllerIsEnabled(Class<?> controller) {
		try {
			Method[] ms = controller.getMethods();
			Method m = null;
			for (int pos = 0; pos < ms.length; pos++) {
				if (ms[pos].getName().equalsIgnoreCase("isEnabled")) {
					m = ms[pos];
				}
			}
			
			if (m == null) {
				return true;
			}
			
			int mod = m.getModifiers();
			if (Modifier.isPublic(mod) == false | Modifier.isStatic(mod) == false) {
				return true;
			}
			if (Modifier.isAbstract(mod) | Modifier.isInterface(mod) | Modifier.isNative(mod) | Modifier.isStrict(mod) | Modifier.isSynchronized(mod)) {
				return true;
			}
			if (m.getReturnType() == boolean.class | m.getReturnType() == Boolean.class) {
				return (Boolean) m.invoke(null);
			} else {
				Loggers.Play.warn("Invalid return type in check is AJS controller is enabled, for " + controller + " (" + m.getReturnType() + ")");
			}
		} catch (Exception e) {
			Loggers.Play.error("Can't check if AJSContoller is enabled for " + controller, e);
		}
		return true;
	}
	
	@AJSIgnore
	public static void registerTypeAdapter(Type type, Object typeAdapter) {
		gson_builder.registerTypeAdapter(type, typeAdapter);
		gson = gson_builder.create();
	}
	
	@AJSIgnore
	public static HashMap<String, AJSControllerItem> getControllers() {
		return controllers;
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
	public static UserNG getUserProfile() throws Exception {
		return Bootstrap.getAuth().getByUserKey(Secure.connected());
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
