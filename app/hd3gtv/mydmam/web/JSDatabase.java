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
import play.Play;
import play.vfs.VirtualFile;

public class JSDatabase {
	
	private static final String BASE_CONF_DIRECTORY = File.separator + "conf";
	
	private static final String BASE_SOURCE_DIRECTORY_JSX = "/app/react";
	private static final String BASE_SOURCE_DIRECTORY_VANILLA_JS = "/public/javascripts/src";
	private static final String BASE_TRANSFORMED_DIRECTORY_JSX = "/public/javascripts/jsx";
	private static final String BASE_REDUCED_DIRECTORY_JS = "/public/javascripts/bin";
	
	/*private static File getDeclarationFile(File transformed_jsx_directory, String module_name) {
		return new File(transformed_jsx_directory + File.separator + "_declarations_" + module_name + ".jsx");
	}*/
	
	@GsonIgnore
	private static final ArrayList<JSDatabase> modules_databases;
	private static final Gson gson_simple;
	private static final Gson gson;
	
	private static final Type type_HM_db = new TypeToken<HashMap<String, JSDatabaseEntry>>() {
	}.getType();
	
	private final static class Serializer implements JsonSerializer<JSDatabase>, JsonDeserializer<JSDatabase> {
		
		public JsonElement serialize(JSDatabase src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = gson_simple.toJsonTree(src).getAsJsonObject();
			result.add("entries", gson_simple.toJsonTree(src.entries, type_HM_db));
			return result;
		}
		
		public JSDatabase deserialize(JsonElement jejson, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JSDatabase result = gson_simple.fromJson(jejson, JSDatabase.class);
			result.entries = gson_simple.fromJson(jejson.getAsJsonObject().get("entries"), type_HM_db);
			return result;
		}
		
	}
	
	static {
		modules_databases = new ArrayList<JSDatabase>(1);
		
		GsonBuilder builder = new GsonBuilder();
		builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		builder.addDeserializationExclusionStrategy(ignore_strategy);
		builder.addSerializationExclusionStrategy(ignore_strategy);
		gson_simple = builder.create();
		
		builder.registerTypeAdapter(JSDatabase.class, new Serializer());
		builder.setPrettyPrinting();
		gson = builder.create();
	}
	
