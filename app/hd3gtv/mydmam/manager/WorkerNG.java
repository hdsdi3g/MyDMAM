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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class WorkerNG {
	
	public enum WorkerCategory {
		INDEXING, METADATA, EXTERNAL_MODULE, USERACTION, INTERNAL
	}
	
	public abstract WorkerCategory getWorkerCategory();
	
	public abstract String getWorkerLongName();
	
	public abstract String getWorkerVendorName();
	
	public abstract List<WorkerCapablities> getWorkerCapablities();
	
	protected abstract void workerProcessJob(JobProgression progression, JobContext context) throws Exception;
	
	protected abstract void forceStopProcess() throws Exception;
	
	protected abstract boolean isActivated();
	
	enum WorkerState {
		PROCESSING, WAITING, STOPPED, PENDING_STOP, DISACTIVATED;
	}
	
	private String reference_key;
	private WorkerExceptionHandler worker_exception;
	private volatile boolean refuse_new_jobs;
	private WorkerStatus status;
	
	public WorkerNG(AppManager manager) {
		manager.workerRegister(this);
		reference_key = "worker:" + UUID.randomUUID().toString();
		lifecyle = new LifeCycle(this);
		refuse_new_jobs = true;
		status = new WorkerStatus(this);
	}
	
	final WorkerStatus getStatus() {
		return status;
	}
	
	List<Class<? extends JobContext>> getWorkerCapablitiesJobContextClasses() {
		List<Class<? extends JobContext>> capablities_classes = new ArrayList<Class<? extends JobContext>>();
		List<WorkerCapablities> current_capablities;
		Class<? extends JobContext> current_capablity_class;
		
		current_capablities = getWorkerCapablities();
		if (current_capablities == null) {
			return capablities_classes;
		}
		for (int pos_cc = 0; pos_cc < current_capablities.size(); pos_cc++) {
			current_capablity_class = current_capablities.get(pos_cc).getJobContextClass();
			if (current_capablity_class == null) {
				continue;
			}
			if (capablities_classes.contains(current_capablity_class) == false) {
				capablities_classes.add(current_capablity_class);
			}
		}
		return capablities_classes;
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
			try {
				workerProcessJob(job.startProcessing(), job.getContext());
				if (refuse_new_jobs) {
					job.endProcessing_Stopped();
				} else {
					job.endProcessing_Done();
				}
			} catch (Exception e) {
				job.endProcessing_Error(e);
				worker_exception.onError(e, "Error during processing", reference);
			}
			/*
			try {
				worker.broker.doneJob(job);//TODO doneJob ?
			} catch (ConnectionException e) {
				Log2.log.error("Lost Cassandra connection", e);
			}
			}
			* */
			current_executor = null;
		}
	}
	
	private LifeCycle lifecyle;
	
	final class LifeCycle {
		private WorkerNG reference;
		
		private LifeCycle(WorkerNG reference) {
			this.reference = reference;
		}
		
		final WorkerState getState() {
			if (isActivated() == false) {
				return WorkerState.DISACTIVATED;
			}
			if (current_executor != null) {
				if (current_executor.isAlive()) {
					if (refuse_new_jobs) {
						return WorkerState.PENDING_STOP;
					} else {
						return WorkerState.PROCESSING;
					}
				} else {
					current_executor = null;
				}
			}
			if (refuse_new_jobs) {
				return WorkerState.STOPPED;
			}
			return WorkerState.WAITING;
		}
		
		final boolean isAvaliableForProcessingNewJobs() {
			return (getState() == WorkerState.WAITING);
		}
		
		final void enable() {
			refuse_new_jobs = false;
		}
		
		final void askToStop() {
			refuse_new_jobs = true;
			if (getState() == WorkerState.PROCESSING) {
				try {
					forceStopProcess();
				} catch (Exception e) {
					reference.worker_exception.onError(e, "Can't stop current process", reference);
				}
			}
		}
		
	}
	
	public LifeCycle getLifecyle() {
		return lifecyle;
	}
	
	final void internalProcess(JobNG job) {
		current_executor = new Executor(job, this);
		current_executor.start();
	}
	
	String getReferenceKey() {
		return reference_key;
	}
	
	JobNG getCurrentJob() {
		if (current_executor == null) {
			return null;
		}
		if (current_executor.isAlive() == false) {
			return null;
		}
		return current_executor.job;
	}
	
	// WorkerEngine engine;
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
	*/
}
