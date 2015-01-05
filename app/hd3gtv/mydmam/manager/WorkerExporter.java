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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.WorkerNG.WorkerCategory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;
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
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.serializers.StringSerializer;

public final class WorkerExporter implements Log2Dumpable {
	
	/**
	 * Start of static realm
	 */
	private static final ColumnFamily<String, String> CF_WORKERS = new ColumnFamily<String, String>("mgrWorkers", StringSerializer.get(), StringSerializer.get());
	
	private static Keyspace keyspace;
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_WORKERS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_WORKERS.getName(), false);
			}
		} catch (Exception e) {
			Log2.log.error("Can't init database CFs", e);
		}
	}
	
	static WorkerExporter getWorkerStatusByKey(String worker_key) throws ConnectionException {
		ColumnList<String> cols = keyspace.prepareQuery(CF_WORKERS).getKey(worker_key).withColumnSlice("source").execute().getResult();
		if (cols.isEmpty()) {
			return null;
		}
		return AppManager.getGson().fromJson(cols.getColumnByName("source").getStringValue(), WorkerExporter.class);
	}
	
	public static void truncate() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_WORKERS.getName());
	}
	
	static void updateWorkerStatus(List<WorkerNG> workers, AppManager manager) {
		if (workers == null) {
			throw new NullPointerException("\"workers\" can't to be null");
		}
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			WorkerExporter we;
			for (int pos = 0; pos < workers.size(); pos++) {
				we = workers.get(pos).getExporter();
				we.update();
				mutator.withRow(CF_WORKERS, we.reference_key).putColumn("source", AppManager.getGson().toJson(we), InstanceStatus.TTL);
			}
			
			if (mutator.isEmpty() == false) {
				mutator.execute();
			}
		} catch (ConnectionException e) {
			manager.getServiceException().onCassandraError(e);
		}
	}
	
	public static List<WorkerExporter> getAllWorkerStatus() throws Exception {
		final List<WorkerExporter> result = new ArrayList<WorkerExporter>();
		CassandraDb.allRowsReader(CF_WORKERS, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				result.add(AppManager.getGson().fromJson(row.getColumns().getColumnByName("source").getStringValue(), WorkerExporter.class));
			}
		});
		return result;
	}
	
	public static String getAllWorkerStatusJson() throws Exception {
		final JsonArray result = new JsonArray();
		final JsonParser parser = new JsonParser();
		CassandraDb.allRowsReader(CF_WORKERS, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				result.add(parser.parse(row.getColumns().getColumnByName("source").getStringValue()));
			}
		});
		return result.toString();
	}
	
	/**
	 * Start of dynamic realm
	 */
	
	@GsonIgnore
	transient WorkerNG worker;
	
	WorkerCategory category;
	String long_name;
	String vendor_name;
	String worker_class;
	WorkerNG.WorkerState state;
	String reference_key;
	JsonObject manager_reference;
	String current_job_key;
	
	@GsonIgnore
	ArrayList<WorkerCapablitiesExporter> capablities;
	
	@SuppressWarnings("unused")
	private WorkerExporter() {
	}
	
	WorkerExporter(WorkerNG worker) {
		this.worker = worker;
		capablities = new ArrayList<WorkerCapablitiesExporter>();
		List<WorkerCapablities> workercapablities = worker.getWorkerCapablities();
		if (workercapablities != null) {
			for (int pos = 0; pos < workercapablities.size(); pos++) {
				capablities.add(workercapablities.get(pos).getExporter());
			}
		}
		category = worker.getWorkerCategory();
		long_name = worker.getWorkerLongName();
		vendor_name = worker.getWorkerVendorName();
		worker_class = worker.getClass().getName();
		reference_key = worker.getReferenceKey();
		manager_reference = worker.getManagerReference();
		update();
	}
	
	private synchronized void update() {
		if (worker == null) {
			return;
		}
		state = worker.getLifecyle().getState();
		JobNG job = worker.getCurrentJob();
		if (job != null) {
			current_job_key = job.getKey();
		} else {
			current_job_key = null;
		}
	}
	
	static class Serializer implements JsonSerializer<WorkerExporter>, JsonDeserializer<WorkerExporter> {
		private static Type al_wcs_typeOfT = new TypeToken<ArrayList<WorkerCapablitiesExporter>>() {
		}.getType();
		
		public WorkerExporter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			WorkerExporter result = AppManager.getSimpleGson().fromJson(json, WorkerExporter.class);
			JsonObject jo = json.getAsJsonObject();
			result.capablities = AppManager.getGson().fromJson(jo.get("capablities"), al_wcs_typeOfT);
			return result;
		}
		
		public JsonElement serialize(WorkerExporter src, Type typeOfSrc, JsonSerializationContext context) {
			src.update();
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("capablities", AppManager.getGson().toJsonTree(src.capablities, al_wcs_typeOfT));
			return result;
		}
		
	}
	
	public String toString() {
		update();
		return AppManager.getPrettyGson().toJson(this);
	}
	
	public Log2Dump getLog2Dump() {
		update();
		Log2Dump dump = new Log2Dump();
		dump.add("worker_class", worker_class);
		dump.add("long_name", long_name);
		dump.add("category", category);
		dump.add("vendor_name", vendor_name);
		dump.add("reference_key", reference_key);
		dump.add("current_job_key", current_job_key);
		dump.add("state", state);
		dump.add("manager_reference", manager_reference);
		return dump;
	}
}
