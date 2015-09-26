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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.JobNG.JobStatus;
import hd3gtv.mydmam.manager.WorkerNG.WorkerState;

class BrokerNG {
	
	/**
	 * In msec
	 */
	private static final int QUEUE_SLEEP_TIME = 5000;
	private static final int GRACE_PERIOD_TO_REMOVE_DELETED_AFTER_COMPLETED_JOB = 1000 * 30;
	
	private AppManager manager;
	private QueueOperations queue_operations;
	private QueueNewJobs queue_new_jobs;
	private volatile List<JobNG> active_jobs;
	private volatile ArrayList<CyclicJobCreator> declared_cyclics;
	private volatile ArrayList<TriggerJobCreator> declared_triggers;
	private boolean active_clean_jobs;
	
	BrokerNG(AppManager manager) {
		this.manager = manager;
		active_jobs = new ArrayList<JobNG>();
		declared_cyclics = new ArrayList<CyclicJobCreator>();
		declared_triggers = new ArrayList<TriggerJobCreator>();
		
		if (Configuration.global.isElementKeyExists("service", "brokercleanjobs")) {
			active_clean_jobs = Configuration.global.getValueBoolean("service", "brokercleanjobs");
		} else {
			active_clean_jobs = true;
		}
	}
	
	ArrayList<CyclicJobCreator> getDeclared_cyclics() {
		return declared_cyclics;
	}
	
	ArrayList<TriggerJobCreator> getDeclared_triggers() {
		return declared_triggers;
	}
	
	void start() {
		if (isAlive()) {
			return;
		}
		queue_operations = new QueueOperations();
		queue_operations.start();
		queue_new_jobs = new QueueNewJobs();
		queue_new_jobs.start();
	}
	
	synchronized void askStop() {
		if (queue_operations != null) {
			queue_operations.stop_queue = true;
		}
		if (queue_new_jobs != null) {
			queue_new_jobs.stop_queue = true;
		}
	}
	
	boolean isAlive() {
		if ((queue_operations == null) | (queue_new_jobs == null)) {
			return false;
		}
		return queue_operations.isAlive() | queue_new_jobs.isAlive();
	}
	
	private class QueueOperations extends Thread {
		boolean stop_queue;
		
		public QueueOperations() {
			setName("Queue operations for Broker " + manager.getInstance_status().getInstanceNamePid());
			setDaemon(true);
		}
		
		public void run() {
			stop_queue = false;
			try {
				MutationBatch mutator = null;
				JobNG job;
				int time_spacer = 0;
				int max_time_spacer = 10;
				List<JobNG> jobs;
				CyclicJobCreator cyclic_creator;
				long precedent_date_trigger = System.currentTimeMillis();
				
				while (stop_queue == false) {
					if (active_jobs.isEmpty() == false) {
						/**
						 * Get statuses of actual processing jobs and push it to database
						 */
						if (mutator == null) {
							mutator = CassandraDb.prepareMutationBatch();
						}
						
						for (int pos = active_jobs.size() - 1; pos > -1; pos--) {
							job = active_jobs.get(pos);
							if (job.isDeleteAfterCompleted() && job.isThisStatus(JobStatus.DONE, JobStatus.CANCELED)) {
								if ((job.getEndDate() + GRACE_PERIOD_TO_REMOVE_DELETED_AFTER_COMPLETED_JOB) < System.currentTimeMillis()) {
									job.delete(mutator);
									active_jobs.remove(pos);
								} else {
									job.saveChanges(mutator);
								}
							} else {
								job.saveChanges(mutator);
								if (job.isThisStatus(JobStatus.DONE, JobStatus.STOPPED, JobStatus.CANCELED, JobStatus.ERROR, JobStatus.TOO_LONG_DURATION)) {
									active_jobs.remove(pos);
								}
							}
							if (job.isThisStatus(JobStatus.DONE)) {
								TriggerJobCreator.doneJob(job, mutator);
							}
						}
					}
					
					if (mutator != null) {
						if (mutator.isEmpty() == false) {
							mutator.execute();
							mutator = null;
						}
					}
					
					if (declared_cyclics.isEmpty() == false) {
						for (int pos_dc = 0; pos_dc < declared_cyclics.size(); pos_dc++) {
							cyclic_creator = declared_cyclics.get(pos_dc);
							if (cyclic_creator.needToCreateJobs()) {
								if (mutator == null) {
									mutator = CassandraDb.prepareMutationBatch();
								}
								Log2.log.debug("Cyclic create jobs", cyclic_creator);
								cyclic_creator.createJobs(mutator);
							}
						}
						
						if (mutator != null) {
							if (mutator.isEmpty() == false) {
								mutator.execute();
								mutator = null;
							}
						}
					}
					
					time_spacer++;
					if (time_spacer == max_time_spacer) {
						mutator = CassandraDb.prepareMutationBatch();
						time_spacer = 0;
						
						if (declared_triggers.isEmpty() == false) {
							TriggerJobCreator.prepareTriggerHooksCreateJobs(declared_triggers, precedent_date_trigger, mutator);
							precedent_date_trigger = System.currentTimeMillis();
						}
						
						if (active_clean_jobs) {
							jobs = JobNG.Utility.watchOldAbandonedJobs(mutator, manager.getInstance_status());
							if (jobs.isEmpty() == false) {
								manager.getServiceException().onQueueJobProblem("There are too old jobs in queue", jobs);
							}
							JobNG.Utility.removeMaxDateForPostponedJobs(mutator, manager.getInstance_status());
						}
						
						if (mutator.isEmpty() == false) {
							mutator.execute();
							mutator = null;
						}
						
					}
					
					if (stop_queue) {
						return;
					}
					
					if (queue_new_jobs != null) {
						if (queue_new_jobs.isAlive() == false) {
							throw new Exception("Queue for new jobs is terminated, stop operations");
						}
					}
					Thread.sleep(QUEUE_SLEEP_TIME);
				}
			} catch (Exception e) {
				manager.getServiceException().onQueueServiceError(e, "Broker fatal error", "Broker operations");
			}
		}
	}
	
