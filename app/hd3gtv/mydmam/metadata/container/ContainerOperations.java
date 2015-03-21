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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
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
import hd3gtv.tools.GsonIgnoreStrategy;
import hd3gtv.tools.StoppableProcessing;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.search.SearchHit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

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
	
	private static final Map<String, ContainerEntry> declared_entries_type;
	private static final GsonBuilder gson_builder;
	private static volatile Gson gson;
	private static final Gson gson_simple;
	
	static {
		declared_entries_type = new LinkedHashMap<String, ContainerEntry>();
		gson_builder = new GsonBuilder();
		gson_builder.serializeNulls();
		
		GsonIgnoreStrategy ignore_strategy = new GsonIgnoreStrategy();
		gson_builder.addDeserializationExclusionStrategy(ignore_strategy);
		gson_builder.addSerializationExclusionStrategy(ignore_strategy);
		
		gson_simple = gson_builder.create();
		
		try {
			declareEntryType(EntrySummary.class);
		} catch (Exception e) {
			Log2.log.error("Can't declare (de)serializer for EntrySummary", e);
		}
		gson_builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Serializer());
		gson_builder.registerTypeAdapter(ContainerPreview.class, new ContainerPreview.Deserializer());
		
		/**
		 * Call MetadataCenter for run static block.
		 */
		MetadataCenter.getAnalysers();
	}
	
	public static void setGsonPrettyPrinting() {
		gson_builder.setPrettyPrinting();
		gson = gson_builder.create();
	}
	
	public static GsonBuilder getGsonBuilder() {
		return gson_builder;
	}
	
	public static Gson getGsonSimple() {
		return gson_simple;
	}
	
	/**
	 * With all declared (de)serializers.
	 */
	public static Gson getGson() {
		return gson;
	}
	
	public synchronized static void declareEntryType(Class<? extends ContainerEntry> entry_class) throws NullPointerException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (entry_class == null) {
			throw new NullPointerException("\"entry_class\" can't to be null");
		}
		ContainerEntry containerEntry = (ContainerEntry) declareType(entry_class);
		declared_entries_type.put(containerEntry.getES_Type(), containerEntry);
		
		List<Class<? extends SelfSerializing>> dependencies = containerEntry.getSerializationDependencies();
		if (dependencies != null) {
			for (int pos = 0; pos < dependencies.size(); pos++) {
				declareType(dependencies.get(pos));
			}
		}
		gson = gson_builder.create();
	}
	
	public synchronized static void declareSelfSerializingType(Class<? extends SelfSerializing> selfserializing_class) throws NullPointerException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (selfserializing_class == null) {
			throw new NullPointerException("\"selfserializing_class\" can't to be null");
		}
		declareType(selfserializing_class);
		gson = gson_builder.create();
	}
	
	private synchronized static SelfSerializing declareType(Class<? extends SelfSerializing> selfserializing_class) throws NullPointerException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		if (selfserializing_class == null) {
			throw new NullPointerException("\"selfserializing_class\" can't to be null");
		}
		SelfSerializing instance = selfserializing_class.getConstructor().newInstance();
		SelfSerialiserBridge.registerInstance(instance);
		return instance;
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
		
		ContainerEntry element = gson.fromJson(getresponse.getSourceAsString(), declared_entries_type.get(type).getClass());
		Container result = new Container(mtd_key, element.getOrigin());
		result.addEntry(element);
		return result;
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
		Containers result = ContainerOperations.searchInMetadataBase(QueryBuilders.termQuery("origin.key", pathelement_key), EntrySummary.type);
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
					Log2Dump dump = new Log2Dump();
					dump.add("list", unknow_types);
					Log2.log.error("Unknow types", null, dump);
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
			result.add(hit.getId(), gson.fromJson(hit.getSourceAsString(), declared_entries_type.get(type).getClass()));
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
			result.add(response.getId(), gson.fromJson(response.getSourceAsString(), declared_entries_type.get(type).getClass()));
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
		
		reader.allReader(new HitReader(result, unknow_types));
		
		if (unknow_types.isEmpty() == false) {
			Log2Dump dump = new Log2Dump();
			dump.add("unknow_types", unknow_types);
			Log2.log.error("Can't found some declared types retrieved by search", null, dump);
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
			Log2Dump dump = new Log2Dump();
			dump.add("unknow_types", unknow_types);
			Log2.log.error("Can't found some declared types retrieved by search", null, dump);
		}
		return result;
	}
	
	/**
	 * Only create/update. No delete operations.
	 */
	public static void save(Container container, boolean refresh_index_after_save, ElasticsearchBulkOperation es_bulk) {
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		List<ContainerEntry> containerEntries = container.getEntries();
		ContainerEntry containerEntry;
		
		for (int pos = 0; pos < containerEntries.size(); pos++) {
			containerEntry = containerEntries.get(pos);
			IndexRequestBuilder index = es_bulk.getClient().prepareIndex(ES_INDEX, containerEntry.getES_Type(), container.getMtd_key());
			index.setSource(gson.toJson(containerEntry));
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
		
		HitPurge(ElasticsearchBulkOperation es_bulk) {
			this.es_bulk = es_bulk;
			explorer = new Explorer();
			elementcount_by_storage = new HashMap<String, Long>();
		}
		
		/**
		 * Protect to no remove all mtd if pathindexing is empty for a storage.
		 * https://github.com/hdsdi3g/MyDMAM/issues/7
		 */
		boolean containsStorageInBase(ContainerOrigin origin) {
			if (elementcount_by_storage.containsKey(origin.storage) == false) {
				elementcount_by_storage.put(origin.storage, explorer.countStorageContentElements(origin.storage));
				if (elementcount_by_storage.get(origin.storage) == 0) {
					Log2.log.info("Missing storage item in datatabase", new Log2Dump("storagename", origin.storage));
				}
			}
			return elementcount_by_storage.get(origin.storage) > 0;
		}
		
		public boolean onFoundHit(SearchHit hit) {
			if (declared_entries_type.containsKey(hit.getType()) == false) {
				return true;
			}
			ContainerEntry containerEntry = gson.fromJson(hit.getSourceAsString(), declared_entries_type.get(hit.getType()).getClass());
			Container container = new Container(hit.getId(), containerEntry.getOrigin());
			
			try {
				container.getOrigin().getPathindexElement();
				return true;
			} catch (FileNotFoundException e) {
			}
			
			if (containsStorageInBase(container.getOrigin()) == false) {
				return true;
			}
			/**
			 * This storage is not empty... Source file is really deleted, we can delete metadatas, and associated rendered files.
			 */
			requestDelete(container, es_bulk);
			RenderedFile.purge(container.getMtd_key());
			
			return true;
		}
	}
	
	/**
	 * Delete orphan (w/o pathindex) metadatas elements
	 */
	public static void purge_orphan_metadatas() throws Exception {
		try {
			ElastisearchCrawlerReader reader = Elasticsearch.createCrawlerReader();
			reader.setIndices(ES_INDEX);
			reader.setQuery(QueryBuilders.matchAllQuery());
			
			ElasticsearchBulkOperation es_bulk = Elasticsearch.prepareBulk();
			
			HitPurge hit_purge = new HitPurge(es_bulk);
			reader.allReader(hit_purge);
			es_bulk.terminateBulk();
			
			Log2.log.info("Start cleaning rendered elements");
			
			RenderedFile.purge_orphan_metadatas_files();
			
		} catch (IndexMissingException ime) {
		}
	}
	
	/**
	 * Recursively
	 * @param from file or directory to copy or move metadatas
	 * @param root_dest
	 */
	public static void copyMoveMetadatas(SourcePathIndexerElement from, String dest_storage, String dest_parent_path, boolean copy, StoppableProcessing stoppable) throws Exception {
		Log2Dump dump = new Log2Dump();
		dump.add("from", from);
		dump.add("dest_storage", dest_storage);
		dump.add("dest_path", dest_parent_path);
		dump.add("copy", copy);
		Log2.log.debug("Prepare copy/move", dump);
		
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
			
			Log2Dump dump = new Log2Dump();
			dump.add("root_from_currentpath", root_from_currentpath);
			dump.add("dest_storage", dest_storage);
			dump.add("dest_parent_path", dest_parent_path);
			dump.add("copy", copy);
			Log2.log.debug("Init CopyMoveMetadatas", dump);
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
			
			Log2Dump dump = new Log2Dump();
			dump.add("source", element);
			dump.add("dest_storage", dest_storage);
			dump.add("dest_parent_path", dest_parent_path);
			dump.add("dest_path", dest_path);
			dump.add("mtd_key_source", mtd_key_source);
			dump.add("mtd_key_dest", container.getMtd_key());
			dump.add("root_from_currentpath", root_from_currentpath);
			Log2.log.debug("Copy/move mtd", dump);
			
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
			Log2Dump dump = new Log2Dump();
			dump.add("unknow_types", unknow_types);
			Log2.log.error("Can't found some declared types retrieved by get", null, dump);
		}
		return result;
	}
}
