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
 * Imported and forked from https://gist.github.com/mingfang/3784a0a6e58c24dda687
 * 
*/
package hd3gtv.mydmam.web;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import controllers.AsyncJavascript;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Router;
import play.vfs.VirtualFile;

public class JSXTransformer {
	
	public static final String JSXTRANSFORMER_PATH = "/public/javascripts/lib/JSXTransformer-0.13.2.js";
	public static final String JSX_SRC = "/app/react";
	
	public static final JSXTransformer global;
	private static final Gson gson_simple;
	
	static {
		global = new JSXTransformer();
		gson_simple = new Gson();
	}
	
	private Context ctx;
	private Scriptable exports;
	private Scriptable topLevelScope;
	private Function transform;
	private boolean is_init = false;
	
	private JSXTransformer() {
		try {
			File jsxtransformer_file = VirtualFile.fromRelativePath(JSXTRANSFORMER_PATH).getRealFile().getAbsoluteFile();
			if (jsxtransformer_file.exists() == false) {
				throw new FileNotFoundException(JSXTRANSFORMER_PATH);
			}
			
			ctx = Context.enter();
			RequireBuilder builder = new RequireBuilder();
			
			List<URI> uris = new ArrayList<URI>();
			URI uri = jsxtransformer_file.getParentFile().getAbsoluteFile().toURI().resolve("");
			uri = new URI(uri + "/");
			uris.add(uri);
			
			ModuleSourceProvider url_module_script_provider = new UrlModuleSourceProvider(uris, null);
			
			ModuleScriptProvider sc_module_script_provider = new SoftCachingModuleScriptProvider(url_module_script_provider);
			
			builder.setModuleScriptProvider(sc_module_script_provider);
			
			topLevelScope = ctx.initStandardObjects();
			Require require = builder.createRequire(ctx, topLevelScope);
			
			exports = require.requireMain(ctx, jsxtransformer_file.getName());
			transform = (Function) exports.get("transform", topLevelScope);
			is_init = true;
		} catch (Exception e) {
			Loggers.Play.error("Can't load JSXTransformer", e);
		} finally {
			Context.exit();
		}
	}
	
	private String _transform(String jsx) throws Error {
		if (is_init == false) {
			throw new InstantiationError("JSXTransformer is not instantiated correctly");
		}
		try {
			Context.enter();
			try {
				NativeObject result = (NativeObject) transform.call(ctx, topLevelScope, exports, new String[] { jsx });
				return result.get("code").toString();
			} finally {
				Context.exit();
			}
		} catch (Error e) {
			if (Loggers.Play.isDebugEnabled()) {
				int jsx_len = 100;
				if (jsx.length() < jsx_len) {
					jsx_len = jsx.length();
				}
				Loggers.Play.error("JSX Transformer error: " + jsx.substring(jsx_len), e);
			} else {
				Loggers.Play.error("JSX Transformer error: " + e.getMessage());
			}
			throw e;
		}
	}
	
	private static String escapeAll(String text) {
		StringBuilder sb = new StringBuilder(text.length() + 1);
		char letter;
		char backshash = "\\".charAt(0);
		char quote = "\"".charAt(0);
		
		for (int pos = 0; pos < text.length(); pos++) {
			letter = text.charAt(pos);
			if (letter == backshash) {
				sb.append("\\\\");
			} else if (letter == quote) {
				sb.append("\\\"");
			} else {
				sb.append(letter);
			}
		}
		return sb.toString();
	}
	
