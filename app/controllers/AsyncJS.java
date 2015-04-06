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
package controllers;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.web.JSXTransformer;
import hd3gtv.mydmam.web.JsCompile;

import java.io.FileNotFoundException;
import java.util.List;

import play.data.validation.Required;
import play.mvc.Controller;
import play.mvc.With;
import play.vfs.VirtualFile;

@With(Secure.class)
public class AsyncJS extends Controller {
	
	public static void index() throws Exception {
		// String js = JSXTransformer.global.transform("React.renderComponent(\n<h1>Hello, world!</h1>,\ndocument.getElementById('example')\n);");
	}
	
	public static void dynamicCompileJSX(@Required String ressource_name) {
		try {
			List<VirtualFile> search_vfile = JsCompile.getAllfromRelativePath(JSXTransformer.JSX_SRC + "/" + ressource_name, true, false);
			if (search_vfile.isEmpty()) {
				throw new FileNotFoundException(JSXTransformer.JSX_SRC + "/" + ressource_name);
			}
			VirtualFile v_file = search_vfile.get(0);
			
			String jsx_compiled = JSXTransformer.getJSXContentFromURLList(v_file, true, true);
			response.setHeader("Content-Length", jsx_compiled.length() + "");
			response.setHeader("Content-Type", "text/javascript");
			renderText(jsx_compiled);
		} catch (Exception e) {
			Log2.log.error("JSX Transformer Error", e);
		}
	}
	
	// TODO create Ajax routing (declaration must follow Secure.checkview()), with systematic validation and Gson xchange for requests/responses, via an Interface.
	// TODO create Ajax routing with module ?
}
