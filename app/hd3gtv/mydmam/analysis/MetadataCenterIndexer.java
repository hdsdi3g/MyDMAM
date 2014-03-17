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
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ExecprocessBadExecutionException;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

class MetadataCenterIndexer implements IndexingEvent {
	
	private Client client;
	private Explorer explorer;
	private boolean force_refresh;
	private boolean stop_analysis;
	private BulkRequestBuilder bulkrequest;
	private MetadataCenter metadatacenter;
	
	MetadataCenterIndexer(MetadataCenter metadatacenter, Client client, boolean force_refresh) throws Exception {
		this.metadatacenter = metadatacenter;
		this.client = client;
		this.force_refresh = force_refresh;
	}
	
	void process(String storagename, String currentpath, long min_index_date) throws Exception {
		stop_analysis = false;
		
		bulkrequest = client.prepareBulk();
		explorer = new Explorer(client);
		explorer.getAllSubElementsFromElementKey(Explorer.getElementKey(storagename, currentpath), min_index_date, this);
		bulkExecute();
	}
	
	private void bulkExecute() {
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
	
	synchronized void stop() {
		stop_analysis = true;
	}
	
	static JSONObject getOriginElement(String element_key, long size, long date) {
		JSONObject origin = new JSONObject();
		origin.put("size", size);
		origin.put("date", date);
		origin.put("key", element_key);
		return origin;
	}
	
	static void preparePushRenderedMetadataElement(Client client, BulkRequestBuilder bulkrequest, String mtd_key, JSONObject origin, Renderer renderer, JSONArray rendering_results) {
		JSONObject processing_result = new JSONObject();
		processing_result.put("origin", origin);
		processing_result.put(MetadataCenter.METADATA_PROVIDER_TYPE, Renderer.METADATA_PROVIDER_RENDERER);
		processing_result.put(Renderer.METADATA_PROVIDER_RENDERER_CONTENT, rendering_results);
		
		bulkrequest.add(client.prepareIndex(MetadataCenter.ES_INDEX, renderer.getElasticSearchIndexType(), mtd_key).setSource(processing_result.toJSONString()));
	}
	
	static void updateSummaryPreviewRenderedMetadataElement(JSONObject jo_summary_previews, Renderer renderer, List<RenderedElement> rendered_elements, JSONObject mtd_summary) {
		PreviewType previewtype = renderer.getPreviewTypeForRenderer(mtd_summary, rendered_elements);
		if (previewtype == null) {
			return;
		}
		JSONObject preview_content = new JSONObject();
		if (rendered_elements.size() == 1) {
			preview_content.put("file", rendered_elements.get(0).getRendered_file().getName());
		} else {
			JSONArray ja_elements_list = new JSONArray();
			for (int pos_re = 0; pos_re < rendered_elements.size(); pos_re++) {
				ja_elements_list.add(rendered_elements.get(pos_re).getRendered_file().getName());
			}
			preview_content.put("files", ja_elements_list);
		}
		
		JSONObject previewconfiguration = renderer.getPreviewConfigurationForRenderer(previewtype, mtd_summary, rendered_elements);
		if (previewconfiguration != null) {
			if (previewconfiguration.isEmpty() == false) {
				preview_content.put("conf", previewconfiguration);
			}
		}
		preview_content.put("type", renderer.getElasticSearchIndexType());
		jo_summary_previews.put(previewtype.toString(), preview_content);
	}
	
	public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
		if (stop_analysis) {
			return false;
		}
		
		if (bulkrequest.numberOfActions() > 1000) {
			bulkExecute();
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
				request.setIndices(MetadataCenter.ES_INDEX);
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
							bulkrequest.add(client.prepareDelete(MetadataCenter.ES_INDEX, hits[pos].getType(), hits[pos].getId()));
							RenderedElement.purge(hits[pos].getId());
							
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
				bulkrequest.add(client.prepareDelete(MetadataCenter.ES_INDEX, valid_mtd_hit.get(pos).getType(), valid_mtd_hit.get(pos).getId()));
				RenderedElement.purge(valid_mtd_hit.get(pos).getId());
				
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
		
		MetadataIndexerResult indexing_result = null;
		try {
			indexing_result = metadatacenter.standaloneIndexing(physical_source, element);
		} catch (ExecprocessBadExecutionException e) {
			/**
			 * Cancel analyst for this file : invalid file !
			 */
			return true;
		}
		
		/**
		 * Wrap result datas into JSON, and prepare push.
		 * Don't forget to update merge() in case of updates
		 */
		JSONObject origin = getOriginElement(element_key, physical_source.length(), physical_source.lastModified());
		
		JSONObject jo_summary = new JSONObject();
		jo_summary.put("mimetype", indexing_result.mimetype);
		if (indexing_result.master_as_preview) {
			jo_summary.put(MetadataCenter.MASTER_AS_PREVIEW, true);
		}
		jo_summary.put("origin", origin);
		JSONObject jo_summary_previews = new JSONObject();
		
		if (indexing_result.analysis_results != null) {
			for (Map.Entry<Analyser, JSONObject> entry : indexing_result.analysis_results.entrySet()) {
				Analyser analyser = entry.getKey();
				JSONObject processing_result = entry.getValue();
				
				entry.getValue().put("origin", origin);
				bulkrequest.add(client.prepareIndex(MetadataCenter.ES_INDEX, analyser.getElasticSearchIndexType(), key).setSource(processing_result.toJSONString()));
				jo_summary.put(analyser.getElasticSearchIndexType(), analyser.getSummary(processing_result));
			}
		}
		
		LinkedHashMap<Renderer, JSONArray> rendering_results = indexing_result.makeJSONRendering_results();
		if (rendering_results != null) {
			for (Map.Entry<Renderer, JSONArray> entry : rendering_results.entrySet()) {
				preparePushRenderedMetadataElement(client, bulkrequest, key, origin, entry.getKey(), entry.getValue());
				updateSummaryPreviewRenderedMetadataElement(jo_summary_previews, entry.getKey(), indexing_result.rendering_results.get(entry.getKey()), jo_summary);
			}
		}
		
		if (jo_summary_previews.isEmpty() == false) {
			jo_summary.put("previews", jo_summary_previews);
		}
		
		bulkrequest.add(client.prepareIndex(MetadataCenter.ES_INDEX, MetadataCenter.ES_TYPE_SUMMARY, key).setSource(jo_summary.toJSONString()));
		return true;
	}
	
	/**
	 * Don't forget to update onFoundElement() in case of code updates
	 */
	public static void merge(Client client, Renderer renderer, List<RenderedElement> rendered_elements, SourcePathIndexerElement source_element, String index_type) throws IOException {
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		if (rendered_elements == null) {
			throw new NullPointerException("\"rendering_results\" can't to be null");
		}
		if (source_element == null) {
			throw new NullPointerException("\"source_element\" can't to be null");
		}
		if (index_type == null) {
			throw new NullPointerException("\"index_type\" can't to be null");
		}
		
		/**
		 * Prepare rendered elements
		 */
		for (int pos = 0; pos < rendered_elements.size(); pos++) {
			rendered_elements.get(pos).consolidate(source_element, renderer);
		}
		RenderedElement.cleanCurrentTempDirectory();
		
		/**
		 * Convert List<RenderedElement> to JSONArray
		 */
		LinkedHashMap<Renderer, List<RenderedElement>> rendering_results = new LinkedHashMap<Renderer, List<RenderedElement>>();
		rendering_results.put(renderer, rendered_elements);
		LinkedHashMap<Renderer, JSONArray> json_rendering_results = MetadataIndexerResult.makeJSONRendering_results(rendering_results);
		JSONArray ja_rendering_results = json_rendering_results.get(renderer);
		
		JSONObject json_origin = getOriginElement(source_element.prepare_key(), source_element.size, source_element.date);
		String mtd_key = getUniqueElementKey(source_element);
		
		BulkRequestBuilder bulkrequest = client.prepareBulk();
		preparePushRenderedMetadataElement(client, bulkrequest, mtd_key, json_origin, renderer, ja_rendering_results);
		
		/**
		 * Search actual mtd rendered file entry.
		 */
		JSONObject jo_summary = MetadataCenter.getSummaryMetadatas(client, source_element);
		if (jo_summary == null) {
			throw new NullPointerException("Can't found element \"" + source_element.toString(" ") + "\" from DB");
		}
		
		/**
		 * Add origin which was deleted by getSummaryMetadatas()
		 */
		jo_summary.put("origin", json_origin);
		
		JSONObject jo_summary_previews = new JSONObject();
		if (jo_summary.containsKey("previews")) {
			jo_summary_previews = (JSONObject) jo_summary.get("previews");
		}
		
		updateSummaryPreviewRenderedMetadataElement(jo_summary_previews, renderer, rendered_elements, jo_summary);
		
		if (jo_summary_previews.isEmpty() == false) {
			jo_summary.put("previews", jo_summary_previews);
		}
		bulkrequest.add(client.prepareIndex(MetadataCenter.ES_INDEX, MetadataCenter.ES_TYPE_SUMMARY, mtd_key).setSource(jo_summary.toJSONString()));
		
		if (bulkrequest.numberOfActions() > 0) {
			BulkResponse bulkresponse = bulkrequest.execute().actionGet();
			if (bulkresponse.hasFailures()) {
				Log2Dump dump = new Log2Dump();
				dump.add("failure message", bulkresponse.buildFailureMessage());
				Log2.log.error("ES errors during update documents", null, dump);
			}
		}
	}
	
	/**
	 * If the file size/date change, this id will change
	 */
	static String getUniqueElementKey(SourcePathIndexerElement element) {
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
	
}
