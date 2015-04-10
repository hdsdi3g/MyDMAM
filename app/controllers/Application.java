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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.useraction.Basket;
import hd3gtv.mydmam.web.PartialContent;
import hd3gtv.mydmam.web.SearchResult;
import hd3gtv.mydmam.web.search.AsyncSearchQuery;
import hd3gtv.mydmam.web.stat.Stat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.indices.IndexMissingException;

import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.jobs.JobsPlugin;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.With;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

@With(Secure.class)
public class Application extends Controller {
	
	public static final int HTTP_bad_request = 400;
	public static final int HTTP_unauthorized = 401;
	public static final int HTTP_forbidden = 403;
	public static final int HTTP_not_found = 404;
	public static final int HTTP_internal_error = 500;
	public static final int HTTP_not_implemented = 501;
	
	@Check("navigate")
	public static void navigate() {
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("application.navigate"));
		
		String current_basket_content = "[]";
		try {
			current_basket_content = Basket.getBasketForCurrentPlayUser().getSelectedContentJson();
		} catch (Exception e) {
			Log2.log.error("Can't get user basket", e);
		}
		render(current_basket_content);
	}
	
	@Check("navigate")
	public static void stat() {
		String[] fileshashs = new String[1];
		String onlyone_filehash = params.get("fileshashs");
		if (onlyone_filehash == null) {
			fileshashs = params.getAll("fileshashs[]");
		} else {
			fileshashs[0] = onlyone_filehash;
		}
		
		String[] scopes_element = new String[1];
		String onlyone_scope_element = params.get("scopes_element");
		if (onlyone_scope_element == null) {
			scopes_element = params.getAll("scopes_element[]");
		} else {
			scopes_element[0] = onlyone_scope_element;
		}
		
		String[] scopes_subelements = new String[1];
		String onlyone_scope_subelements = params.get("scopes_subelements");
		if (onlyone_scope_subelements == null) {
			scopes_subelements = params.getAll("scopes_subelements[]");
		} else {
			scopes_subelements[0] = onlyone_scope_subelements;
		}
		
		Stat stat = new Stat(fileshashs, scopes_element, scopes_subelements);
		stat.setJsonSearch(params.get("search"));
		try {
			stat.setPageFrom(Integer.parseInt(params.get("page_from")));
		} catch (Exception e) {
		}
		try {
			stat.setPageSize(Integer.parseInt(params.get("page_size")));
		} catch (Exception e) {
		}
		
		String result = stat.toJSONString();
		renderJSON(result);
	}
	
	public static void index() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		String current_basket_content = "[]";
		try {
			Basket basket = Basket.getBasketForCurrentPlayUser();
			current_basket_content = basket.getSelectedContentJson();
			if (basket.isKeepIndexDeletedBasketItems() == false) {
				JobsPlugin.executor.submit(new UserBasket.AsyncCleanBasket(basket));
			}
		} catch (Exception e) {
			Log2.log.error("Can't get user basket", e);
		}
		
		render(title, current_basket_content);
	}
	
	@Check("navigate")
	public static void search(String q, int from) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		if (q == null) {
			q = "";
		}
		SearchResult searchresults = null;
		if (q.trim().equals("") == false) {
			from = from - 1;
			if (from < 0) {
				from = 0;
			}
			searchresults = SearchResult.search(q, from, 10);
			q = searchresults.query;
			flash("searchmethod", Messages.all(play.i18n.Lang.get()).getProperty("search.method." + searchresults.mode.toString().toLowerCase()));
		}
		
		flash("q", q);
		flash("pagename", q + " - " + Messages.all(play.i18n.Lang.get()).getProperty("search.pagetitle"));
		
		String current_basket_content = "[]";
		try {
			current_basket_content = Basket.getBasketForCurrentPlayUser().getSelectedContentJson();
		} catch (Exception e) {
			Log2.log.error("Can't get user basket", e);
		}
		render("Application/index.html", title, searchresults, current_basket_content);
	}
	
	@Check("navigate")
	public static void asyncsearch(String q, int from) {
		AsyncSearchQuery s_results = new AsyncSearchQuery(q, from);
		
		if (s_results.hasResults()) {
			flash("q", s_results.getQ());
			flash("pagename", s_results.getQ() + " - " + Messages.all(play.i18n.Lang.get()).getProperty("search.pagetitle"));
		} else {
			flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("search.pagetitle"));
		}
		
		/*
		//TODO set get async basket content (SPEED UP page display).
		String current_basket_content = "[]";
		try {
			current_basket_content = Basket.getBasketForCurrentPlayUser().getSelectedContentJson();
		} catch (Exception e) {
			Log2.log.error("Can't get user basket", e);
		}*/
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		
		String results = s_results.toJsonString();
		
		render(title, results);
	}
	
	public static void i18n() {
		response.cacheFor("24h");
		response.contentType = "application/javascript";
		Properties ymessages = Messages.all(play.i18n.Lang.get());
		render(ymessages);
	}
	
	public static void redirectToHome(String oldURL) {
		index();
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
			Log2Dump dump = new Log2Dump();
			dump.add("filehash", filehash);
			dump.add("type", type);
			dump.add("file", file);
			Log2.log.error("Can't get the file", e, dump);
		} catch (IndexMissingException e) {
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
	
	@Check("navigate")
	public static void resolvePositions() throws ConnectionException {
		String[] keys = params.getAll("keys[]");
		if (keys == null) {
			renderJSON("{}");
			return;
		}
		if (keys.length == 0) {
			renderJSON("{}");
			return;
		}
		
		JsonObject result = new JsonObject();
		Map<String, List<String>> raw_positions = MyDMAMModulesManager.getPositions(keys);
		
		ArrayList<String> queries_locations = new ArrayList<String>();
		
		for (Map.Entry<String, List<String>> position : raw_positions.entrySet()) {
			List<String> l_locations = position.getValue();
			for (int pos_location = 0; pos_location < l_locations.size(); pos_location++) {
				if (queries_locations.contains(l_locations.get(pos_location)) == false) {
					queries_locations.add(l_locations.get(pos_location));
				}
			}
		}
		result.add("positions", new Gson().toJsonTree(raw_positions));
		result.add("locations", new Gson().toJsonTree(MyDMAMModulesManager.getPositionInformationsByTapeName(queries_locations.toArray(new String[0]))));
		renderJSON(result.toString());
	}
	
}
