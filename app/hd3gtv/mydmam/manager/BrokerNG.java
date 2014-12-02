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
import hd3gtv.mydmam.manager.JobNG.JobStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.netflix.astyanax.model.ConsistencyLevel;
import com.netflix.astyanax.recipes.locks.BusyLockException;
import com.netflix.astyanax.recipes.locks.ColumnPrefixDistributedRowLock;
import com.netflix.astyanax.recipes.locks.StaleLockException;

/**
 * TODO Replace Broker
 */
class BrokerNG {
	
	/**
	 * In msec
	 */
	private static final int QUEUE_SLEEP_TIME = 1000;
	
	private AppManager manager;
	private Queue queue;
	
	BrokerNG(AppManager manager) {
		this.manager = manager;
	}
	
	void start() {
		if (isAlive()) {
			return;
		}
		queue = new Queue();
		queue.start();
	}
	
	synchronized void askStop() {
		if (queue == null) {
			return;
		}
		queue.stop_queue = true;
	}
	
	boolean isAlive() {
		if (queue == null) {
			return false;
		}
		return queue.isAlive();
	}
	
	private class Queue extends Thread {
		boolean stop_queue;
		
		public Queue() {
			setName("Queue for Broker " + manager.getInstance_status().getInstanceNamePid());
			setDaemon(true);
		}
		
		public void run() {
			stop_queue = false;
			try {
				while (stop_queue == false) {
					// TODO queue
					
					/*
					From BrokerQueue
					//* Get statuses of actual processing jobs and push it to database
					if (activejobs.isEmpty() == false) {
					mutator = CassandraDb.prepareMutationBatch();
					
					Job job;
					for (int pos = activejobs.size() - 1; pos > -1; pos--) {
						job = activejobs.get(pos);
						if (job.delete_after_done && (job.status == TaskJobStatus.DONE || job.status == TaskJobStatus.CANCELED)) {
							mutator.withRow(Broker.CF_TASKQUEUE, job.key).delete();
							job.pushToDatabaseEndLifejob(mutator, Broker.ttl_job_cyclic_endlife_duration);
							activejobs.remove(pos);
						} else {
							if (job.status == TaskJobStatus.PREPARING | job.status == TaskJobStatus.PROCESSING) {
								job.pushToDatabase(mutator, Broker.ttl_job_process_duration);
							} else {
								job.pushToDatabase(mutator, Broker.ttl_job_ended_duration);
							}
							if (job.end_date > 0) {
								activejobs.remove(pos);
							}
						}
					}
					
					if (mutator.isEmpty() == false) {
						mutator.execute();
					}
					}
					
					time_spacer++;
					if (time_spacer == max_time_spacer) {
					mutator = CassandraDb.prepareMutationBatch();
					
					 //* Callbacks triggers for new terminated tasks
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
					}
					
					if (do_stop) {
						return;
					}
					
					if (activecleantasks) {
						time_spacer = 0;
						
						 //* Watch old abandoned task
						IndexQuery<String, String> index_query = Broker.keyspace.prepareQuery(Broker.CF_TASKQUEUE).searchWithIndex();
						Task.selectTooOldWaitingTasks(index_query, Broker.hostname);
						OperationResult<Rows<String, String>> rows = index_query.execute();
						boolean has_send_a_mail = false;
						for (Row<String, String> row : rows.getResult()) {
							Task task = new Task();
							task.pullFromDatabase(row.getKey(), row.getColumns());
							task.status = TaskJobStatus.TOO_OLD;
							task.pushToDatabase(mutator, Broker.ttl_task_duration);
							Log2.log.info("This task is too old", task);
							if (has_send_a_mail == false) {
								AdminMailAlert.create("There is a too old task in queue", false).addDump(task).setServiceinformations(broker.serviceinformations).send();
								has_send_a_mail = true;
							}
						}
						
						 //* Remove max_date for postponed tasks
						index_query = Broker.keyspace.prepareQuery(Broker.CF_TASKQUEUE).searchWithIndex();
						Task.selectPostponedTasksWithMaxAge(index_query, Broker.hostname);
						rows = index_query.execute();
						for (Row<String, String> row : rows.getResult()) {
							Task task = new Task();
							task.pullFromDatabase(row.getKey(), row.getColumns());
							task.max_date_to_wait_processing = Long.MAX_VALUE;
							task.pushToDatabase(mutator, Broker.ttl_task_duration);
							Log2.log.info("Remove max age to postponed task", task);
						}
						
						Log2.log.debug("Broker clean operation");
					}
					
					if (mutator.isEmpty() == false) {
						mutator.execute();
					}
					}

					
					*/
					
					/*
					 * From WorkerGroupEngine
					setStatusChangesToWorkers();
					
					WorkerCyclicEngine workercyclic;
					for (int pos = 0; pos < workergroup.getWorkercycliclist().size(); pos++) {
					workercyclic = workergroup.getWorkercycliclist().get(pos);
					if (workercyclic.isAvailableForProcessing()) {
						workercyclic.engine = new WorkerEngine(workercyclic, new Job(), workergroup.getServiceinformations());
						workercyclic.engine.start();
					}
					}
					
					//get managed profiles for available workers
					ArrayList<Profile> available_workersprofile = new ArrayList<Profile>();
					for (int pos = 0; pos < workergroup.getWorkerlist().size(); pos++) {
					if (workergroup.getWorkerlist().get(pos).isAvailableForProcessing()) {
						available_workersprofile.addAll(workergroup.getWorkerlist().get(pos).getManagedProfiles());
					}
					}
					
					//Found a job, and start it
					if (available_workersprofile.isEmpty() == false) {
					Job job = broker.getNextJob(available_workersprofile);
					if (job != null) {
						Worker worker = getAvailableWorkerForProfiles(job.profile);
						worker.engine = new WorkerEngine(worker, job, workergroup.getServiceinformations());
						worker.engine.start();
					}
					}
					
					//clean terminated workers engines
					for (int pos = 0; pos < workergroup.getWorkerlist().size(); pos++) {
					if (workergroup.getWorkerlist().get(pos).engine != null) {
						if (workergroup.getWorkerlist().get(pos).engine.isAlive() == false) {
							workergroup.getWorkerlist().get(pos).engine = null;
						}
					}
					}
					
					broker.updateWorkersStatus(workergroup.getWorkerlist(), workergroup.getWorkercycliclist());
					
					sleep(WorkerGroup.sleep_refresh_engine + Math.round(Math.random() * 1000));
					}
					stopAllActiveWorkers();

					
					*/
					Thread.sleep(QUEUE_SLEEP_TIME);
				}
			} catch (Exception e) {
				manager.getServiceException().onGenericServiceError(e, "Broker fatal error", "Broker");
			}
		}
	}
	
