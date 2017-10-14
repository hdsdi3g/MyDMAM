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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import controllers.AsyncJavascript;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.web.NodeJSBabel.Operation;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.ThreadPoolExecutorFactory;
import play.exceptions.NoRouteFoundException;
import play.libs.IO;
import play.mvc.Router;
import play.vfs.VirtualFile;

public class JSSourceModule {
	
	private NodeJSBabel node_js_babel;
	private JSSourceDatabase js_source_database;
	private String module_name;
	private File module_path;
	
	private ThreadPoolExecutorFactory executor;
	
	private ArrayList<JSSourceDatabaseEntry> altered_source_files;
	private ArrayList<JSSourceDatabaseEntry> new_source_files;
	
	static final String BASE_TRANSFORMED_DIRECTORY_JS = "/public/javascripts/_transformed";
	static final String BASE_REDUCED_DIRECTORY_JS = "/public/javascripts/_reduced";
	
	private File transformed_directory;
	private File reduced_directory;
	private File allfiles_concated_file;
	private File reduced_declaration_file;
	
	JSSourceModule(String module_name, File module_path, NodeJSBabel node_js_babel) throws IOException {
		this.module_name = module_name;
		if (module_name == null) {
			throw new NullPointerException("\"module_name\" can't to be null");
		}
		this.module_path = module_path;
		if (module_path == null) {
			throw new NullPointerException("\"module_path\" can't to be null");
		}
		this.node_js_babel = node_js_babel;
		if (node_js_babel == null) {
			throw new NullPointerException("\"node_js_babel\" can't to be null");
		}
		
		transformed_directory = new File(module_path + File.separator + BASE_TRANSFORMED_DIRECTORY_JS);
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
		
		allfiles_concated_file = new File(reduced_directory.getPath() + File.separator + "_" + module_name + "_" + "concated.js.gz");
		reduced_declaration_file = new File(reduced_directory + File.separator + "_" + module_name + "_" + "declarations.js");
		
		executor = new ThreadPoolExecutorFactory(getClass().getSimpleName(), Thread.NORM_PRIORITY);
		
		Loggers.Play_JSSource.debug("Init source module, module_name: " + module_name + ", module_path: " + module_path + ", transformed_directory: " + transformed_directory + ", reduced_directory: " + reduced_directory + ", allfiles_concated_file: " + allfiles_concated_file + ", reduced_declaration_file: " + reduced_declaration_file);
	}
	
	private JSSourceDatabase getDatabase() throws IOException {
		if (js_source_database == null) {
			js_source_database = JSSourceDatabase.create(this);
		}
		return js_source_database;
	}
	
	/**
	 * This will not functionnal after this.
	 */
	void purgeDatabase() throws IOException {
		Loggers.Play_JSSource.info("Purge database for " + module_name);
		
		FileUtils.forceDelete(getDatabase().getDbfile());
		js_source_database = null;
	}
	
	String getModuleName() {
		return module_name;
	}
	
	File getModulePath() {
		return module_path;
	}
	
