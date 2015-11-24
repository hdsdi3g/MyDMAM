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

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.GsonIgnore;

public class JobAction {
	
	private enum Order {
		delete, stop, setinwait, cancel, hipriority, noexpiration, postponed
	}
	
	private @GsonIgnore ArrayList<String> jobs_keys;
	private Order order;
	
	public JsonObject doAction(String caller) throws ConnectionException {
		JsonObject result = new JsonObject();
		
		List<JobNG> jobs = JobNG.Utility.getJobsListByKeys(jobs_keys);
		if (jobs == null) {
			return result;
		}
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		JobNG job;
		String worker_reg;
		for (int pos = 0; pos < jobs.size(); pos++) {
			job = jobs.get(pos);
			Loggers.Job.info("Do action on job (caller " + caller + "):\t" + job);
			
			switch (order) {
			case delete:
				job.delete(mutator);
				break;
			case stop:
				worker_reg = job.getWorker_reference();
				WorkerExporter worker_exporter = WorkerExporter.getWorkerStatusByKey(worker_reg);
				if (worker_exporter == null) {
					job.getActionUtils().setStopped();
				} else if (worker_exporter.current_job_key == null) {
					job.getActionUtils().setStopped();
				} else if (worker_exporter.current_job_key.equals(job.getKey())) {
					JsonObject json_order = new JsonObject();
					json_order.addProperty("state", "stop");
					InstanceAction.addNew("WorkerNG", worker_reg, json_order, caller);
				} else {
					job.getActionUtils().setStopped();
				}
				break;
			case setinwait:
				job.getActionUtils().setWaiting();
				break;
			case cancel:
				job.getActionUtils().setCancel();
				break;
			case postponed:
				job.getActionUtils().setPostponed();
				break;
			case hipriority:
				job.getActionUtils().setMaxPriority();
				break;
			case noexpiration:
				job.getActionUtils().setDontExpiration();
				break;
			}
			
			if (order != JobAction.Order.delete) {
				job.saveChanges(mutator);
				result.add(job.getKey(), AppManager.getGson().toJsonTree(job));
			}
		}
		
		if (mutator.isEmpty() == false) {
			mutator.execute();
		}
		
		return result;
	}
	
	static class Serializer implements JsonSerializer<JobAction>, JsonDeserializer<JobAction> {
		
		private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
		}.getType();
		
		public JobAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jo = json.getAsJsonObject();
			JobAction result = AppManager.getSimpleGson().fromJson(json, JobAction.class);
			result.jobs_keys = AppManager.getGson().fromJson(jo.get("jobs_keys"), al_String_typeOfT);
			return result;
		}
		
		public JsonElement serialize(JobAction src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject result = AppManager.getPrettyGson().toJsonTree(src).getAsJsonObject();
			result.add("jobs_keys", AppManager.getGson().toJsonTree(src.jobs_keys, al_String_typeOfT));
			return result;
		}
	}
	
}
