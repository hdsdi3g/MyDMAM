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

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;
import com.netflix.astyanax.serializers.StringSerializer;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.db.AllRowsFoundRow;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.manager.JobNG.JobStatus;
import hd3gtv.mydmam.manager.WorkerNG.WorkerState;
import hd3gtv.tools.StoppableThread;

public class BrokerNG {
	
	/**
	 * In msec
	 */
	private static final int QUEUE_SLEEP_TIME = 5000;
	private static final int GRACE_PERIOD_TO_REMOVE_DELETED_AFTER_COMPLETED_JOB = 1000 * 30;
	
	private static Keyspace keyspace;
	private static final ColumnFamily<String, String> CF_DONE_JOBS = new ColumnFamily<String, String>("mgrDoneJobs", StringSerializer.get(), StringSerializer.get());
	
	static {
		try {
			keyspace = CassandraDb.getkeyspace();
			String default_keyspacename = CassandraDb.getDefaultKeyspacename();
			if (CassandraDb.isColumnFamilyExists(keyspace, CF_DONE_JOBS.getName()) == false) {
				CassandraDb.createColumnFamilyString(default_keyspacename, CF_DONE_JOBS.getName(), false);
			}
		} catch (Exception e) {
			Loggers.Manager.error("Can't init database CFs", e);
		}
	}
	
	private static final JsonParser parser = new JsonParser();
	
	public static JsonArray getAllDoneJobs() throws Exception {
		final JsonArray ja = new JsonArray();
		
		CassandraDb.allRowsReader(CF_DONE_JOBS, new AllRowsFoundRow() {
			public void onFoundRow(Row<String, String> row) throws Exception {
				ja.add(parser.parse(row.getColumns().getColumnByName("source").getStringValue()));
			}
		}, "source");
		
		return ja;
	}
	
	/**
	 * End of static realm
	 */
	
