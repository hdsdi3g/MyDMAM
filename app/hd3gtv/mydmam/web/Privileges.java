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

import hd3gtv.log2.Log2;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.json.simple.JSONArray;

import play.Play;
import play.mvc.Controller;
import play.mvc.With;
import play.vfs.VirtualFile;
import controllers.Secure;

public class Privileges {
	
	private static List<String> privileges;
	
	static {
		privileges = new ArrayList<String>();
		privileges.add("eatBanana"); // TODO get Real privileges from controllers
		privileges.add("eatStrawberry");
		privileges.add("moveSpinach");
		privileges.add("cookSpinach");
		privileges.add("getSpinach");
		
		try {
			List<String> classes_to_test = new ArrayList<String>();
			
			/**
			 * Play modules
			 */
			for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
				File module_dir = entry.getValue().getRealFile();
				
				File[] module_app_content = (new File(module_dir.getAbsolutePath() + File.separator + "app" + File.separator + "controllers")).listFiles(new FilenameFilter() {
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
			
			classpathelements.add((new File("app/controllers")).getPath());
			
			for (int i = 0; i < classpathelements.size(); i++) {
				if (classpathelements.get(i).endsWith(".jar")) {
					try {
						JarFile jfile = new JarFile(classpathelements.get(i));
						for (Enumeration<JarEntry> entries = jfile.entries(); entries.hasMoreElements();) {
							JarEntry element = entries.nextElement();
							if (element.getName().endsWith(".class") & element.getName().startsWith("controllers")) {
								/**
								 * Not yet tested...
								 */
								classes_to_test.add(element.getName());
							}
						}
						jfile.close();
					} catch (IOException e) {
						Log2.log.error("Can't load/open jar file " + classpathelements.get(i), e);
					}
				} else {
					File directoryclass = new File(classpathelements.get(i));
					if (directoryclass.exists() && directoryclass.isDirectory()) {
						File[] list = directoryclass.listFiles(new FilenameFilter() {
							public boolean accept(File arg0, String arg1) {
								return arg0.getName().equals("controllers") & (arg1.endsWith(".class") | arg1.endsWith(".java"));
							}
						});
						for (int j = 0; j < list.length; j++) {
							classes_to_test.add(list[j].getName());
						}
					}
				}
			}
			
			Class candidate;
			With with;
			Class[] with_classes;
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
					System.out.println(candidate);
					
					// TODO test controller
					/*Object module_loader = Class.forName("controllers." + classname).newInstance();*/
					/*if (module_loader instanceof MyDMAMModule) {
						MODULES.add((MyDMAMModule) module_loader);
					}*/
					// System.out.println(module_loader.getClass().getName());
				} catch (ClassNotFoundException e) {
					Log2.log.error("Class not found " + classes_to_test.get(pos_classes), e);
					/*} catch (InstantiationException e) {
						Log2.log.error("Unvalid class " + classes_to_test.get(pos_classes), e);
					} catch (IllegalAccessException e) {
						Log2.log.error("Can't access to class " + classes_to_test.get(pos_classes), e);*/
				}
			}
			
		} catch (Exception e) {
			Log2.log.error("Can't load modules", e);
		}
		
	}
	
	public static List<String> getPrivileges() {
		return privileges;
	}
	
	public static JSONArray getJSONPrivileges() {
		JSONArray ja = new JSONArray();
		ja.addAll(privileges);
		return ja;
	}
	
	public static JSONArray getJSONPrivileges(String[] privilegenames) {
		JSONArray ja = new JSONArray();
		for (int pos = 0; pos < privilegenames.length; pos++) {
			ja.add(privilegenames[pos]);
		}
		return ja;
	}
	
}