	private String transform(String jsx, boolean catch_js_problem, String filename) throws InstantiationError {
		if (catch_js_problem) {
			try {
				return _transform(jsx);
			} catch (JavaScriptException e) {
				LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
				log.put("message", e.getMessage());
				log.put("source name", e.sourceName());
				log.put("line source", e.lineSource());
				log.put("line number", e.lineNumber());
				log.put("column number", e.columnNumber());
				log.put("script stack trace", e.getScriptStackTrace());
				int jsx_len = 100;
				if (jsx.length() < jsx_len) {
					jsx_len = jsx.length();
				}
				log.put("details", e.details());
				log.put("jsx", jsx.substring(0, jsx_len));
				log.put("message", e.getMessage());
				Loggers.Play.error("JSX Transformer JavaScriptException " + log);
				
				String v_file_content = jsx;
				v_file_content = v_file_content.replaceAll("/r", "");
				String[] lines = v_file_content.split("\n");
				String error_message = escapeAll(e.getMessage());
				
				String error_line = "";
				if (error_message.startsWith("Error: Parse Error: Line ")) {
					int colon = error_message.indexOf(":", "Error: Parse Error: Line ".length());
					int line_num = Integer.parseInt(error_message.substring("Error: Parse Error: Line ".length(), colon));
					error_message = "Line " + line_num + ": " + error_message.substring(colon + 2);
					if (line_num - 2 > -1) {
						error_line = error_line + (line_num - 1) + " > " + escapeAll(lines[line_num - 2]) + "\\n";
					}
					if ((line_num - 1 > 0) & (line_num - 1 < lines.length)) {
						error_line = error_line + (line_num) + " > " + escapeAll(lines[line_num - 1]) + "\\n";
					}
					if (line_num < lines.length) {
						error_line = error_line + (line_num + 1) + " > " + escapeAll(lines[line_num]);
					}
				}
				
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PrintWriter pw = new PrintWriter(baos);
				pw.println("// JSX ERROR");
				pw.println("new function(){");
				pw.println("	$(document).ready(function() {");
				pw.println("		var message = {}");
				pw.println("		message.from = \"" + filename + "\";");
				pw.println("		message.text = \"" + error_message + "\";");
				pw.println("		message.line = \"" + error_line + "\";");
				pw.println("		jsx_error_messages.push(message);");
				pw.println("	});");
				pw.println("}();");
				pw.println();
				pw.println("// SOURCE FILE");
				for (int pos = 0; pos < lines.length; pos++) {
					pw.print("// ");
					pw.print(pos + 1);
					pw.print(" ");
					pw.println(lines[pos]);
				}
				pw.close();
				return new String(baos.toByteArray());
			}
		} else {
			return _transform(jsx);
		}
	}
	
	public static class JSXItem {
		File realfile;
		String namespace;
		
		String getRessourceName() {
			return namespace;
		}
		
		JSXItem(File realfile, File jsx_root_dir) {
			this.realfile = realfile;
			namespace = realfile.getAbsolutePath();
			namespace = namespace.substring(jsx_root_dir.getAbsolutePath().length() + 1, namespace.length()).replaceAll(File.separator, ".");
		}
		
		public static String getRelativePathFromRessourceName(String ressourcename) throws FileNotFoundException {
			if (ressourcename.endsWith(".jsx") == false) {
				throw new FileNotFoundException(ressourcename + " is not a JSX file.");
			}
			StringBuilder sb = new StringBuilder();
			sb.append(JSXTransformer.JSX_SRC);
			sb.append("/");
			sb.append(ressourcename.substring(0, ressourcename.lastIndexOf(".")).replaceAll("\\.", "/"));
			sb.append(".jsx");
			return sb.toString();
		}
	}
	
	private static final String JSX_AHEADER_FILE_PREFIX = "// JSXTRANSFORMER_SETUP:";
	
