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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

import play.exceptions.UnexpectedException;
import play.libs.IO;
import play.mvc.Router;
import play.vfs.VirtualFile;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import controllers.AsyncJavascript;

public class JSXTransformer {
	
	public static final String JSXTRANSFORMER_PATH = "/public/javascripts/lib/JSXTransformer-0.13.2.js";
	public static final String JSX_SRC = "/app/react";
	public static final String JSX_CLASSDEF_NAME = "_package.jsx";
	
	public static final JSXTransformer global;
	private static final Gson gson_simple;
	private static final JsonParser gson_parser;
	
	static {
		global = new JSXTransformer();
		gson_simple = new Gson();
		gson_parser = new JsonParser();
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
			Log2.log.error("Can't load JSXTransformer", e);
		} finally {
			Context.exit();
		}
	}
	
	private String _transform(String jsx) throws Error {
		if (is_init == false) {
			throw new InstantiationError("JSXTransformer is not instantiated correctly");
		}
		Context.enter();
		try {
			NativeObject result = (NativeObject) transform.call(ctx, topLevelScope, exports, new String[] { jsx });
			return result.get("code").toString();
		} finally {
			Context.exit();
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
	
	private static JSXItem makeJSXAHeaderFile(File[] current_list_dirs, File header_file, File jsx_root_dir) {
		header_file.delete();
		
		String root_js_class = header_file.getParentFile().getName();
		if (root_js_class.equals(jsx_root_dir.getName())) {
			root_js_class = "window";
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(JSX_AHEADER_FILE_PREFIX);
		
		ArrayList<String> dirs = new ArrayList<String>();
		for (int pos = 0; pos < current_list_dirs.length; pos++) {
			dirs.add(current_list_dirs[pos].getName());
		}
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("dirs", dirs);
		map.put("root", root_js_class);
		sb.append(gson_simple.toJson(map));
		sb.append("\n");
		sb.append("// DO NOT EDIT THIS FILE\n// THIS FILE WAS GENERATED AUTOMATICALLY BY JSXTRANSFORMER\n\n");
		
		for (int pos = 0; pos < current_list_dirs.length; pos++) {
			sb.append("if (!" + root_js_class + "." + current_list_dirs[pos].getName() + "){" + root_js_class + "." + current_list_dirs[pos].getName() + " = {};}\n");
		}
		
		IO.writeContent(sb.toString(), header_file);
		JSXItem item = new JSXItem(header_file, jsx_root_dir);
		
		return item;
	}
	
	private static boolean checkJSXAHeaderFile(File[] current_list_dirs, File header_file, File jsx_root_dir) {
		String first_line = "";
		BufferedReader read_header_file = null;
		try {
			read_header_file = new BufferedReader(new FileReader(header_file));
			first_line = read_header_file.readLine();
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump();
			dump.add("header_file", header_file);
			Log2.log.error("Can't check JSX file", e, dump);
			return false;
		} finally {
			try {
				read_header_file.close();
			} catch (IOException e) {
				Log2Dump dump = new Log2Dump();
				dump.add("header_file", header_file);
				Log2.log.error("Can't check JSX file", e, dump);
				return false;
			}
		}
		
		if (first_line.startsWith(JSX_AHEADER_FILE_PREFIX) == false) {
			return false;
		}
		JsonObject map = gson_parser.parse(first_line.substring(JSX_AHEADER_FILE_PREFIX.length())).getAsJsonObject();
		
		String root_js_class = header_file.getParentFile().getName();
		if (root_js_class.equals(jsx_root_dir.getName())) {
			root_js_class = "window";
		}
		if (map.get("root").getAsString().equals(root_js_class) == false) {
			return false;
		}
		
		ArrayList<String> declared_dirs = new ArrayList<String>();
		JsonArray ja_declared_dirs = map.get("dirs").getAsJsonArray();
		for (int pos = 0; pos < ja_declared_dirs.size(); pos++) {
			declared_dirs.add(ja_declared_dirs.get(pos).getAsString());
		}
		
		ArrayList<String> current_dirs = new ArrayList<String>();
		for (int pos = 0; pos < current_list_dirs.length; pos++) {
			current_dirs.add(current_list_dirs[pos].getName());
		}
		
		return current_dirs.containsAll(declared_dirs) & declared_dirs.containsAll(current_dirs);
	}
	
	private static void recursiveList(List<JSXItem> list, File from, File jsx_root_dir) {
		File[] current_list_files = from.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getName().endsWith(".jsx") & (pathname.getName().equals(JSX_CLASSDEF_NAME) == false);
			}
		});
		File[] current_list_dirs = from.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		
		if (current_list_dirs.length > 0) {
			/**
			 * Create a header file for declaring subdirs as vars.
			 */
			Log2Dump dump = new Log2Dump();
			dump.add("react directory", from);
			
			File header_file = new File(from.getAbsolutePath() + File.separator + JSX_CLASSDEF_NAME);
			if (header_file.exists() == false) {
				Log2.log.debug("Create new JSX header file for new directory", dump);
				list.add(makeJSXAHeaderFile(current_list_dirs, header_file, jsx_root_dir));
			} else {
				if (checkJSXAHeaderFile(current_list_dirs, header_file, jsx_root_dir)) {
					list.add(new JSXItem(header_file, jsx_root_dir));
				} else {
					Log2.log.debug("Rewrite current JSX header file for renamed directory", dump);
					list.add(makeJSXAHeaderFile(current_list_dirs, header_file, jsx_root_dir));
				}
			}
		}
		
		for (int pos = 0; pos < current_list_files.length; pos++) {
			list.add(new JSXItem(current_list_files[pos], jsx_root_dir));
		}
		for (int pos = 0; pos < current_list_dirs.length; pos++) {
			recursiveList(list, current_list_dirs[pos], jsx_root_dir);
		}
	}
	
	private static List<JSXItem> getAllJSXItems() {
		List<JSXItem> result = new ArrayList<JSXTransformer.JSXItem>();
		List<VirtualFile> main_dirs = JsCompile.getAllfromRelativePath(JSX_SRC, true, true);
		for (int pos = 0; pos < main_dirs.size(); pos++) {
			recursiveList(result, main_dirs.get(pos).getRealFile(), main_dirs.get(pos).getRealFile());
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
		Log2Dump dump = new Log2Dump();
		for (int pos = 0; pos < old_transformed_jsxfiles.length; pos++) {
			dump.add("file", old_transformed_jsxfiles[pos]);
			dump.add("delete", old_transformed_jsxfiles[pos].delete());
		}
		if (old_transformed_jsxfiles.length > 0) {
			Log2.log.debug("Purge transformed jsx files temp", dump);
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
				
				dump = new Log2Dump();
				dump.add("jsx_file", jsx_file.realfile);
				dump.add("jsx_ressource", jsx_file.getRessourceName());
				dump.add("dest_js_file", dest_js_file);
				Log2.log.debug("Transform JSX File", dump);
				
				fw = new FileWriter(dest_js_file);
				fw.write(js_content);
				fw.close();
				fw = null;
			} catch (Exception e) {
				Log2.log.error("Can't transform JSX", e, new Log2Dump("file", jsx_file.realfile));
				continue;
			} finally {
				if (fw != null) {
					try {
						fw.close();
					} catch (Exception e) {
						Log2.log.error("Can't close FileWriter", e);
					}
				}
			}
		}
		
	}
}