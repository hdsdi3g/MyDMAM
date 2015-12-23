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

import java.util.ArrayList;
import java.util.Map;

import play.Play;
import play.vfs.VirtualFile;

public class JSSourceManager {
	
	private JSSourceManager() {
	}
	
	private static final ArrayList<JSSourceModule> js_modules;
	
	static {
		js_modules = new ArrayList<JSSourceModule>(1);
	}
	
	public static void init() throws Exception {
		for (VirtualFile vfile : Play.roots) {
			/**
			 * 1st pass : get only main, the first.
			 */
			js_modules.add(new JSSourceModule("internal", vfile.getRealFile().getAbsoluteFile()));
			break;
		}
		for (Map.Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
			/**
			 * 2nd pass : get the modules paths and names.
			 */
			if (entry.getKey().startsWith("_")) {
				continue;
			}
			js_modules.add(new JSSourceModule(entry.getKey(), entry.getValue().getRealFile().getAbsoluteFile()));
		}
		
		for (int pos = 0; pos < js_modules.size(); pos++) {
			js_modules.get(pos).processSources();
		}
		
		/*Loggers.Play.info("Altered JS files: " + JSSourceDatabase.getAlteredFiles());
		Loggers.Play.info("New JS files: " + JSSourceDatabase.getNewFiles());
		JSSourceDatabase.saveAll();*/
		
		/*
		 * 
		 * 	public ArrayList<JSSourceDatabaseEntry> getAlteredFiles() {
		ArrayList<JSSourceDatabaseEntry> result = new ArrayList<JSSourceDatabaseEntry>();
		JSSourceDatabase db;
		ArrayList<JSSourceDatabaseEntry> entries;
		entries = checkAndClean();
		if (entries.isEmpty() == false) {
			result.put(module_name, entries);
		}
		return result;
		}
		
		public static ArrayList<JSSourceDatabaseEntry> getNewFiles() {
		ArrayList<JSSourceDatabaseEntry> result = new ArrayList<JSSourceDatabaseEntry>();
		JSSourceDatabase db;
		ArrayList<JSSourceDatabaseEntry> entries;
		db = modules_databases.get(pos);
		entries = db.newEntries();
		if (entries.isEmpty() == false) {
			result.put(db.module_name, entries);
		}
		return result;
		}
		 * 
		 * */
		
		/*for (int pos = 0; pos < js_databases.size(); pos++) {
			try {
				js_databases.get(pos).save();
			} catch (IOException e) {
				Loggers.Play.error("Can't save entries");
			}
		}*/
		
	}
	
	// TODO Controler Side
	
	// TODO View side (link)
	
}