	public static void init() throws ClassNotFoundException, IOException {
		for (VirtualFile vfile : Play.roots) {
			/**
			 * 1st pass : get only main, the first.
			 */
			modules_databases.add(JSDatabase.create("internal", vfile.getRealFile().getAbsoluteFile()));
			break;
		}
		for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
			/**
			 * 2nd pass : get the modules paths and names.
			 */
			if (entry.getKey().startsWith("_")) {
				continue;
			}
			modules_databases.add(JSDatabase.create(entry.getKey(), entry.getValue().getRealFile().getAbsoluteFile()));
		}
	}
	
	private transient File dbfile;
	private transient String module_name;
	private transient File module_path;
	private HashMap<String, JSDatabaseEntry> entries;
	
	private static JSDatabase create(String module_name, File module_path) throws IOException {
		if (module_name == null) {
			throw new NullPointerException("\"module_name\" can't to be null");
		}
		if (module_path == null) {
			throw new NullPointerException("\"module_path\" can't to be null");
		}
		
		JSDatabase jsdb = null;
		File dbfile = new File(module_path.getPath() + BASE_CONF_DIRECTORY + File.separator + "jsfiles.json");
		
		if (dbfile.exists() == false) {
			jsdb = new JSDatabase();
			jsdb.dbfile = dbfile;
			jsdb.module_name = module_name;
			jsdb.module_path = module_path;
			jsdb.entries = new HashMap<String, JSDatabaseEntry>();
			jsdb.save();
		} else {
			jsdb = gson.fromJson(FileUtils.readFileToString(dbfile), JSDatabase.class);
			jsdb.dbfile = dbfile;
			jsdb.module_name = module_name;
			jsdb.module_path = module_path;
		}
		return jsdb;
	}
	
	private JSDatabase() {
	}
	
	private void save() throws IOException {
		FileUtils.write(dbfile, gson.toJson(this));
	}
	
	/**
	 * Remove old/invalid db entries.
	 * @return it can be maybe deleted files !
	 */
	public ArrayList<JSDatabaseEntry> checkAndClean() {
		ArrayList<JSDatabaseEntry> result = new ArrayList<JSDatabaseEntry>();
		ArrayList<String> remove_this = new ArrayList<String>();
		
		for (Map.Entry<String, JSDatabaseEntry> entry : entries.entrySet()) {
			try {
				entry.getValue().checkRealFile(module_path);
			} catch (FileNotFoundException e) {
				Loggers.Play.debug("Deleted file for JSDB entry: " + entry.getValue(), e);
				remove_this.add(entry.getKey());
				result.add(entry.getValue());
			} catch (IOException e) {
				Loggers.Play.debug("Altered file for JSDB entry: " + entry.getValue(), e);
				result.add(entry.getValue());
			}
		}
		
		for (String key : remove_this) {
			entries.remove(key);
		}
		
		return result;
	}
	
	public ArrayList<JSDatabaseEntry> newEntries() {
		ArrayList<JSDatabaseEntry> new_files = new ArrayList<JSDatabaseEntry>();
		
		File_Filter file_filter = new File_Filter();
		File founded;
		String key_name;
		
		File source_vanilla_js = new File(module_path.getAbsolutePath() + BASE_SOURCE_DIRECTORY_VANILLA_JS);
		if (source_vanilla_js.exists()) {
			FileUtils.listFilesAndDirs(source_vanilla_js, file_filter, TrueFileFilter.INSTANCE);
			for (int pos = 0; pos < file_filter.all_files.size(); pos++) {
				founded = file_filter.all_files.get(pos);
				key_name = JSDatabaseEntry.makeKeyName(module_path, founded);
				if (entries.containsKey(key_name) == false) {
					JSDatabaseEntry entry;
					entry = new JSDatabaseEntry(module_path, founded, BASE_SOURCE_DIRECTORY_VANILLA_JS);
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
				key_name = JSDatabaseEntry.makeKeyName(module_path, founded);
				if (entries.containsKey(key_name) == false) {
					JSDatabaseEntry entry;
					entry = new JSDatabaseEntry(module_path, founded, BASE_SOURCE_DIRECTORY_JSX);
					entries.put(key_name, entry);
					new_files.add(entry);
				}
			}
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
	
	/**
	 * @return module name -> file list
	 */
	public static LinkedHashMap<String, ArrayList<JSDatabaseEntry>> getAlteredFiles() {
		LinkedHashMap<String, ArrayList<JSDatabaseEntry>> result = new LinkedHashMap<String, ArrayList<JSDatabaseEntry>>();
		JSDatabase db;
		ArrayList<JSDatabaseEntry> entries;
		for (int pos = 0; pos < modules_databases.size(); pos++) {
			db = modules_databases.get(pos);
			entries = db.checkAndClean();
			if (entries.isEmpty() == false) {
				result.put(db.module_name, entries);
			}
		}
		return result;
	}
	
	/**
	 * @return module name -> file list
	 */
	public static LinkedHashMap<String, ArrayList<JSDatabaseEntry>> getNewFiles() {
		LinkedHashMap<String, ArrayList<JSDatabaseEntry>> result = new LinkedHashMap<String, ArrayList<JSDatabaseEntry>>();
		JSDatabase db;
		ArrayList<JSDatabaseEntry> entries;
		for (int pos = 0; pos < modules_databases.size(); pos++) {
			db = modules_databases.get(pos);
			entries = db.newEntries();
			if (entries.isEmpty() == false) {
				result.put(db.module_name, entries);
			}
		}
		return result;
	}
	
	public static void saveAll() {
		for (int pos = 0; pos < modules_databases.size(); pos++) {
			try {
				modules_databases.get(pos).save();
			} catch (IOException e) {
				Loggers.Play.error("Can't save entries");
			}
		}
	}
	
}
