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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import controllers.Secure;
import ext.Bootstrap;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.auth.UserNG;
import hd3gtv.mydmam.web.AJSControllerItem.Verb;

public class AJSController {
	
	public static final String ASYNC_PACKAGE_NAME = "controllers.ajs";
	
	private static HashMap<String, AJSControllerItem> controllers;
	
	/**
	 * Get all class files from all ASYNC_CLASS_PATH directory and get all AJSController implementations from this classes.
	 */
	@AJSIgnore
	public static HashMap<String, AJSControllerItem> getControllers() {
		if (controllers == null) {
			synchronized (ASYNC_PACKAGE_NAME) {
				controllers = new HashMap<String, AJSControllerItem>();
				
				try {
					MyDMAM.factory.getAllClassesFromPackage(AJSController.ASYNC_PACKAGE_NAME).forEach(cl -> {
						if (AJSController.class.isAssignableFrom(cl) == false) {
							Loggers.Play.warn("Class " + cl.getName() + " is not an AJSController.");
							// System.out.println(); Class<?> checked = Class.forName(class_name);
							return;
						}
						if (isControllerIsEnabled(cl) == false) {
							Loggers.Play.debug("Controller " + cl.getName() + " is currently disabled.");
							return;
						}
						AJSControllerItem item = new AJSControllerItem(cl);
						
						if (item.isEmpty() == false) {
							controllers.put(cl.getSimpleName().toLowerCase(), item);
						}
					});
				} catch (ClassNotFoundException e) {
					Loggers.Play.error("Can't load AJS controllers list", e);
				}
			}
		}
		return controllers;
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
	
	/**
	 * @return controller -> verbs
	 */
	@AJSIgnore
	public static HashMap<String, ArrayList<String>> getAllControllersVerbsForThisUser() throws DisconnectedUser {
		HashMap<String, ArrayList<String>> result = new HashMap<String, ArrayList<String>>();
		HashSet<String> session_privileges = Secure.getSessionPrivileges();
		
		String controler_name;
		ArrayList<String> accessible_user_verbs_name;
		
		for (Map.Entry<String, AJSControllerItem> controller : getControllers().entrySet()) {
			controler_name = controller.getKey();
			accessible_user_verbs_name = controller.getValue().getAllAccessibleUserVerbsName(session_privileges);
			
			if (accessible_user_verbs_name.isEmpty() == false) {
				result.put(controler_name, accessible_user_verbs_name);
			}
		}
		return result;
	}
	
	@AJSIgnore
	public static UserNG getUserProfile() throws NullPointerException {
		if (Secure.isConnected() == false) {
			throw new NullPointerException("No session user");
		}
		return Bootstrap.getAuth().getByUserKey(Secure.connected());
	}
	
	@AJSIgnore
	public static String getUserProfileLongName() {
		UserNG user = getUserProfile();
		if (user == null) {
			return "(Deleted, please log-off)";
		}
		return user.getFullname();
	}
	
	@AJSIgnore
	public static String doRequest(String request_name, String verb_name, String request) throws SecurityException, ClassNotFoundException, DisconnectedUser {
		
		AJSControllerItem controller = getControllers().get(request_name);
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
