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
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.ColumnListMutation;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;

class CyclicJobDeclaration {
	
	/**
	 * In msec
	 */
	private long period;
	private List<JobContext> contexts;
	private String name;
	transient private AppManager manager;
	private Class<?> creator;
	
	CyclicJobDeclaration(AppManager manager, Class<?> creator, String name, long period, JobContext... contexts) {
		this.manager = manager;
		this.creator = creator;
		this.name = name;
		this.period = period;
		this.contexts = new ArrayList<JobContext>(contexts.length);
		for (int pos = 0; pos < contexts.length; pos++) {
			this.contexts.add(contexts[pos]);
		}
	}
	
	void createJobs(MutationBatch mutator) throws ConnectionException {
		JobNG require = null;
		for (int pos_dc = 0; pos_dc < contexts.size(); pos_dc++) {
			JobContext declatation_context = contexts.get(pos_dc);
			JobNG job = manager.createJob(declatation_context);
			job.setDeleteAfterCompleted();
			job.setCreator(creator);
			job.setExpirationTime(period, TimeUnit.MILLISECONDS);
			job.setMaxExecutionTime(period, TimeUnit.MILLISECONDS);
			if (contexts.size() > 0) {
				job.setName(name);
			} else {
				job.setName(name + " (" + (pos_dc + 1) + "/" + contexts.size() + ")");
			}
			job.setRequireCompletedJob(require);
			require = job;
			job.publish(mutator);
		}
	}
	
	static class Serializer implements JsonSerializer<CyclicJobDeclaration>, JsonDeserializer<CyclicJobDeclaration>, CassandraDbImporterExporter<CyclicJobDeclaration> {
		
		@Override
		public void exportToDatabase(CyclicJobDeclaration src, ColumnListMutation<String> mutator) {
			// TODO Auto-generated method stub
		}
		
		@Override
		public String getDatabaseKey(CyclicJobDeclaration src) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public CyclicJobDeclaration importFromDatabase(ColumnList<String> columnlist) {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public CyclicJobDeclaration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public JsonElement serialize(CyclicJobDeclaration src, Type typeOfSrc, JsonSerializationContext context) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	/*
	static class Serializer implements JsonSerializer<WorkerExporter>, JsonDeserializer<WorkerExporter>, CassandraDbImporterExporter<WorkerExporter> {
		private static Type al_wcs_typeOfT = new TypeToken<ArrayList<WorkerCapablitiesExporter>>() {
		}.getType();
		
		public WorkerExporter deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			WorkerExporter result = AppManager.getSimpleGson().fromJson(json, WorkerExporter.class);
			JsonObject jo = json.getAsJsonObject();
			result.capablities = AppManager.getSimpleGson().fromJson(jo.get("capablities"), al_wcs_typeOfT);
			return result;
		}
		
		public JsonElement serialize(WorkerExporter src, Type typeOfSrc, JsonSerializationContext context) {
			src.update();
			JsonObject result = AppManager.getSimpleGson().toJsonTree(src).getAsJsonObject();
			result.add("capablities", AppManager.getSimpleGson().toJsonTree(src.capablities, al_wcs_typeOfT).getAsJsonArray());
			return result;
		}
		
		public String getDatabaseKey(WorkerExporter src) {
			return src.reference_key;
		}
		
		public void exportToDatabase(WorkerExporter src, ColumnListMutation<String> mutator) {
			mutator.putColumn("source", AppManager.getGson().toJson(src), InstanceStatus.TTL);
		}
		
		public WorkerExporter importFromDatabase(ColumnList<String> columnlist) {
			return AppManager.getGson().fromJson(columnlist.getColumnByName("source").getStringValue(), WorkerExporter.class);
		}
		
	}
	 * */
	
}
