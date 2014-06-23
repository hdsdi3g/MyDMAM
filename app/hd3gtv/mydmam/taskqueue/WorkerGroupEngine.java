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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.mydmam.mail.AdminMailAlert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.RowSliceQuery;

class WorkerGroupEngine extends Thread {
	
	private boolean stopwatching;
	private WorkerGroup workergroup;
	private Broker broker;
	
	WorkerGroupEngine(WorkerGroup referer, Broker broker) {
		this.workergroup = referer;
		if (referer == null) {
			throw new NullPointerException("\"referer\" can't to be null");
		}
		this.broker = broker;
		if (broker == null) {
			throw new NullPointerException("\"broker\" can't to be null");
		}
		setName("WorkerGroupEngine");
		setDaemon(false);
	}
	
	public final void run() {
		try {
			stopwatching = false;
			
			for (Map.Entry<String, Worker> entry : workergroup.getWorkers_key_map().entrySet()) {
				entry.getValue().setEnabled();
			}
			
			while (stopwatching == false) {
				setStatusChangesToWorkers();
				
				WorkerCyclicEngine workercyclic;
				for (int pos = 0; pos < workergroup.getWorkercycliclist().size(); pos++) {
					workercyclic = workergroup.getWorkercycliclist().get(pos);
					if (workercyclic.isAvailableForProcessing()) {
						workercyclic.engine = new WorkerEngine(workercyclic, new Job(), workergroup.getServiceinformations());
						workercyclic.engine.start();
					}
				}
				
				/**
				 * get managed profiles for available workers
				 */
				ArrayList<Profile> available_workersprofile = new ArrayList<Profile>();
				for (int pos = 0; pos < workergroup.getWorkerlist().size(); pos++) {
					if (workergroup.getWorkerlist().get(pos).isAvailableForProcessing()) {
						available_workersprofile.addAll(workergroup.getWorkerlist().get(pos).getManagedProfiles());
					}
				}
				
				/**
				 * Found a job, and start it
				 */
				if (available_workersprofile.isEmpty() == false) {
					Job job = broker.getNextJob(available_workersprofile);
					if (job != null) {
						Worker worker = getAvailableWorkerForProfiles(job.profile);
						worker.engine = new WorkerEngine(worker, job, workergroup.getServiceinformations());
						worker.engine.start();
					}
				}
				
				/**
				 * clean terminated workers engines
				 */
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
		} catch (Exception e) {
			Log2.log.error("Generic error", e);
			AdminMailAlert.create("Worker group error", true).setThrowable(e).setServiceinformations(workergroup.getServiceinformations()).send();
		}
		Log2.log.info("Worker group is ended");
	}
	
	synchronized void stopWatch() {
		stopwatching = true;
	}
	
	private synchronized void stopAllActiveWorkers() throws ConnectionException {
		Log2.log.info("Stop all workers");
		ArrayList<Thread> group = new ArrayList<Thread>();
		
		/**
		 * Prepare stop for all workers
		 */
		for (int pos = 0; pos < workergroup.getWorkerlist().size(); pos++) {
			final Worker worker = workergroup.getWorkerlist().get(pos);
			Thread t = new Thread("Stop for " + worker.getShortWorkerName()) {
				public void run() {
					worker.setDisabled();
					Log2.log.info("Worker is manually stopped");
				}
			};
			t.setDaemon(true);
			t.start();
			group.add(t);
		}
		
		/**
		 * Prepare stop for all workercyclic
		 */
		WorkerCyclicEngine workercyclic;
		for (int pos = 0; pos < workergroup.getWorkercycliclist().size(); pos++) {
			workercyclic = workergroup.getWorkercycliclist().get(pos);
			workercyclic.setDisabled();
			Log2.log.info("WorkerCyclic is manually stopped");
			if (workercyclic.engine != null) {
				group.add(workercyclic.engine);
			}
		}
		
		/**
		 * Wait all close thread are terminated
		 */
		while (group.isEmpty() == false) {
			for (int pos = group.size() - 1; pos > -1; pos--) {
				if (group.get(pos).isAlive() == false) {
					group.remove(pos);
				}
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Log2.log.error("Can't sleep", e);
			}
		}
		
		/**
		 * Update database
		 */
		broker.updateWorkersStatus(workergroup.getWorkerlist(), workergroup.getWorkercycliclist());
	}
	
	private Worker getAvailableWorkerForProfiles(Profile profile) {
		List<Profile> managedprofiles;
		for (int pos = 0; pos < workergroup.getWorkerlist().size(); pos++) {
			if (workergroup.getWorkerlist().get(pos).isAvailableForProcessing()) {
				managedprofiles = workergroup.getWorkerlist().get(pos).getManagedProfiles();
				for (int pos_profile = 0; pos_profile < managedprofiles.size(); pos_profile++) {
					if (managedprofiles.get(pos_profile).equals(profile)) {
						return workergroup.getWorkerlist().get(pos);
					}
				}
			}
		}
		return null;
	}
	
	private void setStatusChangesToWorkers() throws ConnectionException {
		RowSliceQuery<String, String> rs = broker.getKeyspace().prepareQuery(Broker.CF_WORKERGROUPS).getKeySlice(workergroup.getWorkers_key_map().keySet());
		rs.withColumnSlice(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, WorkerGroup.COL_NAME_STATUSCHANGE);
		Rows<String, String> rows = rs.execute().getResult();
		
		WorkerStatusChange newstatus;
		Worker worker;
		WorkerCyclicEngine workercyclic;
		int newperiod;
		if (rows.isEmpty() == false) {
			MutationBatch mutator = CassandraDb.prepareMutationBatch();
			
			for (Row<String, String> row : rows) {
				newstatus = WorkerStatusChange.pullFromDatabase(row.getColumns());
				if (newstatus != null) {
					worker = workergroup.getWorkers_key_map().get(row.getKey());
					if (worker != null) {
						if (newstatus == WorkerStatusChange.ENABLED) {
							worker.setEnabled();
							mutator.withRow(Broker.CF_WORKERGROUPS, row.getKey()).deleteColumn(WorkerGroup.COL_NAME_STATUSCHANGE);
						} else if (newstatus == WorkerStatusChange.DISABLED) {
							worker.setDisabled();
							mutator.withRow(Broker.CF_WORKERGROUPS, row.getKey()).deleteColumn(WorkerGroup.COL_NAME_STATUSCHANGE);
						}
					}
				} else {
					newperiod = row.getColumns().getIntegerValue(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, 0);
					if (newperiod > 0) {
						worker = workergroup.getWorkers_key_map().get(row.getKey());
						if (worker instanceof WorkerCyclicEngine) {
							workercyclic = (WorkerCyclicEngine) worker;
							Log2Dump dump = new Log2Dump();
							dump.add("original value", workercyclic.cyclic.getInitialCyclicPeriodTasks());
							dump.add("actual value", workercyclic.getTime_to_sleep());
							dump.add("new value", newperiod * 1000);
							Log2.log.info("Change worker cyclic time to sleep", dump);
							
							workercyclic.setTime_to_sleep(newperiod * 1000);
							mutator.withRow(Broker.CF_WORKERGROUPS, row.getKey()).deleteColumn(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD);
						}
					}
				}
			}
			mutator.execute();
		}
		
	}
}
