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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.indices.IndexMissingException;

import ext.Bootstrap;
import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.web.JSSourceManager;
import hd3gtv.mydmam.web.JSi18nCached;
import hd3gtv.mydmam.web.PartialContent;
import play.Play;
import play.Play.Mode;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Lang;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.With;
import play.utils.Utils;

@With(Secure.class)
public class Application extends Controller {
	
	public static final int HTTP_not_found = 404;
	public static final int HTTP_unauthorized = 403;
	
	public static void index() {
		render();
	}
	
	public static void i18n() {
		if (Play.mode == Mode.DEV && Configuration.global.getValueBoolean("play", "check_i18n_cache_files") && JSSourceManager.isJsDevMode()) {
			Bootstrap.i18n_cache = new JSi18nCached();
		}
		
		File ressource_file = Bootstrap.i18n_cache.getCachedFile(Lang.get());
		if (ressource_file == null) {
			notFound();
		}
		long last_modified = ressource_file.lastModified();
		
		String etag = last_modified + "--";
		if (Play.mode == Mode.PROD) {
			response.setHeader("Cache-Control", "max-age=864000");
		} else {
			response.setHeader("Cache-Control", "max-age=60");
		}
		
		if (request.isModified(etag, last_modified) == false) {
			response.setHeader("Etag", etag);
			notModified();
		}
		
		if (FilenameUtils.isExtension(ressource_file.getName(), "gz")) {
			if (request.headers.containsKey("accept-encoding") == false) {
				badRequest("// Your browser don't accept encoding files.");
			}
			if (request.headers.get("accept-encoding").value().indexOf("gzip") == -1) {
				badRequest("// Your browser don't accept GZipped files.");
			}
			response.setHeader("Content-Encoding", "gzip");
		}
		
		response.setHeader("Content-Length", ressource_file.length() + "");
		response.setHeader("Content-Type", "text/javascript");
		response.setHeader("Etag", etag);
		response.setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(last_modified)));
		
		try {
			FileUtils.copyFile(ressource_file, response.out);
		} catch (IOException e) {
			Loggers.Play_i18n.error("Can't response (send) js file: " + ressource_file, e);
			notFound();
		}
		ok();
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
	
}
