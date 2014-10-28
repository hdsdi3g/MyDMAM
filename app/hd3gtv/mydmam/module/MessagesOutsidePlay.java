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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.Yaml;

import play.i18n.Lang;
import play.utils.OrderSafeProperties;

/**
 * Don't manage modules files in Jars.
 */
public class MessagesOutsidePlay {
	
	private static final Map<Locale, Properties> locale_messages;
	private static final Locale default_locale = Locale.ENGLISH;
	
	private static void importMessageFile(File message_file) throws IOException {
		Locale current_locale = Lang.getLocale(message_file.getName().substring("messages.".length()));
		Properties current_content = new OrderSafeProperties();
		FileInputStream fis = new FileInputStream(message_file);
		current_content.load(fis);
		fis.close();
		
		if (locale_messages.containsKey(current_locale)) {
			locale_messages.get(current_locale).putAll(current_content);
		} else {
			locale_messages.put(current_locale, current_content);
		}
	}
	
	static {
		locale_messages = new HashMap<Locale, Properties>();
		
		String[] classpathelements = System.getProperty("java.class.path").split(System.getProperty("path.separator"));
		
		/**
		 * Search & parse application.conf & dependencies.yml
		 * Import all conf/messages.* for main app.
		 */
		Properties applicationconf = new OrderSafeProperties();
		List<String> modules_names = null;
		try {
			for (int i = 0; i < classpathelements.length; i++) {
				if (classpathelements[i].endsWith(".jar") == false) {
					File applicationconf_file = new File(classpathelements[i] + File.separator + "application.conf");
					File dependenciesyml_file = new File(classpathelements[i] + File.separator + "dependencies.yml");
					if (applicationconf_file.exists() && dependenciesyml_file.exists()) {
						if (applicationconf_file.isFile() && dependenciesyml_file.isFile()) {
							FileInputStream fis = new FileInputStream(applicationconf_file);
							applicationconf.load(fis);
							fis.close();
							
							fis = new FileInputStream(dependenciesyml_file);
							Yaml yaml = new Yaml();
							for (Object data : yaml.loadAll(fis)) {
								LinkedHashMap<?, ?> root_item = (LinkedHashMap<?, ?>) data;
								modules_names = (List<String>) root_item.get("require");
							}
							fis.close();
							
							/**
							 * Import all conf/messages.* for main app.
							 */
							File[] modules_files = new File(classpathelements[i]).listFiles(new FilenameFilter() {
								public boolean accept(File dir, String name) {
									return name.startsWith("messages.");
								}
							});
							for (int pos_mf = 0; pos_mf < modules_files.length; pos_mf++) {
								importMessageFile(modules_files[pos_mf]);
							}
							
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			Log2.log.error("Can't import modules configuration files", e);
		}
		
		// Add messages modules files in classpath jar files ?
		
		/**
		 * Import for each modules all conf/messages.*
		 */
		if (modules_names != null) {
			modules_names.remove("play");
			for (int pos_mn = 0; pos_mn < modules_names.size(); pos_mn++) {
				try {
					File conf_dir = new File(applicationconf.getProperty("module." + modules_names.get(pos_mn), "") + File.separator + "conf");
					if ((conf_dir.exists() == false) | (conf_dir.isDirectory() == false)) {
						Log2.log.error("Can't found module conf directory", new FileNotFoundException(conf_dir.getPath()), new Log2Dump("module name", modules_names.get(pos_mn)));
						continue;
					}
					File[] modules_files = conf_dir.listFiles(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.startsWith("messages.");
						}
					});
					for (int pos_mf = 0; pos_mf < modules_files.length; pos_mf++) {
						importMessageFile(modules_files[pos_mf]);
					}
				} catch (Exception e) {
					Log2.log.error("Can't import module message files", e, new Log2Dump("module name", modules_names.get(pos_mn)));
				}
			}
		}
		
		/**
		 * Overload some messages locales by configuration.
		 */
		if (Configuration.global.isElementExists("messages")) {
			LinkedHashMap<String, String> overload_messages = Configuration.global.getValues("messages");
			if (overload_messages.isEmpty() == false) {
				for (Map.Entry<Locale, Properties> entry_messages : locale_messages.entrySet()) {
					for (Map.Entry<String, String> entry_conf : overload_messages.entrySet()) {
						entry_messages.getValue().put(entry_conf.getKey(), entry_conf.getValue());
					}
				}
			}
		}
		
		/**
		 * Check if missing messages.
		 */
		Properties comparable_messages = null;
		Locale comparable_locale = null;
		for (Map.Entry<Locale, Properties> messages : locale_messages.entrySet()) {
			Locale current_locale = messages.getKey();
			Properties current_messages = messages.getValue();
			
			if (comparable_locale == null) {
				comparable_locale = current_locale;
				comparable_messages = current_messages;
				continue;
			}
			
			boolean has_problems = false;
			Log2Dump dump = new Log2Dump();
			for (Object current_messages_key : current_messages.keySet()) {
				if (comparable_messages.containsKey(current_messages_key) == false) {
					has_problems = true;
					dump.add("missing message", (String) current_messages_key + " in " + comparable_locale.getLanguage() + " message file");
					// TODO map key <-> source file
				}
			}
			for (Object comparable_messages_key : comparable_messages.keySet()) {
				if (current_messages.containsKey(comparable_messages_key) == false) {
					has_problems = true;
					dump.add("missing message", (String) comparable_messages_key + " in " + current_locale.getLanguage() + " message file");
					// TODO map key <-> source file
				}
			}
			if (has_problems) {
				Log2.log.error("Missing messages keys, please check messages files", null, dump);
			}
		}
		
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
	
}
