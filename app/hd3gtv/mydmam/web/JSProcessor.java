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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.commonjs.module.ModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.ModuleSourceProvider;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;

import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import hd3gtv.mydmam.Loggers;
import hd3gtv.tools.CopyMove;
import play.vfs.VirtualFile;
import yuiforkorgmozillajavascript.ErrorReporter;

/**
 * Autonomus functions for JS/JSX processing.
 */
public class JSProcessor {
	
	@Deprecated
	private static RhinoCompilerErrorReporter rhino_compiler_error_reporter;
	
	@Deprecated
	public static final String JSXTRANSFORMER_PATH = "/public/javascripts/lib/JSXTransformer-0.13.3.js";
	private static Context ctx;
	private static Scriptable exports;
	private static Scriptable topLevelScope;
	private static Function transform;
	
	static {
		rhino_compiler_error_reporter = new RhinoCompilerErrorReporter();
		
		try {
			Loggers.Play_JSSource.debug("Init JSProcessor");
			
			File jsxtransformer_file = VirtualFile.fromRelativePath(JSXTRANSFORMER_PATH).getRealFile().getAbsoluteFile();
			if (jsxtransformer_file.exists() == false) {
				throw new FileNotFoundException(jsxtransformer_file.getPath());
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
		} catch (Exception e) {
			Loggers.Play_JSSource.error("Can't init jsxtransformer lib", e);
		}
	}
	
	private static class RhinoCompilerErrorReporter implements ErrorReporter {
		
		public void error(String arg0, String arg1, int arg2, String arg3, int arg4) {
			Loggers.Play_JSSource.error("Rhino error during javascript parsing, arg0: " + arg0 + ", arg1: " + arg1 + ", arg2: " + arg2 + ", arg3: " + arg3 + ", arg4: " + arg4);
		}
		
		public yuiforkorgmozillajavascript.EvaluatorException runtimeError(String arg0, String arg1, int arg2, String arg3, int arg4) {
			Loggers.Play_JSSource.error("Rhino runtime error during javascript parsing, arg0: " + arg0 + ", arg1: " + arg1 + ", arg2: " + arg2 + ", arg3: " + arg3 + ", arg4: " + arg4);
			return null;
		}
		
		public void warning(String arg0, String arg1, int arg2, String arg3, int arg4) {
			if (Loggers.Play_JSSource.isTraceEnabled()) {
				Loggers.Play_JSSource.trace("Rhino warn during javascript parsing, arg0: " + arg0 + ", arg1: " + arg1 + ", arg2: " + arg2 + ", arg3: " + arg3 + ", arg4: " + arg4);
			}
		}
	}
	
	private String input;
	private String output;
	private File filename;
	private String module_name;
	private String module_path;
	
	public JSProcessor(File filename, String module_name, String module_path) throws NullPointerException, IOException {
		this.module_name = module_name;
		if (module_name == null) {
			throw new NullPointerException("\"module_name\" can't to be null");
		}
		this.module_path = module_path;
		if (module_path == null) {
			throw new NullPointerException("\"module_path\" can't to be null");
		}
		this.filename = filename;
		if (filename == null) {
			throw new NullPointerException("\"filename\" can't to be null");
		}
		Loggers.Play_JSSource.debug("Create JSProcessor for " + filename + " (module: " + module_name + " in " + module_path + ")");
		CopyMove.checkExistsCanRead(filename);
		input = FileUtils.readFileToString(filename);
		output = input;
	}
	
	public void reduceJS() throws Exception {
		Loggers.Play_JSSource.debug("Reduce JS: " + filename.getPath() + " (module: " + module_name + " in " + module_path + ")");
		JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(input), rhino_compiler_error_reporter);
		StringWriter sw = new StringWriter();
		compressor.compress(sw, 80, true, false, true, false);
		output = sw.toString();
		input = output;
	}
	
	/**
	 * Not thread safe !
	 */
	public void transformJSX() throws Exception {
		Loggers.Play_JSSource.debug("Transform JSX: " + filename.getPath() + " (module: " + module_name + " in " + module_path + ")");
		Context.enter();
		try {
			NativeObject result = (NativeObject) transform.call(ctx, topLevelScope, exports, new String[] { input });
			output = result.get("code").toString();
			input = output;
		} finally {
			Context.exit();
		}
	}
	
