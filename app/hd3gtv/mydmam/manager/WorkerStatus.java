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

import hd3gtv.mydmam.manager.WorkerNG.WorkerCategory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.model.ColumnList;

public class WorkerStatus {
	
	@GsonIgnore
	transient WorkerNG worker;
	
	WorkerCategory category;
	String long_name;
	String vendor_name;
	boolean isactivated;
	Class worker_class;
	WorkerNG.WorkerState state;
	String reference_key;
	String current_job_key;
	
	@GsonIgnore
	ArrayList<WorkerCapablitiesStatus> capablities;
	
	@SuppressWarnings("unused")
	private WorkerStatus() {
	}
	
	WorkerStatus(WorkerNG worker) {
		this.worker = worker;
	}
	
	private void update() {
		category = worker.getWorkerCategory();
		long_name = worker.getWorkerLongName();
		vendor_name = worker.getWorkerVendorName();
		worker_class = worker.getClass();
		isactivated = worker.isActivated();
		state = worker.getLifecyle().getStatus();
		reference_key = worker.getReferenceKey();
		JobNG job = worker.getCurrentJob();
		if (job != null) {
			current_job_key = job.getKey();
		}
		
		List<WorkerCapablities> workercapablities = worker.getWorkerCapablities();
		if (workercapablities != null) {
			ArrayList<WorkerCapablitiesStatus> capablities = new ArrayList<WorkerCapablitiesStatus>();
			for (int pos = 0; pos < workercapablities.size(); pos++) {
				capablities.add(workercapablities.get(pos).getStatus());
			}
		}
	}
	
	static class Serializer implements JsonSerializer<WorkerStatus>, JsonDeserializer<WorkerStatus>, CassandraDbImporterExporter<WorkerStatus> {
		private static Type al_wcs_typeOfT = new TypeToken<ArrayList<WorkerCapablitiesStatus>>() {
		}.getType();
		
		public WorkerStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			WorkerStatus result = AppManager.getSimpleGson().fromJson(json, WorkerStatus.class);
			JsonObject jo = json.getAsJsonObject();
			result.capablities = AppManager.getSimpleGson().fromJson(jo.get("capablities"), al_wcs_typeOfT);
			return result;
		}
		
		public JsonElement serialize(WorkerStatus src, Type typeOfSrc, JsonSerializationContext context) {
			src.update();
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("capablities", AppManager.getSimpleGson().toJsonTree(src.capablities, al_wcs_typeOfT).getAsJsonArray());
			return result;
		}
		
		public String getDatabaseKey(WorkerStatus src) {
			return src.reference_key;
		}
		
		public void exportToDatabase(WorkerStatus src, ColumnListMutation<String> mutator) {
			mutator.putColumn("source", AppManager.getGson().toJson(src), InstanceStatus.TTL);
		}
		
		public WorkerStatus importFromDatabase(ColumnList<String> columnlist) {
			return AppManager.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), WorkerStatus.class);
		}
		
	}
	
	public String toString() {
		return AppManager.getPrettyGson().toJson(this);
	}
	
}
