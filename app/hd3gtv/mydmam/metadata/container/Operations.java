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
import hd3gtv.mydmam.db.ElastisearchCrawlerHit;
import hd3gtv.mydmam.db.ElastisearchCrawlerReader;
import hd3gtv.mydmam.db.ElastisearchMultipleCrawlerReader;
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.RenderedFile;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.GsonIgnoreStrategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
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
public class Operations {
	
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
	
	private static final Map<String, Entry> declared_entries_type;
	private static final GsonBuilder gson_builder;
	private static volatile Gson gson;
	private static final Gson gson_simple;
	private static Client client;
	
	static {
		client = Elasticsearch.getClient();
		declared_entries_type = new LinkedHashMap<String, Entry>();
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
		gson_builder.registerTypeAdapter(Preview.class, new Preview.Serializer());
		gson_builder.registerTypeAdapter(Preview.class, new Preview.Deserializer());
		
		/**
		 * Call MetadataCenter for run static block.
		 */
		MetadataCenter.doNothing();
	}
	
	public static void setGsonPrettyPrinting() {
		gson_builder.setPrettyPrinting();
		gson = gson_builder.create();
	}
	
	static Client getClient() {
		return client;
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
	
	public synchronized static void declareEntryType(Class<? extends Entry> entry_class) throws NullPointerException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		if (entry_class == null) {
			throw new NullPointerException("\"entry_class\" can't to be null");
		}
		Entry entry = (Entry) declareType(entry_class);
		declared_entries_type.put(entry.getES_Type(), entry);
		
		List<Class<? extends SelfSerializing>> dependencies = entry.getSerializationDependencies();
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
	
	public static Container getByMtdKey(String mtd_key) throws NullPointerException {
		if (mtd_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		Containers result = Operations.searchInMetadataBase(QueryBuilders.termQuery("_id", mtd_key));
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
		
		GetResponse getresponse = client.get(request).actionGet();
		if (getresponse.isExists() == false) {
			return null;
		}
		
		Entry element = gson.fromJson(getresponse.getSourceAsString(), declared_entries_type.get(type).getClass());
		Container result = new Container(mtd_key, element.getOrigin());
		result.addEntry(element);
		return result;
	}
	
	public static Container getByPathIndexId(String pathelement_key) {
		if (pathelement_key == null) {
			throw new NullPointerException("\"pathelement_key\" can't to be null");
		}
		Containers result = Operations.searchInMetadataBase(QueryBuilders.termQuery("origin.key", pathelement_key));
		if (result.getAll().isEmpty()) {
			return null;
		} else {
			return result.getAll().get(0);
		}
	}
	
	public static Container getByPathIndexIdOnlySummary(String pathelement_key) {
		if (pathelement_key == null) {
			throw new NullPointerException("\"pathelement_key\" can't to be null");
		}
		Containers result = Operations.searchInMetadataBase(QueryBuilders.termQuery("origin.key", pathelement_key), EntrySummary.type);
		if (result.getAll().isEmpty()) {
			return null;
		} else {
			return result.getAll().get(0);
		}
	}
	
	public static Containers getByPathIndexId(List<String> pathelement_keys, boolean only_summaries) {
		if (pathelement_keys == null) {
			throw new NullPointerException("pathelement_keys");
		}
		if (pathelement_keys.isEmpty()) {
			throw new NullPointerException("pathelement_keys is empty");
		}
		ArrayList<QueryBuilder> queries = new ArrayList<QueryBuilder>();
		for (int pos = 0; pos < pathelement_keys.size(); pos++) {
			queries.add(QueryBuilders.termQuery("origin.key", pathelement_keys.get(pos)));
		}
		
		if (only_summaries) {
			return multipleSearchInMetadataBase(queries, 1, EntrySummary.type);
		} else {
			return multipleSearchInMetadataBase(queries, 1000, (String[]) null);
		}
	}
	
	public static Containers getByPathIndex(List<SourcePathIndexerElement> pathelements, boolean only_summaries) {
		if (pathelements == null) {
			throw new NullPointerException("pathelements");
		}
		if (pathelements.isEmpty()) {
			throw new NullPointerException("pathelements is empty");
		}
		ArrayList<QueryBuilder> queries = new ArrayList<QueryBuilder>();
		for (int pos = 0; pos < pathelements.size(); pos++) {
			queries.add(QueryBuilders.termQuery("_id", Origin.getUniqueElementKey(pathelements.get(pos))));
		}
		/*	MultiGetRequestBuilder multigetrequestbuilder = new MultiGetRequestBuilder(client);
			multigetrequestbuilder.add(ES_INDEX, EntrySummary.type, Origin.getUniqueElementKey(pathelements.get(pos)));
			MultiGetItemResponse[] response = multigetrequestbuilder.execute().actionGet().getResponses();
		*/
		
		if (only_summaries) {
			return multipleSearchInMetadataBase(queries, 1, EntrySummary.type);
		} else {
			return multipleSearchInMetadataBase(queries, 1000, (String[]) null);
		}
	}
	
	public static Containers searchInMetadataBase(QueryBuilder query) {
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
	}
	
	public static Containers searchInMetadataBase(QueryBuilder query, final String... restric_to_specific_types) {
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
	
	public static Containers multipleSearchInMetadataBase(List<QueryBuilder> queries, int maxsize, final String... restric_to_specific_types) {
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
	public static void save(Container container, boolean refresh_index_after_save, BulkRequestBuilder bulkrequest) {
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		List<Entry> entries = container.getEntries();
		Entry entry;
		
		for (int pos = 0; pos < entries.size(); pos++) {
			entry = entries.get(pos);
			IndexRequestBuilder index = client.prepareIndex(ES_INDEX, entry.getES_Type(), container.getMtd_key());
			index.setSource(gson.toJson(entry));
			index.setRefresh(refresh_index_after_save);
			bulkrequest.add(index);
		}
	}
	
	/**
	 * Only create/update. No delete operations.
	 */
	public static void save(Containers containers, boolean refresh_index_after_save, BulkRequestBuilder bulkrequest) {
		if (containers == null) {
			throw new NullPointerException("\"containers\" can't to be null");
		}
		Container container;
		for (int pos_container = 0; pos_container < containers.getAll().size(); pos_container++) {
			container = containers.getAll().get(pos_container);
			save(container, refresh_index_after_save, bulkrequest);
		}
	}
	
	// Type typeOfT = new TypeToken<List<EntrySummary>>() {}.getType();
	
	public static void requestDelete(Container container, BulkRequestBuilder bulkrequest) throws NullPointerException {
		if (container == null) {
			throw new NullPointerException("\"container\" can't to be null");
		}
		for (int pos = 0; pos < container.getEntries().size(); pos++) {
			bulkrequest.add(client.prepareDelete(ES_INDEX, container.getEntries().get(pos).getES_Type(), container.getMtd_key()));
		}
		
	}
	
	public static void requestDelete(String mtd_key, String type, BulkRequestBuilder bulkrequest) throws NullPointerException {
		if (mtd_key == null) {
			throw new NullPointerException("\"mtd_key\" can't to be null");
		}
		bulkrequest.add(client.prepareDelete(ES_INDEX, type, mtd_key));
	}
	
	public static RenderedFile getMetadataFile(String pathelement_key, String type, String filename, boolean check_hash) throws IOException {
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
	
	public static RenderedFile getMasterAsPreviewFile(String pathelement_key) throws IOException {
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
		BulkRequestBuilder bulkrequest;
		
		HitPurge(BulkRequestBuilder bulkrequest) {
			this.bulkrequest = bulkrequest;
			explorer = new Explorer();
			elementcount_by_storage = new HashMap<String, Long>();
		}
		
		boolean containsStorageInBase(Origin origin) {
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
			Entry entry = gson.fromJson(hit.getSourceAsString(), declared_entries_type.get(hit.getType()).getClass());
			Container container = new Container(hit.getId(), entry.getOrigin());
			
			try {
				container.getOrigin().getPathindexElement();
				return true;
			} catch (FileNotFoundException e) {
			}
			
			/**
			 * Protect to no remove all mtd if pathindexing is empty for a storage.
			 * https://github.com/hdsdi3g/MyDMAM/issues/7
			 */
			if (containsStorageInBase(container.getOrigin()) == false) {
				return true;
			}
			/**
			 * This storage is not empty... Source file is really deleted, we can delete metadatas, and associated rendered files.
			 */
			requestDelete(container, bulkrequest);
			RenderedFile.purge(container.getMtd_key());
			
			return true;
		}
	}
	
	/**
	 * Delete orphan (w/o pathindex) metadatas elements
	 */
	public static void purge_orphan_metadatas() throws IOException {
		try {
			ElastisearchCrawlerReader reader = Elasticsearch.createCrawlerReader();
			reader.setIndices(ES_INDEX);
			reader.setQuery(QueryBuilders.matchAllQuery());
			
			BulkRequestBuilder bulkrequest = new BulkRequestBuilder(client);
			HitPurge hit_purge = new HitPurge(bulkrequest);
			reader.allReader(hit_purge);
			
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
			
			RenderedFile.purge_orphan_metadatas_files();
		} catch (IndexMissingException ime) {
		}
	}
	
}
