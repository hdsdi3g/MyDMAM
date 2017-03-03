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

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.gson.GsonIgnore;

public final class WorkerExporter implements InstanceStatusItem {
	
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
			Loggers.Manager.error("Can't init database CFs", e);
		}
	}
	
	public static WorkerExporter getWorkerStatusByKey(String worker_key) throws ConnectionException {
		ColumnList<String> cols = keyspace.prepareQuery(CF_WORKERS).getKey(worker_key).withColumnSlice("source").execute().getResult();
		if (cols.isEmpty()) {
			return null;
		}
		return MyDMAM.gson_kit.getGson().fromJson(cols.getColumnByName("source").getStringValue(), WorkerExporter.class);
	}
	
	public static void truncate() throws ConnectionException {
		CassandraDb.truncateColumnFamilyString(keyspace, CF_WORKERS.getName());
	}
	
	static void updateWorkerStatus(List<WorkerNG> workers, AppManager manager) {
		if (workers == null) {
			throw new NullPointerException("\"workers\" can't to be null");
		}
		long start_time = System.currentTimeMillis();
		try {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			WorkerExporter we;
			for (int pos = 0; pos < workers.size(); pos++) {
				we = workers.get(pos).getExporter();
				we.update();
				mutator.withRow(CF_WORKERS, we.reference_key).putColumn("source", MyDMAM.gson_kit.getGson().toJson(we), InstanceStatus.TTL);
				Loggers.Manager.trace("Update worker status [" + we.reference_key + "], " + we.worker_class);
			}
			
			if (mutator.isEmpty() == false) {
				mutator.execute();
			}
		} catch (ConnectionException e) {
			manager.getServiceException().onCassandraError(e);
		}
		
		Loggers.Manager.debug("Update all workers status, took " + (System.currentTimeMillis() - start_time) + " ms");
	}
	
	/**
	 * Start of dynamic realm
	 */
	
	@GsonIgnore
	transient WorkerNG worker;
	
	String worker_class;
	WorkerNG.WorkerState state;
	String reference_key;
	String current_job_key;
	
	@SuppressWarnings("unused")
	private WorkerExporter() {
	}
	
	WorkerExporter(WorkerNG worker, AppManager manager) {
		this.worker = worker;
		worker_class = worker.getClass().getName();
		reference_key = worker.getReferenceKey();
		update();
		manager.getInstanceStatus().registerInstanceStatusItem(this);
	}
	
	public String getCurrent_job_key() {
		return current_job_key;
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
	
	public String toString() {
		update();
		return MyDMAM.gson_kit.getGsonPretty().toJson(this);
	}
	
	private transient JsonArray ja_capablities;
	
	public JsonElement getInstanceStatusItem() {
		JsonObject jo = MyDMAM.gson_kit.getGson().toJsonTree(this).getAsJsonObject();
		
		if (ja_capablities == null) {
			ja_capablities = new JsonArray();
			List<WorkerCapablities> capablities = worker.getWorkerCapablities();
			for (int pos = 0; pos < capablities.size(); pos++) {
				ja_capablities.add(MyDMAM.gson_kit.getGson().toJsonTree(capablities.get(pos).getExporter()));
			}
		}
		
		jo.addProperty("long_name", worker.getWorkerLongName());
		jo.addProperty("vendor", worker.getWorkerVendorName());
		jo.addProperty("category", worker.getWorkerCategory().toString());
		jo.add("capablities", ja_capablities);
		jo.add("specific", worker.exportSpecificInstanceStatusItems());
		return jo;
	}
	
	public String getReferenceKey() {
		return reference_key;
	}
	
	public Class<?> getInstanceStatusItemReferenceClass() {
		return WorkerNG.class;
	}
}
