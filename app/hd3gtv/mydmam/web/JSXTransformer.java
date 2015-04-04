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

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

import play.mvc.Router;
import play.vfs.VirtualFile;
import controllers.AsyncJS;

public class JSXTransformer {
	
	public static final String JSXTRANSFORMER_PATH = "/public/javascripts/lib/JSXTransformer.js";
	public static final String JSX_SRC = "/app/react";
	
	public static final JSXTransformer global;
	
	static {
		global = new JSXTransformer();
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
	
	public String transform(String jsx) throws InstantiationError {
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
	
	public static List<String> getJSXURLList() {
		List<String> list = new ArrayList<String>();
		
		List<VirtualFile> jsx_vfiles = VirtualFile.fromRelativePath(JSX_SRC).list();
		
		VirtualFile jsx_file;
		HashMap<String, Object> args = new HashMap<String, Object>();
		for (int pos = 0; pos < jsx_vfiles.size(); pos++) {
			jsx_file = jsx_vfiles.get(pos);
			if (jsx_file.getName().endsWith(".jsx") == false) {
				continue;
			}
			args.put("ressource_name", jsx_file.getName());
			list.add(Router.reverse(AsyncJS.class.getName() + "." + "dynamicCompileJSX", args).url);
		}
		
		Collections.sort(list);
		return list;
	}
	
	public static String getJSXContentFromURLList(String ressource_name, boolean transfrom) throws FileNotFoundException {
		VirtualFile v_file = VirtualFile.fromRelativePath(JSX_SRC + "/" + ressource_name);
		if (v_file.exists() == false) {
			throw new FileNotFoundException(JSX_SRC + " / " + ressource_name);
		}
		if (transfrom == false) {
			v_file.contentAsString();
		}
		return global.transform(v_file.contentAsString());
	}
}