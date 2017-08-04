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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;

import com.google.gson.JsonObject;

import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.assetsxcross.PathindexScanToESMatchingItem.NotMatchReason;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

@Deprecated
public class PathindexScanToES {
	private String es_index;
	private String es_type;
	private Explorer explorer;
	
	/**
	 * storage_name > spie.id >> es_index.es_type.key
	 */
	public PathindexScanToES(String es_index, String es_type) {
		this.es_index = es_index;
		if (es_index == null) {
			throw new NullPointerException("\"es_index\" can't to be null");
		}
		this.es_type = es_type;
		if (es_type == null) {
			throw new NullPointerException("\"es_type\" can't to be null");
		}
		explorer = new Explorer();
	}
	
	/**
	 * Gson will be used with match_handler_database_class for create T match handler
	 */
	public <T> void process(String storage_name, String root_path_index, PathindexScanToESMatchingItem<T> match_handler, Class<? extends T> match_handler_database_class) throws Exception {
		String start_root_key = SourcePathIndexerElement.prepare_key(storage_name, root_path_index);
		
		explorer.getAllSubElementsFromElementKey(start_root_key, 0, element -> {
			if (element.directory == false) {
				if (element.id == null) {
					match_handler.notMatch(element, NotMatchReason.NO_ID_FOR_ELEMENT);
					return true;
				}
				if (element.id.isEmpty()) {
					match_handler.notMatch(element, NotMatchReason.NO_ID_FOR_ELEMENT);
					return true;
				}
				
				GetResponse response = Elasticsearch.get(new GetRequest(es_index, es_type, element.id));
				if (response.isExists()) {
					if (response.isSourceEmpty() == false) {
						match_handler.match(element, MyDMAM.gson_kit.getGson().fromJson(response.getSourceAsString(), match_handler_database_class));
						return true;
					}
				}
				match_handler.notMatch(element, NotMatchReason.MISSING_IN_DATABASE);
			}
			return true;
		});
	}
	
	public void processToJsonObject(String storage_name, String root_path_index, PathindexScanToESMatchingItem<JsonObject> match_handler) throws Exception {
		process(storage_name, root_path_index, match_handler, JsonObject.class);
	}
	
}
