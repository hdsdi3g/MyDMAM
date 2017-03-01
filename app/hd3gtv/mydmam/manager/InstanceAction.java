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
package hd3gtv.mydmam.manager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.gson.GsonIgnore;

public final class InstanceAction {
	
	private static final int TTL = 5 * 60;
	
	private static final ColumnFamily<String, String> CF_ACTION = new ColumnFamily<String, String>("mgrAction", StringSerializer.get(), StringSerializer.get());
	private static Keyspace keyspace;
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_ACTION.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_ACTION.getName(), false);
			}
		} catch (Exception e) {
			Loggers.Manager.error("Can't init database CFs", e);
		}
	}
	
	private String key;
	private String target_class_name;
	private String target_reference_key;
	private @GsonIgnore JsonObject order;
	private String caller;
	private long created_at;
	
	/**
	 * Used for de/serializer.
	 */
	private InstanceAction() {
	}
	
	/**
	 * Used for create a new instance action and publish it.
	 */
	public static void addNew(String target_class_name, String target_reference_key, JsonObject order, String caller) throws ConnectionException {
		InstanceAction new_instance_action = new InstanceAction();
		new_instance_action.target_class_name = target_class_name;
		if (target_class_name == null) {
			throw new NullPointerException("\"target_class_name\" can't to be null");
		}
		new_instance_action.target_reference_key = target_reference_key;
		if (target_reference_key == null) {
			throw new NullPointerException("\"target_reference_key\" can't to be null");
		}
		new_instance_action.order = order;
		if (order == null) {
			throw new NullPointerException("\"order\" can't to be null");
		}
		new_instance_action.caller = caller;
		if (caller == null) {
			throw new NullPointerException("\"caller\" can't to be null");
		}
		new_instance_action.key = UUID.randomUUID().toString();
		new_instance_action.created_at = System.currentTimeMillis();
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		mutator.withRow(CF_ACTION, new_instance_action.key).putColumn("source", MyDMAM.gson_kit.getGson().toJson(new_instance_action), TTL);
		mutator.execute();
		
		Loggers.Manager.info("Create manager action:\t" + new_instance_action.toString());
	}
	
	private static final JsonParser parser = new JsonParser();
	
	public static JsonArray getAllPending() throws Exception {
		final JsonArray ja = new JsonArray();
		
		CassandraDb.allRowsReader(CF_ACTION, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				ja.add(parser.parse(row.getColumns().getColumnByName("source").getStringValue()));
			}
		}, "source");
		
		return ja;
	}
	
	public static void truncate() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_ACTION.getName());
	}
	
	static boolean performInstanceActions(ArrayList<InstanceActionReceiver> all_receviers) throws Exception {
		final List<InstanceAction> pending_actions = new ArrayList<InstanceAction>(1);
		
		CassandraDb.allRowsReader(CF_ACTION, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				pending_actions.add(MyDMAM.gson_kit.getGson().fromJson(row.getColumns().getColumnByName("source").getStringValue(), InstanceAction.class));
			}
		});
		
		if (pending_actions.isEmpty()) {
			return false;
		}
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		for (int pos_pa = 0; pos_pa < pending_actions.size(); pos_pa++) {
			InstanceAction current_instance_action = pending_actions.get(pos_pa);
			String target_class_name = current_instance_action.target_class_name;
			
			InstanceActionReceiver recevier = null;
			for (int pos_rcv = 0; pos_rcv < all_receviers.size(); pos_rcv++) {
				if (all_receviers.get(pos_rcv).getClassToCallback().getSimpleName().equalsIgnoreCase(target_class_name)) {
					if (all_receviers.get(pos_rcv).getReferenceKey().equalsIgnoreCase(current_instance_action.target_reference_key)) {
						recevier = all_receviers.get(pos_rcv);
						break;
					}
				}
			}
			
			if (recevier == null) {
				if (Loggers.Manager.isDebugEnabled()) {
					Loggers.Manager.debug("An instance action exists in database but this Instance is not concerned " + current_instance_action);
				}
				continue;
			}
			
			Loggers.Manager.info("Do an Instance Action " + current_instance_action);
			
			current_instance_action.delete(mutator);
			
			try {
				recevier.doAnAction(current_instance_action.order);
			} catch (Exception e) {
				Loggers.Manager.error("Problem with an Instance Action " + current_instance_action, e);
			}
		}
		
		if (mutator.isEmpty() == false) {
			mutator.execute();
		}
		
		return true;
	}
	
	void delete(MutationBatch mutator) {
		mutator.withRow(CF_ACTION, key).delete();
	}
	
	public static class Serializer implements JsonSerializer<InstanceAction>, JsonDeserializer<InstanceAction> {
		
		public InstanceAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject src = json.getAsJsonObject();
			InstanceAction result = MyDMAM.gson_kit.getGsonSimple().fromJson(json, InstanceAction.class);
			result.order = src.getAsJsonObject("order");
			return result;
		}
		
		public JsonElement serialize(InstanceAction src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = MyDMAM.gson_kit.getGsonSimple().toJsonTree(src).getAsJsonObject();
			result.add("order", src.order);
			return result;
		}
	}
	
	public String toString() {
		LinkedHashMap<String, Object> log = new LinkedHashMap<String, Object>();
		log.put("key", key);
		log.put("target_class_name", target_class_name);
		log.put("target_reference_key", target_reference_key);
		log.put("order", order);
		log.put("created_at", Loggers.dateLog(created_at));
		log.put("caller", caller);
		return log.toString();
	}
	
}