	public void writeTo(File filename) throws IOException {
		Loggers.Play_JSSource.debug("Write " + this.filename + " processed to " + filename + " (module: " + module_name + " in " + module_path + ")");
		FileUtils.write(filename, output, false);
	}
	
	public void wrapScopeDeclaration(String source_scope, String hash) {
		Loggers.Play_JSSource.debug("Wrap scope declaration, for " + filename + ", scope: " + source_scope + " (module: " + module_name + " in " + module_path + ")");
		StringBuilder sb = new StringBuilder(input.length() + 100);
		sb.append("/** This file is automatically generated! Do not edit. */ (function(");
		if (source_scope.lastIndexOf(".") > -1) {
			sb.append(source_scope.substring(source_scope.lastIndexOf(".") + 1, source_scope.length()));
		} else {
			sb.append(source_scope);
		}
		sb.append(") { ");
		sb.append(input);
		sb.append("\n})(window.");
		sb.append(source_scope);
		sb.append(");\n");
		sb.append("// Generated by ");
		sb.append(getClass().getName());
		sb.append(" for the module ");
		sb.append(module_name);
		sb.append("\n");
		sb.append("// Source hash: ");
		sb.append(hash);
		sb.append("\n");
		
		output = sb.toString();
		input = output;
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
	
	public void wrapTransformationError(Exception e) {
		Loggers.Play_JSSource.debug("Wrap transformation error, for " + filename + " (module: " + module_name + " in " + module_path + ")", e);
		
		input = input.replaceAll("/r", "");
		String[] lines = input.split("\n");
		String error_message = escapeAll(e.getMessage());
		
		String error_line = "";
		if (error_message.startsWith("Error: Parse Error: Line ")) {
			int colon = error_message.indexOf(":", "Error: Parse Error: Line ".length());
			int line_num = Integer.parseInt(error_message.substring("Error: Parse Error: Line ".length(), colon));
			error_message = "Line " + line_num + ": " + error_message.substring(colon + 2);
			if (line_num - 5 > -1) {
				error_line = error_line + (line_num - 4) + " >   " + escapeAll(lines[line_num - 5]) + "\\n";
			}
			if (line_num - 4 > -1) {
				error_line = error_line + (line_num - 3) + " >   " + escapeAll(lines[line_num - 4]) + "\\n";
			}
			if (line_num - 3 > -1) {
				error_line = error_line + (line_num - 2) + " >   " + escapeAll(lines[line_num - 3]) + "\\n";
			}
			if (line_num - 2 > -1) {
				error_line = error_line + (line_num - 1) + " >   " + escapeAll(lines[line_num - 2]) + "\\n";
			}
			if ((line_num - 1 > 0) & (line_num - 1 < lines.length)) {
				error_line = error_line + (line_num) + " >>> " + escapeAll(lines[line_num - 1]) + "\\n";
			}
			if (line_num < lines.length) {
				error_line = error_line + (line_num + 1) + " >   " + escapeAll(lines[line_num]) + "\\n";
			}
			if (line_num + 1 < lines.length) {
				error_line = error_line + (line_num + 2) + " >   " + escapeAll(lines[line_num + 1]) + "\\n";
			}
			if (line_num + 2 < lines.length) {
				error_line = error_line + (line_num + 3) + " >   " + escapeAll(lines[line_num + 2]) + "\\n";
			}
			if (line_num + 3 < lines.length) {
				error_line = error_line + (line_num + 4) + " >   " + escapeAll(lines[line_num + 3]);
			}
		} else {
			Loggers.Play_JSSource.error("Unknow transformation error", e);
		}
		
		if (error_message.indexOf("(file:") > -1) {
			error_message = error_message.substring(0, error_message.lastIndexOf("(file:")).trim();
		}
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		pw.println("// JSX ERROR");
		pw.println("new function(){");
		pw.println("	$(document).ready(function() {");
		pw.println("		var message = {}");
		pw.println("		message.from = \"" + module_name + " module: " + filename.getAbsolutePath().substring(module_path.length()) + "\";");
		pw.println("		message.text = \"" + error_message + "\";");
		pw.println("		message.line = \"" + error_line + "\";");
		pw.println("		jsx_error_messages.push(message);");
		pw.println("	});");
		pw.println("}();");
		pw.close();
		output = new String(baos.toByteArray());
		input = output;
	}
	
}
