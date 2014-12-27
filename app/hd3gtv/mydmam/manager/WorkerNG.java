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
import hd3gtv.mydmam.MyDMAM;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

public abstract class WorkerNG implements Log2Dumpable, InstanceActionReceiver {
	
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
	
	private enum StopReason {
		full_functionnal, refuse_new_jobs, simple_job_stop
	}
	
	private LifeCycle lifecyle;
	private volatile Executor current_executor;
	private String reference_key;
	private volatile StopReason stopreason;
	private WorkerExporter exporter;
	private AppManager manager;
	
	public WorkerNG() {
	}
	
	private String computeKey() {
		StringBuffer sb = new StringBuffer();
		sb.append(manager.getInstance_status().getAppName());
		sb.append(manager.getInstance_status().getHostName());
		sb.append(manager.getInstance_status().getInstanceName());
		sb.append(getClass().getName());
		sb.append(getWorkerCategory().name());
		sb.append(getWorkerLongName());
		sb.append(getWorkerVendorName());
		List<WorkerCapablities> cap = getWorkerCapablities();
		if (cap != null) {
			for (int pos = 0; pos < cap.size(); pos++) {
				sb.append("/cap:");
				sb.append(cap.get(pos).toString());
			}
		}
		
		try {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			md.update(sb.toString().getBytes());
			return "worker:" + MyDMAM.byteToString(md.digest());
		} catch (Exception e) {
			return "worker:" + UUID.randomUUID().toString();
		}
	}
	
	void setManager(AppManager manager) {
		this.manager = manager;
		reference_key = computeKey();
		lifecyle = new LifeCycle(this);
		stopreason = StopReason.full_functionnal;
		exporter = new WorkerExporter(this);
	}
	
	final WorkerExporter getExporter() {
		return exporter;
	}
	
