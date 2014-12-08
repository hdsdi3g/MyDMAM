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

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.JobNG.JobStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;

class BrokerNG {
	
	/**
	 * In msec
	 */
	private static final int QUEUE_SLEEP_TIME = 1000;
	
	private AppManager manager;
	private QueueOperations queue_operations;
	private QueueNewJobs queue_new_jobs;
	private volatile List<JobNG> active_jobs;
	private volatile ArrayList<CyclicJobsCreator> declared_cyclics;
	private boolean active_clean_tasks;
	
	BrokerNG(AppManager manager) {
		this.manager = manager;
		active_jobs = new ArrayList<JobNG>();
		declared_cyclics = new ArrayList<CyclicJobsCreator>();
		
		if (Configuration.global.isElementKeyExists("service", "brokercleantasks")) {
			active_clean_tasks = Configuration.global.getValueBoolean("service", "brokercleantasks");
		} else {
			active_clean_tasks = true;
		}
	}
	
	ArrayList<CyclicJobsCreator> getDeclared_cyclics() {
		return declared_cyclics;
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
				int max_time_spacer = 100;
				List<JobNG> jobs;
				CyclicJobsCreator cyclic_creator;
				
				while (stop_queue == false) {
					if (active_jobs.isEmpty() == false) {
						/**
						 * Get statuses of actual processing jobs and push it to database
						 */
						mutator = CassandraDb.prepareMutationBatch();
						for (int pos = active_jobs.size() - 1; pos > -1; pos--) {
							job = active_jobs.get(pos);
							if (job.isDeleteAfterCompleted() && job.isThisStatus(JobStatus.DONE, JobStatus.CANCELED)) {
								job.delete(mutator);
								active_jobs.remove(pos);
							} else {
								job.saveChanges(mutator);
								if (job.isThisStatus(JobStatus.DONE, JobStatus.STOPPED, JobStatus.CANCELED, JobStatus.ERROR)) {
									active_jobs.remove(pos);
								}
							}
						}
					}
					
					if (mutator != null) {
						if (mutator.isEmpty() == false) {
							mutator.execute();
							mutator = null;
						}
					}
					
					if (declared_cyclics.isEmpty() != false) {
						// TODO cyclic only on the off hours
						for (int pos_dc = 0; pos_dc < declared_cyclics.size(); pos_dc++) {
							cyclic_creator = declared_cyclics.get(pos_dc);
							if (cyclic_creator.needToCreateJobs()) {
								if (mutator == null) {
									mutator = CassandraDb.prepareMutationBatch();
								}
								cyclic_creator.createJobs(mutator);
							}
						}
						
						if (mutator != null) {
							if (mutator.isEmpty() == false) {
								mutator.execute();
							}
						}
					}
					// TODO push terminated jobs to db (and keep terminated jobs duration)
					
					time_spacer++;
					if (time_spacer == max_time_spacer) {
						mutator = CassandraDb.prepareMutationBatch();
						time_spacer = 0;
						/* TODO Callback triggers for new terminated tasks
						Profile profile;
						List<TriggerWorker> workers_to_callback;
						long last_date_updated;
						for (Map.Entry<Profile, List<TriggerWorker>> entry : callbacks_triggers.entrySet()) {
						profile = entry.getKey();
						workers_to_callback = entry.getValue();
						last_date_updated = last_dates_profile_updated.get(profile);
						
						if (broker.isRecentJobIsEnded(profile, last_date_updated)) {
							Log2Dump dump = new Log2Dump();
							dump.add("profilekey", profile);
							for (int pos_wks = 0; pos_wks < workers_to_callback.size(); pos_wks++) {
								dump.add("worker", workers_to_callback.get(pos_wks).getTriggerLongName());
								workers_to_callback.get(pos_wks).triggerCreateTasks(profile);
							}
							Log2.log.debug("Trigger", dump);
							last_dates_profile_updated.put(profile, System.currentTimeMillis());
						}
						
						for (int poswkr = 0; poswkr < workers_to_callback.size(); poswkr++) {
							JSONObject jo = new JSONObject();
							jo.put("longname", workers_to_callback.get(poswkr).getTriggerLongName());
							jo.put("shortname", workers_to_callback.get(poswkr).getTriggerShortName());
							mutator.withRow(Broker.CF_QUEUETRIGGER, profile.computeKey()).putColumn(instancename + "_" + profile + "_" + poswkr, jo.toJSONString(), ttl_active_trigger_worker);
						}
						}*/
						
						if (active_clean_tasks) {
							jobs = JobNG.Utility.watchOldAbandonedJobs(mutator, manager.getInstance_status());
							if (jobs.isEmpty() == false) {
								manager.getServiceException().onQueueJobProblem("There are too old jobs in queue", jobs);
							}
							JobNG.Utility.removeMaxDateForPostponedJobs(mutator, manager.getInstance_status());
						}
						
						if (mutator.isEmpty() == false) {
							mutator.execute();
						}
					}
					
					if (stop_queue) {
						return;
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
			try {
				MutationBatch mutator = null;
				Map<Class<? extends JobContext>, List<WorkerNG>> available_workers_capablities;
				ArrayList<String> available_classes_names;
				ColumnPrefixDistributedRowLock<String> lock;
				List<JobNG> waiting_jobs;
				int best_priority;
				JobNG best_job = null;
				JobNG current_job = null;
				List<WorkerNG> workers;
				WorkerNG best_job_worker = null;
				JobContext context;
				
				while (stop_queue == false) {
					Thread.sleep(QUEUE_SLEEP_TIME);
					
					available_workers_capablities = manager.getAllCurrentWaitingWorkersByCapablitiesJobContextClasses();
					
					if (available_workers_capablities.isEmpty()) {
						return;
					}
					
					/**
					 * Prepare a list with all JobContext classes names that can be process.
					 */
					available_classes_names = new ArrayList<String>();
					for (Class<? extends JobContext> available_class : available_workers_capablities.keySet()) {
						if (available_classes_names.contains(available_class.getName()) == false) {
							available_classes_names.add(available_class.getName());
						}
					}
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
						best_priority = Integer.MIN_VALUE;
						best_job = null;
						current_job = null;
						workers = null;
						best_job_worker = null;
						context = null;
						mutator = null;
						
						/**
						 * For this jobs, found the best to start.
						 */
						for (int pos_wj = 0; pos_wj < waiting_jobs.size(); pos_wj++) {
							current_job = waiting_jobs.get(pos_wj);
							
							if (current_job.getPriority() < best_priority) {
								/**
								 * This job priority is not the best for the moment
								 */
								continue;
							}
							
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
								 * If the job require the processing done of another task.
								 */
								continue;
							}
							
							workers = available_workers_capablities.get(context.getClass());
							
							for (int pos_wr = 0; pos_wr < workers.size(); pos_wr++) {
								if (workers.get(pos_wr).canProcessThis(context)) {
									/**
									 * This is actually the best job found.
									 */
									best_priority = current_job.getPriority();
									best_job = current_job;
									best_job_worker = workers.get(pos_wr);
									break;
								}
							}
						}
						
						if (mutator != null) {
							if (mutator.isEmpty() == false) {
								mutator.execute();
							}
						}
						
						if (best_job == null) {
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
						best_job.prepareProcessing(mutator);
						mutator.execute();
						
						lock.release();
						
						active_jobs.add(best_job);
						
						best_job_worker.internalProcess(best_job);
						
						if (stop_queue == false) {
							return;
						}
						Thread.sleep(Math.round(Math.random() * 1000));
					} catch (StaleLockException e) {
						/**
						 * The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false)
						 */
						Log2.log.error("Can't lock CF: abandoned lock.", e);
					} catch (BusyLockException e) {
						Log2.log.error("Can't lock CF, it's currently locked.", e);
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
