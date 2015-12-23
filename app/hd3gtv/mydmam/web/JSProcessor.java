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
	
	private static RhinoCompilerErrorReporter rhino_compiler_error_reporter;
	
	public static final String JSXTRANSFORMER_PATH = "/public/javascripts/lib/JSXTransformer-0.13.3.js";
	private static Context ctx;
	private static Scriptable exports;
	private static Scriptable topLevelScope;
	private static Function transform;
	
	static {
		rhino_compiler_error_reporter = new RhinoCompilerErrorReporter();
		
		try {
			
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
			Loggers.Play.debug("JSXTransformer URI: " + uri);
			ModuleSourceProvider url_module_script_provider = new UrlModuleSourceProvider(uris, null);
			ModuleScriptProvider sc_module_script_provider = new SoftCachingModuleScriptProvider(url_module_script_provider);
			builder.setModuleScriptProvider(sc_module_script_provider);
			topLevelScope = ctx.initStandardObjects();
			Require require = builder.createRequire(ctx, topLevelScope);
			exports = require.requireMain(ctx, jsxtransformer_file.getName());
			transform = (Function) exports.get("transform", topLevelScope);
		} catch (Exception e) {
			Loggers.Play.error("Can't init jsxtransformer lib", e);
		}
	}
	
	private static class RhinoCompilerErrorReporter implements ErrorReporter {
		
		public void error(String arg0, String arg1, int arg2, String arg3, int arg4) {
			Loggers.Play.error("Rhino error during javascript parsing, arg0: " + arg0 + ", arg1: " + arg1 + ", arg2: " + arg2 + ", arg3: " + arg3 + ", arg4: " + arg4);
		}
		
		public yuiforkorgmozillajavascript.EvaluatorException runtimeError(String arg0, String arg1, int arg2, String arg3, int arg4) {
			Loggers.Play.error("Rhino error during javascript parsing, arg0: " + arg0 + ", arg1: " + arg1 + ", arg2: " + arg2 + ", arg3: " + arg3 + ", arg4: " + arg4);
			return null;
		}
		
		public void warning(String arg0, String arg1, int arg2, String arg3, int arg4) {
			if (Loggers.Play.isTraceEnabled() == false) {
				return;
			}
			Loggers.Play.trace("Rhino warn during javascript parsing, arg0: " + arg0 + ", arg1: " + arg1 + ", arg2: " + arg2 + ", arg3: " + arg3 + ", arg4: " + arg4);
		}
	}
	
	private String input;
	private String output;
	private String ref;
	
	public JSProcessor(String input, String ref) {
		this.input = input;
		if (input == null) {
			throw new NullPointerException("\"input\" can't to be null");
		}
		this.ref = ref;
		if (ref == null) {
			throw new NullPointerException("\"ref\" can't to be null");
		}
		
	}
	
	public JSProcessor(File filename) throws NullPointerException, IOException {
		if (filename == null) {
			throw new NullPointerException("\"input\" can't to be null");
		}
		CopyMove.checkExistsCanRead(filename);
		input = FileUtils.readFileToString(filename);
	}
	
	public void reduceJS() throws Exception {
		Loggers.Play.debug("Reduce JS: " + ref);
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
		Loggers.Play.debug("Transform JSX: " + ref);
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
		Loggers.Play.debug("Write " + ref + " to " + filename);
		FileUtils.write(filename, output, false);
	}
	
}
