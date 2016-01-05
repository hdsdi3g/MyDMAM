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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import controllers.Check;
import controllers.Secure;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.vfs.VirtualFile;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class Privileges {
	
	private static final List<String> privileges;
	private static final String privileges_json_array;
	
	static {
		HashSet<String> all_privileges = new HashSet<String>();
		try {
			List<String> classes_to_test = new ArrayList<String>();
			
			/**
			 * Play modules
			 */
			File[] module_app_content;
			for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
				File module_dir = entry.getValue().getRealFile();
				
				module_app_content = (new File(module_dir.getAbsolutePath() + File.separator + "app" + File.separator + "controllers")).listFiles(new FilenameFilter() {
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith(".java");
					}
				});
				if (module_app_content != null) {
					for (int pos = 0; pos < module_app_content.length; pos++) {
						classes_to_test.add(module_app_content[pos].getName());
					}
				}
			}
			
			/**
			 * Classpath modules
			 */
			ArrayList<String> classpathelements = new ArrayList<String>();
			
			String[] classpathelementsstr = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
			
			for (int i = 0; i < classpathelementsstr.length; i++) {
				classpathelements.add(classpathelementsstr[i]);
			}
			
			classpathelements.add((new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY.getAbsolutePath() + File.separator + "app" + File.separator + "controllers")).getPath());
			
			JarFile jfile;
			JarEntry element;
			FilenameFilter filenamefilter = new FilenameFilter() {
				public boolean accept(File arg0, String arg1) {
					return arg0.getName().equals("controllers") & (arg1.endsWith(".class") | arg1.endsWith(".java"));
				}
			};
			
			for (int i = 0; i < classpathelements.size(); i++) {
				if (classpathelements.get(i).endsWith(".jar")) {
					try {
						jfile = new JarFile(classpathelements.get(i));
						for (Enumeration<JarEntry> entries = jfile.entries(); entries.hasMoreElements();) {
							element = entries.nextElement();
							if (element.getName().endsWith(".class") & element.getName().startsWith("controllers")) {
								/**
								 * Not yet tested...
								 */
								classes_to_test.add(element.getName());
							}
						}
						jfile.close();
					} catch (IOException e) {
						Loggers.Play.error("Can't load/open jar file " + classpathelements.get(i), e);
					}
				} else {
					File directoryclass = new File(classpathelements.get(i));
					if (directoryclass.exists() && directoryclass.isDirectory()) {
						File[] list = directoryclass.listFiles(filenamefilter);
						for (int j = 0; j < list.length; j++) {
							classes_to_test.add(list[j].getName());
						}
					}
				}
			}
			
			Class candidate;
			With with;
			Class[] with_classes;
			Method[] methods;
			Method candidate_method;
			Annotation candidate_method_check;
			String[] checks;
			for (int pos_classes = 0; pos_classes < classes_to_test.size(); pos_classes++) {
				try {
					String classname = classes_to_test.get(pos_classes);
					if (classname.endsWith(".java")) {
						classname = classname.substring(0, classname.length() - (".java".length()));
					} else if (classname.endsWith(".class")) {
						classname = classname.substring(0, classname.length() - (".class".length()));
					}
					
					candidate = Class.forName("controllers." + classname);
					
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
							all_privileges.add(checks[pos_checks]);
						}
					}
				} catch (ClassNotFoundException e) {
					Loggers.Play.error("Class not found " + classes_to_test.get(pos_classes), e);
				}
			}
		} catch (Exception e) {
			Loggers.Play.error("Can't load modules", e);
		}
		
		AJSController.putAllPrivilegesNames(all_privileges);
		
		ArrayList list = new ArrayList<String>(all_privileges);
		Collections.sort(list);
		privileges = Collections.unmodifiableList(list);
		
		JsonArray ja_privileges = new JsonArray();
		for (int pos = 0; pos < privileges.size(); pos++) {
			ja_privileges.add(new JsonPrimitive(privileges.get(pos)));
		}
		privileges_json_array = ja_privileges.toString();
	}
	
	public static List<String> getAllSortedPrivileges() {
		return privileges;
	}
	
	public static String getJSONArrayStringPrivileges() {
		return privileges_json_array;
	}
	
	public static String getJSONStringPrivileges(String[] privilegenames) {
		JsonArray ja = new JsonArray();
		for (int pos = 0; pos < privilegenames.length; pos++) {
			ja.add(new JsonPrimitive(privilegenames[pos]));
		}
		return ja.toString();
	}
	
}
