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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2013
 * 
*/
package controllers;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.analysis.MetadataCenter;
import hd3gtv.mydmam.analysis.RenderedElement;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.web.PartialContent;
import hd3gtv.mydmam.web.SearchResult;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.client.Client;
import org.elasticsearch.indices.IndexMissingException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import play.data.validation.Required;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Http.Header;
import play.mvc.With;
import play.mvc.results.NotFound;

import com.google.gson.Gson;
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
		render();
	}
	
	@Check("navigate")
	public static void stat(String filehash) {
		if (filehash == null) {
			throw new NotFound("No filehash");
		}
		if (filehash.equals("")) {
			throw new NotFound("No filehash");
		}
		Client client = Elasticsearch.createClient();
		Explorer explorer = new Explorer(client);
		
		SourcePathIndexerElement pathelement = explorer.getelementByIdkey(filehash);
		
		try {
			if (pathelement != null) {
				JSONObject jo_result = pathelement.toJson();
				
				if (pathelement.directory) {
					try {
						List<SourcePathIndexerElement> subpathelement;
						
						if (pathelement.storagename != null) {
							/**
							 * Directory list
							 */
							subpathelement = explorer.getDirectoryContentByIdkey(pathelement.prepare_key(), 500);
						} else {
							/**
							 * Storage list
							 */
							subpathelement = explorer.getDirectoryContentByIdkey(SourcePathIndexerElement.hashThis(""), 500);
						}
						JSONArray ja_subelements = new JSONArray();
						JSONObject sub_element;
						JSONObject metadatas;
						boolean metadatas_exists = true;
						for (int pos = 0; pos < subpathelement.size(); pos++) {
							sub_element = subpathelement.get(pos).toJson();
							if (subpathelement.get(pos).directory) {
								sub_element.put("count", explorer.countDirectoryContentElements(subpathelement.get(pos).prepare_key()));
							}
							if (sub_element.containsKey("idxfilename") == false) {
								sub_element.put("idxfilename", subpathelement.get(pos).storagename);
							}
							
							if (metadatas_exists & (subpathelement.get(pos).directory == false)) {
								try {
									metadatas = MetadataCenter.getSummaryMetadatas(client, subpathelement.get(pos));
									if (metadatas != null) {
										sub_element.put("metadatas", metadatas);
									}
								} catch (IndexMissingException ime) {
									metadatas_exists = false;
								}
							}
							
							ja_subelements.add(sub_element);
						}
						jo_result.put("items", ja_subelements);
					} catch (IndexOutOfBoundsException e) {
						jo_result.put("toomanyitems", Integer.parseInt(e.getMessage()));
					}
				} else {
					JSONObject metadatas = MetadataCenter.getSummaryMetadatas(client, pathelement);
					if (metadatas != null) {
						jo_result.put("metadatas", metadatas);
					}
				}
				client.close();
				renderJSON(jo_result.toJSONString());
			} else {
				client.close();
				throw new NotFound(filehash);
			}
		} catch (Exception e) {
			if ((e instanceof IndexMissingException) & (e.getMessage().endsWith("[pathindex] missing"))) {
				Log2.log.error("No pathindex items in ES database", null);
			} else {
				Log2.log.error("Error during pathindex search", e);
			}
			renderJSON("{}");
		}
	}
	
	public static void index() {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		render(title);
	}
	
	@Check("navigate")
	public static void search(String q, int from) {
		String title = Messages.all(play.i18n.Lang.get()).getProperty("site.name");
		if (q == null) {
			q = "";
		}
		SearchResult searchresults = null;
		if (q.trim().equals("") == false) {
			StringBuffer cleanquery = new StringBuffer(q.length());
			char current;
			boolean keepchar;
			for (int pos_q = 0; pos_q < q.length(); pos_q++) {
				current = q.charAt(pos_q);
				keepchar = true;
				for (int pos = 0; pos < Elasticsearch.forbidden_query_chars.length; pos++) {
					if (current == Elasticsearch.forbidden_query_chars[pos]) {
						keepchar = false;
						break;
					}
				}
				if (keepchar) {
					cleanquery.append(current);
				} else {
					cleanquery.append(" ");
				}
			}
			q = cleanquery.toString();
			
			from = from - 1;
			if (from < 0) {
				from = 0;
			}
			searchresults = SearchResult.search(q, from, 10);
			flash("searchmethod", Messages.all(play.i18n.Lang.get()).getProperty("search.method." + searchresults.mode.toString().toLowerCase()));
		}
		
		flash("q", q);
		flash("pagename", Messages.all(play.i18n.Lang.get()).getProperty("search.pagetitle"));
		render("Application/index.html", title, searchresults);
	}
	
	public static void i18n() {
		response.cacheFor("60s");
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
	public static void metadatas(Boolean full) {
		Client client = Elasticsearch.createClient();
		String[] pathelementskeys = params.getAll("fileshash[]");
		JSONObject result = MetadataCenter.getSummaryMetadatas(client, pathelementskeys);
		client.close();
		if (result == null) {
			renderJSON("{}");
		} else {
			renderJSON(result.toJSONString());
		}
	}
	
	@Check("navigate")
	public static void metadatafile(@Required String filehash, @Required String type, @Required String file) {
		response.setHeader("Accept-Ranges", "bytes");
		
		if (validation.hasErrors()) {
			forbidden();
		}
		response.cacheFor("60s");
		
		Client client = Elasticsearch.createClient();
		
		RenderedElement element = null;
		if (type.equals(MetadataCenter.MASTER_AS_PREVIEW)) {
			element = MetadataCenter.getMasterAsPreviewFile(client, filehash);
		} else {
			element = MetadataCenter.getMetadataFileReference(client, filehash, type, file, false);
		}
		
		client.close();
		if (element == null) {
			forbidden();
		}
		
		try {
			Header rangeHeader = request.headers.get("range");
			if (rangeHeader != null) {
				System.out.print("range:\t");
				System.out.println(rangeHeader.values);
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
		
		JSONObject result = new JSONObject();
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
		result.put("positions", raw_positions);
		result.put("locations", MyDMAMModulesManager.getPositionInformationsByTapeName(queries_locations.toArray(new String[0])));
		
		renderJSON(result.toJSONString());
	}
	
}
