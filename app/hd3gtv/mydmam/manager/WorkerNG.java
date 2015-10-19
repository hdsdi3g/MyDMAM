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

import com.google.gson.JsonObject;

import hd3gtv.mydmam.Loggers;

public abstract class WorkerNG implements InstanceActionReceiver {
	
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
		Loggers.Worker.debug("Create worker " + getClass().getName());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(reference_key);
		sb.append("] ");
		sb.append(getClass().getName());
		sb.append(" \"");
		sb.append(getWorkerLongName());
		sb.append("\" ");
		sb.append(stopreason);
		sb.append(" ");
		if (current_executor != null) {
			sb.append(" job:");
			sb.append(current_executor.job.getKey());
		} else {
			sb.append(" job:null");
		}
		return sb.toString();
	}
	
	private String toStringLight() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(reference_key.substring(0, 20));
		sb.append("...]");
		return sb.toString();
	}
	
	void setManager(AppManager manager) {
		this.manager = manager;
		reference_key = "worker:" + UUID.randomUUID().toString();
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
				if (Loggers.Worker.isTraceEnabled()) {
					Loggers.Worker.trace("Worker " + toStringLight() + " can process\t" + context.toString());
				}
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
			setName("WorkerExec:" + getWorkerCategory() + ":" + reference.getClass().getSimpleName() + "/" + job.getKey().substring(0, 12));
			
			this.job = job;
			this.reference = reference;
			setDaemon(true);
			if (Loggers.Worker.isDebugEnabled()) {
				Loggers.Worker.debug("Init worker executor for " + reference.toStringLight());
			}
			if (job.hasAMaxExecutionTime()) {
				current_executor_watch_dog = new ExecutorWatchDog(this);
			}
		}
		
		public void run() {
			if (current_executor_watch_dog != null) {
				current_executor_watch_dog.start();
			}
			try {
				if (Loggers.Worker.isInfoEnabled()) {
					if (job.isDeleteAfterCompleted() & Loggers.Worker.isDebugEnabled()) {
						Loggers.Worker.debug("Start processing DeleAfteComptd job for worker " + reference.toStringLight() + ":\t" + job.toString());
					} else {
						Loggers.Worker.info("Start processing job for worker " + reference.toStringLight() + ":\t" + job.toString());
					}
				}
				job.saveChanges();
				
				workerProcessJob(job.startProcessing(manager, reference), job.getContext());
				
				if (job.isMaxExecutionTimeIsReached()) {
					Loggers.Worker.warn("Job processing has reach the max execution time, for worker " + reference.toStringLight() + ":\t" + job.toString());
					job.endProcessing_TooLongDuration();
					manager.getServiceException().onMaxExecJobTime(job);
				} else {
					switch (stopreason) {
					case full_functionnal:
						if (Loggers.Worker.isDebugEnabled()) {
							Loggers.Worker.debug("Job processing is done, for worker " + reference.toStringLight() + ":\t" + job.toString());
						}
						job.endProcessing_Done();
						break;
					case refuse_new_jobs:
						Loggers.Worker.warn("Job processing is stopped, for worker (and it will refuse new jobs) " + reference.toStringLight() + ":\t" + job.toString());
						job.endProcessing_Stopped();
						break;
					case simple_job_stop:
						Loggers.Worker.warn("This job processing is stopped, for worker " + reference.toStringLight() + ":\t" + job.toString());
						job.endProcessing_Stopped();
						stopreason = StopReason.full_functionnal;
						break;
					}
				}
			} catch (Exception e) {
				Loggers.Worker.error("Error during job processing, for worker " + reference.toStringLight() + ":\t" + job.toString(), e);
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
				Loggers.Worker.debug("Init ExecutorWatchDog for worker " + executor.reference.toString());
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
					Loggers.Worker.error("Can't monitor Executor for worker " + executor.reference.toString(), e);
				}
			}
		}
	}
	
	final class LifeCycle {
		private WorkerNG reference;
		
		private LifeCycle(WorkerNG reference) {
			this.reference = reference;
			if (Loggers.Worker.isDebugEnabled()) {
				Loggers.Worker.debug("Init life cycle for worker " + reference.toStringLight());
			}
		}
		
		final WorkerState getState() {
			if (isActivated() == false) {
				if (Loggers.Worker.isTraceEnabled()) {
					Loggers.Worker.trace("Worker state is disactived " + reference.toStringLight());
				}
				return WorkerState.DISACTIVATED;
			}
			if (current_executor != null) {
				if (current_executor.isAlive()) {
					switch (stopreason) {
					case full_functionnal:
						if (Loggers.Worker.isTraceEnabled()) {
							Loggers.Worker.trace("Worker state is PROCESSING " + reference.toStringLight());
						}
						return WorkerState.PROCESSING;
					case refuse_new_jobs:
						if (Loggers.Worker.isTraceEnabled()) {
							Loggers.Worker.trace("Worker state is PENDING_STOP and refuse_new_jobs " + reference.toStringLight());
						}
						return WorkerState.PENDING_STOP;
					case simple_job_stop:
						if (Loggers.Worker.isTraceEnabled()) {
							Loggers.Worker.trace("Worker state is PENDING_STOP and simple_job_stop " + reference.toStringLight());
						}
						return WorkerState.PENDING_STOP;
					}
				} else {
					current_executor = null;
				}
			}
			switch (stopreason) {
			case full_functionnal:
				if (Loggers.Worker.isTraceEnabled()) {
					Loggers.Worker.trace("Worker state is WAITING new jobs " + reference.toStringLight());
				}
				return WorkerState.WAITING;
			case refuse_new_jobs:
				if (Loggers.Worker.isTraceEnabled()) {
					Loggers.Worker.trace("Worker state is STOPPED and refuse new jobs " + reference.toStringLight());
				}
				return WorkerState.STOPPED;
			case simple_job_stop:
				if (Loggers.Worker.isTraceEnabled()) {
					Loggers.Worker.trace("Worker state is STOP current job " + reference.toStringLight());
				}
				return WorkerState.WAITING;
			}
			return WorkerState.WAITING;
		}
		
		final void enable() {
			if (Loggers.Worker.isTraceEnabled()) {
				Loggers.Worker.trace("Set state to enable " + reference.toStringLight());
			}
			stopreason = StopReason.full_functionnal;
		}
		
		private final void askToStop() {
			if (getState() == WorkerState.PROCESSING) {
				try {
					if (current_executor != null) {
						Loggers.Worker.info("Force stop process " + reference.toStringLight() + " for job:\t" + current_executor.job);
					}
					forceStopProcess();
				} catch (Exception e) {
					Loggers.Worker.error("Can't stop (forced) process " + reference.toStringLight() + " for job:\t" + current_executor.job, e);
					manager.getServiceException().onError(e, "Can't stop current process", reference);
				}
			}
		}
		
		final void askToStopAndRefuseNewJobs() {
			Loggers.Worker.debug("Switch worker state to askToStopAndRefuseNewJobs " + reference.toStringLight());
			askToStop();
			stopreason = StopReason.refuse_new_jobs;
		}
		
		final void justAskToStop() {
			Loggers.Worker.debug("Switch worker state to justAskToStop " + reference.toStringLight());
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
	
	public final void doAnAction(JsonObject order) {
		if (order.has("state")) {
			if (order.get("state").getAsString().equals("enable")) {
				Loggers.Manager.info("Enable worker:\t" + this);
				lifecyle.enable();
			} else if (order.get("state").getAsString().equals("disable")) {
				Loggers.Manager.info("Disable worker:\t" + this);
				lifecyle.askToStopAndRefuseNewJobs();
			} else if (order.get("state").getAsString().equals("stop")) {
				Loggers.Manager.info("Stop current job:\t" + this);
				lifecyle.justAskToStop();
			}
		}
		
	}
	
}
