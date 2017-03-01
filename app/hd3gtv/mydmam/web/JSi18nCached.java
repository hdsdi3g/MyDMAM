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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.web;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.CopyMove;
import play.i18n.Messages;

public class JSi18nCached {
	
	/**
	 * Test if it needs to update cache
	 * Make gzip by lang
	 * Make & publish link by lang
	 * Inject conf personalized messages in page.
	 */
	
	private Db db;
	/**
	 * @deprecated
	 */
	private Gson simple_gson;
	private boolean is_debug_mode = false;
	
	public JSi18nCached() {
		/**
		 * Compare Messages entries between languages
		 */
		String first_locales_lang = null;
		Properties first_locales_messages = null;
		Set<String> first_locales_messages_string;
		
		String actual_locales_lang = null;
		Set<String> actual_messages_string;
		StringBuilder sb;
		boolean has_missing = false;
		
		for (Map.Entry<String, Properties> entry_messages_locale : Messages.locales.entrySet()) {
			if (first_locales_lang == null) {
				first_locales_lang = entry_messages_locale.getKey();
				first_locales_messages = entry_messages_locale.getValue();
				continue;
			}
			first_locales_messages_string = first_locales_messages.stringPropertyNames();
			actual_messages_string = entry_messages_locale.getValue().stringPropertyNames();
			actual_locales_lang = entry_messages_locale.getKey();
			
			sb = new StringBuilder();
			has_missing = false;
			for (String string : actual_messages_string) {
				if (first_locales_messages_string.contains(string) == false) {
					sb.append(" missing: " + string);
					has_missing = true;
				}
			}
			if (has_missing) {
				Loggers.Play_i18n.warn("Missing Messages strings in messages." + first_locales_lang + " lang (declared in messages." + actual_locales_lang + ") " + sb.toString());
			}
			
			sb = new StringBuilder();
			has_missing = false;
			for (String string : first_locales_messages_string) {
				if (actual_messages_string.contains(string) == false) {
					sb.append(" missing: " + string);
					has_missing = true;
				}
			}
			if (has_missing) {
				Loggers.Play_i18n.warn("Missing Messages strings in messages." + actual_locales_lang + " lang (declared in messages." + first_locales_lang + ") " + sb.toString());
			}
		}
		
		is_debug_mode = Configuration.global.getValueBoolean("play", "check_i18n_cache_files");
		
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson simple_pretty_gson = builder.create();
		simple_gson = builder.create();
		
		/**
		 * Load actual entries.
		 */
		db = new Db();
		db.entries = new ArrayList<>(Messages.locales.size());
		
		Messages.locales.keySet().forEach(lang -> {
			try {
				db.entries.add(new I18nEntry(lang, Messages.all(lang)));
			} catch (Exception e) {
				Loggers.Play_i18n.error("Can't process i18n entry", e);
			}
		});
		
		if (is_debug_mode) {
			/**
			 * Load db file
			 */
			File dbfile = new File(MyDMAM.APP_ROOT_PLAY_CONF_DIRECTORY + File.separator + "messages-db.json");
			
			try {
				if (dbfile.exists() == false) {
					FileUtils.write(dbfile, simple_pretty_gson.toJson(db), MyDMAM.UTF8);
					db.entries.forEach(entry -> {
						entry.makeCacheFile();
					});
					
				} else {
					Db db_compare = simple_pretty_gson.fromJson(FileUtils.readFileToString(dbfile, MyDMAM.UTF8), Db.class);
					
					List<I18nEntry> to_refresh = db.entries.stream().filter(current -> {
						Optional<I18nEntry> o_previous = db_compare.entries.stream().filter(previous -> {
							return previous.lang.equals(current.lang);
						}).findFirst();
						
						if (o_previous.isPresent()) {
							return o_previous.get().hash.equals(current.hash) == false;
						}
						
						return true;
					}).collect(Collectors.toList());
					
					if (to_refresh.isEmpty() == false) {
						to_refresh.forEach(r -> {
							r.makeCacheFile();
						});
						
						FileUtils.write(dbfile, simple_pretty_gson.toJson(db), MyDMAM.UTF8);
					}
				}
			} catch (Exception e) {
				Loggers.Play_i18n.error("Can't check db i18n messages with " + dbfile, e);
			}
		}
	}
	
	private class Db {
		private ArrayList<I18nEntry> entries;
	}
	
	private class I18nEntry {
		String lang;
		String hash;
		int size;
		transient Properties messages;
		transient File cache_file;
		
		I18nEntry(String lang, Properties messages) throws Exception {
			this.lang = lang;
			this.messages = messages;
			size = messages.size();
			cache_file = new File(MyDMAM.APP_ROOT_PLAY_DIRECTORY + RELATIVE_JS_DEST.replace("/", File.separator) + File.separator + lang + ".js.gz");
			
			if (is_debug_mode) {
				final MessageDigest md = MessageDigest.getInstance("MD5");
				
				messages.forEach((k, v) -> {
					md.update(((String) k).getBytes(MyDMAM.UTF8));
					md.update(":".getBytes());
					md.update(((String) v).getBytes(MyDMAM.UTF8));
					md.update(";".getBytes());
				});
				hash = MyDMAM.byteToString(md.digest());
				
				Loggers.Play_i18n.debug("Prepare message " + lang + ": " + size + " items with hash: " + hash);
			}
		}
		
		void makeCacheFile() {
			Loggers.Play_i18n.info("Make i18n cache file: " + lang);
			
			GZIPOutputStream gz_concated_out_stream_gzipped = null;
			try {
				String js_content = "var i18nMessages = " + simple_gson.toJson(messages) + ";";
				
				gz_concated_out_stream_gzipped = new GZIPOutputStream(new FileOutputStream(getFile()), 0xFFFF);
				gz_concated_out_stream_gzipped.write(js_content.getBytes(MyDMAM.UTF8));
				gz_concated_out_stream_gzipped.finish();
				gz_concated_out_stream_gzipped.close();
			} catch (Exception e) {
				if (gz_concated_out_stream_gzipped != null) {
					IOUtils.closeQuietly(gz_concated_out_stream_gzipped);
				}
				Loggers.Play_i18n.error("Can't make i18n cache file: " + lang);
			}
		}
		
		File getFile() {
			return cache_file;
		}
		
	}
	
	static final String RELATIVE_JS_DEST = "/public/javascripts/i18n";
	
	/**
	 * @param locale like "fr" or "en-US"
	 */
	public File getCachedFile(String locale) {
		if (locale == null) {
			locale = "en";
		} else if (locale.isEmpty()) {
			locale = "en";
		} else if (locale.length() < 2) {
			locale = "en";
		}
		final String _local = locale.toLowerCase();
		
		Optional<I18nEntry> o_entry = db.entries.stream().filter(entry -> {
			return entry.lang.toLowerCase().startsWith(_local);
		}).findFirst();
		
		try {
			if (o_entry.isPresent()) {
				File f = o_entry.get().getFile();
				CopyMove.checkExistsCanRead(f);
				return f;
			}
		} catch (Exception e) {
			Loggers.Play_i18n.error("Can't get i18n cache file for " + locale, e);
		}
		
		File f = db.entries.get(0).getFile();
		
		if (f.exists() == false) {
			return null;
		}
		
		return f;
	}
	
}