	final List<Class<? extends JobContext>> getWorkerCapablitiesJobContextClasses() {
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
	
	final boolean canProcessThis(JobContext context) {
		List<WorkerCapablities> current_capablities = getWorkerCapablities();
		for (int pos_cc = 0; pos_cc < current_capablities.size(); pos_cc++) {
			if (current_capablities.get(pos_cc).isAssignableFrom(context)) {
				return true;
			}
		}
		return false;
	}
	
	private final class Executor extends Thread {
		private JobNG job;
		private WorkerNG reference;
		ExecutorWatchDog current_executor_watch_dog;
		
		private Executor(JobNG job, WorkerNG reference) {
			setName("Worker for " + job.getKey() + " (" + getWorkerCategory() + ")");
			this.job = job;
			this.reference = reference;
			setDaemon(true);
			if (job.hasAMaxExecutionTime()) {
				current_executor_watch_dog = new ExecutorWatchDog(this);
			}
		}
		
		public void run() {
			if (current_executor_watch_dog != null) {
				current_executor_watch_dog.start();
			}
			try {
				Log2.log.debug("Start processing", job);
				
				workerProcessJob(job.startProcessing(manager, reference), job.getContext());
				
				if (job.isMaxExecutionTimeIsReached()) {
					job.endProcessing_TooLongDuration();
					manager.getServiceException().onMaxExecJobTime(job);
					Log2.log.error("Max execution time is reached", null, job);
				} else {
					switch (stopreason) {
					case full_functionnal:
						job.endProcessing_Done();
						Log2.log.debug("End processing", job);
						break;
					case refuse_new_jobs:
						job.endProcessing_Stopped();
						Log2.log.debug("Stop execution", job);
						break;
					case simple_job_stop:
						job.endProcessing_Stopped();
						Log2.log.debug("Stop execution", job);
						stopreason = StopReason.full_functionnal;
						break;
					}
				}
			} catch (Exception e) {
				Log2.log.error("Processing error", e, job);
				job.endProcessing_Error(e);
				manager.getServiceException().onError(e, "Error during processing", reference);
			}
			current_executor = null;
		}
		
		private final class ExecutorWatchDog extends Thread {
			Executor executor;
			
			private ExecutorWatchDog(Executor executor) {
				this.executor = executor;
				setName("ExecutorWatchDog for " + executor.getName());
				setDaemon(true);
			}
			
			public void run() {
				try {
					while (executor.isAlive()) {
						if (executor.job.isMaxExecutionTimeIsReached()) {
							forceStopProcess();
							return;
						}
						sleep(1000);
					}
				} catch (Exception e) {
					Log2.log.error("Can't monitor Executor", e);
				}
			}
		}
	}
	
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
					switch (stopreason) {
					case full_functionnal:
						return WorkerState.PROCESSING;
					case refuse_new_jobs:
						return WorkerState.PENDING_STOP;
					case simple_job_stop:
						return WorkerState.PENDING_STOP;
					}
				} else {
					current_executor = null;
				}
			}
			switch (stopreason) {
			case full_functionnal:
				return WorkerState.WAITING;
			case refuse_new_jobs:
				return WorkerState.STOPPED;
			case simple_job_stop:
				return WorkerState.WAITING;
			}
			return WorkerState.WAITING;
		}
		
		final void enable() {
			stopreason = StopReason.full_functionnal;
		}
		
		private final void askToStop() {
			if (getState() == WorkerState.PROCESSING) {
				try {
					if (current_executor != null) {
						Log2.log.debug("Force stop process", current_executor.job);
					}
					forceStopProcess();
				} catch (Exception e) {
					manager.getServiceException().onError(e, "Can't stop current process", reference);
				}
			}
		}
		
		final void askToStopAndRefuseNewJobs() {
			askToStop();
			stopreason = StopReason.refuse_new_jobs;
		}
		
		final void justAskToStop() {
			askToStop();
			stopreason = StopReason.simple_job_stop;
		}
		
		final boolean isThisState(WorkerState... states) {
			if (states == null) {
				return false;
			}
			if (states.length == 0) {
				return false;
			}
			WorkerState state = getState();
			for (int pos = 0; pos < states.length; pos++) {
				if (state == states[pos]) {
					return true;
				}
			}
			return false;
		}
		
	}
	
	final public LifeCycle getLifecyle() {
		return lifecyle;
	}
	
	final void internalProcess(JobNG job) {
		current_executor = new Executor(job, this);
		current_executor.start();
	}
	
	final String getReferenceKey() {
		return reference_key;
	}
	
	final JsonObject getManagerReference() {
		JsonObject jo = new JsonObject();
		jo.addProperty("app_name", manager.getInstance_status().getAppName());
		jo.addProperty("host_name", manager.getInstance_status().getHostName());
		jo.addProperty("instance_name", manager.getInstance_status().getInstanceName());
		jo.addProperty("instance_ref", manager.getInstance_status().getInstanceNamePid());
		return jo;
	}
	
	final JobNG getCurrentJob() {
		if (current_executor == null) {
			return null;
		}
		if (current_executor.isAlive() == false) {
			return null;
		}
		return current_executor.job;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.addAll(exporter);
		if (current_executor != null) {
			dump.add("executor", current_executor.getName() + " [" + current_executor.getId() + "]");
			dump.add("executor is alive", current_executor.isAlive());
		}
		dump.add("stopreason", stopreason);
		return dump;
	}
	
	public final void doAnAction(JsonObject order) {
		if (order.has("state")) {
			if (order.get("state").getAsString().equals("enable")) {
				lifecyle.enable();
				Log2.log.info("Enable worker", this);
			} else if (order.get("state").getAsString().equals("disable")) {
				lifecyle.askToStopAndRefuseNewJobs();
				Log2.log.info("Disable worker", this);
			} else if (order.get("state").getAsString().equals("stop")) {
				lifecyle.justAskToStop();
				Log2.log.info("Stop current job", this);
			}
		}
		
	}
	
}