	void processSources(PlayBootstrap play_bootstrap) throws IOException {
		Loggers.Play_JSSource.debug("Process source, module_name: " + module_name);
		
		altered_source_files = getDatabase().checkAndClean();
		new_source_files = getDatabase().newEntries();
		
		if (altered_source_files.isEmpty() & new_source_files.isEmpty() & allfiles_concated_file.exists()) {
			return;
		}
		
		if (Loggers.Play_JSSource.isTraceEnabled()) {
			Loggers.Play_JSSource.trace("Process source, altered_source_files: " + altered_source_files + ", new_source_files: " + new_source_files);
		}
		
		if (allfiles_concated_file.exists()) {
			Loggers.Play_JSSource.debug("allfiles_concated_file exists, remove it, module_name: " + module_name + ", allfiles_concated_file: " + allfiles_concated_file);
			FileUtils.forceDelete(allfiles_concated_file);
		}
		
		/**
		 * Remove old transformed and reduced files.
		 */
		if (altered_source_files.isEmpty() == false & Loggers.Play_JSSource.isDebugEnabled()) {
			if (Loggers.Play_JSSource.isTraceEnabled()) {
				Loggers.Play_JSSource.trace("Remove old transformed and reduced files: " + altered_source_files + ", module_name: " + module_name);
			} else {
				Loggers.Play_JSSource.debug("Remove old transformed and reduced files (" + altered_source_files.size() + " sources), module_name: " + module_name);
			}
		}
		
		JSSourceDatabaseEntry entry;
		File transformed_file;
		File reduced_file;
		for (int pos = altered_source_files.size() - 1; pos > -1; pos--) {
			entry = altered_source_files.get(pos);
			if (entry.getRealFile(module_path).exists() == false) {
				altered_source_files.remove(pos);
			}
			transformed_file = entry.computeTransformedFilepath(module_path, transformed_directory);
			FileUtils.deleteQuietly(transformed_file);
			
			reduced_file = entry.computeReducedFilepath(module_path, reduced_directory);
			FileUtils.deleteQuietly(reduced_file);
			
			Loggers.Play_JSSource.trace("Delete quietly Transformed " + transformed_file.getAbsolutePath() + " and reduced " + reduced_file + " files for " + entry + ", module_name: " + module_name);
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
		if (must_process_source_files.isEmpty() == false) {
			int file_count = must_process_source_files.size();
			
			if (file_count == 1) {
				File file = must_process_source_files.get(0).getRealFile(module_path);
				Loggers.Play_JSSource.info("Start Babel processing for " + file.getName() + " (in " + file.getParent() + ")");
			} else if (file_count < 10) {
				String f_list = must_process_source_files.stream().map(s_f -> {
					return FilenameUtils.getName(s_f.getRelativePath());
				}).collect(Collectors.joining(", "));
				Loggers.Play_JSSource.info("Start Babel processing for " + f_list + " (" + file_count + " JS files), in module " + module_name);
			} else {
				Loggers.Play_JSSource.info("Start Babel processing for " + file_count + " JS files, in module " + module_name);
			}
			
			LinkedHashMap<JSSourceDatabaseEntry, CompletableFuture<Void>> cf_process_map = new LinkedHashMap<>(must_process_source_files.size() + 1);
			
			must_process_source_files.forEach(jsentry -> {
				cf_process_map.put(jsentry, CompletableFuture.runAsync(() -> {
					try {
						long start_time = System.currentTimeMillis();
						
						String source_scope = jsentry.computeJSScope();
						File source_file = jsentry.getRealFile(module_path);
						
						Loggers.Play_JSSource.trace("Prepare processing for " + jsentry + ", with source_scope: " + source_scope + ", source_file: " + source_file);
						
						JSProcessor processor = new JSProcessor(source_file, module_name, module_path.getAbsolutePath(), node_js_babel);
						
						if (FilenameUtils.isExtension(source_file.getPath(), "jsx")) {
							try {
								/**
								 * Process file JSX -> vanilla JS
								 */
								Loggers.Play_JSSource.trace("Transform JSX processing for " + jsentry);
								processor.transformJSX();
							} catch (BabelException e) {
								processor.wrapTransformationError(e);
							}
						}
						
						if (source_scope != null) {
							/**
							 * Add JS wrapper for place source file in a scope
							 */
							processor.wrapScopeDeclaration(source_scope, jsentry.getHash());
						}
						
						/**
						 * Write current processed file. If this file is not processed, it will be a simple copy here.
						 */
						processor.writeTo(jsentry.computeTransformedFilepath(module_path, transformed_directory), Operation.TRANSFORM);
						
						try {
							/**
							 * Reduce the JS to a production standard.
							 */
							Loggers.Play_JSSource.trace("Reduce JS processing for " + jsentry);
							processor.reduceJS();
						} catch (BabelException e) {
							processor.wrapTransformationError(e);
						}
						
						processor.writeTo(jsentry.computeReducedFilepath(module_path, reduced_directory), Operation.REDUCE);
						
						if (play_bootstrap.getJSRessourceProcessTimeLog() != null) {
							play_bootstrap.getJSRessourceProcessTimeLog().addEntry(System.currentTimeMillis() - start_time, source_scope + "/" + source_file.getName());
						}
						
						/*return*/ jsentry.getRealFile(module_path).getPath();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}, executor));
			});
			
			AtomicInteger fail_count = new AtomicInteger(0);
			
			cf_process_map.forEach((jsentry, cf) -> {
				try {
					cf.get(3, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					Loggers.Play_JSSource.error("Can't process with babel " + jsentry.getRealFile(module_path).getAbsolutePath(), e);
					fail_count.incrementAndGet();
				}
			});
			
			if (fail_count.get() > 0) {
				throw new IOException("Babel processing exception for " + fail_count + " problematic file(s)");
			}
		}
		
		/**
		 * Get all source items
		 */
		ArrayList<String> source_scopes = new ArrayList<String>();
		ArrayList<JSSourceDatabaseEntry> all_entries = getDatabase().getSortedEntries();
		
		String scope;
		StringBuilder parent_scope;
		for (int pos_ae = 0; pos_ae < all_entries.size(); pos_ae++) {
			entry = all_entries.get(pos_ae);
			scope = entry.computeJSScope();
			if (scope == null) {
				continue;
			}
			if (source_scopes.contains(scope)) {
				continue;
			}
			if (scope.indexOf(".") == -1) {
				/**
				 * No parent scope to check.
				 */
				source_scopes.add(scope);
				continue;
			}
			
			/**
			 * Need to check all parent scope: if scope is "aaa.bbbb.cc.dd" -> check & add "aaa", "aaa.bbbb", "aaa.bbbb.cc" and "aaa.bbbb.cc.dd"
			 */
			String[] parents_scope = scope.split("\\.");
			parent_scope = new StringBuilder();
			for (int pos_ps = 0; pos_ps < parents_scope.length; pos_ps++) {
				if (parent_scope.length() > 0) {
					parent_scope.append(".");
				}
				parent_scope.append(parents_scope[pos_ps]);
				if (source_scopes.contains(parent_scope.toString()) == false) {
					source_scopes.add(parent_scope.toString());
				}
			}
		}
		
		/**
		 * Create JS header for all scopes.
		 */
		if (source_scopes.isEmpty() == false) {
			if (Loggers.Play_JSSource.isDebugEnabled()) {
				Loggers.Play_JSSource.debug("Create JS header for all scopes, module_name: " + module_name + ", source_scopes: " + source_scopes);
			}
			File declaration_file = new File(transformed_directory + File.separator + "_" + module_name + "_" + "declarations.js");
			createDeclarationFile(declaration_file, source_scopes);
			JSProcessor processor = new JSProcessor(declaration_file, module_name, module_path.getAbsolutePath(), node_js_babel);
			try {
				processor.reduceJS();
				processor.writeTo(reduced_declaration_file, Operation.REDUCE);
			} catch (Exception e) {
				Loggers.Play_JSSource.error("Can't reduce declaration file source: " + declaration_file + ", module_name: " + module_name, e);
				return;
			}
		}
		
		/**
		 * Concate all reduced files, in async mode
		 */
		executor.execute(() -> {
			Loggers.Play_JSSource.info("Concate all reduced files to a GZip file, allfiles_concated_file: " + allfiles_concated_file + ", module_name: " + module_name);
			
			FileOutputStream concated_out_stream_gzipped = null;
			try {
				concated_out_stream_gzipped = new FileOutputStream(allfiles_concated_file);
				GZIPOutputStream gz_concated_out_stream_gzipped = new GZIPOutputStream(concated_out_stream_gzipped, 0xFFFF);
				
				if (reduced_declaration_file.exists()) {
					FileUtils.copyFile(reduced_declaration_file, gz_concated_out_stream_gzipped);
				}
				
				all_entries.stream().map(thisentry -> {
					return thisentry.computeReducedFilepath(module_path, reduced_directory);
				}).filter(thisreduced_file -> {
					try {
						CopyMove.checkExistsCanRead(thisreduced_file);
						return true;
					} catch (IOException e) {
						Loggers.Play_JSSource.error("Can't found reduced file: " + thisreduced_file, e);
					}
					return false;
				}).forEach(thisreduced_file -> {
					try {
						FileUtils.copyFile(thisreduced_file, gz_concated_out_stream_gzipped);
					} catch (IOException e) {
						Loggers.Play_JSSource.error("Can't add file to gzip archive: " + thisreduced_file, e);
					}
				});
				
				gz_concated_out_stream_gzipped.finish();
				concated_out_stream_gzipped.flush();
				concated_out_stream_gzipped.close();
			} catch (Exception e) {
				if (concated_out_stream_gzipped != null) {
					IOUtils.closeQuietly(concated_out_stream_gzipped);
				}
				Loggers.Play_JSSource.error("Can't make concated file: " + allfiles_concated_file + ", module_name: " + module_name, e);
			}
			
			/**
			 * After that, save the internal database.
			 */
			executor.execute(() -> {
				try {
					getDatabase().save();
				} catch (IOException e) {
					Loggers.Play_JSSource.error("Can't save the internal JS source database", e);
				}
			});
		});
	}
	
	private void createDeclarationFile(File declare_file, ArrayList<String> source_scopes) {
		StringBuilder sb = new StringBuilder();
		sb.append("// MYDMAM JS DECLARATION FILE\n");
		sb.append("// ");
		sb.append(MyDMAM.APP_COPYRIGHT);
		sb.append("\n");
		sb.append("// Do not edit this file");
		sb.append("\n");
		sb.append("// This file was generated automatically by ");
		sb.append(getClass().getName());
		sb.append(" for the module ");
		sb.append(module_name);
		sb.append("\n\n");
		
		for (int pos = 0; pos < source_scopes.size(); pos++) {
			sb.append("if(!window.");
			sb.append(source_scopes.get(pos));
			sb.append("){window.");
			sb.append(source_scopes.get(pos));
			sb.append(" = {};}");
			sb.append("\n");
		}
		
		if (Loggers.Play_JSSource.isDebugEnabled()) {
			Loggers.Play_JSSource.debug("Create declaration file: " + declare_file + ", source_scopes: " + source_scopes + ", module_name: " + module_name);
		}
		IO.writeContent(sb.toString(), declare_file);
	}
	
	public String getConcatedFileRelativeURL() throws IOException {
		CopyMove.checkExistsCanRead(allfiles_concated_file);
		
		HashMap<String, Object> args = new HashMap<String, Object>(2);
		args.put("name", allfiles_concated_file.getName());
		args.put("suffix_date", allfiles_concated_file.lastModified() / 1000);
		return Router.reverse(AsyncJavascript.class.getName() + "." + "JavascriptRessource", args).url;
	}
	
	ArrayList<String> getTransformedFilesRelativeURLs() throws IOException {
		ArrayList<JSSourceDatabaseEntry> all_entries = getDatabase().getSortedEntries();
		ArrayList<String> results = new ArrayList<String>(all_entries.size() + 1);
		String url;
		
		if (reduced_declaration_file.exists()) {
			try {
				url = Router.reverse(VirtualFile.open(reduced_declaration_file));
				results.add(url);
			} catch (NoRouteFoundException e) {
				throw new IOException("Play can't resolve route for " + reduced_declaration_file.getPath());
			}
		}
		
		File transformed_file;
		for (int pos = 0; pos < all_entries.size(); pos++) {
			transformed_file = all_entries.get(pos).computeTransformedFilepath(module_path, transformed_directory);
			if (transformed_file.exists() == false) {
				Loggers.Play_JSSource.warn("A transformed file don't exists: " + transformed_file);
				continue;
			}
			url = Router.reverse(VirtualFile.open(transformed_file));
			results.add(url);
		}
		return results;
	}
	
}
