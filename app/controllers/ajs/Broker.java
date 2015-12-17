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
 * Copyright (C) hdsdi3g for hd3g.tv 2015
 * 
*/
package controllers.ajs;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.netflix.astyanax.MutationBatch;

import controllers.Check;
import controllers.Secure;
import hd3gtv.configuration.GitInfo;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.AppManager;
import hd3gtv.mydmam.manager.AsyncJSBrokerRequestAction;
import hd3gtv.mydmam.manager.AsyncJSBrokerRequestAction.Order;
import hd3gtv.mydmam.manager.AsyncJSBrokerRequestList;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseAction;
import hd3gtv.mydmam.manager.AsyncJSBrokerResponseList;
import hd3gtv.mydmam.manager.InstanceAction;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.WorkerExporter;
import hd3gtv.mydmam.web.AJSController;

public class Broker extends AJSController {
	
	private static Type al_String_typeOfT = new TypeToken<ArrayList<String>>() {
	}.getType();
	
	private static Type hm_StringJob_typeOfT = new TypeToken<HashMap<String, JobNG>>() {
	}.getType();
	
	static {
		AJSController.registerTypeAdapter(AsyncJSBrokerResponseList.class, new JsonSerializer<AsyncJSBrokerResponseList>() {
			public JsonElement serialize(AsyncJSBrokerResponseList src, Type typeOfSrc, JsonSerializationContext context) {
				return src.list;
			}
		});
		
		AJSController.registerTypeAdapter(AsyncJSBrokerRequestAction.class, new JsonDeserializer<AsyncJSBrokerRequestAction>() {
			public AsyncJSBrokerRequestAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
				JsonObject jo = json.getAsJsonObject();
				AsyncJSBrokerRequestAction result = AppManager.getSimpleGson().fromJson(json, AsyncJSBrokerRequestAction.class);
				result.jobs_keys = AppManager.getGson().fromJson(jo.get("jobs_keys"), al_String_typeOfT);
				return result;
			}
		});
		
		AJSController.registerTypeAdapter(AsyncJSBrokerResponseAction.class, new JsonSerializer<AsyncJSBrokerResponseAction>() {
			public JsonElement serialize(AsyncJSBrokerResponseAction src, Type typeOfSrc, JsonSerializationContext context) {
				return AppManager.getGson().toJsonTree(src.modified_jobs, hm_StringJob_typeOfT);
			}
		});
	}
	
	@Check("showBroker")
	public static AsyncJSBrokerResponseList list(AsyncJSBrokerRequestList request) throws Exception {
		AsyncJSBrokerResponseList result = new AsyncJSBrokerResponseList();
		result.list = JobNG.Utility.getJobsFromUpdateDate(request.since);
		return result;
	}
	
	@Check("showBroker")
	public static String appversion() throws Exception {
		return GitInfo.getFromRoot().getActualRepositoryInformation();
	}
	
	@Check("actionBroker")
	public static AsyncJSBrokerResponseAction action(AsyncJSBrokerRequestAction request) throws Exception {
		AsyncJSBrokerResponseAction result = new AsyncJSBrokerResponseAction();
		result.modified_jobs = new HashMap<String, JobNG>(request.jobs_keys.size());
		
		List<JobNG> jobs = JobNG.Utility.getJobsListByKeys(request.jobs_keys);
		if (jobs == null) {
			return result;
		}
		if (jobs.isEmpty()) {
			return result;
		}
		
		MutationBatch mutator = CassandraDb.prepareMutationBatch();
		
		JobNG job;
		String worker_reg;
		for (int pos = 0; pos < jobs.size(); pos++) {
			job = jobs.get(pos);
			Loggers.Job.info("Do action on job (caller " + AJSController.getUserProfile().key + " " + Secure.getRequestAddress() + "):\t" + job.toStringLight());
			
			switch (request.order) {
			case delete:
				job.delete(mutator);
				break;
			case stop:
				worker_reg = job.getWorker_reference();
				WorkerExporter worker_exporter = WorkerExporter.getWorkerStatusByKey(worker_reg);
				if (worker_exporter == null) {
					job.getActionUtils().setStopped();
				} else if (worker_exporter.getCurrent_job_key() == null) {
					job.getActionUtils().setStopped();
				} else if (worker_exporter.getCurrent_job_key().equals(job.getKey())) {
					JsonObject json_order = new JsonObject();
					json_order.addProperty("state", "stop");
					InstanceAction.addNew("WorkerNG", worker_reg, json_order, Secure.getRequestAddress());
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
			
			if (request.order != Order.delete) {
				job.saveChanges(mutator);
				result.modified_jobs.put(job.getKey(), job);
			}
		}
		
		if (mutator.isEmpty() == false) {
			mutator.execute();
		}
		
		return result;
	}
	
}
