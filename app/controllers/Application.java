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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2014
 * 
*/
package controllers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.indices.IndexMissingException;

import com.google.gson.Gson;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.web.PartialContent;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.With;

@With(Secure.class)
public class Application extends Controller {
	
	public static final int HTTP_bad_request = 400;
	public static final int HTTP_unauthorized = 401;
	public static final int HTTP_forbidden = 403;
	public static final int HTTP_not_found = 404;
	public static final int HTTP_internal_error = 500;
	public static final int HTTP_not_implemented = 501;
	
	public static void index() {
		// redirect(Router.getFullUrl("Application.indexjs"), true);
		/*String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		String current_basket_content = "[]";
		render(title, current_basket_content);*/
		render();
	}
	
	public static void i18n() {
		// XXX replace by Async JS
		response.cacheFor("24h");
		response.contentType = "application/javascript";
		Properties ymessages = Messages.all(play.i18n.Lang.get());
		render(ymessages);
	}
	
	public static void iconsmap() {
		response.cacheFor("600s");
		
		if (Configuration.global.isElementExists("iconsmap") == false) {
			renderJSON("{}");
		} else {
			renderJSON((new Gson()).toJson(Configuration.global.getValues("iconsmap")));
		}
	}
	
	@Check("navigate")
	public static void metadatafile(@Required String filehash, @Required String type, @Required String file) throws Exception {
		response.setHeader("Accept-Ranges", "bytes");
		
		if (Validation.hasErrors()) {
			forbidden();
		}
		response.cacheFor("60s");
		
		RenderedFile element = null;
		try {
			if (type.equals(EntrySummary.MASTER_AS_PREVIEW)) {
				element = ContainerOperations.getMasterAsPreviewFile(filehash);
			} else {
				element = ContainerOperations.getMetadataFile(filehash, type, file, false);
			}
		} catch (IOException e) {
			Loggers.Play.error("Can't get the file, filehash: " + filehash + ", type:" + type + ", file: " + file, e);
		} catch (IndexMissingException e) {
			Loggers.Play.debug("Index mising", e);
		} catch (SearchPhaseExecutionException e) {
			Loggers.Play.debug("No datas", e);
		}
		
		if (element == null) {
			forbidden();
		}
		
		try {
			Header rangeHeader = request.headers.get("range");
			if (rangeHeader != null) {
				throw new PartialContent(element.getRendered_file(), element.getRendered_mime());
			} else {
				renderBinary(new FileInputStream(element.getRendered_file()), file, element.getRendered_file().length(), element.getRendered_mime(), false);
			}
		} catch (FileNotFoundException e) {
			forbidden();
		}
	}
	
	@Deprecated
	public static void indexjs() {
		Loggers.Play.warn("Client use indexjs controller (/!#)");
		render();
	}
	
}
