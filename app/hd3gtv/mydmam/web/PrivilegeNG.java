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

package hd3gtv.mydmam.web;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import controllers.Check;
import controllers.Secure;
import hd3gtv.mydmam.Loggers;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.vfs.VirtualFile;

public class PrivilegeNG {
	
	public static final String PLAY_CONTROLLERS_PATH = "/app/controllers";
	public static final String PLAY_CONTROLLERS_PACKAGE_NAME = "controllers";
	
	private static final LinkedHashMap<String, PrivilegeNG> all_privileges;
	
	static {
		all_privileges = new LinkedHashMap<String, PrivilegeNG>();
		
		/**
		 * Get all class files from all PLAY_CONTROLLERS_PATH directory, module by module.
		 */
		List<VirtualFileModule> main_dirs = getAllfromRelativePath(PLAY_CONTROLLERS_PATH, true, true);
		List<VirtualFile> class_vfiles = new ArrayList<VirtualFile>();
		for (int pos = 0; pos < main_dirs.size(); pos++) {
			class_vfiles.addAll(main_dirs.get(pos).getVfile().list());
		}
		
		/**
		 * Get all AJSController implementations from this classes.
		 */
		Class<?> candidate;
		With with;
		Class<?>[] with_classes;
		Method[] methods;
		Method candidate_method;
		Annotation candidate_method_check;
		String[] checks;
		
		for (int pos = 0; pos < class_vfiles.size(); pos++) {
			String name = class_vfiles.get(pos).getName();
			if (name.endsWith(".java") == false) {
				continue;
			}
			try {
				candidate = Class.forName(PLAY_CONTROLLERS_PACKAGE_NAME + "." + name.substring(0, name.length() - ".java".length()));
			} catch (Exception e) {
				Loggers.Play.warn("Invalid class loading", e);
				continue;
			}
			
			if (Controller.class.isAssignableFrom(candidate) == false) {
				continue;
			}
			
			if (candidate.isAnnotationPresent(With.class) == false) {
				continue;
			}
			with = (With) candidate.getAnnotation(With.class);
			with_classes = with.value();
			boolean valid = false;
			for (int pos_with = 0; pos_with < with_classes.length; pos_with++) {
				if (with_classes[pos_with].equals(Secure.class)) {
					valid = true;
					break;
				}
			}
			if (valid == false) {
				continue;
			}
			
			methods = candidate.getDeclaredMethods();
			for (int pos_methods = 0; pos_methods < methods.length; pos_methods++) {
				candidate_method = methods[pos_methods];
				if (Modifier.isStatic(candidate_method.getModifiers()) == false) {
					continue;
				}
				if (Modifier.isPublic(candidate_method.getModifiers()) == false) {
					continue;
				}
				candidate_method_check = candidate_method.getAnnotation(Check.class);
				if (candidate_method_check == null) {
					continue;
				}
				
				checks = ((Check) candidate_method_check).value();
				for (int pos_checks = 0; pos_checks < checks.length; pos_checks++) {
					createAndGetPrivilege(checks[pos_checks]).addController(candidate, candidate_method);
				}
			}
		}
		
		AJSController.getControllers().forEach((k, v) -> {
			v.mergueAllPrivileges();
		});
		
		if (Loggers.Auth.isDebugEnabled()) {
			ArrayList<String> al = new ArrayList<>();
			all_privileges.forEach((name, privilege) -> {
				al.add(name);
			});
			Loggers.Auth.debug("Actual Privilege list: " + al);
		}
		
	}
	
	/**
	 * Get all items named and on this path, from all modules, and not only the first.
	 */
	private static List<VirtualFileModule> getAllfromRelativePath(String path, boolean must_exists, boolean must_directory) {
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
	
	static PrivilegeNG createAndGetPrivilege(String privilege_name) {
		if (all_privileges.containsKey(privilege_name)) {
			return all_privileges.get(privilege_name);
		} else {
			PrivilegeNG p = new PrivilegeNG(privilege_name);
			all_privileges.put(privilege_name, p);
			return p;
		}
	}
	
	private String privilege_name;
	private ArrayList<String> associated_controllers;
	
	PrivilegeNG(String privilege_name) {
		this.privilege_name = privilege_name;
		if (privilege_name == null) {
			throw new NullPointerException("\"privilege_name\" can't to be null");
		}
		associated_controllers = new ArrayList<String>(1);
	}
	
	PrivilegeNG addController(Class<?> controller_class, Method method) {
		StringBuilder sb = new StringBuilder();
		sb.append(controller_class.getName());
		sb.append(": ");
		
		if (method.getReturnType().getSimpleName().equalsIgnoreCase("void") == false) {
			sb.append(method.getReturnType().getSimpleName());
			sb.append(" ");
		}
		sb.append(method.getName());
		sb.append("(");
		if (method.getParameterCount() > 0) {
			Parameter[] params = method.getParameters();
			for (int pos = 0; pos < params.length; pos++) {
				sb.append(params[pos].getType().getSimpleName());
				if (pos + 1 < params.length) {
					sb.append(", ");
				}
			}
		}
		sb.append(")");
		
		associated_controllers.add(sb.toString());
		return this;
	}
	
	public JsonArray getControllers() {
		JsonArray ja = new JsonArray();
		associated_controllers.forEach(item -> {
			ja.add(new JsonPrimitive(item));
		});
		return ja;
	}
	
	public String getName() {
		return privilege_name;
	}
	
	public String toString() {
		return privilege_name + ": " + getControllers();
	}
	
	public static JsonObject dumpAllPrivileges() {
		JsonObject jo = new JsonObject();
		all_privileges.forEach((k, v) -> {
			jo.add(k, v.getControllers());
		});
		return jo;
	}
	
	public static Set<String> getAllPrivilegesName() {
		return all_privileges.keySet();
	}
	
	private static class VirtualFileModule {
		
		private VirtualFile vfile;
		// private String module_name;
		
		VirtualFileModule(VirtualFile vfile, String module_name) {
			this.vfile = vfile;
			if (vfile == null) {
				throw new NullPointerException("\"vfile\" can't to be null");
			}
			// this.module_name = module_name;
			if (module_name == null) {
				throw new NullPointerException("\"module_name\" can't to be null");
			}
		}
		
		/*String getModule_name() {
			return module_name;
		}*/
		
		VirtualFile getVfile() {
			return vfile;
		}
		
		@Override
		public boolean equals(Object other) {
			if (other instanceof VirtualFileModule) {
				VirtualFileModule vf = (VirtualFileModule) other;
				if (vfile != null && vf.vfile != null) {
					return vfile.equals(vf.vfile);
				}
			}
			return super.equals(other);
		}
		
		@Override
		public int hashCode() {
			if (vfile != null) {
				return vfile.hashCode();
			}
			return super.hashCode();
		}
		
	}
	
}
