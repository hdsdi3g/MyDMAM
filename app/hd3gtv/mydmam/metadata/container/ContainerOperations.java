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
import hd3gtv.mydmam.metadata.MetadataCenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
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
	// TODO export to ES (all entries -> Index)
	// TODO "search" by Entry Type
	
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
	
	private static final Map<String, EntryBase> declared_entries_type;
	private static final GsonBuilder gson_builder;
	static volatile Gson gson;
	private static Client client;
	
	static {
		client = Elasticsearch.getClient();
		declared_entries_type = new LinkedHashMap<String, EntryBase>();
		gson_builder = new GsonBuilder();
		gson_builder.setPrettyPrinting();// TODO remove this after tests
		gson_builder.serializeNulls();
	}
	
	public static GsonBuilder getGsonBuilder() {
		return gson_builder;
	}
	
	public synchronized static void declareEntryType(EntryBase entry) throws NullPointerException {
		if (entry == null) {
			throw new NullPointerException("\"serialiser\" can't to be null");
		}
		declared_entries_type.put(entry.getESType(), entry);
		entry.createEntrySerialiserBridge();
	}
	
	public static Container getByMtdId(String mtd_id) {
		if (mtd_id == null) {
			throw new NullPointerException("\"mtd_id\" can't to be null");
		}
		Containers result = ContainerOperations.searchInMetadataBase(QueryBuilders.termQuery("_id", mtd_id));
		if (result.getAll().isEmpty()) {
			return null;
		} else {
			return result.getAll().get(0);
		}
	}
	
	public static Containers searchInMetadataBase(QueryBuilder query) {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		return searchInMetadataBase(query, (String[]) null);
	}
	
	public static Containers searchInMetadataBase(QueryBuilder query, final String... restric_to_specific_types) {
		if (query == null) {
			throw new NullPointerException("\"query\" can't to be null");
		}
		
		final ArrayList<String> unknow_types = new ArrayList<String>();
		
		ElastisearchCrawlerReader reader = Elasticsearch.createCrawlerReader();
		reader.setIndices(MetadataCenter.ES_INDEX);
		
		/**
		 * If some restric_to_specific_types are not declared in declared_entries_type: error
		 */
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
				reader.setTypes(restric_to_specific_types);
			}
		}
		
		reader.setQuery(query);
		
		final Containers result = new Containers();
		
		if (gson == null) {
			gson = gson_builder.create();
		}
		
		reader.allReader(new ElastisearchCrawlerHit() {
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
		});
		
		if (unknow_types.isEmpty() == false) {
			Log2Dump dump = new Log2Dump();
			dump.add("unknow_types", unknow_types);
			Log2.log.error("Can't found some declared types retrieved by search", null, dump);
		}
		return result;
	}
}