	private class QueueNewJobs extends Thread {
		boolean stop_queue;
		
		public QueueNewJobs() {
			setName("Queue new jobs for Broker " + manager.getInstance_status().getInstanceNamePid());
			setDaemon(true);
		}
		
		public void run() {
			stop_queue = false;
			boolean first_start = true;
			try {
				MutationBatch mutator = null;
				HashMap<Class<? extends JobContext>, List<WorkerNG>> available_workers_capablities = new HashMap<Class<? extends JobContext>, List<WorkerNG>>();
				ArrayList<String> available_classes_names = new ArrayList<String>();
				
				List<WorkerNG> enabled_workers;
				WorkerNG worker;
				List<Class<? extends JobContext>> current_capablities;
				List<WorkerNG> workers_for_capablity;
				Class<? extends JobContext> current_capablity;
				
				ColumnPrefixDistributedRowLock<String> lock;
				List<JobNG> waiting_jobs;
				LinkedHashMap<JobNG, WorkerNG> best_jobs_worker = new LinkedHashMap<JobNG, WorkerNG>();
				
				JobNG current_job = null;
				List<WorkerNG> workers;
				JobContext context;
				boolean some_jobs_to_execute;
				
				while (stop_queue == false) {
					if (first_start == false) {
						/**
						 * Don't sleep at the start for speed up start.
						 */
						Thread.sleep(QUEUE_SLEEP_TIME);
					}
					first_start = false;
					
					/**
					 * Prepare a list with all JobContext classes names that can be process.
					 */
					available_classes_names.clear();
					available_workers_capablities.clear();
					
					enabled_workers = manager.getEnabledWorkers();
					for (int pos_wr = 0; pos_wr < enabled_workers.size(); pos_wr++) {
						worker = enabled_workers.get(pos_wr);
						if (worker.getLifecyle().getState() != WorkerState.WAITING) {
							continue;
						}
						current_capablities = worker.getWorkerCapablitiesJobContextClasses();
						if (current_capablities == null) {
							continue;
						}
						for (int pos_cc = 0; pos_cc < current_capablities.size(); pos_cc++) {
							current_capablity = current_capablities.get(pos_cc);
							
							if (available_classes_names.contains(current_capablity.getName()) == false) {
								available_classes_names.add(current_capablity.getName());
							}
							
							if (available_workers_capablities.containsKey(current_capablity) == false) {
								available_workers_capablities.put(current_capablity, new ArrayList<WorkerNG>(1));
							}
							workers_for_capablity = available_workers_capablities.get(current_capablity);
							if (workers_for_capablity.contains(worker) == false) {
								workers_for_capablity.add(worker);
							}
						}
					}
					
					if (available_classes_names.isEmpty()) {
						continue;
					}
					
					/**
					 * 1st pass: check if there are some waiting jobs to process here, before to try lock.
					 */
					waiting_jobs = JobNG.Utility.getJobsByStatus(JobStatus.WAITING);
					some_jobs_to_execute = false;
					
					for (int pos_wj = 0; pos_wj < waiting_jobs.size(); pos_wj++) {
						current_job = waiting_jobs.get(pos_wj);
						
						context = current_job.getContext();
						if (available_classes_names.contains(context.getClass().getName()) == false) {
							/**
							 * Can't process this job (no workers for this).
							 */
							continue;
						}
						
						if (current_job.isTooOldjob()) {
							/**
							 * This job is to old !
							 */
							continue;
						}
						
						if (current_job.isRequireIsDone() == false) {
							/**
							 * If the job require the processing done of another job.
							 */
							continue;
						}
						
						workers = available_workers_capablities.get(context.getClass());
						
						for (int pos_wr = 0; pos_wr < workers.size(); pos_wr++) {
							if (workers.get(pos_wr).canProcessThis(context)) {
								/**
								 * This is actually the best job found.
								 */
								some_jobs_to_execute = true;
								break;
							}
						}
					}
					
					if (some_jobs_to_execute == false) {
						continue;
					}
					
					/**
					 * 2nd pass: there are some waiting jobs to process here, try to lock and execute.
					 */
					lock = null;
					try {
						/**
						 * Prepare and acquire lock for CF
						 */
						lock = JobNG.prepareLock();
						lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
						lock.expireLockAfter(500, TimeUnit.MILLISECONDS);
						lock.failOnStaleLock(false);
						lock.acquire();
						
						/**
						 * Get all waiting jobs for this category profile.
						 */
						waiting_jobs = JobNG.Utility.getJobsByStatus(JobStatus.WAITING);
						best_jobs_worker.clear();
						mutator = null;
						
						/**
						 * For this jobs, found the best to start (Try to start a maximum of jobs)
						 */
						for (int pos_wj = 0; pos_wj < waiting_jobs.size(); pos_wj++) {
							current_job = waiting_jobs.get(pos_wj);
							
							context = current_job.getContext();
							if (available_classes_names.contains(context.getClass().getName()) == false) {
								/**
								 * Can't process this job (no workers for this).
								 */
								continue;
							}
							
							if (current_job.isTooOldjob()) {
								/**
								 * This job is to old !
								 */
								continue;
							}
							
							workers = available_workers_capablities.get(context.getClass());
							
							if (workers.isEmpty()) {
								continue;
							}
							
							worker = null;
							for (int pos_wr = 0; pos_wr < workers.size(); pos_wr++) {
								if (workers.get(pos_wr).canProcessThis(context)) {
									worker = workers.get(pos_wr);
									break;
								}
							}
							
							if (current_job.isRequireIsDone() == false) {
								/**
								 * If the job require the processing done of another job.
								 */
								continue;
							}
							
							/**
							 * This is actually the best jobs found.
							 */
							best_jobs_worker.put(current_job, worker);
							workers.remove(worker);
						}
						
						/**
						 * Now, start all selected jobs, if exists.
						 */
						
						if (best_jobs_worker.isEmpty()) {
							/**
							 * Not found a valid job
							 */
							lock.release();
							continue;
						}
						
						/**
						 * Prepare job.
						 */
						mutator = CassandraDb.prepareMutationBatch();
						for (JobNG job : best_jobs_worker.keySet()) {
							job.prepareProcessing(mutator);
						}
						mutator.execute();
						
						lock.release();
						
						active_jobs.addAll(best_jobs_worker.keySet());
						
						for (Map.Entry<JobNG, WorkerNG> entry : best_jobs_worker.entrySet()) {
							entry.getValue().internalProcess(entry.getKey());
						}
						
						if (stop_queue) {
							return;
						}
						Thread.sleep(Math.round(Math.random() * 3000));
					} catch (StaleLockException e) {
						/**
						 * The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false)
						 */
						Log2.log.error("Can't lock CF: abandoned lock.", e);
					} catch (BusyLockException e) {
						Log2.log.debug("Can't lock CF, it's currently locked.");
					} finally {
						if (lock != null) {
							lock.release();
						}
					}
				}
			} catch (Exception e) {
				manager.getServiceException().onQueueServiceError(e, "Broker fatal error", "Broker new jobs");
			}
		}
	}
	
}
