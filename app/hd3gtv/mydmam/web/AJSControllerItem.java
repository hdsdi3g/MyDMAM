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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.gson.JsonObject;

import controllers.Check;
import controllers.Secure;
import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.GsonIgnore;

class AJSControllerItem {
	
	private Class<?> controller_class;
	private HashMap<String, Verb> verbs;
	
	AJSControllerItem(Class<?> controller_class) {
		this.controller_class = controller_class;
		if (controller_class == null) {
			throw new NullPointerException("\"controller_class\" can't to be null");
		}
		verbs = new HashMap<String, AJSControllerItem.Verb>();
		
		Method[] raw_methods;
		raw_methods = controller_class.getMethods();
		
		Method m;
		int mod;
		for (int pos = 0; pos < raw_methods.length; pos++) {
			m = raw_methods[pos];
			mod = m.getModifiers();
			if (Modifier.isPublic(mod) == false | Modifier.isStatic(mod) == false) {
				continue;
			}
			if (Modifier.isAbstract(mod) | Modifier.isInterface(mod) | Modifier.isNative(mod) | Modifier.isStrict(mod) | Modifier.isSynchronized(mod)) {
				continue;
			}
			if (m.isAnnotationPresent(AJSIgnore.class)) {
				continue;
			}
			try {
				verbs.put(m.getName().toLowerCase(), new Verb(m));
			} catch (Exception e) {
				if (e.getCause() != null) {
					Loggers.Play.warn("Can't load AJS controller verb for " + controller_class + "." + m.getName() + "()", e.getCause());
				} else {
					Loggers.Play.warn("Can't load AJS controller verb for " + controller_class + "." + m.getName() + "()", e);
				}
			}
		}
	}
	
	class Verb {
		
		@GsonIgnore
		private Method method;
		
		private Class<?> return_type;
		private Class<?> parameter_type;
		
		private ArrayList<String> mandatory_privileges;
		
		Verb(Method m) throws InstantiationException, IllegalAccessException, IndexOutOfBoundsException {
			this.method = m;
			if (method == null) {
				throw new NullPointerException("\"method\" can't to be null");
			}
			return_type = m.getReturnType();
			if (return_type != null) {
				if (return_type.getName().equals("void") == false) {
					return_type.newInstance();
				} else {
					return_type = null;
				}
			}
			
			if (m.getParameterTypes().length == 1) {
				parameter_type = m.getParameterTypes()[0];
			}
			if (m.getParameterTypes().length > 1) {
				throw new IndexOutOfBoundsException("Invalid parameter count (" + m.getParameterTypes().length + ") for verb");
			}
			
			if (m.isAnnotationPresent(Check.class)) {
				mandatory_privileges = new ArrayList<String>(Arrays.asList(m.getAnnotation(Check.class).value()));
			} else {
				mandatory_privileges = new ArrayList<String>();
			}
		}
		
		boolean hasMandatoryPrivileges() {
			return hasMandatoryPrivileges(Secure.getSessionPrivileges());
		}
		
		boolean hasMandatoryPrivileges(ArrayList<String> session_privileges) {
			if (mandatory_privileges.isEmpty()) {
				return true;
			}
			for (int pos = 0; pos < mandatory_privileges.size(); pos++) {
				if (session_privileges.contains(mandatory_privileges.get(pos))) {
					return true;
				}
			}
			
			return false;
		}
		
		String invoke(String json_request) {
			Object request = null;
			if (parameter_type != null) {
				request = AJSController.gson.fromJson(json_request, parameter_type);
			}
			
			Object response = null;
			try {
				if (parameter_type == null) {
					response = method.invoke(null);
				} else {
					response = method.invoke(null, request);
				}
			} catch (IllegalAccessException e) {
				Loggers.Play.warn("Can't invoke AJS controller verb for " + controller_class + "." + method.getName() + "()", e);
			} catch (IllegalArgumentException e) {
				Loggers.Play.warn("Can't invoke AJS controller verb for " + controller_class + "." + method.getName() + "()", e);
			} catch (InvocationTargetException e) {
				Loggers.Play.error("Exception during AJS controller verb invoke for " + controller_class + "." + method.getName() + "()", e.getCause());
				if (return_type != null) {
					try {
						return AJSController.gson.toJson(return_type.newInstance(), return_type);
					} catch (Exception e1) {
						Loggers.Play.error("Can't create return object during AJS controller verb invoke for " + controller_class + "." + method.getName() + "()", e1);
					}
				}
			}
			
			if (return_type != null) {
				return AJSController.gson.toJson(response, return_type);
			}
			
			return new JsonObject().toString();
		}
		
	}
	
	boolean hasVerb(String verb_name) {
		return verbs.containsKey(verb_name);
	}
	
	Verb getVerb(String verb_name) {
		return verbs.get(verb_name);
	}
	
	boolean isEmpty() {
		return verbs.isEmpty();
	}
	
	void putAllPrivilegesNames(HashSet<String> mergue_with_list) {
		for (Verb verb : verbs.values()) {
			mergue_with_list.addAll(verb.mandatory_privileges);
		}
	}
	
	ArrayList<String> getAllAccessibleUserVerbsName(ArrayList<String> session_privileges) {
		ArrayList<String> result = new ArrayList<String>(verbs.size());
		
		for (Map.Entry<String, Verb> entry : verbs.entrySet()) {
			if (entry.getValue().hasMandatoryPrivileges(session_privileges)) {
				result.add(entry.getKey());
			}
		}
		
		return result;
	}
	
}
