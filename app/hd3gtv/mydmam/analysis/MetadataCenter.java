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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.analysis;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.cli.CliModule;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.ExecprocessBadExecutionException;
import hd3gtv.tools.MimeutilsWrapper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

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
	 * name -> analyser
	 */
	private LinkedHashMap<String, Analyser> analysers;
	private Index index;
	
	public MetadataCenter() {
		analysers = new LinkedHashMap<String, Analyser>();
		addAnalyser(new FFprobeAnalyser());
	}
	
	/**
	 * If the file size/date change, this id will change
	 */
	public static String getUniqueElementKey(SourcePathIndexerElement element) {
		StringBuffer sb = new StringBuffer();
		sb.append(element.storagename);
		sb.append(element.currentpath);
		sb.append(element.size);
		sb.append(element.date);
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(sb.toString().getBytes());
			return "mtd-" + MyDMAM.byteToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new NullPointerException(e.getMessage());
		}
	}
	
	public synchronized void addAnalyser(Analyser analyser) {
		if (analyser == null) {
			throw new NullPointerException("\"analyser\" can't to be null");
		}
		if (analyser.isEnabled()) {
			if (analysers.containsKey(analyser.getName())) {
				Log2Dump dump = new Log2Dump();
				dump.add("this", analyser);
				dump.add("previous", analysers.get(analyser.getName()));
				Log2.log.info("Analyser with this name exists", dump);
			} else {
				analysers.put(analyser.getName(), analyser);
			}
		} else {
			Log2.log.info("Analyser " + analyser.getName() + " is disabled");
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
		
		index = new Index(client, storagename, currentpath, min_index_date, force_refresh);
		index.done();
		index = null;
	}
	
	public synchronized void stopAnalysis() {
		if (index == null) {
			return;
		}
		index.stop();
	}
	
	private class Index implements IndexingEvent {
		Client client;
		Explorer explorer;
		boolean force_refresh;
		boolean stop_analysis;
		BulkRequestBuilder bulkrequest;
		
		public Index(Client client, String storagename, String currentpath, long min_index_date, boolean force_refresh) throws Exception {
			this.client = client;
			this.force_refresh = force_refresh;
			
			bulkrequest = client.prepareBulk();
			stop_analysis = false;
			
			explorer = new Explorer(client);
			explorer.getAllSubElementsFromElementKey(Explorer.getElementKey(storagename, currentpath), min_index_date, this);
		}
		
		public synchronized void stop() {
			stop_analysis = true;
		}
		
		public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
			if (stop_analysis) {
				return false;
			}
			
			if (bulkrequest.numberOfActions() > 1000) {
				done();
				bulkrequest = client.prepareBulk();
			}
			
			String element_key = element.prepare_key();
			
			boolean must_analyst = false;
			ArrayList<SearchHit> valid_mtd_hit = new ArrayList<SearchHit>();
			
			if (force_refresh) {
				must_analyst = true;
			} else {
				try {
					/**
					 * Search old metadatas element
					 */
					SearchRequestBuilder request = client.prepareSearch();
					request.setIndices(ES_INDEX);
					request.setQuery(QueryBuilders.termQuery("origin.key", element_key));
					SearchHit[] hits = request.execute().actionGet().getHits().hits();
					if (hits.length == 0) {
						must_analyst = true;
						/*Log2Dump dump = new Log2Dump();
						dump.add("element_key", element_key);
						dump.addAll(element);
						Log2.log.debug("New element to analysis", dump);*/
					} else {
						Map<String, Object> mtd_element_source;
						Map<String, Object> mtd_element;
						long mtd_date;
						long mtd_size;
						
						for (int pos = 0; pos < hits.length; pos++) {
							if (stop_analysis) {
								return false;
							}
							
							/**
							 * For all metadata elements for this source path indexed element
							 */
							mtd_element = hits[pos].getSource();
							mtd_element_source = (Map<String, Object>) mtd_element.get("origin");
							mtd_size = ((Number) mtd_element_source.get("size")).longValue();
							mtd_date = ((Number) mtd_element_source.get("date")).longValue();
							
							if ((element.date != mtd_date) | (element.size != mtd_size)) {
								bulkrequest.add(client.prepareDelete(ES_INDEX, hits[pos].getType(), hits[pos].getId()));
								
								Log2Dump dump = new Log2Dump();
								dump.addAll(element);
								dump.add("origin", mtd_element_source);
								Log2.log.debug("Obsolete analysis", dump);
								
								must_analyst = true;
							} else {
								valid_mtd_hit.add(hits[pos]);
							}
						}
					}
				} catch (IndexMissingException ime) {
					must_analyst = true;
				}
			}
			
			if (must_analyst == false) {
				return true;
			}
			
			File physical_source = Explorer.getLocalBridgedElement(element);
			
			if (stop_analysis) {
				return false;
			}
			
			Log2Dump dump = new Log2Dump();
			dump.addAll(element);
			dump.add("physical_source", physical_source);
			dump.add("force_refresh", force_refresh);
			Log2.log.debug("Analyst this", dump);
			
			/**
			 * Test if real file exists and if it's valid
			 */
			if (physical_source == null) {
				throw new IOException("Can't analyst element : there is no Configuration bridge for the \"" + element.storagename + "\" storage index name.");
			}
			if (physical_source.exists() == false) {
				for (int pos = 0; pos < valid_mtd_hit.size(); pos++) {
					bulkrequest.add(client.prepareDelete(ES_INDEX, valid_mtd_hit.get(pos).getType(), valid_mtd_hit.get(pos).getId()));
					dump = new Log2Dump();
					dump.add("ES_TYPE", valid_mtd_hit.get(pos).getType());
					dump.add("ES_ID", valid_mtd_hit.get(pos).getId());
					dump.add("physical_source", physical_source);
					Log2.log.debug("Delete obsolete analysis : original file isn't exists", dump);
				}
				
				bulkrequest.add(explorer.deleteRequestFileElement(element_key));
				dump = new Log2Dump();
				dump.add("key", element_key);
				dump.add("physical_source", physical_source);
				Log2.log.debug("Delete path element : original file isn't exists", dump);
				
				return true;
			}
			if (physical_source.isFile() == false) {
				throw new IOException(physical_source.getPath() + " is not a file");
			}
			if (physical_source.canRead() == false) {
				throw new IOException("Can't read " + physical_source.getPath());
			}
			
			if (stop_analysis) {
				return false;
			}
			
			/**
			 * Tests file size : must be constant
			 */
			long current_length = physical_source.length();
			
			if (element.size != current_length) {
				/**
				 * Ignore this file, the size isn't constant... May be this file is in copy ?
				 */
				return true;
			}
			
			if (physical_source.exists() == false) {
				/**
				 * Ignore this file, it's deleted !
				 */
				return true;
			}
			
			/**
			 * Start real analysis
			 */
			String key = getUniqueElementKey(element);
			
			AnalysisResult analysis_result = null;
			try {
				analysis_result = standaloneAnalysis(physical_source);
			} catch (ExecprocessBadExecutionException e) {
				/**
				 * Cancel analyst for this file : invalid file !
				 */
				return true;
			}
			
			/**
			 * Wrap result datas into JSON, and prepare push.
			 */
			JSONObject origin = new JSONObject();
			origin.put("size", physical_source.length());
			origin.put("date", physical_source.lastModified());
			origin.put("key", element_key);
			
			JSONObject jo_summary = new JSONObject();
			jo_summary.put("mimetype", analysis_result.mimetype);
			jo_summary.put("origin", origin);
			
			if (analysis_result.processing_results != null) {
				for (Map.Entry<Analyser, JSONObject> entry : analysis_result.processing_results.entrySet()) {
					Analyser analyser = entry.getKey();
					JSONObject processing_result = entry.getValue();
					
					entry.getValue().put("origin", origin);
					bulkrequest.add(client.prepareIndex(ES_INDEX, analyser.getElasticSearchIndexType(), key).setSource(processing_result.toJSONString()));
					jo_summary.put(analyser.getElasticSearchIndexType(), analyser.getSummary(processing_result));
				}
			}
			
			bulkrequest.add(client.prepareIndex(ES_INDEX, ES_TYPE_SUMMARY, key).setSource(jo_summary.toJSONString()));
			return true;
		}
		
		public void done() {
			if (bulkrequest.numberOfActions() == 0) {
				return;
			}
			BulkResponse bulkresponse = bulkrequest.execute().actionGet();
			if (bulkresponse.hasFailures()) {
				Log2Dump dump = new Log2Dump();
				dump.add("failure message", bulkresponse.buildFailureMessage());
				Log2.log.error("ES errors during add/delete documents", null, dump);
			}
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
				response = client.get(new GetRequest(ES_INDEX, ES_TYPE_SUMMARY, getUniqueElementKey(element))).actionGet();
			} else {
				response = client.get(new GetRequest(ES_INDEX, type, getUniqueElementKey(element))).actionGet();
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
	
	public AnalysisResult standaloneAnalysis(File physical_source) throws Exception {
		AnalysisResult analysis_result = new AnalysisResult();
		analysis_result.origin = physical_source;
		
		if (physical_source.length() == 0) {
			analysis_result.mimetype = "application/null";
		} else {
			analysis_result.mimetype = MimeutilsWrapper.getMime(physical_source);
		}
		
		if (analysers.size() == 0) {
			return analysis_result;
		}
		
		analysis_result.processing_results = new LinkedHashMap<Analyser, JSONObject>(analysers.size());
		
		for (Map.Entry<String, Analyser> entry : analysers.entrySet()) {
			Analyser analyser = entry.getValue();
			if (analyser.canProcessThis(analysis_result.mimetype)) {
				JSONObject jo_processing_result = analyser.process(physical_source);
				if (jo_processing_result == null) {
					continue;
				}
				if (jo_processing_result.isEmpty()) {
					continue;
				}
				analysis_result.processing_results.put(analyser, jo_processing_result);
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
		if (args.getParamExist("-a")) {
			File dir_testformats = new File(args.getSimpleParamValue("-a"));
			if (dir_testformats.exists() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			if (dir_testformats.isDirectory() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			
			File[] files = dir_testformats.listFiles();
			for (int pos = 0; pos < files.length; pos++) {
				if (files[pos].isDirectory()) {
					continue;
				}
				if (files[pos].isHidden()) {
					continue;
				}
				
				Log2.log.info("Analysis", standaloneAnalysis(files[pos]));
			}
			
			return;
		}
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println(" * standalone directory analysis: ");
		System.out.println("   " + getCliModuleName() + " -a /full/path");
	}
	
}