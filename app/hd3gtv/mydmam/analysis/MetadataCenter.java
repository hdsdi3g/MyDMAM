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
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.Importer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.transcode.FFmpegLowresRenderer;
import hd3gtv.mydmam.transcode.FFmpegSnapshoot;
import hd3gtv.mydmam.transcode.FFprobeAnalyser;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class MetadataCenter implements CliModule {
	
	public static final String ES_INDEX = "metadata";
	public static final String ES_TYPE_SUMMARY = "summary";
	
	public static final String METADATA_PROVIDER_TYPE = "metadata-provider-type";
	public static final String MASTER_AS_PREVIEW = "master_as_preview";
	
	private LinkedHashMap<String, Analyser> analysers;
	private LinkedHashMap<String, Renderer> renderers;
	private MasterAsPreviewProvider master_as_preview;
	
	private volatile List<MetadataCenterIndexer> analysis_indexers;
	
	public MetadataCenter() {
		analysers = new LinkedHashMap<String, Analyser>();
		renderers = new LinkedHashMap<String, Renderer>();
		master_as_preview = new MasterAsPreviewProvider();
		
		addProvider(new FFprobeAnalyser());
		addProvider(new FFmpegSnapshoot());
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.transcode_profile_ffmpeg_lowres_lq, PreviewType.video_lq_pvw, false));
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.transcode_profile_ffmpeg_lowres_sd, PreviewType.video_sd_pvw, false));
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.transcode_profile_ffmpeg_lowres_hd, PreviewType.video_hd_pvw, false));
		addProvider(new FFmpegLowresRenderer(FFmpegLowresRenderer.transcode_profile_ffmpeg_lowres_audio, PreviewType.audio_pvw, true));
		
		analysis_indexers = new ArrayList<MetadataCenterIndexer>();
	}
	
	private void addProvider(Analyser analyser) {
		if (analyser == null) {
			throw new NullPointerException("\"analyser\" can't to be null");
		}
		if (analyser.isEnabled()) {
			if (analysers.containsKey(analyser.getElasticSearchIndexType())) {
				Log2Dump dump = new Log2Dump();
				dump.add("this", analyser);
				dump.add("previous", analysers.get(analyser.getElasticSearchIndexType()));
				Log2.log.info("Provider with this name exists", dump);
			} else {
				analysers.put(analyser.getElasticSearchIndexType(), analyser);
				master_as_preview.addAnalyser(analyser);
			}
		} else {
			Log2.log.info("Analyser " + analyser.getElasticSearchIndexType() + " is disabled");
		}
	}
	
	private void addProvider(Renderer renderer) {
		if (renderer == null) {
			throw new NullPointerException("\"renderer\" can't to be null");
		}
		if (renderer.isEnabled()) {
			if (renderers.containsKey(renderer.getElasticSearchIndexType())) {
				Log2Dump dump = new Log2Dump();
				dump.add("this", renderer);
				dump.add("previous", renderers.get(renderer.getElasticSearchIndexType()));
				Log2.log.info("Provider with this name exists", dump);
			} else {
				renderers.put(renderer.getElasticSearchIndexType(), renderer);
			}
		} else {
			Log2.log.info("Renderer " + renderer.getElasticSearchIndexType() + " is disabled");
		}
	}
	
	synchronized LinkedHashMap<String, Renderer> getRenderers() {
		return renderers;
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
		analysis_indexers.add(metadataCenterIndexer);
		metadataCenterIndexer.process(storagename, currentpath, min_index_date);
		analysis_indexers.remove(metadataCenterIndexer);
	}
	
	public void stopAnalysis() {
		for (int pos = 0; pos < analysis_indexers.size(); pos++) {
			analysis_indexers.get(pos).stop();
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
						RenderedElement.purge(hits[pos].getId());
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
			
			RenderedElement.gc(client);
			
		} catch (IndexMissingException ime) {
			Log2.log.info("No metadatas exists in database, no clean to do");
		}
	}
	
	/**
	 * Beware: "origin" key is deleted
	 */
	public static JSONObject getSummaryMetadatas(Client client, SourcePathIndexerElement element) throws IndexMissingException {
		if (element == null) {
			throw new NullPointerException("\"pathelementskeys\" can't to be null");
		}
		try {
			GetResponse response = null;
			response = client.get(new GetRequest(ES_INDEX, ES_TYPE_SUMMARY, MetadataCenterIndexer.getUniqueElementKey(element))).actionGet();
			
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
	
	/**
	 * Beware: "origin" key is deleted
	 */
	public static JSONObject getSummaryMetadatas(Client client, String[] pathelementskeys) throws IndexMissingException {
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
			request.setTypes(ES_TYPE_SUMMARY);
			
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
	
	public static RenderedElement getMasterAsPreviewFile(Client client, String origin_key) throws IndexMissingException {
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
		if (origin_key == null) {
			throw new NullPointerException("\"origin_key\" can't to be null");
		}
		
		try {
			SearchRequestBuilder request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			request.setTypes(ES_TYPE_SUMMARY);
			
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
			
			Explorer explorer = new Explorer(client);
			SourcePathIndexerElement spie = explorer.getelementByIdkey(origin_key);
			if (spie == null) {
				return null;
			}
			
			return RenderedElement.fromDatabaseMasterAsPreview(spie, (String) current_mtd.get("mimetype"));
		} catch (IndexMissingException e) {
			return null;
		} catch (ParseException e) {
			Log2.log.error("Invalid ES response", e);
		} catch (IOException e) {
			Log2.log.error("Can't found valid file", e);
		}
		return null;
	}
	
	public static RenderedElement getMetadataFileReference(Client client, String origin_key, String index_type, String filename, boolean check_hash) throws IndexMissingException {
		if (client == null) {
			throw new NullPointerException("\"client\" can't to be null");
		}
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
			SearchRequestBuilder request = client.prepareSearch();
			request.setIndices(ES_INDEX);
			request.setTypes(index_type);
			
			BoolQueryBuilder query = QueryBuilders.boolQuery();
			query.must(QueryBuilders.termQuery("origin.key", origin_key));
			query.must(QueryBuilders.termQuery(METADATA_PROVIDER_TYPE, Renderer.METADATA_PROVIDER_RENDERER));
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
						return RenderedElement.fromDatabase(current_content, hits[pos_hit].getId(), check_hash);
					}
				}
				
			}
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
	 * Database independant
	 */
	public MetadataIndexerResult standaloneIndexing(File physical_source, SourcePathIndexerElement reference) throws Exception {
		MetadataIndexerResult indexing_result = new MetadataIndexerResult(reference);
		indexing_result.origin = physical_source;
		
		if (physical_source.length() == 0) {
			indexing_result.mimetype = "application/null";
		} else {
			indexing_result.mimetype = MimeExtract.getMime(physical_source);
		}
		
		for (Map.Entry<String, Analyser> entry : analysers.entrySet()) {
			Analyser analyser = entry.getValue();
			if (analyser.canProcessThis(indexing_result.mimetype)) {
				try {
					JSONObject jo_processing_result = analyser.process(indexing_result);
					if (jo_processing_result == null) {
						continue;
					}
					if (jo_processing_result.isEmpty()) {
						continue;
					}
					jo_processing_result.put(METADATA_PROVIDER_TYPE, Analyser.METADATA_PROVIDER_ANALYSER);
					indexing_result.analysis_results.put(analyser, jo_processing_result);
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("analyser class", analyser);
					dump.add("analyser name", analyser.getLongName());
					dump.add("physical_source", physical_source);
					Log2.log.error("Can't analyst/render file", e, dump);
				}
			}
		}
		
		indexing_result.master_as_preview = master_as_preview.isFileIsValidForMasterAsPreview(indexing_result);
		
		for (Map.Entry<String, Renderer> entry : renderers.entrySet()) {
			Renderer renderer = entry.getValue();
			if (renderer.canProcessThis(indexing_result.mimetype)) {
				try {
					List<RenderedElement> renderedelements = renderer.process(indexing_result);
					if (renderedelements == null) {
						continue;
					}
					if (renderedelements.size() == 0) {
						continue;
					}
					for (int pos = 0; pos < renderedelements.size(); pos++) {
						renderedelements.get(pos).consolidate(reference, renderer);
					}
					indexing_result.rendering_results.put(renderer, renderedelements);
					RenderedElement.cleanCurrentTempDirectory();
				} catch (Exception e) {
					Log2Dump dump = new Log2Dump();
					dump.add("provider class", renderer);
					dump.add("provider name", renderer.getLongName());
					dump.add("physical_source", physical_source);
					Log2.log.error("Can't analyst/render file", e, dump);
				}
			}
		}
		
		return indexing_result;
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
			
			MetadataIndexerResult result;
			File[] files = dir_testformats.listFiles();
			for (int pos = 0; pos < files.length; pos++) {
				if (files[pos].isDirectory()) {
					continue;
				}
				if (files[pos].isHidden()) {
					continue;
				}
				result = standaloneIndexing(files[pos], spie);
				System.out.print(result.origin);
				System.out.print("\t");
				System.out.print(result.mimetype);
				System.out.print("\t");
				if (result.master_as_preview) {
					System.out.print("MasterAsPreview");
				}
				System.out.print("\t");
				if ((result.analysis_results != null) & (verbose | prettify)) {
					for (Map.Entry<Analyser, JSONObject> entry : result.analysis_results.entrySet()) {
						System.out.println();
						System.out.print("\t\t");
						System.out.print(entry.getKey().getLongName());
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
				
				if (verbose | prettify) {
					LinkedHashMap<Renderer, JSONArray> rendering_results = result.makeJSONRendering_results();
					if (rendering_results != null) {
						for (Map.Entry<Renderer, JSONArray> entry : rendering_results.entrySet()) {
							System.out.println();
							System.out.print("\t\t");
							System.out.print(entry.getKey().getLongName());
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