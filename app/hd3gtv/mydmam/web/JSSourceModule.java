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
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.CopyMove;

public class JSSourceModule {
	
	private JSSourceDatabase database;
	private String module_name;
	private File module_path;
	
	private ArrayList<JSSourceDatabaseEntry> altered_source_files;
	private ArrayList<JSSourceDatabaseEntry> new_source_files;
	
	private static final String BASE_TRANSFORMED_DIRECTORY_JSX = "/public/javascripts/_transformed";
	private static final String BASE_REDUCED_DIRECTORY_JS = "/public/javascripts/_reduced";
	
	private File transformed_directory;
	private File reduced_directory;
	private File allfiles_concated_file;
	
	JSSourceModule(String module_name, File module_path) throws IOException {
		this.module_name = module_name;
		if (module_name == null) {
			throw new NullPointerException("\"module_name\" can't to be null");
		}
		this.module_path = module_path;
		if (module_path == null) {
			throw new NullPointerException("\"module_path\" can't to be null");
		}
		
		transformed_directory = new File(module_path + File.separator + BASE_TRANSFORMED_DIRECTORY_JSX);
		if (transformed_directory.exists()) {
			CopyMove.checkExistsCanRead(transformed_directory);
			CopyMove.checkIsDirectory(transformed_directory);
			CopyMove.checkIsWritable(transformed_directory);
		} else {
			FileUtils.forceMkdir(transformed_directory);
		}
		
		reduced_directory = new File(module_path + File.separator + BASE_REDUCED_DIRECTORY_JS);
		if (reduced_directory.exists()) {
			CopyMove.checkExistsCanRead(reduced_directory);
			CopyMove.checkIsDirectory(reduced_directory);
			CopyMove.checkIsWritable(reduced_directory);
		} else {
			FileUtils.forceMkdir(reduced_directory);
		}
		
		allfiles_concated_file = new File(reduced_directory.getPath() + File.separator + "reduced.js");
		database = JSSourceDatabase.create(this);
		altered_source_files = database.checkAndClean();
		new_source_files = database.newEntries();
	}
	
	String getModuleName() {
		return module_name;
	}
	
	File getModulePath() {
		return module_path;
	}
	
	void processSources() throws IOException {
		if (altered_source_files.isEmpty() & new_source_files.isEmpty()) {
			return;
		}
		
		if (allfiles_concated_file.exists()) {
			FileUtils.forceDelete(allfiles_concated_file);
		}
		
		/**
		 * Remove old transformed and reduced files.
		 */
		JSSourceDatabaseEntry entry;
		File transformed_file;
		File reduced_file;
		for (int pos = 0; pos < altered_source_files.size(); pos++) {
			entry = altered_source_files.get(pos);
			transformed_file = entry.computeTransformedFilepath(module_path, transformed_directory);
			FileUtils.deleteQuietly(transformed_file);
			
			reduced_file = entry.computeReducedFilepath(module_path, reduced_directory);
			FileUtils.deleteQuietly(reduced_file);
		}
		
		/**
		 * Group old file to re-process and new files
		 */
		ArrayList<JSSourceDatabaseEntry> must_process_source_files = new ArrayList<JSSourceDatabaseEntry>();
		must_process_source_files.addAll(new_source_files);
		must_process_source_files.addAll(altered_source_files);
		
		/**
		 * For each (re) process source file:
		 * - Transform JSX if needed
		 * - Reduce JS
		 */
		JSProcessor processor;
		File source_file;
		String source_scope;
		for (int pos = 0; pos < must_process_source_files.size(); pos++) {
			entry = must_process_source_files.get(pos);
			source_scope = entry.computeJSScope();
			source_file = entry.getRealFile(module_path);
			
			processor = new JSProcessor(source_file);
			
			if (FilenameUtils.isExtension(source_file.getPath(), "jsx")) {
				try {
					processor.transformJSX();
				} catch (Exception e) {
					processor.wrapTransformationError(e);
				}
				transformed_file = entry.computeTransformedFilepath(module_path, transformed_directory);
				processor.writeTo(transformed_file);
			}
			
			processor.wrapScopeDeclaration(source_scope);
			
			try {
				processor.reduceJS();
			} catch (Exception e) {
				Loggers.Play.error("Can't reduce JS source: " + entry, e);
				continue;
			}
			reduced_file = entry.computeReducedFilepath(module_path, reduced_directory);
			processor.writeTo(reduced_file);
			
		}
		
		/**
		 * Create JS header
		 */
		
		// TODO set *all* to allfiles_concated_file, after sort it by file name
	}
	
	// TODO Controler Side
	// TODO View side (link)
	
	/*private static File getDeclarationFile(File transformed_jsx_directory, String module_name) {
	return new File(transformed_jsx_directory + File.separator + "_declarations_" + module_name + ".jsx");
	}*/
	
}
