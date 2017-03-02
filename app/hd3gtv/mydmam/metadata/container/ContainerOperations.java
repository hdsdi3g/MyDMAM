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
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.metadata.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.db.ElasticsearchMultiGetRequest;
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;
import hd3gtv.mydmam.db.ElastisearchMultipleCrawlerReader;
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.pathindexing.WebCacheInvalidation;
import hd3gtv.mydmam.transcode.images.ImageAttributes;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailerCartridge;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailerFullDisplay;
import hd3gtv.mydmam.transcode.images.ImageMagickThumbnailerIcon;
import hd3gtv.mydmam.transcode.mtdcontainer.BBCBmx;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegAudioDeepAnalyst;
import hd3gtv.mydmam.transcode.mtdcontainer.FFmpegInterlacingStats;
import hd3gtv.mydmam.transcode.mtdcontainer.FFprobe;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAlbumartwork;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegAudioDeepAnalyser;
import hd3gtv.mydmam.transcode.mtdgenerator.FFmpegSnapshot;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererAudio;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererHD;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererLQ;
import hd3gtv.mydmam.transcode.mtdgenerator.JobContextFFmpegLowresRendererSD;
import hd3gtv.tools.StoppableProcessing;

/**
 * Import and exports Container items from and to database.
 */
public class ContainerOperations {
	
	private static final String ES_INDEX = "metadata";
	
	public static JsonObject getJsonObject(JsonElement json, boolean can_null) throws JsonParseException {
		if (json.isJsonNull()) {
			if (can_null) {
				return null;
			} else {
				throw new JsonParseException("Json element is null");
			}
		}
		if (json.isJsonObject() == false) {
			throw new JsonParseException("Json element is not an object: " + json.toString());
		}
		return (JsonObject) json.getAsJsonObject();
	}
	
	private static final Map<String, Class<? extends ContainerEntry>> declared_entries_type;
	
