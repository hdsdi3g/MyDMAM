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

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest.OpType;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.support.replication.ReplicationType;
import org.elasticsearch.indices.IndexMissingException;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.assetsxcross.InterplayAPI.AssetType;
import hd3gtv.mydmam.db.Elasticsearch;

/**
 * Manage Internal ID for create MyDMAM Id in Interplay Db.
 * DON'T CREATE Ids IN A PARALLEL WAY AND/OR ON OTHER INSTANCE IN THE SAME TIME
 */
public class InternalId {
	
	public static final String FIRST_LETTER = Configuration.global.getValue("assetsxcross_internalid", "first_letter", "X");
	public static final int ID_SIZE = Configuration.global.getValue("assetsxcross_internalid", "id_size", 8);
	public static final String ES_INDEX = Configuration.global.getValue("assetsxcross_internalid", "es_index", "internalid");
	public static final String ES_TYPE = Configuration.global.getValue("assetsxcross_internalid", "es_type", "autogenerated");
	
	private InternalId() {
	}
	
	private transient String _id;
	public String name;
	private long created_date;
	public String mob_id;
	public String source_id;
	public String interplay_uri;
	public String interplay_full_path;
	public String tc_start;
	public String tc_duration;
	public String tracks;
	public String comment;
	public String path_referer;
	public String interplay_uri_referer;
	public AssetType type;
	
	public static InternalId create(InterplayAsset asset, String path_referer, String interplay_uri_referer) {
		InternalId result = new InternalId();
		result.name = asset.getDisplayName();
		result.created_date = System.currentTimeMillis();
		result.mob_id = asset.getMobID();
		result.source_id = asset.getSourceID();
		result.interplay_uri = asset.interplay_uri;
		result.interplay_full_path = asset.getPath();
		result.tc_start = asset.getStart();
		result.tc_duration = asset.getAttribute("Duration", "00:00:00:00");
		result.tracks = asset.getTracks();
		result.comment = asset.getAttribute("comment");
		result.type = asset.getType();
		result.path_referer = path_referer;
		result.interplay_uri_referer = interplay_uri_referer;
		result.save();
		return result;
	}
	
	public InternalId getFromId(String id) {
		GetResponse r = Elasticsearch.getClient().prepareGet(ES_INDEX, ES_TYPE, id).get();
		if (r.isExists() == false) {
			return null;
		}
		return MyDMAM.gson_kit.getGsonSimple().fromJson(r.getSourceAsString(), InternalId.class);
	}
	
	private void createAutogeneratedId() {
		int actual_size = 0;
		try {
			actual_size = (int) Elasticsearch.getClient().prepareCount(ES_INDEX).setTypes(ES_TYPE).execute().actionGet().getCount();
		} catch (IndexMissingException e) {
		}
		_id = FIRST_LETTER + StringUtils.leftPad(String.valueOf(actual_size), ID_SIZE - FIRST_LETTER.length(), "0");
	}
	
	/**
	 * Slower save, but high protected save.
	 */
	public void save() {
		IndexRequestBuilder create = Elasticsearch.getClient().prepareIndex(ES_INDEX, ES_TYPE, _id).setRefresh(true);
		create.setSource(MyDMAM.gson_kit.getGsonSimple().toJson(this));
		
		if (_id == null) {
			create.setCreate(true).setOpType(OpType.CREATE).setConsistencyLevel(WriteConsistencyLevel.ALL).setReplicationType(ReplicationType.SYNC);
			
			createAutogeneratedId();
			create.setId(_id);
			while (create.get().isCreated() == false) {
				createAutogeneratedId();
				create.setId(_id);
			}
		} else {
			create.get();
		}
	}
	
	public String getId() {
		return _id;
	}
	
	public long getCreatedDate() {
		return created_date;
	}
}
