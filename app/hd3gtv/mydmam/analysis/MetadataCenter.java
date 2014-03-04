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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.analysis;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MetadataCenter implements CliModule {
	
	public static final String ES_INDEX = "metadata";
	public static final String ES_TYPE_SUMMARY = "summary";
	
	/**
	 * name -> provider
	 */
	private LinkedHashMap<String, MetadataProvider> providers;
	private volatile List<MetadataCenterIndexer> indexers;
	
	public MetadataCenter() {
		providers = new LinkedHashMap<String, MetadataProvider>();
		addAnalyser(new FFprobeAnalyser());
		addAnalyser(new FFmpegSnapshoot());
		indexers = new ArrayList<MetadataCenterIndexer>();
	}
	
	public synchronized void addAnalyser(MetadataProvider provider) {
		if (provider == null) {
			throw new NullPointerException("\"provider\" can't to be null");
		}
		if (provider.isEnabled()) {
			if (providers.containsKey(provider.getName())) {
				Log2Dump dump = new Log2Dump();
				dump.add("this", provider);
				dump.add("previous", providers.get(provider.getName()));
				Log2.log.info("Provider with this name exists", dump);
			} else {
				providers.put(provider.getName(), provider);
			}
		} else {
			Log2.log.info("Provider " + provider.getName() + " is disabled");
		}
	}
	
	/**
	 * @param min_index_date set 0 for all
	 */
	public void performAnalysis(Client client, String storagename, String currentpath, long min_index_date, boolean force_refresh) throws Exception {
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		if (storagename == null) {
			throw new NullPointerException("\"storagename\" can't to be null");
		}
		if (currentpath == null) {
			throw new NullPointerException("\"currentpath\" can't to be null");
		}
		
		MetadataCenterIndexer metadataCenterIndexer = new MetadataCenterIndexer(this, client, force_refresh);
		indexers.add(metadataCenterIndexer);
		metadataCenterIndexer.process(storagename, currentpath, min_index_date);
		indexers.remove(metadataCenterIndexer);
	}
	
	public void stopAnalysis() {
		for (int pos = 0; pos < indexers.size(); pos++) {
			indexers.get(pos).stop();
		}
	}
	
	/**
	 * Delete orphan (w/o pathindex) metadatas elements
	 */
	public static void database_gc(Client client) {
		try {
			BulkRequestBuilder bulkrequest = client.prepareBulk();
			SearchRequestBuilder request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			request.setQuery(QueryBuilders.matchAllQuery());
			SearchResponse response = request.execute().actionGet();
			
			SearchHit[] hits = response.getHits().hits();
			int count_remaining = (int) response.getHits().getTotalHits();
			int totalhits = count_remaining;
			
			Map<String, Object> mtd_element;
			Map<String, Object> mtd_element_source;
			String element_source_key;
			GetResponse getresponse;
			
			boolean can_continue = true;
			while (can_continue) {
				for (int pos = 0; pos < hits.length; pos++) {
					mtd_element = hits[pos].getSource();
					mtd_element_source = (Map<String, Object>) mtd_element.get("origin");
					element_source_key = (String) mtd_element_source.get("key");
					
					getresponse = client.get(new GetRequest(Importer.ES_INDEX, Importer.ES_TYPE_FILE, element_source_key)).actionGet();
					if (getresponse.isExists() == false) {
						bulkrequest.add(client.prepareDelete(ES_INDEX, hits[pos].getType(), hits[pos].getId()));
						// TODO if mtd file, delete it
					}
					
					count_remaining--;
					if (can_continue == false) {
						count_remaining = 0;
						break;
					}
				}
				if (count_remaining == 0) {
					break;
				}
				request.setFrom(totalhits - count_remaining);
				response = request.execute().actionGet();
				hits = response.getHits().hits();
				if (hits.length == 0) {
					can_continue = false;
				}
			}
			
			if (bulkrequest.numberOfActions() == 0) {
				return;
			} else {
				Log2.log.info("Remove " + bulkrequest.numberOfActions() + " orphan element(s)");
			}
			BulkResponse bulkresponse = bulkrequest.execute().actionGet();
			if (bulkresponse.hasFailures()) {
				Log2Dump dump = new Log2Dump();
				dump.add("failure message", bulkresponse.buildFailureMessage());
				Log2.log.error("ES errors during add/delete documents", null, dump);
			}
		} catch (IndexMissingException ime) {
			Log2.log.info("No metadatas exists in database, no clean to do");
		}
	}
	
	/**
	 * @param type if null, use default (summary).
	 */
	public static JSONObject getMetadatas(Client client, SourcePathIndexerElement element, String type) throws IndexMissingException {
		if (element == null) {
			throw new NullPointerException("\"pathelementskeys\" can't to be null");
		}
		try {
			GetResponse response = null;
			if (type == null) {
				response = client.get(new GetRequest(ES_INDEX, ES_TYPE_SUMMARY, MetadataCenterIndexer.getUniqueElementKey(element))).actionGet();
			} else {
				response = client.get(new GetRequest(ES_INDEX, type, MetadataCenterIndexer.getUniqueElementKey(element))).actionGet();
			}
			
			if (response.isExists() == false) {
				return null;
			}
			if (response.isSourceEmpty()) {
				return null;
			}
			
			JSONParser parser = new JSONParser();
			JSONObject current_mtd = (JSONObject) parser.parse(response.getSourceAsString());
			current_mtd.remove("origin");
			
			return current_mtd;
		} catch (ParseException e) {
			Log2.log.error("Invalid ES response", e);
			return null;
		}
	}
	
	public static JSONObject getMetadatas(Client client, String[] pathelementskeys, boolean full_metadatas) throws IndexMissingException {
		if (pathelementskeys == null) {
			throw new NullPointerException("\"pathelementskeys\" can't to be null");
		}
		if (pathelementskeys.length == 0) {
			return null;
		}
		
		JSONObject result = new JSONObject();
		
		try {
			SearchRequestBuilder request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			if (full_metadatas == false) {
				request.setTypes(ES_TYPE_SUMMARY);
			}
			
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			for (int pos = 0; pos < pathelementskeys.length; pos++) {
				query.should(QueryBuilders.termQuery("origin.key", pathelementskeys[pos]));
			}
			request.setQuery(query);
			SearchHit[] hits = request.execute().actionGet().getHits().hits();
			
			JSONParser parser = new JSONParser();
			JSONObject current_mtd;
			JSONObject current_pathelement;
			String currect_key;
			for (int pos = 0; pos < hits.length; pos++) {
				parser.reset();
				current_mtd = (JSONObject) parser.parse(hits[pos].getSourceAsString());
				currect_key = (String) ((JSONObject) current_mtd.get("origin")).get("key");
				current_mtd.remove("origin");
				
				current_pathelement = (JSONObject) result.get(currect_key);
				if (current_pathelement == null) {
					current_pathelement = new JSONObject();
					current_pathelement.put(hits[pos].getType(), current_mtd);
					result.put(currect_key, current_pathelement);
				} else {
					current_pathelement.put(hits[pos].getType(), current_mtd);
				}
			}
		} catch (IndexMissingException e) {
			return null;
		} catch (ParseException e) {
			Log2.log.error("Invalid ES response", e);
			return null;
		}
		return result;
	}
	
	/**
	 * Database independant
	 */
	public AnalysisResult standaloneAnalysis(File physical_source, SourcePathIndexerElement reference) throws Exception {
		AnalysisResult analysis_result = new AnalysisResult();
		analysis_result.origin = physical_source;
		
		if (physical_source.length() == 0) {
			analysis_result.mimetype = "application/null";
		} else {
			analysis_result.mimetype = MimeExtract.getMime(physical_source);
		}
		
		if (providers.size() == 0) {
			return analysis_result;
		}
		analysis_result.processing_results = new LinkedHashMap<MetadataProvider, JSONObject>(providers.size());
		
		for (Map.Entry<String, MetadataProvider> entry : providers.entrySet()) {
			MetadataProvider provider = entry.getValue();
			if (provider.canProcessThis(analysis_result.mimetype)) {
				if (provider instanceof Analyser) {
					Analyser analyser = (Analyser) provider;
					JSONObject jo_processing_result = analyser.process(physical_source);
					if (jo_processing_result == null) {
						continue;
					}
					if (jo_processing_result.isEmpty()) {
						continue;
					}
					analysis_result.processing_results.put(analyser, jo_processing_result);
				} else if (provider instanceof Renderer) {
					Renderer renderer = (Renderer) provider;
					List<RenderedElement> renderedelements = renderer.process(physical_source);
					if (renderedelements == null) {
						continue;
					}
					if (renderedelements.size() == 0) {
						continue;
					}
					
					JSONObject jo_processing_result = new JSONObject();
					for (int pos = 0; pos < renderedelements.size(); pos++) {
						renderedelements.get(pos).consolidate(reference, renderer);
						JSONObject jo = renderedelements.get(pos).toDatabase();
						jo_processing_result.put(renderer.getElasticSearchIndexType() + "-" + pos, jo);
					}
					analysis_result.processing_results.put(renderer, jo_processing_result);
					
					RenderedElement.cleanCurrentTempDirectory();
				} else {
					Log2Dump dump = new Log2Dump();
					dump.add("provider class", provider);
					dump.add("provider name", provider.getName());
					Log2.log.error("Can't handle this MetadataProvider", null, dump);
				}
			}
		}
		
		return analysis_result;
	}
	
	public String getCliModuleName() {
		return "mtd";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on metadatas and file analysis";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		boolean verbose = args.getParamExist("-v");
		boolean prettify = args.getParamExist("-vv");
		
		SourcePathIndexerElement spie = new SourcePathIndexerElement();
		spie.currentpath = "/execCli/" + System.currentTimeMillis();
		spie.date = System.currentTimeMillis();
		spie.dateindex = spie.date;
		spie.directory = false;
		spie.parentpath = "/execCli";
		spie.size = 0;
		spie.storagename = "Test_MyDMAM_CLI";
		
		if (args.getParamExist("-a")) {
			File dir_testformats = new File(args.getSimpleParamValue("-a"));
			if (dir_testformats.exists() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			if (dir_testformats.isDirectory() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			
			AnalysisResult result;
			File[] files = dir_testformats.listFiles();
			for (int pos = 0; pos < files.length; pos++) {
				if (files[pos].isDirectory()) {
					continue;
				}
				if (files[pos].isHidden()) {
					continue;
				}
				result = standaloneAnalysis(files[pos], spie);
				System.out.print(result.origin);
				System.out.print("\t");
				System.out.print(result.mimetype);
				System.out.print("\t");
				if ((result.processing_results != null) & (verbose | prettify)) {
					for (Map.Entry<MetadataProvider, JSONObject> entry : result.processing_results.entrySet()) {
						System.out.println();
						System.out.print("\t\t");
						System.out.print(entry.getKey().getName());
						System.out.print(" [");
						System.out.print(entry.getKey().getElasticSearchIndexType());
						System.out.print("]");
						System.out.print("\t");
						if (prettify) {
							System.out.print(json_prettify(entry.getValue()));
						} else {
							System.out.print(entry.getValue().toJSONString());
						}
					}
				}
				System.out.println();
			}
			
			return;
		}
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println(" * standalone directory analysis: ");
		System.out.println("   " + getCliModuleName() + " -a /full/path [-v | -vv]");
		System.out.println("   -v verbose");
		System.out.println("   -vv verbose and prettify");
	}
	
	public static String json_prettify(JSONObject json) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectWriter writer = mapper.writer().withDefaultPrettyPrinter();
		try {
			return writer.writeValueAsString(json);
		} catch (Exception e) {
			Log2Dump dump = new Log2Dump();
			dump.add("json", json);
			Log2.log.error("Bad JSON prettify, cancel it", e);
			return json.toJSONString();
		}
	}
}