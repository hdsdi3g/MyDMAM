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
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import play.i18n.Lang;
import play.utils.OrderSafeProperties;

/**
 * Don't manage modules files in Jars.
 */
public class MessagesOutsidePlay {
	
	private static final Map<Locale, Properties> locale_messages;
	private static final Locale default_locale = Locale.ENGLISH;
	
	static {
		Map<String, String> map_key_sourcemessage = new HashMap<String, String>();
		
		locale_messages = new HashMap<Locale, Properties>();
		
		LinkedHashMap<String, File> conf_dirs = MyDMAMModulesManager.getAllConfDirectories();
		
		String module_name;
		File conf_dir;
		File message_file;
		Locale current_locale;
		Properties current_messages;
		FileInputStream fis;
		for (Map.Entry<String, File> entry : conf_dirs.entrySet()) {
			module_name = entry.getKey();
			conf_dir = entry.getValue();
			try {
				File[] modules_files = conf_dir.listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith("messages.");
					}
				});
				for (int pos_mf = 0; pos_mf < modules_files.length; pos_mf++) {
					message_file = modules_files[pos_mf];
					current_locale = Lang.getLocale(message_file.getName().substring("messages.".length()));
					current_messages = new OrderSafeProperties();
					fis = new FileInputStream(message_file);
					current_messages.load(fis);
					fis.close();
					
					for (Object message_key : current_messages.keySet()) {
						map_key_sourcemessage.put((String) message_key, module_name);
					}
					
					if (locale_messages.containsKey(current_locale)) {
						locale_messages.get(current_locale).putAll(current_messages);
					} else {
						locale_messages.put(current_locale, current_messages);
					}
				}
			} catch (Exception e) {
				Log2.log.error("Can't import module message files", e, new Log2Dump("module name", module_name));
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
			current_locale = messages.getKey();
			current_messages = messages.getValue();
			
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
					module_name = map_key_sourcemessage.get((String) current_messages_key);
					dump.add("missing message", "\"" + (String) current_messages_key + "\" in " + module_name + " module, conf/message." + comparable_locale.getLanguage() + " file");
				}
			}
			for (Object comparable_messages_key : comparable_messages.keySet()) {
				if (current_messages.containsKey(comparable_messages_key) == false) {
					has_problems = true;
					module_name = map_key_sourcemessage.get((String) comparable_messages_key);
					dump.add("missing message", "\"" + (String) comparable_messages_key + "\" in " + module_name + " module, conf/message." + current_locale.getLanguage() + " file");
				}
			}
			if (has_problems) {
				Log2.log.error("Missing messages keys, please check messages files", null, dump);
			}
		}
		
	}
	
	private Properties current_locale_messages;
	private Locale locale;
	
	public MessagesOutsidePlay(Locale locale) {
		this.locale = locale;
		if (locale == null) {
			throw new NullPointerException("\"locale\" can't to be null");
		}
		if (locale_messages.containsKey(locale)) {
			current_locale_messages = locale_messages.get(locale);
		} else {
			current_locale_messages = locale_messages.get(default_locale);
		}
	}
	
	public String get(String key, Object... args) {
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
		
		if (args == null) {
			return value;
		}
		if (args.length == 0) {
			return value;
		}
		return String.format(locale, value, args);
	}
}
