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
package hd3gtv.mydmam.metadata;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.EntryAnalyser;
import hd3gtv.mydmam.metadata.container.EntryRenderer;
import hd3gtv.mydmam.metadata.container.EntrySummary;
import hd3gtv.mydmam.metadata.container.Operations;
import hd3gtv.mydmam.metadata.container.Origin;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.FutureCreateTasks;
import hd3gtv.mydmam.transcode.FFmpegAlbumartwork;
import hd3gtv.mydmam.transcode.FFmpegLowresRenderer;
import hd3gtv.mydmam.transcode.FFmpegSnapshoot;
import hd3gtv.mydmam.transcode.FFprobeAnalyser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequestBuilder;
import org.elasticsearch.action.search.MultiSearchRequestBuilder;
import org.elasticsearch.action.search.MultiSearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.QuerySourceBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MetadataCenter {
	
	public static final String ES_INDEX = "metadata";
	
	public static final String METADATA_PROVIDER_TYPE = "metadata-provider-type";
	public static final String MASTER_AS_PREVIEW = "master_as_preview";
	
	private static Client client;
	private static List<GeneratorAnalyser> generatorAnalysers;
	private static List<GeneratorRenderer> generatorRenderers;
	private static Map<String, GeneratorAnalyser> master_as_preview_mime_list_providers;
	
	static {
		client = Elasticsearch.getClient();
		generatorAnalysers = new ArrayList<GeneratorAnalyser>();
		generatorRenderers = new ArrayList<GeneratorRenderer>();
		
		addProvider(new FFprobeAnalyser());
		addProvider(new FFmpegSnapshoot());
		addProvider(new FFmpegAlbumartwork());
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.profile_ffmpeg_lowres_lq, PreviewType.video_lq_pvw, false));
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.profile_ffmpeg_lowres_sd, PreviewType.video_sd_pvw, false));
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.profile_ffmpeg_lowres_hd, PreviewType.video_hd_pvw, false));
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.profile_ffmpeg_lowres_audio, PreviewType.audio_pvw, true));
		
		List<Generator> all_external_providers = MyDMAMModulesManager.getAllExternalMetadataGenerator();
		for (int pos = 0; pos < all_external_providers.size(); pos++) {
			addProvider(all_external_providers.get(pos));
		}
		
		master_as_preview_mime_list_providers = null;
		if (Configuration.global.isElementExists("master_as_preview") == false) {
			if (Configuration.global.getValueBoolean("master_as_preview", "enable") == false) {
				master_as_preview_mime_list_providers = new HashMap<String, GeneratorAnalyser>();
			}
		}
	}
	
	private static void addProvider(Generator provider) {
		if (provider == null) {
			return;
		}
		if (provider.isEnabled() == false) {
			Log2.log.info("Provider " + provider.getLongName() + " is disabled");
			return;
		}
		Operations.declareEntryType(provider.getEntrySample());
		
		GeneratorAnalyser generatorAnalyser;
		if (provider instanceof GeneratorAnalyser) {
			generatorAnalyser = (GeneratorAnalyser) provider;
			generatorAnalysers.add(generatorAnalyser);
			if (master_as_preview_mime_list_providers != null) {
				List<String> list = generatorAnalyser.getMimeFileListCanUsedInMasterAsPreview();
				if (list != null) {
					for (int pos = 0; pos < list.size(); pos++) {
						master_as_preview_mime_list_providers.put(list.get(pos).toLowerCase(), generatorAnalyser);
					}
				}
			}
		} else if (provider instanceof GeneratorRenderer) {
			generatorRenderers.add((GeneratorRenderer) provider);
		} else {
			Log2.log.error("Can't add unrecognized provider", null);
		}
	}
	
	private MetadataCenter() {
	}
	
	public static List<GeneratorRenderer> getRenderers() {
		return generatorRenderers;
	}
	
	/**
	 * Delete orphan (w/o pathindex) metadatas elements
	 */
	public static void database_gc() {
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
			String element_source_storage;
			GetResponse getresponse;
			
			/**
			 * Protect to no remove all mtd if pathindexing is empty for a storage.
			 * https://github.com/hdsdi3g/MyDMAM/issues/7
			 */
			CountRequest countrequest;
			CountResponse countresponse;
			HashMap<String, Long> elementcount_by_storage = new HashMap<String, Long>();
			
			boolean can_continue = true;
			while (can_continue) {
				for (int pos = 0; pos < hits.length; pos++) {
					mtd_element = hits[pos].getSource();
					mtd_element_source = (Map<String, Object>) mtd_element.get("origin");
					element_source_key = (String) mtd_element_source.get("key");
					element_source_storage = (String) mtd_element_source.get("storage");
					
					getresponse = client.get(new GetRequest(Importer.ES_INDEX, Importer.ES_TYPE_FILE, element_source_key)).actionGet();
					if (getresponse.isExists() == false) {
						if (elementcount_by_storage.containsKey(element_source_storage) == false) {
							countrequest = new CountRequest(Importer.ES_INDEX).types(Importer.ES_TYPE_FILE).source(
									new QuerySourceBuilder().setQuery(QueryBuilders.termQuery("storagename", element_source_storage)));
							countresponse = client.count(countrequest).actionGet();
							elementcount_by_storage.put(element_source_storage, countresponse.getCount());
						}
						if (elementcount_by_storage.get(element_source_storage) > 0) {
							/**
							 * This storage is not empty... Source file is really deleted, we can delete metadatas
							 */
							bulkrequest.add(client.prepareDelete(ES_INDEX, hits[pos].getType(), hits[pos].getId()));
							RenderedFile.purge(hits[pos].getId());
						}
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
			
			if (bulkrequest.numberOfActions() > 0) {
				Log2.log.info("Remove " + bulkrequest.numberOfActions() + " orphan element(s)");
				BulkResponse bulkresponse = bulkrequest.execute().actionGet();
				if (bulkresponse.hasFailures()) {
					Log2Dump dump = new Log2Dump();
					dump.add("failure message", bulkresponse.buildFailureMessage());
					Log2.log.error("ES errors during add/delete documents", null, dump);
				}
			}
			
			Log2.log.info("Start cleaning rendered elements");
			
			RenderedFile.gc(client);
			
		} catch (IOException e) {
			Log2.log.error("Can't purge directories", e);
		} catch (IndexMissingException ime) {
			Log2.log.info("No metadatas exists in database, no clean to do");
		}
	}
	
	/**
	 * TODO refactor
	 * Beware: "origin" key is deleted
	 * @return never null, SourcePathIndexerElement key > Metadata element key > Metadata element value
	 */
	private static Map<String, Map<String, Object>> getProcessedSummaries(List<Map<String, Object>> sources) {
		if (sources.size() == 0) {
			return new HashMap<String, Map<String, Object>>(1);
		}
		
		HashMap<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>(sources.size());
		
		Map<String, Object> source;
		Map<String, Object> source_origin;
		String pathelementkey;
		for (int pos = 0; pos < sources.size(); pos++) {
			source = sources.get(pos);
			source_origin = (Map) source.get("origin");
			pathelementkey = (String) source_origin.get("key");
			source.remove("origin");
			result.put(pathelementkey, source);
		}
		return result;
	}
	
	/**
	 * TODO refactor
	 */
	public static Map<String, Map<String, Object>> getSummariesByPathElements(List<SourcePathIndexerElement> pathelements) throws IndexMissingException {
		if (pathelements == null) {
			return new HashMap<String, Map<String, Object>>(1);
		}
		if (pathelements.size() == 0) {
			return new HashMap<String, Map<String, Object>>(1);
		}
		
		MultiGetRequestBuilder multigetrequestbuilder = new MultiGetRequestBuilder(client);
		
		for (int pos = 0; pos < pathelements.size(); pos++) {
			multigetrequestbuilder.add(ES_INDEX, EntrySummary.type, Origin.getUniqueElementKey(pathelements.get(pos)));
		}
		
		MultiGetItemResponse[] response = multigetrequestbuilder.execute().actionGet().getResponses();
		List<Map<String, Object>> sources = new ArrayList<Map<String, Object>>();
		GetResponse current_response;
		for (int pos = 0; pos < response.length; pos++) {
			current_response = response[pos].getResponse();
			if (current_response == null) {
				continue;
			}
			if (current_response.isSourceEmpty()) {
				continue;
			}
			sources.add(response[pos].getResponse().getSource());
		}
		return getProcessedSummaries(sources);
	}
	
	/**
	 * TODO refactor
	 */
	public static Map<String, Map<String, Object>> getSummariesByPathElementKeys(List<String> pathelementkeys) throws IndexMissingException {
		if (pathelementkeys == null) {
			return new HashMap<String, Map<String, Object>>(1);
		}
		if (pathelementkeys.size() == 0) {
			return new HashMap<String, Map<String, Object>>(1);
		}
		
		MultiSearchRequestBuilder multisearchrequestbuilder = new MultiSearchRequestBuilder(client);
		
		SearchRequestBuilder request;
		for (int pos = 0; pos < pathelementkeys.size(); pos++) {
			request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			request.setTypes(EntrySummary.type);
			request.setSize(1);
			request.setQuery(QueryBuilders.termQuery("origin.key", pathelementkeys.get(pos)));
			multisearchrequestbuilder.add(request);
		}
		MultiSearchResponse.Item[] items = multisearchrequestbuilder.execute().actionGet().getResponses();
		List<Map<String, Object>> sources = new ArrayList<Map<String, Object>>();
		
		if (items.length == 0) {
			return new HashMap<String, Map<String, Object>>();
		}
		
		SearchHit[] hits;
		SearchResponse response;
		for (int pos = 0; pos < items.length; pos++) {
			response = items[pos].getResponse();
			if (response == null) {
				continue;
			}
			if (response.getHits() == null) {
				continue;
			}
			hits = response.getHits().hits();
			if (hits.length == 0) {
				continue;
			}
			sources.add(hits[0].getSource());
		}
		return getProcessedSummaries(sources);
	}
	
	/**
	 * TODO refactor
	 */
	public static RenderedFile getMasterAsPreviewFile(String origin_key) throws IndexMissingException {
		if (origin_key == null) {
			throw new NullPointerException("\"origin_key\" can't to be null");
		}
		
		try {
			Client client = Elasticsearch.getClient();
			SearchRequestBuilder request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			request.setTypes(EntrySummary.type);
			
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			query.must(QueryBuilders.termQuery("origin.key", origin_key));
			query.must(QueryBuilders.termQuery(MASTER_AS_PREVIEW, true));
			request.setQuery(query);
			SearchHit[] hits = request.execute().actionGet().getHits().hits();
			if (hits.length == 0) {
				return null;
			}
			
			JSONParser parser = new JSONParser();
			JSONObject current_mtd = (JSONObject) parser.parse(hits[0].getSourceAsString());
			if (current_mtd.containsKey("mimetype") == false) {
				return null;
			}
			
			Explorer explorer = new Explorer();
			SourcePathIndexerElement spie = explorer.getelementByIdkey(origin_key);
			if (spie == null) {
				return null;
			}
			
			return RenderedFile.fromDatabaseMasterAsPreview(spie, (String) current_mtd.get("mimetype"));
		} catch (IndexMissingException e) {
			return null;
		} catch (ParseException e) {
			Log2.log.error("Invalid ES response", e);
		} catch (IOException e) {
			Log2.log.error("Can't found valid file", e);
		}
		return null;
	}
	
	/**
	 * TODO refactor this !
	 */
	public static RenderedFile getMetadataFileReference(String origin_key, String index_type, String filename, boolean check_hash) throws IndexMissingException {
		if (origin_key == null) {
			throw new NullPointerException("\"origin_key\" can't to be null");
		}
		if (index_type == null) {
			throw new NullPointerException("\"index_type\" can't to be null");
		}
		if (filename == null) {
			throw new NullPointerException("\"filename\" can't to be null");
		}
		if (origin_key.length() == 0) {
			throw new NullPointerException("\"origin_key\" can't to be empty");
		}
		if (index_type.length() == 0) {
			throw new NullPointerException("\"index_type\" can't to be empty");
		}
		if (filename.length() == 0) {
			throw new NullPointerException("\"filename\" can't to be empty");
		}
		
		try {
			Client client = Elasticsearch.getClient();
			SearchRequestBuilder request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			request.setTypes(index_type);
			
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			query.must(QueryBuilders.termQuery("origin.key", origin_key));
			query.must(QueryBuilders.termQuery(METADATA_PROVIDER_TYPE, "renderer"));
			request.setQuery(query);
			SearchHit[] hits = request.execute().actionGet().getHits().hits();
			
			JSONParser parser = new JSONParser();
			JSONObject current_mtd;
			JSONArray current_content_list;
			JSONObject current_content;
			for (int pos_hit = 0; pos_hit < hits.length; pos_hit++) {
				parser.reset();
				current_mtd = (JSONObject) parser.parse(hits[pos_hit].getSourceAsString());
				if (current_mtd.containsKey("content") == false) {
					continue;
				}
				current_content_list = (JSONArray) current_mtd.get("content");
				for (int pos_content = 0; pos_content < current_content_list.size(); pos_content++) {
					current_content = (JSONObject) current_content_list.get(pos_content);
					if (((String) current_content.get("name")).equals(filename)) {
						// import_from_entry(RenderedContent content, String metadata_reference_id, boolean check_hash)//TODO use this instead this:
						// return RenderedFile.fromDatabase(current_content, hits[pos_hit].getId(), check_hash);
						return null;
					}
				}
				
			}
		} catch (IndexMissingException e) {
			return null;
		} catch (ParseException e) {
			Log2.log.error("Invalid ES response", e);
			// } catch (IOException e) {
			// Log2.log.error("Can't found valid file", e);
		}
		return null;
	}
	
	/**
	 * Database independant
	 */
	public static Container standaloneIndexing(File physical_source, SourcePathIndexerElement reference, List<FutureCreateTasks> current_create_task_list) throws Exception {
		Origin origin = Origin.fromSource(reference, physical_source);
		Container container = new Container(origin.getUniqueElementKey(), origin);
		EntrySummary entry_summary = new EntrySummary();
		container.addEntry(entry_summary);
		
		if (physical_source.length() == 0) {
			entry_summary.setMimetype("application/null");
		} else {
			entry_summary.setMimetype(MimeExtract.getMime(physical_source));
		}
		
		for (int pos = 0; pos < generatorAnalysers.size(); pos++) {
			GeneratorAnalyser generatorAnalyser = generatorAnalysers.get(pos);
			if (generatorAnalyser.canProcessThis(entry_summary.getMimetype())) {
				try {
					EntryAnalyser entry_analyser = generatorAnalyser.process(container);
					if (entry_analyser == null) {
						continue;
					}
					container.addEntry(entry_analyser);
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("analyser class", generatorAnalyser);
					dump.add("analyser name", generatorAnalyser.getLongName());
					dump.add("physical_source", physical_source);
					Log2.log.error("Can't analyst/render file", e, dump);
				}
			}
		}
		
		if (master_as_preview_mime_list_providers != null) {
			String mime = container.getSummary().getMimetype().toLowerCase();
			if (master_as_preview_mime_list_providers.containsKey(mime)) {
				entry_summary.master_as_preview = master_as_preview_mime_list_providers.get(mime).isCanUsedInMasterAsPreview(container);
			}
		}
		
		for (int pos = 0; pos < generatorRenderers.size(); pos++) {
			GeneratorRenderer generatorRenderer = generatorRenderers.get(pos);
			if (generatorRenderer.canProcessThis(entry_summary.getMimetype())) {
				try {
					EntryRenderer entry_renderer = generatorRenderer.process(container);
					if (generatorRenderer instanceof GeneratorRendererViaWorker) {
						GeneratorRendererViaWorker renderer_via_worker = (GeneratorRendererViaWorker) generatorRenderer;
						renderer_via_worker.prepareTasks(container, current_create_task_list);
					}
					if (entry_renderer == null) {
						continue;
					}
					container.addEntry(entry_renderer);
					RenderedFile.cleanCurrentTempDirectory();
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("provider class", generatorRenderer);
					dump.add("provider name", generatorRenderer.getLongName());
					dump.add("physical_source", physical_source);
					Log2.log.error("Can't analyst/render file", e, dump);
				}
			}
		}
		
		return container;
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
	
	public static String json_prettify(JSONArray json) {
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