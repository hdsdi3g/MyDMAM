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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.taskqueue;

import hd3gtv.configuration.Configuration;
import hd3gtv.javamailwrapper.MessageAlert;
import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.CassandraDb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONObject;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.IndexQuery;

class BrokerQueue extends Thread {
	
	private ArrayList<Job> activejobs;
	private Broker broker;
	private boolean activecleantasks;
	private HashMap<Profile, List<TriggerWorker>> callbacks_triggers;
	private HashMap<Profile, Long> last_dates_profile_updated;
	
	BrokerQueue(Broker referer) {
		this.broker = referer;
		if (referer == null) {
			throw new NullPointerException("\"referer\" can't to be null");
		}
		setName("Queue");
		setDaemon(true);
		activejobs = new ArrayList<Job>();
		callbacks_triggers = new HashMap<Profile, List<TriggerWorker>>();
		last_dates_profile_updated = new HashMap<Profile, Long>();
		
		if (Configuration.global.isElementKeyExists("service", "brokercleantasks")) {
			activecleantasks = Configuration.global.getValueBoolean("service", "brokercleantasks");
		} else {
			activecleantasks = true;
		}
	}
	
	ArrayList<Job> getActivejobs() {
		return activejobs;
	}
	
	synchronized void addTrigger(TriggerWorker trigger) {
		if (trigger == null) {
			throw new NullPointerException("\"trigger\" can't to be null");
		}
		List<Profile> profile_list = trigger.plugToProfiles();
		if (profile_list == null) {
			return;
		}
		if (profile_list.size() == 0) {
			return;
		}
		
		Profile profile_to_add;
		List<TriggerWorker> trigger_list;
		for (int pos_prolist = 0; pos_prolist < profile_list.size(); pos_prolist++) {
			profile_to_add = profile_list.get(pos_prolist);
			trigger_list = callbacks_triggers.get(profile_to_add);
			if (trigger_list == null) {
				trigger_list = new ArrayList<TriggerWorker>();
			}
			if (trigger_list.contains(trigger) == false) {
				trigger_list.add(trigger);
			}
			callbacks_triggers.put(profile_to_add, trigger_list);
			
			if (last_dates_profile_updated.containsKey(profile_to_add) == false) {
				last_dates_profile_updated.put(profile_to_add, System.currentTimeMillis());
			}
		}
		
	}
	
	synchronized void removeTrigger(TriggerWorker trigger) {
		if (trigger == null) {
			throw new NullPointerException("\"trigger\" can't to be null");
		}
		Profile profile;
		List<TriggerWorker> workers_for_profile;
		ArrayList<Profile> removethis = new ArrayList<Profile>();
		
		for (Map.Entry<Profile, List<TriggerWorker>> entry : callbacks_triggers.entrySet()) {
			profile = entry.getKey();
			workers_for_profile = entry.getValue();
			if (workers_for_profile.contains(trigger)) {
				workers_for_profile.remove(trigger);
			}
			if (workers_for_profile.isEmpty()) {
				removethis.add(profile);
			}
		}
		for (int pos = 0; pos < removethis.size(); pos++) {
			callbacks_triggers.remove(removethis.get(pos));
			last_dates_profile_updated.remove(removethis.get(pos));
		}
		
	}
	
	public void run() {
		try {
			int time_spacer = 0;
			int max_time_spacer = 100;
			long sleeptime = 500;
			MutationBatch mutator;
			int ttl_active_trigger_worker = (int) (max_time_spacer * sleeptime * 2l) / 1000;
			String instancename = Broker.hostname + "-" + ServiceManager.getInstancename();
			
			while (true) {
				
				/**
				 * Get statuses of actual processing jobs and push it to database
				 */
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
					
					/**
					 * Callbacks triggers for new terminated tasks
					 */
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
					
					if (activecleantasks) {
						time_spacer = 0;
						
						/**
						 * Watch old abandoned task
						 */
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
								MessageAlert.create("There is a too old task in queue", false).addDump(task).setServiceinformations(broker.serviceinformations).send();
								has_send_a_mail = true;
							}
						}
						
						/**
						 * Remove max_date for postponed tasks
						 */
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
				
				sleep(sleeptime);
			}
		} catch (Exception e) {
			Log2.log.error("Generic error during broker update", e);
			MessageAlert.create("Error during broker update", true).setThrowable(e).setServiceinformations(broker.serviceinformations).send();
		}
	}
}
