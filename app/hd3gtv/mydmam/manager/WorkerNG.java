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
import java.util.UUID;

import com.google.gson.JsonObject;

public abstract class WorkerNG {
	
	// TODO push & pull db
	
	public enum WorkerCategory {
		INDEXING, METADATA, EXTERNAL_MODULE, USERACTION, INTERNAL
	}
	
	public abstract WorkerCategory getWorkerCategory();
	
	public abstract String getWorkerLongName();
	
	public abstract String getWorkerVendorName();
	
	public abstract List<WorkerCapablities> getWorkerCapablities();
	
	public abstract void workerProcessJob(JobNG.Progression progression, JobContext context) throws Exception;
	
	public abstract void forceStopProcess() throws Exception;
	
	public abstract boolean isEnabled();
	
	final JsonObject toJson() {
		JsonObject result = new JsonObject();
		// TODO for web site display...
		return result;
	}
	
	enum WorkerStatus {
		PROCESSING, WAITING, STOPPED, PENDING_STOP;
	}
	
	private volatile WorkerStatus status;
	private String reference;
	
	public WorkerNG(AppManager manager) {
		manager.workerRegister(this);
		status = WorkerStatus.WAITING;
		reference = "worker:" + UUID.randomUUID().toString();
	}
	
	private volatile Executor current_executor;
	
	private final class Executor extends Thread {
		private JobNG job;
		
		private Executor(JobNG job) {
			setName("Worker " + getWorkerLongName() + " (" + getWorkerCategory() + ")");// TODO add job.key in name
			setDaemon(true);
		}
		
		public void run() {
			status = WorkerStatus.PROCESSING;
			/*job.worker = worker;
			if (worker instanceof WorkerCyclicEngine) {
				job.delete_after_done = true;
			}
			job.start_date = System.currentTimeMillis();
			job.status = TaskJobStatus.PROCESSING;
			job.processing_error = null;
			if (job.cyclic_source == false) {
				Log2.log.info("Start process", job);
			}*/
			try {
				workerProcessJob(job.startProcessing(), job.getContext());
			} catch (Exception e) {
				job.endProcessing_Error(e);
				// TODO handle exception
				/*
				job.processing_error = exceptionToString(e);
				job.status = TaskJobStatus.ERROR;
				Log2.log.error("Error during processing", null, job);
				AdminMailAlert.create("Error during processing", false).addDump(job).addDump(worker).setServiceinformations(serviceinformations).send();
				 * */
			}
			if (status == WorkerStatus.PENDING_STOP) {
				status = WorkerStatus.STOPPED;
				job.endProcessing_Stopped();
			} else {
				status = WorkerStatus.WAITING;
				job.endProcessing_Done();
				/*
				try {
					worker.broker.doneJob(job);
				} catch (ConnectionException e) {
					Log2.log.error("Lost Cassandra connection", e);
				}
				}
				* */
			}
			current_executor = null;
		}
	}
	
	final void internalProcess(JobNG job) {
		current_executor = new Executor(job);
		current_executor.start();
	}
	
	final void requestStopProcess() {
		if (current_executor == null) {
			return;
		}
		status = WorkerStatus.PENDING_STOP;
		try {
			forceStopProcess();
		} catch (Exception e) {
			// TODO handle exception
		}
	}
	
	final void resetToWaiting() {
		if ((status == WorkerStatus.STOPPED) | (status == WorkerStatus.PENDING_STOP)) {
			status = WorkerStatus.WAITING;
		}
	}
	
	final WorkerStatus getStatus() {
		return status;
	}
	
	String getReference() {
		return reference;
	}
	
	// WorkerEngine engine;
	// String worker_ref;
	// WorkerStatus status = WorkerStatus.STOPPED;
	// Broker broker;
	/*final boolean isAvailableForProcessing() {
		return (status == WorkerStatus.WAITING);
	}
	
	public final boolean isEnabled() {
		switch (status) {
		case PROCESSING:
			return true;
		case WAITING:
			return true;
		case PENDING_CANCEL_TASK:
			return true;
		default:
			return false;
		}
	}
	
	public final void setEnabled() {
		if (status != WorkerStatus.WAITING) {
			Log2.log.debug("Change worker status to waiting");
		}
		status = WorkerStatus.WAITING;
	}
	
	public final void setDisabled() {
		Log2.log.info("Change worker status to disabled");
		status = WorkerStatus.PENDING_STOP;
		if (engine != null) {
			if (engine.isAlive()) {
				engine.askStopProcess();
				Log2.log.debug("Worker is stopped");
			}
		}
		engine = null;
		status = WorkerStatus.STOPPED;
	}
	
	final void pushToDatabase(MutationBatch mutator, String hostname, int ttl) {
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("hostname", hostname, ttl);
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("instancename", ServiceManager.getInstancename(false), ttl);
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("short_worker_name", getShortWorkerName(), ttl);
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("long_worker_name", getLongWorkerName(), ttl);
		status.pushToDatabase(mutator, worker_ref, ttl);
		
		JSONArray ja = new JSONArray();
		List<Profile> mp = getManagedProfiles();
		for (int pos = 0; pos < mp.size(); pos++) {
			ja.add(mp.get(pos).toJson());
		}
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("managed_profiles", ja.toJSONString(), ttl);
		
		if (engine != null) {
			mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("job", engine.job.key, ttl);
		}
		if (this instanceof WorkerCyclicEngine) {
			WorkerCyclicEngine workercyclicengine = (WorkerCyclicEngine) this;
			mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("cyclic", true, ttl);
			
			if (workercyclicengine.canChangeTimeToSleep()) {
				mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("time_to_sleep", (int) workercyclicengine.getTime_to_sleep() / 1000, ttl);
			} else {
				mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("time_to_sleep", 0, ttl);
			}
			mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("countdown_to_process", workercyclicengine.getCountdown_to_process(), ttl);
		} else {
			mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("cyclic", false, ttl);
		}
	}
	
	static JSONObject pullJSONFromDatabase(ColumnList<String> columns) throws ParseException {
		JSONObject jo = new JSONObject();
		jo.put("hostname", columns.getStringValue("hostname", "?"));
		jo.put("instancename", columns.getStringValue("instancename", "?"));
		jo.put("short_worker_name", columns.getStringValue("short_worker_name", ""));
		jo.put("long_worker_name", columns.getStringValue("long_worker_name", ""));
		jo.put("job", columns.getStringValue("job", ""));
		jo.put("cyclic", columns.getBooleanValue("cyclic", false));
		jo.put("time_to_sleep", columns.getIntegerValue("time_to_sleep", 0));
		jo.put("countdown_to_process", columns.getIntegerValue("countdown_to_process", 0));
		jo.put("status", WorkerStatus.pullFromDatabase(columns).name().toUpperCase());
		
		JSONParser parser = new JSONParser();
		jo.put("managed_profiles", (JSONArray) parser.parse(columns.getStringValue("managed_profiles", "[]")));
		
		jo.put(WorkerGroup.COL_NAME_STATUSCHANGE, columns.getStringValue(WorkerGroup.COL_NAME_STATUSCHANGE, ""));
		jo.put(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, columns.getIntegerValue(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, 0));
		return jo;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add(worker_ref, worker_ref);
		dump.add("short worker name", getShortWorkerName());
		dump.add("long worker name", getLongWorkerName());
		dump.add("worker status", status);
		if (engine != null) {
			dump.addAll(engine.job);
			dump.add("worker alive", engine.isAlive());
		}
		return dump;
	}
	*/
}