	/**
	 * TODO @return multiple ?
	 */
	private JobNG getNextJob() throws Exception {
		Map<Class<? extends JobContext>, List<WorkerNG>> available_workers_capablities = manager.getAllCurrentWaitingWorkersByCapablitiesJobContextClasses();
		
		if (available_workers_capablities.isEmpty()) {
			return null;
		}
		
		/**
		 * Prepare a list with all JobContext classes names that can be process.
		 */
		ArrayList<String> available_classes_names = new ArrayList<String>();
		Set<Class<? extends JobContext>> available_classes = available_workers_capablities.keySet();
		for (Class<? extends JobContext> available_class : available_classes) {
			if (available_classes_names.contains(available_class.getName()) == false) {
				available_classes_names.add(available_class.getName());
			}
		}
		ColumnPrefixDistributedRowLock<String> lock = null;
		try {
			/**
			 * Prepare and acquire lock for CF
			 */
			lock = JobNG.prepareLock();
			lock.withConsistencyLevel(ConsistencyLevel.CL_ALL);
			lock.expireLockAfter(500, TimeUnit.MILLISECONDS);
			lock.failOnStaleLock(false);
			lock.acquire();
			
			// Get all waiting task for this category profile.
			
			List<JobNG> waiting_jobs = JobNG.getJobsByStatus(JobStatus.WAITING);
			
			int bestpriority = Integer.MIN_VALUE;
			JobNG best_task = null;
			JobNG current_job = null;
			
			/**
			 * For this jobs, found the best to start.
			 */
			for (int pos_wj = 0; pos_wj < waiting_jobs.size(); pos_wj++) {
				current_job = waiting_jobs.get(pos_wj);
				
				if (current_job.isRequireIsDone() == false) {
					/**
					 * If the job require the processing done of another task.
					 */
					continue;
				}
				if (current_job.isTooOldjob()) {
					/**
					 * This job is to old !
					 */
					continue;
				}
				if (current_job.getPriority() < bestpriority) {
					/**
					 * This job priority is not the best for the moment
					 */
					continue;
				}
				// TODO continue next job...
				/*
					for (int pos = 0; pos < l_profiles_for_cat.size(); pos++) {
						if (l_profiles_for_cat.get(pos).equalsIgnoreCase(task.profile.name)) {
							 // Profile name is handled
							bestpriority = task.priority;
							best_task = task;
							break;
						}
					}
				*/
			}
			
			if (best_task == null) {
				/**
				 * Not found a valid job
				 */
				lock.release();
				return null;
			}
			
			/* // Prepare Job from valid Task
			Job job = best_task.toJob();
			job.status = TaskJobStatus.PREPARING;
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			job.pushToDatabase(mutator, ttl_job_process_duration);
			mutator.execute();
			
			 // Job is ready to start, release lock.
			lock.release();
			Log2.log.debug("Get next job", job);
			
			if (queue != null) {
				//Add Job to active list for watching it.
				queue.getActivejobs().add(job);
			}
			return job;*/
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
		return null;
	}
}
