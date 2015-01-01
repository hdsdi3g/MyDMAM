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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated
 */
@Deprecated
public class WorkerGroup {
	
	private WorkerGroupEngine engine;
	private ArrayList<Worker> workerlist;
	private ArrayList<WorkerCyclicEngine> workercycliclist;
	private HashMap<String, Worker> workers_key_map;
	private HashMap<String, TriggerWorker> triggerlist;
	
	private Broker broker;
	static long sleep_refresh_engine = 1000;
	static final String COL_NAME_CHANGECYCLICPERIOD = "changecyclicperiod";
	static final String COL_NAME_STATUSCHANGE = "statuschange";
	
	public WorkerGroup(Broker broker) {
		this.broker = broker;
		if (broker == null) {
			throw new NullPointerException("\"broker\" can't to be null");
		}
		workerlist = new ArrayList<Worker>();
		workercycliclist = new ArrayList<WorkerCyclicEngine>();
		workers_key_map = new HashMap<String, Worker>();
		triggerlist = new HashMap<String, TriggerWorker>();
	}
	
	ArrayList<WorkerCyclicEngine> getWorkercycliclist() {
		return workercycliclist;
	}
	
	HashMap<String, Worker> getWorkers_key_map() {
		return workers_key_map;
	}
	
	public ArrayList<Worker> getWorkerlist() {
		return workerlist;
	}
	
	public synchronized void start() {
		Log2.log.info("Start worker group");
		if (engine == null) {
			engine = new WorkerGroupEngine(this, broker);
			engine.start();
			
			for (Map.Entry<String, TriggerWorker> entry : triggerlist.entrySet()) {
				broker.declareProfilesForTriggerWorker(entry.getValue());
			}
		} else {
			Log2.log.error("WorkerGroup is already started", null);
		}
	}
	
	public synchronized void requestStop() {
		Log2.log.debug("Request stop");
		if (engine != null) {
			engine.stopWatch();
			
			for (Map.Entry<String, TriggerWorker> entry : triggerlist.entrySet()) {
				broker.removeProfilesForTriggerWorker(entry.getValue());
			}
			
			try {
				while (engine.isAlive()) {
					Thread.sleep(10);
				}
				engine = null;
			} catch (InterruptedException e) {
			}
		} else {
			Log2.log.error("WorkerGroup is already stopped", null);
		}
		Log2.log.info("All workers are stopped");
	}
	
	public void addWorker(Worker worker) {
		if (worker.isConfigurationAllowToEnabled() == false) {
			return;
		}
		worker.worker_ref = Broker.createKey("worker", worker.getShortWorkerName());
		worker.broker = broker;
		workerlist.add(worker);
		workers_key_map.put(worker.worker_ref, worker);
	}
	
	public void addCyclicWorker(CyclicCreateTasks worker) {
		if (worker.isCyclicConfigurationAllowToEnabled() == false) {
			return;
		}
		WorkerCyclicEngine wcyclic = new WorkerCyclicEngine(worker);
		wcyclic.worker_ref = Broker.createKey("worker", worker.getShortCyclicName());
		wcyclic.broker = broker;
		
		workercycliclist.add(wcyclic);
		workers_key_map.put(wcyclic.worker_ref, wcyclic);
	}
	
	public void addTriggerWorker(TriggerWorker worker) {
		if (worker.isTriggerWorkerConfigurationAllowToEnabled() == false) {
			return;
		}
		triggerlist.put(Broker.createKey("trigger", worker.getTriggerShortName()), worker);
	}
	
	public boolean isWorking() {
		for (int pos = 0; pos < workerlist.size(); pos++) {
			if (workerlist.get(pos).status == WorkerStatus.PROCESSING) {
				return true;
			}
		}
		return false;
	}
	
}
