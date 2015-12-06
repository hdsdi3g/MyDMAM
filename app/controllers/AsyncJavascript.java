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

import java.io.FileNotFoundException;
import java.util.Date;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.web.AJSController;
import hd3gtv.mydmam.web.JSXTransformer;
import hd3gtv.mydmam.web.JSXTransformer.JSXItem;
import hd3gtv.mydmam.web.JsCompile;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Controller;
import play.mvc.With;
import play.utils.Utils;
import play.vfs.VirtualFile;

@With(Secure.class)
public class AsyncJavascript extends Controller {
	
	public static void index(@Required String name, @Required String verb, @Required String jsonrq) throws Exception {
		if (Validation.hasErrors()) {
			response.status = Application.HTTP_not_found;
			renderJSON("{}");
		}
		
		try {
			renderJSON(AJSController.doRequest(name, verb, jsonrq));
		} catch (Exception e) {
			Loggers.Play.error("Can't process AJS request name: " + name + ", verb: " + verb + ", jsonrq: " + jsonrq, e);
		}
		renderJSON("{}");
	}
	
	public static void dynamicCompileJSX(@Required String ressource_name) {
		try {
			VirtualFile v_file = JsCompile.getTheFirstFromRelativePath(JSXItem.getRelativePathFromRessourceName(ressource_name), true, false);
			if (v_file == null) {
				throw new FileNotFoundException(ressource_name);
			}
			String etag = v_file.getRealFile().lastModified() + "--";
			long last_modified = v_file.getRealFile().lastModified();
			
			if (request.isModified(etag, last_modified) == false) {
				response.setHeader("Etag", etag);
				notModified();
			}
			
			String jsx_compiled = JSXTransformer.getJSXContentFromURLList(v_file.getRealFile(), ressource_name, true, true);
			response.setHeader("Content-Length", jsx_compiled.length() + "");
			response.setHeader("Content-Type", "text/javascript");
			response.setHeader("Etag", etag);
			response.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last_modified)));
			renderText(jsx_compiled);
			
		} catch (Exception e) {
			Loggers.Play.error("JSX Transformer Error", e);
		}
	}
	
}
