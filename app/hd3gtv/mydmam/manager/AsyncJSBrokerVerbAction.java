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
package hd3gtv.mydmam.manager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.gson.JsonObject;
import com.netflix.astyanax.MutationBatch;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.AsyncJSBrokerRequestAction.Order;
import hd3gtv.mydmam.web.AsyncJSControllerVerb;
import hd3gtv.mydmam.web.AsyncJSDeserializer;
import hd3gtv.mydmam.web.AsyncJSGsonProvider;
import hd3gtv.mydmam.web.AsyncJSSerializer;

public class AsyncJSBrokerVerbAction extends AsyncJSControllerVerb<AsyncJSBrokerRequestAction, AsyncJSBrokerResponseAction> {
	
	public String getVerbName() {
		return "action";
	}
	
	public Class<AsyncJSBrokerRequestAction> getRequestClass() {
		return AsyncJSBrokerRequestAction.class;
	}
	
	public Class<AsyncJSBrokerResponseAction> getResponseClass() {
		return AsyncJSBrokerResponseAction.class;
	}
	
	public AsyncJSBrokerResponseAction onRequest(AsyncJSBrokerRequestAction request, String caller) throws Exception {
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
			Loggers.Job.info("Do action on job (caller " + getUserProfile().key + " " + caller + "):\t" + job.toStringLight());
			
			switch (request.order) {
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
	
	public List<String> getMandatoryPrivileges() {
		return Arrays.asList("actionBroker");
	}
	
	public List<? extends AsyncJSDeserializer<?>> getJsonDeserializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncJSBrokerRequestAction.Deserializer());
	}
	
	public List<? extends AsyncJSSerializer<?>> getJsonSerializers(AsyncJSGsonProvider gson_provider) {
		return Arrays.asList(new AsyncJSBrokerResponseAction.Serializer());
	}
}
