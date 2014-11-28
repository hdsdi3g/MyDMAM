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

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
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
	
	protected abstract void workerProcessJob(JobNG.Progression progression, JobContext context) throws Exception;
	
	protected abstract void forceStopProcess() throws Exception;
	
	protected abstract boolean isActivated();
	
	final JsonObject toJson() {
		JsonObject result = new JsonObject();
		result.addProperty("category", getWorkerCategory().name());
		result.addProperty("long_name", getWorkerLongName());
		List<WorkerCapablities> capablities = getWorkerCapablities();
		JsonArray ja_capablities = new JsonArray();
		if (capablities != null) {
			for (int pos = 0; pos < capablities.size(); pos++) {
				ja_capablities.add(capablities.get(pos).toJson());
			}
		}
		result.add("capablities", ja_capablities);
		result.addProperty("isactivated", isActivated());
		result.addProperty("status", this.status.name());
		result.addProperty("reference", reference);
		if (current_executor == null) {
			result.add("current_job_key", JsonNull.INSTANCE);
		} else {
			result.addProperty("current_job_key", current_executor.job.getKey());
		}
		return result;
	}
	
	enum WorkerStatus {
		PROCESSING, WAITING, STOPPED, PENDING_STOP, DISACTIVATED;
	}
	
	private volatile WorkerStatus status;
	private String reference;
	private WorkerExceptionHandler worker_exception;
	
	public WorkerNG(AppManager manager) {
		manager.workerRegister(this);
		status = WorkerStatus.WAITING;
		reference = "worker:" + UUID.randomUUID().toString();
		lifecyle = new LifeCycle(this);
	}
	
	void setWorker_exception(WorkerExceptionHandler worker_exception) {
		this.worker_exception = worker_exception;
	}
	
	private volatile Executor current_executor;
	
	private final class Executor extends Thread {
		private JobNG job;
		private WorkerNG reference;
		
		private Executor(JobNG job, WorkerNG reference) {
			setName("Worker for " + job.getKey() + " (" + getWorkerCategory() + ")");
			this.job = job;
			this.reference = reference;
			setDaemon(true);
		}
		
		public void run() {
			status = WorkerStatus.PROCESSING;
			boolean is_ok = false;
			try {
				workerProcessJob(job.startProcessing(), job.getContext());
				is_ok = true;
			} catch (Exception e) {
				job.endProcessing_Error(e);
				worker_exception.onError(e, "Error during processing", reference);
			}
			if (status == WorkerStatus.PENDING_STOP) {
				status = WorkerStatus.STOPPED;
				if (is_ok) {
					job.endProcessing_Stopped();
				}
			} else {
				status = WorkerStatus.WAITING;
				if (is_ok) {
					job.endProcessing_Done();
				}
				/*
				try {
					worker.broker.doneJob(job);//TODO doneJob ?
				} catch (ConnectionException e) {
					Log2.log.error("Lost Cassandra connection", e);
				}
				}
				* */
			}
			current_executor = null;
		}
	}
	
	private LifeCycle lifecyle;
	
	final class LifeCycle {
		private WorkerNG reference;
		
		private LifeCycle(WorkerNG reference) {
			this.reference = reference;
		}
		
		final WorkerStatus getStatus() {
			if (isActivated() == false) {
				status = WorkerStatus.DISACTIVATED;
				return status;
			}
			return status;
		}
		
		final boolean isEnabledForProcessing() {
			if (isActivated() == false) {
				return false;
			}
			return (status == WorkerStatus.WAITING);
		}
		
		final void enable() {
			if (status == WorkerStatus.STOPPED) {
				status = WorkerStatus.WAITING;
			}
		}
		
		final void disable(boolean wait_to_stop) {
			if (isActivated() == false) {
				return;
			}
			stop(wait_to_stop);
			status = WorkerStatus.STOPPED;
		}
		
		final void stop(boolean wait_to_stop) {
			if (isActivated() == false) {
				return;
			}
			if (status == WorkerStatus.PROCESSING) {
				status = WorkerStatus.PENDING_STOP;
				try {
					forceStopProcess();
				} catch (Exception e) {
					reference.worker_exception.onError(e, "Can't stop currenr process", reference);
				}
				if (wait_to_stop) {
					while (current_executor.isAlive()) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					}
				}
			}
			status = WorkerStatus.WAITING;
		}
		
	}
	
	public LifeCycle getLifecyle() {
		return lifecyle;
	}
	
	final void internalProcess(JobNG job) {
		current_executor = new Executor(job, this);
		current_executor.start();
	}
	
	String getReference() {
		return reference;
	}
	
	// WorkerEngine engine;
	// String worker_ref;
	// WorkerStatus status = WorkerStatus.STOPPED;
	// Broker broker;
	/*
	
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
