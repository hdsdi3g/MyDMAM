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
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.CopyMove;
import play.Play;
import play.vfs.VirtualFile;

public class JSSourceManager {
	
	private JSSourceManager() {
	}
	
	private static final ArrayList<JSSourceModule> js_modules;
	private static final ArrayList<String> list_urls;
	private static boolean js_dev_mode;
	
	static {
		js_modules = new ArrayList<JSSourceModule>(1);
		list_urls = new ArrayList<String>(1);
		js_dev_mode = Configuration.global.getValueBoolean("play", "js_dev_mode");
	}
	
	public static void init() throws Exception {
		if (isJsDevMode()) {
			Loggers.Play_JSSource.info("JS Source manager is in dev mode.");
			// TODO init Babel
		}
		
		js_modules.clear();
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
		
		refreshAllSources();
	}
	
	public static void refreshAllSources() throws Exception {
		synchronized (list_urls) {
			Loggers.Play_JSSource.debug("Refresh for all sources from all modules.");
			
			list_urls.clear();
			for (int pos = 0; pos < js_modules.size(); pos++) {
				js_modules.get(pos).processSources();
				if (isJsDevMode()) {
					list_urls.addAll(js_modules.get(pos).getTransformedFilesRelativeURLs());
				} else {
					list_urls.add(js_modules.get(pos).getConcatedFileRelativeURL());
				}
			}
		}
	}
	
	public static File getPhysicalFileFromRessourceName(String ressource_name) {
		if (Loggers.Play_JSSource.isTraceEnabled()) {
			Loggers.Play_JSSource.trace("Get physical file: " + ressource_name);
		}
		
		String base_dir = JSSourceModule.BASE_REDUCED_DIRECTORY_JS;
		if (isJsDevMode()) {
			base_dir = JSSourceModule.BASE_TRANSFORMED_DIRECTORY_JS;
		}
		
		VirtualFile ressource_file = VirtualFile.search(Play.roots, base_dir + File.separator + FilenameUtils.getName(ressource_name));
		if (ressource_file == null) {
			return null;
		}
		File result = ressource_file.getRealFile();
		try {
			CopyMove.checkExistsCanRead(result);
			if (result.isFile() == false) {
				throw new FileNotFoundException("File \"" + result + "\" exists, but is not a file.");
			}
			
		} catch (Exception e) {
			Loggers.Play_JSSource.warn("Invalid JS ressource: " + ressource_name, e);
			return null;
		}
		return result;
	}
	
	public static ArrayList<String> getURLs() {
		if (isJsDevMode()) {
			try {
				refreshAllSources();
			} catch (Exception e) {
				Loggers.Play_JSSource.warn("Can't refresh all JS source in dev mode", e);
			}
		}
		return list_urls;
	}
	
	public static boolean isJsDevMode() {
		return js_dev_mode;
	}
	
	public static void switchSetJsDevMode() throws Exception {
		js_dev_mode = !js_dev_mode;
	}
	
	/**
	 * Like a Play reboot with json's file db delete.
	 */
	public static void purgeAll() throws Exception {
		for (int pos = 0; pos < js_modules.size(); pos++) {
			js_modules.get(pos).purgeDatabase();
		}
		init();
	}
	
}