	private static final Type type_AL_String = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	private static JSXItem createJavaScriptDeclareFile(File root_jsx_dir, String module_name, ArrayList<File> all_jsx_dirs) {
		String root_jsx_dir_path = root_jsx_dir.getAbsolutePath();
		File declare_file = new File(root_jsx_dir_path + File.separator + "_declarations_" + module_name + ".jsx");
		JSXItem result = new JSXItem(declare_file, root_jsx_dir);
		
		ArrayList<String> all_jsx_dirs_simple_path = new ArrayList<String>();
		String jsx_dir;
		for (int pos = 0; pos < all_jsx_dirs.size(); pos++) {
			jsx_dir = all_jsx_dirs.get(pos).getAbsolutePath().substring(root_jsx_dir_path.length());
			if (jsx_dir.equals("")) {
				continue;
			}
			all_jsx_dirs_simple_path.add(jsx_dir);
		}
		
		/**
		 * Check if the current file is valid.
		 */
		if (declare_file.exists()) {
			String first_line = "";
			BufferedReader read_header_file = null;
			try {
				read_header_file = new BufferedReader(new FileReader(declare_file));
				first_line = read_header_file.readLine();
			} catch (Exception e) {
				Loggers.Play.error("Can't check JSX file, declare_file: " + declare_file, e);
			} finally {
				try {
					read_header_file.close();
				} catch (IOException e) {
					Loggers.Play.error("Can't check JSX file, declare_file: " + declare_file, e);
				}
			}
			
			if (first_line.startsWith(JSX_AHEADER_FILE_PREFIX)) {
				ArrayList<String> items = gson_simple.fromJson(first_line.substring(JSX_AHEADER_FILE_PREFIX.length()), type_AL_String);
				if (items.equals(all_jsx_dirs_simple_path)) {
					return result;
				}
			}
		}
		
		Loggers.Play.info("Create/overwrite declare JSX file: " + declare_file.getName() + " for " + module_name);
		
		StringBuilder sb = new StringBuilder();
		sb.append(JSX_AHEADER_FILE_PREFIX);
		sb.append(gson_simple.toJson(all_jsx_dirs_simple_path));
		sb.append(MyDMAM.LINESEPARATOR);
		
		sb.append("// ");
		sb.append(MyDMAM.APP_COPYRIGHT);
		sb.append(MyDMAM.LINESEPARATOR);
		
		sb.append("// DO NOT EDIT THIS FILE");
		sb.append(MyDMAM.LINESEPARATOR);
		
		sb.append("// THIS FILE WAS GENERATED AUTOMATICALLY BY JSXTRANSFORMER");
		sb.append(MyDMAM.LINESEPARATOR);
		sb.append(MyDMAM.LINESEPARATOR);
		
		for (int pos = 0; pos < all_jsx_dirs_simple_path.size(); pos++) {
			jsx_dir = all_jsx_dirs_simple_path.get(pos).substring(1).replaceAll(File.separator, ".");
			sb.append("if(!window.");
			sb.append(jsx_dir);
			sb.append("){window.");
			sb.append(jsx_dir);
			sb.append(" = {};}");
			sb.append(MyDMAM.LINESEPARATOR);
		}
		
		IO.writeContent(sb.toString(), declare_file);
		return result;
	}
	
	private static class JSX_File_Filter implements IOFileFilter {
		
		ArrayList<JSXItem> all_jsx_files = new ArrayList<JSXTransformer.JSXItem>();
		
		File root_jsx_dir;
		
		void reset(File root_jsx_dir) {
			this.root_jsx_dir = root_jsx_dir;
			all_jsx_files.clear();
		}
		
		public boolean accept(File dir, String name) {
			return true;
		}
		
		public boolean accept(File file) {
			if (file.isHidden()) {
				return false;
			}
			if (file.isFile() & FilenameUtils.isExtension(file.getName(), "jsx") & file.getName().startsWith("_declarations_") == false) {
				synchronized (all_jsx_files) {
					all_jsx_files.add(new JSXItem(file, root_jsx_dir));
				}
			}
			
			return file.isDirectory();
		}
	}
	
	private static List<JSXItem> getAllJSXItems() {
		List<JSXItem> result = new ArrayList<JSXTransformer.JSXItem>();
		
		JSX_File_Filter jsx_ff = new JSX_File_Filter();
		
		ArrayList<File> all_jsx_dirs = new ArrayList<File>();
		File root_jsx_dir;
		
		List<VirtualFileModule> main_dirs = JsCompile.getAllfromRelativePath(JSX_SRC, true, true);
		for (int pos = 0; pos < main_dirs.size(); pos++) {
			root_jsx_dir = main_dirs.get(pos).getVfile().getRealFile();
			jsx_ff.reset(root_jsx_dir);
			
			all_jsx_dirs.clear();
			all_jsx_dirs.addAll(FileUtils.listFilesAndDirs(root_jsx_dir, jsx_ff, TrueFileFilter.INSTANCE));
			Collections.sort(all_jsx_dirs);
			
			result.add(createJavaScriptDeclareFile(root_jsx_dir, main_dirs.get(pos).getModule_name(), all_jsx_dirs));
			result.addAll(jsx_ff.all_jsx_files);
		}
		return result;
	}
	