	private AppManager manager;
	private volatile List<JobNG> active_jobs;
	private volatile ArrayList<CyclicJobCreator> declared_cyclics;
	private volatile ArrayList<TriggerJobCreator> declared_triggers;
	private boolean active_clean_jobs;
	private QueueWatchDog watch_dog;
	
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
		watch_dog = new QueueWatchDog();
	}
	
	ArrayList<CyclicJobCreator> getDeclared_cyclics() {
		return declared_cyclics;
	}
	
	ArrayList<TriggerJobCreator> getDeclared_triggers() {
		return declared_triggers;
	}
	
	void cyclicJobsRegister(CyclicJobCreator cyclic_creator) {
		if (cyclic_creator == null) {
			throw new NullPointerException("\"cyclic_creator\" can't to be null");
		}
		
		try {
			String first_context_key = cyclic_creator.getFirstContextKey();
			Loggers.Manager.trace("Get last end date for this cyclic job: " + cyclic_creator.toString() + ", with first_context_key: " + first_context_key);
			Column<String> cols = keyspace.prepareQuery(CF_DONE_JOBS).getKey(first_context_key).getColumn("end_date").execute().getResult();
			if (cols.hasValue()) {
				cyclic_creator.setLastDateCreatedJobLikeThis(cols.getLongValue());
			}
		} catch (ConnectionException e) {
			Loggers.Manager.warn("Can't access to Cassandra CF", e);
		}
		
		declared_cyclics.add(cyclic_creator);
		Loggers.Manager.debug("Register cyclic job creator: " + cyclic_creator.toString());
	}
	
	void triggerJobsRegister(TriggerJobCreator trigger_creator) {
		if (trigger_creator == null) {
			throw new NullPointerException("\"trigger_creator\" can't to be null");
		}
		declared_triggers.add(trigger_creator);
		Loggers.Manager.debug("Register trigger job creator: " + trigger_creator.toString());
	}
	
	void start() {
		if (isAlive()) {
			return;
		}
		watch_dog = new QueueWatchDog();
		watch_dog.start();
	}
	
	synchronized void askStop() {
		if (isAlive()) {
			watch_dog.waitToStop();
		}
	}
	
	boolean isAlive() {
		return watch_dog.isAlive();
	}
	
	private class QueueOperations extends StoppableThread {
		
		public QueueOperations() {
			super("Queue operations for Broker " + manager.getInstanceStatus().summary.getInstanceNamePid());
			setLogger(Loggers.Broker);
			Loggers.Broker.debug("Init queue operations thread for " + manager.getInstanceStatus().summary.getInstanceNamePid());
		}
		
		public void run() {
			try {
				MutationBatch mutator = null;
				JobNG job;
				int time_spacer = 0;
				int max_time_spacer = 10;
				List<JobNG> jobs;
				CyclicJobCreator cyclic_creator;
				long precedent_date_trigger = System.currentTimeMillis();
				HashMap<String, TriggerJobCreator> map_triggers = new HashMap<String, TriggerJobCreator>();
				Rows<String, String> rows = null;
				
				while (isWantToRun()) {
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
									Loggers.Broker.trace("Remove job from active_jobs [" + job.getKey() + "] " + job.getName() + ";\treason: DAC " + job.getStatus());
									job.delete(mutator);
									active_jobs.remove(pos);
								} else {
									job.saveChanges(mutator);
								}
							} else {
								job.saveChanges(mutator);
								if (job.isThisStatus(JobStatus.DONE, JobStatus.STOPPED, JobStatus.CANCELED, JobStatus.ERROR, JobStatus.TOO_LONG_DURATION)) {
									Loggers.Broker.trace("Remove job from active_jobs [" + job.getKey() + "] " + job.getName() + ";\treason: " + job.getStatus());
									active_jobs.remove(pos);
								}
							}
							if (job.isThisStatus(JobStatus.DONE)) {
								Loggers.Broker.trace("Set done job [" + job.getKey() + "] " + job.getName());
								if (job.getContext() != null) {
									String key = JobContext.Utility.prepareContextKeyForTrigger(job.getContext());
									mutator.withRow(CF_DONE_JOBS, key).putColumn("source", AppManager.getGson().toJson(job), JobNG.TTL_WAITING);
									mutator.withRow(CF_DONE_JOBS, key).putColumn("end_date", job.getEndDate(), JobNG.TTL_WAITING);
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
					
					if (declared_cyclics.isEmpty() == false) {
						for (int pos_dc = 0; pos_dc < declared_cyclics.size(); pos_dc++) {
							cyclic_creator = declared_cyclics.get(pos_dc);
							if (cyclic_creator.needToCreateJobs()) {
								if (mutator == null) {
									mutator = CassandraDb.prepareMutationBatch();
								}
								Loggers.Broker.debug("Cyclic create jobs [" + cyclic_creator.getReferenceKey() + "] " + cyclic_creator.getLongName());
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
							Loggers.Broker.debug("Prepare trigger hooks create jobs (" + declared_triggers.size() + " items) since " + Loggers.dateLog(precedent_date_trigger));
							
							map_triggers.clear();
							for (int pos = 0; pos < declared_triggers.size(); pos++) {
								map_triggers.put(declared_triggers.get(pos).getContextHookTriggerKey(), declared_triggers.get(pos));
							}
							
							rows = keyspace.prepareQuery(CF_DONE_JOBS).getKeySlice(map_triggers.keySet()).withColumnSlice("end_date").execute().getResult();
							for (int pos = 0; pos < rows.size(); pos++) {
								long last_date = rows.getRowByIndex(pos).getColumns().getLongValue("end_date", 0l);
								if (last_date > precedent_date_trigger) {
									map_triggers.get(rows.getRowByIndex(pos).getKey()).createJobs(mutator);
								}
							}
							
							precedent_date_trigger = System.currentTimeMillis();
						}
						
						if (active_clean_jobs) {
							Loggers.Broker.debug("Watch old abandoned jobs...");
							jobs = JobNG.Utility.watchOldAbandonedJobs(mutator, manager.getInstanceStatus());
							if (jobs.isEmpty() == false) {
								Loggers.Broker.debug("Watch old abandoned jobs: there are too old jobs (" + jobs.size() + ") in queue:\t" + jobs);
								manager.getServiceException().onQueueJobProblem("There are too old jobs in queue", jobs);
							}
							Loggers.Broker.debug("Remove max date for postponed jobs for " + manager.getInstanceStatus().summary.getHostName());
							JobNG.Utility.removeMaxDateForPostponedJobs(mutator, manager.getInstanceStatus().summary.getHostName());
						}
						
						if (mutator.isEmpty() == false) {
							mutator.execute();
							mutator = null;
						}
						
					}
					
					stoppableSleep(QUEUE_SLEEP_TIME);
				}
			} catch (Exception e) {
				manager.getServiceException().onQueueServiceError(e, "Broker fatal error", "Broker operations");
			}
		}
	}
	
	private class QueueNewJobs extends StoppableThread {
		
		public QueueNewJobs() {
			super("Queue new jobs for Broker " + manager.getInstanceStatus().summary.getInstanceNamePid());
			setLogger(Loggers.Broker);
			Loggers.Broker.debug("Init queue new jobs thread for " + manager.getInstanceStatus().summary.getInstanceNamePid());
		}
		
		public void run() {
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
				
				while (isWantToRun()) {
					if (first_start == false) {
						/**
						 * Don't sleep at the start for speed up start.
						 */
						stoppableSleep(QUEUE_SLEEP_TIME);
					}
					first_start = false;
					
					/**
					 * Prepare a list with all JobContext classes names that can be process.
					 */
					available_classes_names.clear();
					available_workers_capablities.clear();
					
					enabled_workers = manager.getEnabledWorkers();
					Loggers.Broker.trace("Get get waiting workers and list all capablities...");
					
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
					
					Loggers.Broker.trace("1st pass: check if there are some waiting jobs to process here, before to try lock.");
					
					waiting_jobs = JobNG.Utility.getJobsByStatus(JobStatus.WAITING);
					some_jobs_to_execute = false;
					
					if (waiting_jobs.isEmpty() == false && Loggers.Broker.isDebugEnabled()) {
						Loggers.Broker.debug("available_workers_capablities dump list\t" + available_workers_capablities);
					}
					
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
						
						// if (workers.isEmpty() == false && Loggers.Broker.isDebugEnabled()) {
						// Loggers.Broker.debug("workers dump list avaliable for " + context.getClass() + "\t" + workers);
						// }
						
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
					
					Loggers.Broker.trace("2nd pass: there are some waiting jobs to process here, try to lock and execute.");
					
					lock = null;
					try {
						Loggers.Broker.trace("Prepare and acquire lock for CF.");
						
						lock = JobNG.prepareLock();
						lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
						lock.expireLockAfter(500, TimeUnit.MILLISECONDS);
						lock.failOnStaleLock(false);
						lock.acquire();
						
						Loggers.Broker.trace("Get all waiting jobs for this category profile.");
						
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
							
							if (Loggers.Broker.isDebugEnabled()) {
								Loggers.Broker.debug("workers dump list avaliable for " + context.getClass() + "\t" + workers);
							}
							
							worker = null;
							for (int pos_wr = 0; pos_wr < workers.size(); pos_wr++) {
								if (workers.get(pos_wr).canProcessThis(context)) {
									worker = workers.get(pos_wr);
									break;
								}
							}
							
							if (worker == null) {
								Loggers.Broker.trace("Can't found an avaliable worker for this context: " + context.getClass());
								continue;
							}
							
							if (current_job.isRequireIsDone() == false) {
								/**
								 * If the job require the processing done of another job.
								 */
								continue;
							}
							
							if (best_jobs_worker.containsValue(worker)) {
								/**
								 * This worker is actually reserved by a previous job attribution, and can't to link to a new job.
								 * This is only happend if a worker as several capabilities.
								 */
								continue;
							}
							
							/**
							 * This is actually the best jobs found.
							 */
							best_jobs_worker.put(current_job, worker);
							
							if (workers.remove(worker) == false) {
								throw new Exception("Broker exception: can't remove a worker (" + worker.toStringLight() + ") from waiting workers list");
							}
						}
						
						/**
						 * Now, start all selected jobs, if exists.
						 */
						
						if (best_jobs_worker.isEmpty()) {
							Loggers.Broker.trace("Not found a valid job, release lock");
							
							lock.release();
							continue;
						}
						
						mutator = CassandraDb.prepareMutationBatch();
						for (JobNG job : best_jobs_worker.keySet()) {
							if (Loggers.Job.isDebugEnabled()) {
								Loggers.Broker.debug("Prepare new job:\t" + job.toStringLight());
							}
							job.prepareProcessing(mutator);
						}
						mutator.execute();
						
						Loggers.Broker.trace("Release lock");
						lock.release();
						
						active_jobs.addAll(best_jobs_worker.keySet());
						
						for (Map.Entry<JobNG, WorkerNG> entry : best_jobs_worker.entrySet()) {
							Loggers.Broker.trace("Start new job [" + entry.getKey().getKey() + "] " + entry.getKey().getName());
							entry.getValue().internalProcess(entry.getKey());
						}
						
						stoppableSleep(Math.round(Math.random() * 3000));
					} catch (StaleLockException e) {
						/**
						 * The row contains a stale or these can either be manually clean up or automatically cleaned up (and ignored) by calling failOnStaleLock(false)
						 */
						Loggers.Broker.debug("Can't lock CF: abandoned lock", e);
					} catch (BusyLockException e) {
						Loggers.Broker.debug("Can't lock CF, it's currently locked", e);
					} finally {
						if (lock != null) {
							Loggers.Broker.trace("Finally, release lock");
							lock.release();
						}
					}
				}
			} catch (Exception e) {
				manager.getServiceException().onQueueServiceError(e, "Broker fatal error", "Broker new jobs");
			}
		}
	}
	
	/**
	 * Start, watch and stop Operations and NewJobs
	 */
	private class QueueWatchDog extends StoppableThread {
		
		public QueueWatchDog() {
			super("Queue Watchdog");
			setLogger(Loggers.Broker);
		}
		
		public void run() {
			/**
			 * Start zone
			 */
			if (Loggers.Broker.isDebugEnabled()) {
				Loggers.Broker.debug("Start Broker");
			}
			
			QueueOperations queue_operations = new QueueOperations();
			queue_operations.start();
			
			QueueNewJobs queue_new_jobs = new QueueNewJobs();
			queue_new_jobs.start();
			
			while (isWantToRun()) {
				if (queue_operations.isAlive() == false) {
					Loggers.Broker.warn("QueueOperations thread is not alive...");
					stoppableSleep(10000);
					if (isWantToStop()) {
						break;
					}
					queue_operations = new QueueOperations();
					queue_operations.start();
				}
				
				if (queue_new_jobs.isAlive() == false) {
					Loggers.Broker.warn("QueueNewJobs thread is not alive...");
					stoppableSleep(10000);
					if (isWantToStop()) {
						break;
					}
					queue_new_jobs = new QueueNewJobs();
					queue_new_jobs.start();
				}
				
				stoppableSleep(10);
			}
			
			/**
			 * Stop zone
			 */
			if (Loggers.Broker.isDebugEnabled()) {
				Loggers.Broker.debug("Stop Broker");
			}
			
			queue_operations.wantToStop();
			queue_new_jobs.wantToStop();
			
			queue_operations.waitToStop();
			queue_new_jobs.waitToStop();
		}
	}
}
