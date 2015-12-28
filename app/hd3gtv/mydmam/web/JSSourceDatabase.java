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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.GsonIgnore;
import hd3gtv.tools.GsonIgnoreStrategy;

public class JSSourceDatabase {
	
	private static final String BASE_CONF_DIRECTORY = File.separator + "conf";
	
	private static final String BASE_SOURCE_DIRECTORY_JSX = "/app/react";
	private static final String BASE_SOURCE_DIRECTORY_VANILLA_JS = "/public/javascripts/src";
	
	private static final Gson gson_simple;
	private static final Gson gson;
	
	private static final Type type_HM_db = new TypeToken<HashMap<String, JSSourceDatabaseEntry>>() {
	}.getType();
	
	private final static class Serializer implements JsonSerializer<JSSourceDatabase>, JsonDeserializer<JSSourceDatabase> {
		
		public JsonElement serialize(JSSourceDatabase src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = gson_simple.toJsonTree(src).getAsJsonObject();
			result.add("entries", gson_simple.toJsonTree(src.entries, type_HM_db));
			return result;
		}
		
		public JSSourceDatabase deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JSSourceDatabase result = gson_simple.fromJson(jejson, JSSourceDatabase.class);
			result.entries = gson_simple.fromJson(jejson.getAsJsonObject().get("entries"), type_HM_db);
			return result;
		}
		
	}
	
	static {
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		gson_simple = builder.create();
		
		builder.registerTypeAdapter(JSSourceDatabase.class, new Serializer());
		builder.setPrettyPrinting();
		gson = builder.create();
	}
	
	private transient File dbfile;
	private transient String module_name;
	private transient File module_path;
	@GsonIgnore
	private HashMap<String, JSSourceDatabaseEntry> entries;
	
	static JSSourceDatabase create(JSSourceModule source_module) throws IOException {
		JSSourceDatabase jsdb = null;
		File dbfile = new File(source_module.getModulePath().getPath() + BASE_CONF_DIRECTORY + File.separator + "jsfiles.json");
		
		if (dbfile.exists() == false) {
			jsdb = new JSSourceDatabase();
			jsdb.dbfile = dbfile;
			jsdb.module_name = source_module.getModuleName();
			jsdb.module_path = source_module.getModulePath();
			jsdb.entries = new HashMap<String, JSSourceDatabaseEntry>();
			jsdb.save();
		} else {
			jsdb = gson.fromJson(FileUtils.readFileToString(dbfile), JSSourceDatabase.class);
			jsdb.dbfile = dbfile;
			jsdb.module_name = source_module.getModuleName();
			jsdb.module_path = source_module.getModulePath();
		}
		return jsdb;
	}
	
	private JSSourceDatabase() {
	}
	
	File getDbfile() {
		return dbfile;
	}
	
	void save() throws IOException {
		Loggers.Play_JSSource.debug("Save database: " + dbfile + " (module: " + module_name + ")");
		FileUtils.write(dbfile, gson.toJson(this));
	}
	
	/**
	 * Remove old/invalid db entries.
	 * @return it can be maybe deleted files !
	 */
	ArrayList<JSSourceDatabaseEntry> checkAndClean() {
		Loggers.Play_JSSource.trace("Check and clean: " + dbfile + " (module: " + module_name + ")");
		
		ArrayList<JSSourceDatabaseEntry> result = new ArrayList<JSSourceDatabaseEntry>();
		ArrayList<String> remove_this = new ArrayList<String>();
		
		for (Map.Entry<String, JSSourceDatabaseEntry> entry : entries.entrySet()) {
			try {
				entry.getValue().checkRealFile(module_path);
			} catch (FileNotFoundException e) {
				Loggers.Play_JSSource.info("Deleted file for JSDB entry: " + entry.getValue() + " (" + e.getMessage() + ")");
				remove_this.add(entry.getKey());
				result.add(entry.getValue());
			} catch (IOException e) {
				Loggers.Play_JSSource.debug("Altered file for JSDB entry: " + entry.getValue() + " (" + e.getMessage() + ")");
				entry.getValue().refresh(module_path);
				result.add(entry.getValue());
			}
		}
		
		for (String key : remove_this) {
			entries.remove(key);
		}
		
		if ((result.isEmpty() == false | remove_this.isEmpty() == false) & Loggers.Play_JSSource.isDebugEnabled()) {
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put("result", result);
			log.put("remove_this", remove_this);
			Loggers.Play_JSSource.debug("Check and clean results for " + dbfile + " (module: " + module_name + ") " + log);
		}
		
		return result;
	}
	
	ArrayList<JSSourceDatabaseEntry> newEntries() {
		Loggers.Play_JSSource.trace("New entries check: " + dbfile + " (module: " + module_name + ")");
		ArrayList<JSSourceDatabaseEntry> new_files = new ArrayList<JSSourceDatabaseEntry>();
		
		File_Filter file_filter = new File_Filter();
		File founded;
		String key_name;
		
		File source_vanilla_js = new File(module_path.getAbsolutePath() + BASE_SOURCE_DIRECTORY_VANILLA_JS);
		if (source_vanilla_js.exists()) {
			FileUtils.listFilesAndDirs(source_vanilla_js, file_filter, TrueFileFilter.INSTANCE);
			for (int pos = 0; pos < file_filter.all_files.size(); pos++) {
				founded = file_filter.all_files.get(pos);
				key_name = JSSourceDatabaseEntry.makeKeyName(module_path, founded);
				if (entries.containsKey(key_name) == false) {
					JSSourceDatabaseEntry entry;
					entry = new JSSourceDatabaseEntry(module_path, founded, BASE_SOURCE_DIRECTORY_VANILLA_JS);
					entries.put(key_name, entry);
					new_files.add(entry);
				}
			}
			file_filter.all_files.clear();
		}
		
		File source_jsx = new File(module_path.getAbsolutePath() + BASE_SOURCE_DIRECTORY_JSX);
		if (source_jsx.exists()) {
			FileUtils.listFilesAndDirs(source_jsx, file_filter, TrueFileFilter.INSTANCE);
			for (int pos = 0; pos < file_filter.all_files.size(); pos++) {
				founded = file_filter.all_files.get(pos);
				key_name = JSSourceDatabaseEntry.makeKeyName(module_path, founded);
				if (entries.containsKey(key_name) == false) {
					JSSourceDatabaseEntry entry;
					entry = new JSSourceDatabaseEntry(module_path, founded, BASE_SOURCE_DIRECTORY_JSX);
					entries.put(key_name, entry);
					new_files.add(entry);
				}
			}
		}
		
		if (new_files.isEmpty() == false & Loggers.Play_JSSource.isDebugEnabled()) {
			Loggers.Play_JSSource.debug("New entries check results for " + dbfile + " (module: " + module_name + ") " + new_files);
		}
		
		return new_files;
	}
	
	private class File_Filter implements IOFileFilter {
		
		ArrayList<File> all_files = new ArrayList<File>();
		
		public boolean accept(File dir, String name) {
			return true;
		}
		
		public boolean accept(File file) {
			if (file.isHidden()) {
				return false;
			}
			if (file.isFile() == false) {
				return false;
			}
			all_files.add(file);
			return file.isDirectory();
		}
	}
	
	ArrayList<JSSourceDatabaseEntry> getSortedEntries() {
		ArrayList<JSSourceDatabaseEntry> items = new ArrayList<JSSourceDatabaseEntry>(entries.values());
		items.sort(JSSourceDatabaseEntry.COMPARATOR);
		return items;
	}
	
}