	public static List<String> getJSXURLList() throws IOException {
		List<String> list = new ArrayList<String>();
		List<JSXItem> jsx_vfiles = getAllJSXItems();
		JSXItem jsx_file;
		HashMap<String, Object> args = new HashMap<String, Object>();
		for (int pos = 0; pos < jsx_vfiles.size(); pos++) {
			jsx_file = jsx_vfiles.get(pos);
			args.put("ressource_name", jsx_file.getRessourceName());
			list.add(Router.reverse(AsyncJavascript.class.getName() + "." + "dynamicCompileJSX", args).url);
		}
		
		Collections.sort(list);
		return list;
	}
	
	public static String getJSXContentFromURLList(File jsx_file, String ressource_name, boolean transfrom, boolean catch_js_problem) throws FileNotFoundException {
		String v_file_content = "";
		try {
			if (jsx_file.getName().equals(ressource_name)) {
				v_file_content = IO.readContentAsString(jsx_file);
			} else {
				StringBuilder sb = new StringBuilder();
				String package_name = ressource_name.substring(0, (ressource_name.length() - jsx_file.getName().length()) - 1);
				String current_package_name = jsx_file.getParentFile().getName();
				
				sb.append("(function(" + current_package_name + ") {\n");
				sb.append(IO.readContentAsString(jsx_file));
				sb.append("\n})(window." + package_name + ");\n");
				v_file_content = sb.toString();
			}
		} catch (Exception e) {
			throw new UnexpectedException(e);
		}
		
		if (transfrom == false) {
			return v_file_content;
		}
		return global.transform(v_file_content, catch_js_problem, ressource_name);
	}
	
	public static void transformAllJSX() {
		String js_dest_dir_path = VirtualFile.fromRelativePath(JsCompile.PUBLIC_JAVASCRIPT_DIRECTORY + "/" + JsCompile.SOURCE_DIRECTORY).getRealFile().getAbsolutePath();
		
		/**
		 * Remove old transformed jsx files from /public/js/src dir
		 */
		File[] old_transformed_jsxfiles = (new File(js_dest_dir_path)).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return (name.startsWith("ZZZZZZZZZZZZZZZ.react.") && name.endsWith(".jsx.js"));
			}
		});
		
		for (int pos = 0; pos < old_transformed_jsxfiles.length; pos++) {
			Loggers.Play.debug("Purge transformed jsx file temp: " + old_transformed_jsxfiles[pos]);
			try {
				FileUtils.forceDelete(old_transformed_jsxfiles[pos]);
			} catch (Exception e) {
				Loggers.Play.warn("Can't delete jsx file temp: " + old_transformed_jsxfiles[pos], e);
			}
		}
		
		if (JsCompile.COMPILE_JS == false) {
			return;
		}
		
		List<JSXItem> jsx_vfiles = getAllJSXItems();
		JSXItem jsx_file;
		File dest_js_file;
		String js_content;
		FileWriter fw = null;
		
		for (int pos = 0; pos < jsx_vfiles.size(); pos++) {
			jsx_file = jsx_vfiles.get(pos);
			try {
				js_content = "// DO NOT EDIT THIS FILE\n// THIS FILE WAS GENERATED AUTOMATICALLY BY JSXTRANSFORMER\n\n"
						+ getJSXContentFromURLList(jsx_file.realfile, jsx_file.getRessourceName(), true, false);
				dest_js_file = new File(js_dest_dir_path + "/" + "ZZZZZZZZZZZZZZZ.react." + jsx_file.getRessourceName() + ".js");
				
				if (Loggers.Play.isDebugEnabled()) {
					Loggers.Play.info("Transform JSX file " + jsx_file.realfile + ", ressource: " + jsx_file.getRessourceName() + ", dest_js_file: " + dest_js_file);
				}
				
				fw = new FileWriter(dest_js_file);
				fw.write(js_content);
				fw.close();
				fw = null;
			} catch (Exception e) {
				Loggers.Play.error("Can't transform JSX file:" + jsx_file.realfile, e);
				continue;
			} finally {
				if (fw != null) {
					try {
						fw.close();
					} catch (Exception e) {
						Loggers.Play.error("Can't close FileWriter", e);
					}
				}
			}
		}
		
	}
}