	static {
		declared_entries_type = new LinkedHashMap<>();
		declared_entries_type.put(EntrySummary.ES_TYPE, EntrySummary.class);
		
		declared_entries_type.put(ImageAttributes.ES_TYPE, ImageAttributes.class);
		declared_entries_type.put(BBCBmx.ES_TYPE, BBCBmx.class);
		declared_entries_type.put(FFmpegAudioDeepAnalyst.ES_TYPE, FFmpegAudioDeepAnalyst.class);
		declared_entries_type.put(FFmpegInterlacingStats.ES_TYPE, FFmpegInterlacingStats.class);
		declared_entries_type.put(FFprobe.ES_TYPE, FFprobe.class);
		
		declared_entries_type.put(FFmpegSnapshot.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(FFmpegAudioDeepAnalyser.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(FFmpegAlbumartwork.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(JobContextFFmpegLowresRendererAudio.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(JobContextFFmpegLowresRendererHD.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(JobContextFFmpegLowresRendererLQ.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(JobContextFFmpegLowresRendererSD.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(ImageMagickThumbnailerCartridge.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(ImageMagickThumbnailerFullDisplay.ES_TYPE, EntryRenderer.class);
		declared_entries_type.put(ImageMagickThumbnailerIcon.ES_TYPE, EntryRenderer.class);
		
		/**
		 * Call MetadataCenter for run static block.
		 */
		MetadataCenter.getExtractors();
	}
	
	public static Container getByMtdKey(String mtd_key) throws Exception {
		if (mtd_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		Containers result = ContainerOperations.searchInMetadataBase(QueryBuilders.termQuery("_id", mtd_key));
		if (result.getAll().isEmpty()) {
			return null;
		} else {
			return result.getAll().get(0);
		}
	}
	
	/**
	 * Simple and light request.
	 */
	public static Container getByMtdKeyForOnlyOneType(String mtd_key, String type) throws NullPointerException {
		if (mtd_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		if (type == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		if (declared_entries_type.containsKey(type) == false) {
			throw new NullPointerException("Can't found type: " + type);
		}
		
		GetRequest request = new GetRequest(ES_INDEX);
		request.type(type);
		request.id(mtd_key);
		
		GetResponse getresponse = Elasticsearch.get(request);
		if (getresponse.isExists() == false) {
			return null;
		}
		
		ContainerEntry element = MyDMAM.gson_kit.getGson().fromJson(getresponse.getSourceAsString(), declared_entries_type.get(type).getClass());
		Container result = new Container(mtd_key, element.getOrigin());
		result.addEntry(element);
		return result;
	}
	
	/**
	 * Simple and light request. No deserializing.
	 */
	public static JsonObject getRawByMtdKeyForOnlyOneTypeAndCheckedToBeSendedToWebclients(String pathelement_key, String type) throws NullPointerException {
		if (pathelement_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		if (type == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		if (declared_entries_type.containsKey(type) == false) {
			throw new NullPointerException("Can't found type: " + type);
		}
		
		if ((declared_entries_type.get(type).equals(EntryAnalyser.class)) == false) {
			return null;
		}
		
		try {
			EntryAnalyser analyser = (EntryAnalyser) declared_entries_type.get(type).newInstance();
			if (analyser.canBeSendedToWebclients() == false) {
				return null;
			}
			
			ElastisearchCrawlerReader reader = Elasticsearch.createCrawlerReader();
			reader.setIndices(ES_INDEX);
			reader.setTypes(type);
			reader.setQuery(QueryBuilders.termQuery("origin.key", pathelement_key));
			reader.setMaximumSize(1);
			
			final ArrayList<JsonObject> results = new ArrayList<JsonObject>(1);
			try {
				reader.allReader(new ElastisearchCrawlerHit() {
					public boolean onFoundHit(SearchHit hit) throws Exception {
						results.add(Elasticsearch.getJSONFromSimpleResponse(hit));
						return false;
					}
				});
			} catch (Exception e) {
				Loggers.Metadata.warn("Can't get from db", e);
				return null;
			}
			
			if (results.isEmpty()) {
				return null;
			}
			return results.get(0);
		} catch (InstantiationException | IllegalAccessException e1) {
			throw new NullPointerException(e1.getMessage());
		}
	}
	
	public static Container getByPathIndexId(String pathelement_key) throws Exception {
		if (pathelement_key == null) {
			throw new NullPointerException("\"pathelement_key\" can't to be null");
		}
		Containers result = ContainerOperations.searchInMetadataBase(QueryBuilders.termQuery("origin.key", pathelement_key));
		if (result.getAll().isEmpty()) {
			return null;
		} else {
			return result.getAll().get(0);
		}
	}
	
	public static Container getByPathIndexIdOnlySummary(String pathelement_key) throws Exception {
		if (pathelement_key == null) {
			throw new NullPointerException("\"pathelement_key\" can't to be null");
		}
		Containers result = ContainerOperations.searchInMetadataBase(QueryBuilders.termQuery("origin.key", pathelement_key), EntrySummary.ES_TYPE);
		if (result.getAll().isEmpty()) {
			return null;
		} else {
			return result.getAll().get(0);
		}
	}
	
	public static Containers searchInMetadataBase(QueryBuilder query) throws Exception {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		return searchInMetadataBase(query, (String[]) null);
	}
	
	/**
	 * If some restric_to_specific_types are not declared in declared_entries_type: error
	 */
	private static void validateRestricSpecificTypes(ArrayList<String> unknow_types, String... restric_to_specific_types) {
		if (restric_to_specific_types != null) {
			if (restric_to_specific_types.length > 0) {
				String type;
				for (int pos = 0; pos < restric_to_specific_types.length; pos++) {
					type = restric_to_specific_types[pos];
					if (declared_entries_type.containsKey(type) == false) {
						if (unknow_types.contains(type) == false) {
							unknow_types.add(type);
						}
					}
				}
				if (unknow_types.isEmpty() == false) {
					Loggers.Metadata.error("Unknow types, list: " + unknow_types);
					throw new NullPointerException("Can't found some types");
				}
			}
		}
	}
	
	private static class HitReader implements ElastisearchCrawlerHit {
		ArrayList<String> unknow_types;
		Containers result;
		
		HitReader(Containers result, ArrayList<String> unknow_types) {
			this.unknow_types = unknow_types;
			this.result = result;
		}
		
		public boolean onFoundHit(SearchHit hit) {
			String type = hit.getType();
			if (declared_entries_type.containsKey(type) == false) {
				if (unknow_types.contains(type) == false) {
					unknow_types.add(type);
				}
				return true;
			}
			result.add(hit.getId(), MyDMAM.gson_kit.getGson().fromJson(hit.getSourceAsString(), declared_entries_type.get(type)));
			return true;
		}
		
		void onFoundGetResponse(GetResponse response) {
			String type = response.getType();
			if (declared_entries_type.containsKey(type) == false) {
				if (unknow_types.contains(type) == false) {
					unknow_types.add(type);
				}
				return;
			}
			result.add(response.getId(), MyDMAM.gson_kit.getGson().fromJson(response.getSourceAsString(), declared_entries_type.get(type)));
		}
	}
	
	public static Containers searchInMetadataBase(QueryBuilder query, final String... restric_to_specific_types) throws Exception {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		
		final ArrayList<String> unknow_types = new ArrayList<String>();
		
		ElastisearchCrawlerReader reader = Elasticsearch.createCrawlerReader();
		reader.setIndices(ES_INDEX);
		
		validateRestricSpecificTypes(unknow_types, restric_to_specific_types);
		if (restric_to_specific_types != null) {
			if (restric_to_specific_types.length > 0) {
				reader.setTypes(restric_to_specific_types);
			}
		}
		
		reader.setQuery(query);
		
		Containers result = new Containers();
		
		HitReader hr = new HitReader(result, unknow_types);
		
		reader.allReader(hr);
		
		if (unknow_types.isEmpty() == false) {
			Loggers.Metadata.error("Can't found some declared types retrieved by search, unknow_types: " + unknow_types);
		}
		return result;
	}
	
	public static Containers multipleSearchInMetadataBase(List<QueryBuilder> queries, int maxsize, final String... restric_to_specific_types) throws Exception {
		if (queries == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		if (queries.isEmpty()) {
			throw new NullPointerException("\"query\" can't to be empty");
		}
		if (maxsize < 1) {
			throw new IndexOutOfBoundsException("maxsize: " + maxsize);
		}
		
		final ArrayList<String> unknow_types = new ArrayList<String>();
		
		ElastisearchMultipleCrawlerReader multiple_reader = Elasticsearch.createMultipleCrawlerReader();
		multiple_reader.setDefaultIndices(ES_INDEX);
		
		validateRestricSpecificTypes(unknow_types, restric_to_specific_types);
		if (restric_to_specific_types != null) {
			if (restric_to_specific_types.length > 0) {
				multiple_reader.setDefaultTypes(restric_to_specific_types);
			}
		}
		
		multiple_reader.setDefaultMaxSize(maxsize);
		
		for (int pos = 0; pos < queries.size(); pos++) {
			multiple_reader.addNewQuery(queries.get(pos));
		}
		
		Containers result = new Containers();
		
		multiple_reader.allReader(new HitReader(result, unknow_types));
		
		if (unknow_types.isEmpty() == false) {
			Loggers.Metadata.error("Can't found some declared types retrieved by search, unknow_types: " + unknow_types);
		}
		return result;
	}
	
	/**
	 * Only create/update. No delete operations.
	 */
	public static void save(Container container, boolean refresh_index_after_save, ElasticsearchBulkOperation es_bulk) throws JsonIOException {
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		
		List<ContainerEntry> containerEntries = container.getEntries();
		ContainerEntry containerEntry;
		
		for (int pos = 0; pos < containerEntries.size(); pos++) {
			containerEntry = containerEntries.get(pos);
			IndexRequestBuilder index = es_bulk.getClient().prepareIndex(ES_INDEX, containerEntry.getES_Type(), container.getMtd_key());
			
			try {
				index.setSource(MyDMAM.gson_kit.getGson().toJson(containerEntry));
			} catch (Exception e) {
				/**
				 * Check serializators.
				 */
				Loggers.Metadata.error("Problem during serialization with " + containerEntry.getClass().getName(), e);
				return;
			}
			
			index.setRefresh(refresh_index_after_save);
			es_bulk.add(index);
		}
	}
	
	public static void requestDelete(Container container, ElasticsearchBulkOperation es_bulk) throws NullPointerException {
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		for (int pos = 0; pos < container.getEntries().size(); pos++) {
			es_bulk.add(es_bulk.getClient().prepareDelete(ES_INDEX, container.getEntries().get(pos).getES_Type(), container.getMtd_key()));
			// es_bulk.add(es_bulk.getClient().prepareDelete(ES_INDEX, type, mtd_key));
		}
	}
	
	public static RenderedFile getMetadataFile(String pathelement_key, String type, String filename, boolean check_hash) throws Exception {
		Containers containers = searchInMetadataBase(QueryBuilders.termQuery("origin.key", pathelement_key), type);
		
		EntryRenderer current;
		for (int pos = 0; pos < containers.size();) {
			if (containers.getItemAtPos(pos).getByType(type) instanceof EntryRenderer) {
				current = (EntryRenderer) containers.getItemAtPos(pos).getByType(type);
				RenderedContent content = current.getByFile(filename);
				if (content == null) {
					return null;
				}
				return RenderedFile.import_from_entry(content, containers.getItemAtPos(pos).getMtd_key(), check_hash);
			} else {
				/**
				 * Type problem : security protection.
				 */
				break;
			}
		}
		return null;
	}
	
	public static RenderedFile getMasterAsPreviewFile(String pathelement_key) throws Exception {
		Container container = getByPathIndexIdOnlySummary(pathelement_key);
		if (container == null) {
			return null;
		}
		if (container.getSummary() == null) {
			return null;
		}
		if (container.getSummary().master_as_preview == false) {
			return null;
		}
		if (container.getSummary().getMimetype() == null) {
			return null;
		}
		Explorer explorer = new Explorer();
		SourcePathIndexerElement spie = explorer.getelementByIdkey(pathelement_key);
		if (spie == null) {
			return null;
		}
		return RenderedFile.fromDatabaseMasterAsPreview(spie, container.getSummary().getMimetype());
	}
	
	private static class HitPurge implements ElastisearchCrawlerHit {
		Explorer explorer;
		HashMap<String, Long> elementcount_by_storage;
		ElasticsearchBulkOperation es_bulk;
		boolean let_clean_empty_and_removed_storages;
		
		HitPurge(ElasticsearchBulkOperation es_bulk, boolean let_clean_empty_and_removed_storages) {
			this.es_bulk = es_bulk;
			this.let_clean_empty_and_removed_storages = let_clean_empty_and_removed_storages;
			explorer = new Explorer();
			elementcount_by_storage = new HashMap<String, Long>();
		}
		
		public boolean onFoundHit(SearchHit hit) {
			
			JsonObject entry = Elasticsearch.getJSONFromSimpleResponse(hit);
			if (entry.has("origin")) {
				JsonObject origin = entry.get("origin").getAsJsonObject();
				
				/**
				 * Is item is actually indexed ?
				 */
				if (origin.has("key")) {
					String origin_key = origin.get("key").getAsString();
					if (explorer.getelementByIdkey(origin_key) != null) {
						return true;
					}
				} else {
					Loggers.Metadata.warn("Bad metadata origin entry founded during purge: " + hit.getId() + "/" + hit.getType() + "/" + hit.getSourceAsString());
				}
				
				/**
				 * Protect to no remove all mtd if pathindexing is empty for a storage...
				 * https://github.com/hdsdi3g/MyDMAM/issues/7
				 */
				if (origin.has("storage")) {
					/**
					 * ...but if the user want to remove it:
					 */
					if (let_clean_empty_and_removed_storages == false) {
						String origin_storage = origin.get("storage").getAsString();
						if (elementcount_by_storage.containsKey(origin_storage) == false) {
							elementcount_by_storage.put(origin_storage, explorer.countStorageContentElements(origin_storage));
							if (elementcount_by_storage.get(origin_storage) == 0) {
								Loggers.Metadata.info("Missing storage item in datatabase, storagename: " + origin_storage);
							}
						}
						if (elementcount_by_storage.get(origin_storage) == 0) {
							/**
							 * Empty storage !!
							 */
							return true;
						}
					}
				} else {
					Loggers.Metadata.warn("Bad metadata origin entry founded during purge: " + hit.getId() + "/" + hit.getType() + "/" + hit.getSourceAsString());
				}
			} else {
				Loggers.Metadata.warn("Bad metadata entry founded during purge: " + hit.getId() + "/" + hit.getType() + "/" + hit.getSourceAsString());
			}
			
			/**
			 * This storage is not empty and source file is really deleted, we can delete metadatas, and associated rendered files.
			 */
			if (Loggers.Metadata.isTraceEnabled()) {
				Loggers.Metadata.trace("Purge: request delete for " + hit.getId() + "/" + hit.getType());
			}
			
			es_bulk.add(es_bulk.getClient().prepareDelete(ES_INDEX, hit.getType(), hit.getId()));
			
			RenderedFile.purge(hit.getId());
			
			return true;
		}
	}
	
	/**
	 * Delete orphan (w/o pathindex) metadatas elements
	 */
	public static void purge_orphan_metadatas(boolean let_clean_empty_and_removed_storages) throws Exception {
		try {
			ElastisearchCrawlerReader reader = Elasticsearch.createCrawlerReader();
			reader.setIndices(ES_INDEX);
			reader.setQuery(QueryBuilders.matchAllQuery());
			
			ElasticsearchBulkOperation es_bulk = Elasticsearch.prepareBulk();
			
			HitPurge hit_purge = new HitPurge(es_bulk, let_clean_empty_and_removed_storages);
			reader.allReader(hit_purge);
			es_bulk.terminateBulk();
			
			Loggers.Metadata.info("Start cleaning rendered elements");
			
			RenderedFile.purge_orphan_metadatas_files();
			
		} catch (IndexMissingException ime) {
			Loggers.Metadata.warn("Can't purge orphan metadatas: " + ES_INDEX + " index is not present", ime);
		} catch (SearchPhaseExecutionException e) {
			Loggers.Metadata.warn("Can't purge orphan metadatas", e);
		}
	}
	
	/**
	 * Recursively
	 * @param from file or directory to copy or move metadatas
	 * @param root_dest
	 */
	public static void copyMoveMetadatas(SourcePathIndexerElement from, String dest_storage, String dest_parent_path, boolean copy, StoppableProcessing stoppable) throws Exception {
		Loggers.Metadata.debug("Prepare copy/move, from: " + from + ", dest_storage: " + dest_storage + ", dest_parent_path: " + dest_parent_path + ", copy: " + copy);
		
		Explorer explorer = new Explorer();
		CopyMoveMetadatas cmm;
		if (from.directory) {
			cmm = new CopyMoveMetadatas(from.currentpath, dest_storage, dest_parent_path, copy, stoppable);
			explorer.getAllSubElementsFromElementKey(from.prepare_key(), 0, cmm);
		} else {
			SourcePathIndexerElement from_root = explorer.getelementByIdkey(from.parentpath);
			cmm = new CopyMoveMetadatas(from_root.currentpath, dest_storage, dest_parent_path, copy, stoppable);
			cmm.onFoundElement(from);
		}
		
		if (copy == false) {
			WebCacheInvalidation.addInvalidation(from.storagename, dest_storage);
		} else {
			WebCacheInvalidation.addInvalidation(dest_storage);
		}
	}
	
	private static class CopyMoveMetadatas implements IndexingEvent {
		
		String root_from_currentpath;
		String dest_storage;
		String dest_parent_path;
		boolean copy;
		StoppableProcessing stoppable;
		
		CopyMoveMetadatas(String root_from_currentpath, String dest_storage, String dest_parent_path, boolean copy, StoppableProcessing stoppable) {
			this.root_from_currentpath = root_from_currentpath;
			this.dest_storage = dest_storage;
			this.dest_parent_path = dest_parent_path;
			this.copy = copy;
			this.stoppable = stoppable;
			
			Loggers.Metadata.debug(
					"Init CopyMoveMetadatas, root_from_currentpath: " + root_from_currentpath + ", dest_storage: " + dest_storage + ", dest_parent_path: " + dest_parent_path + ", copy: " + copy);
		}
		
		/**
		 * For each "from" elements, get Container
		 */
		public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
			Container container = getByPathIndexId(element.prepare_key());
			if (container == null) {
				return true;
			}
			
			ElasticsearchBulkOperation es_bulk = Elasticsearch.prepareBulk();
			if (copy == false) {
				requestDelete(container, es_bulk);
			}
			
			String mtd_key_source = container.getMtd_key();
			String dest_path = dest_parent_path + element.currentpath.substring(root_from_currentpath.length());
			
			LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
			log.put("source", element);
			log.put("dest_storage", dest_storage);
			log.put("dest_parent_path", dest_parent_path);
			log.put("dest_path", dest_path);
			log.put("mtd_key_source", mtd_key_source);
			log.put("mtd_key_dest", container.getMtd_key());
			log.put("root_from_currentpath", root_from_currentpath);
			Loggers.Metadata.debug("Copy/move mtd: " + log);
			
			/**
			 * Change origin for each entry
			 */
			ContainerOrigin new_origin = container.getOrigin().migrateOrigin(dest_storage, dest_path);
			container.changeAllOrigins(new_origin);
			
			/**
			 * rename/copy directories for each entry renderer
			 */
			if (container.hasRenderers()) {
				RenderedFile.copyMoveAllMetadataContent(mtd_key_source, container.getMtd_key(), copy);
			}
			
			container.save(es_bulk);
			es_bulk.terminateBulk();
			
			return stoppable.isWantToStopCurrentProcessing() == false;
		}
		
		public void onRemoveFile(String storagename, String path) throws Exception {
		}
		
	}
	
	public static Containers multipleGetInMetadataBase(List<String> mtd_keys, String... types) throws Exception {
		if (mtd_keys == null) {
			throw new NullPointerException("\"mtd_keys\" can't to be null");
		}
		if (mtd_keys.isEmpty()) {
			throw new NullPointerException("\"mtd_keys\" can't to be empty");
		}
		if (types == null) {
			throw new NullPointerException("\"type\" can't to be null");
		}
		if (types.length == 0) {
			throw new NullPointerException("\"type\" can't to be empty");
		}
		
		final ArrayList<String> unknow_types = new ArrayList<String>();
		
		ElasticsearchMultiGetRequest multiple_get = Elasticsearch.prepareMultiGetRequest();
		
		validateRestricSpecificTypes(unknow_types, types);
		
		for (int pos_types = 0; pos_types < types.length; pos_types++) {
			multiple_get.add(ES_INDEX, types[pos_types], mtd_keys);
		}
		
		Containers result = new Containers();
		
		List<GetResponse> get_responses = multiple_get.responses();
		HitReader hr = new HitReader(result, unknow_types);
		
		for (int pos_gresp = 0; pos_gresp < get_responses.size(); pos_gresp++) {
			hr.onFoundGetResponse(get_responses.get(pos_gresp));
		}
		
		if (unknow_types.isEmpty() == false) {
			Loggers.Metadata.error("Can't found some declared types retrieved by get, unknow_types: " + unknow_types);
		}
		return result;
	}
	
}
