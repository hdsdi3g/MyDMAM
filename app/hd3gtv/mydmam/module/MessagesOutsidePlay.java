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
package hd3gtv.mydmam.module;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class MessagesOutsidePlay {
	
	private static Map<Locale, Properties> locale_messages = new HashMap<Locale, Properties>();
	private static Locale default_locale;// TODO set
	
	static {
		// TODO init
		// TODO warn if missing messages
	}
	
	private Properties current_locale_messages;
	
	public MessagesOutsidePlay(Locale locale) {
		if (locale == null) {
			throw new NullPointerException("\"locale\" can't to be null");
		}
		if (locale_messages.containsKey(locale)) {
			current_locale_messages = locale_messages.get(locale);
		} else {
			current_locale_messages = locale_messages.get(default_locale);
		}
	}
	
	public String get(String key) {
		String value = null;
		if (key == null) {
			return "";
		}
		if (current_locale_messages.containsKey(key)) {
			value = current_locale_messages.getProperty(key);
		} else {
			value = locale_messages.get(default_locale).getProperty(key);
		}
		if (value == null) {
			return key;
		}
		return value;
	}
	
	/*
	private static List<String> all_privileges;
	
	static {
		all_privileges = new ArrayList<String>();
		try {
			List<String> classes_to_test = new ArrayList<String>();
			
			 //* Play modules
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
			
			 // Classpath modules
			ArrayList<String> classpathelements = new ArrayList<String>();
			
			String[] classpathelementsstr = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
			
			for (int i = 0; i < classpathelementsstr.length; i++) {
				classpathelements.add(classpathelementsstr[i]);
			}
			
			classpathelements.add((new File("app/controllers")).getPath());
			
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
								 //Not yet tested...
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
							if (all_privileges.contains(checks[pos_checks]) == false) {
								all_privileges.add(checks[pos_checks]);
							}
						}
					}
				} catch (ClassNotFoundException e) {
					Log2.log.error("Class not found " + classes_to_test.get(pos_classes), e);
				}
			}
		} catch (Exception e) {
			Log2.log.error("Can't load modules", e);
		}
	}
	 * */
	
}
