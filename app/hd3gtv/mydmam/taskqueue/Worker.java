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

import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.log2.Log2Dumpable;

import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.ColumnList;

public abstract class Worker implements Log2Dumpable {
	
	public abstract void process(Job job) throws Exception;
	
	/**
	 * @return like "mysuperservice"
	 */
	public abstract String getShortWorkerName();
	
	/**
	 * @return like "My Super Service"
	 */
	public abstract String getLongWorkerName();
	
	public abstract List<Profile> getManagedProfiles();
	
	public abstract void forceStopProcess() throws Exception;
	
	public abstract boolean isConfigurationAllowToEnabled();
	
	WorkerEngine engine;
	
	String worker_ref;
	
	WorkerStatus status = WorkerStatus.STOPPED;
	
	Broker broker;
	
	final boolean isAvailableForProcessing() {
		return (status == WorkerStatus.WAITING);
	}
	
	public final void setEnabled() {
		if (status != WorkerStatus.WAITING) {
			Log2.log.debug("Change worker status to waiting");
		}
		status = WorkerStatus.WAITING;
	}
	
	public final void setDisabled() {
		Log2.log.info("Change worker status to disabled");
		status = WorkerStatus.PENDING_STOP;
		if (engine != null) {
			if (engine.isAlive()) {
				engine.askStopProcess();
				Log2.log.debug("Worker is stopped");
			}
		}
		engine = null;
		status = WorkerStatus.STOPPED;
	}
	
	final void pushToDatabase(MutationBatch mutator, String hostname, int ttl) {
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("hostname", hostname, ttl);
		mutator.withRow(Broker.CF_WORKERGROUPS, worker_ref).putColumnIfNotNull("instancename", ServiceManager.getInstancename(), ttl);
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
	
	static JSONObject pullJSONFromDatabase(ColumnList<String> columns) throws ParseException {
		JSONObject jo = new JSONObject();
		jo.put("hostname", columns.getStringValue("hostname", "?"));
		jo.put("instancename", columns.getStringValue("instancename", "?"));
		jo.put("short_worker_name", columns.getStringValue("short_worker_name", ""));
		jo.put("long_worker_name", columns.getStringValue("long_worker_name", ""));
		jo.put("job", columns.getStringValue("job", ""));
		jo.put("cyclic", columns.getBooleanValue("cyclic", false));
		jo.put("time_to_sleep", columns.getIntegerValue("time_to_sleep", 0));
		jo.put("countdown_to_process", columns.getIntegerValue("countdown_to_process", 0));
		jo.put("status", WorkerStatus.pullFromDatabase(columns).name().toUpperCase());
		
		JSONParser parser = new JSONParser();
		jo.put("managed_profiles", (JSONArray) parser.parse(columns.getStringValue("managed_profiles", "[]")));
		
		jo.put(WorkerGroup.COL_NAME_STATUSCHANGE, columns.getStringValue(WorkerGroup.COL_NAME_STATUSCHANGE, ""));
		jo.put(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, columns.getIntegerValue(WorkerGroup.COL_NAME_CHANGECYCLICPERIOD, 0));
		return jo;
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add(worker_ref, worker_ref);
		dump.add("short worker name", getShortWorkerName());
		dump.add("long worker name", getLongWorkerName());
		dump.add("worker status", status);
		if (engine != null) {
			dump.addAll(engine.job);
			dump.add("worker alive", engine.isAlive());
		}
		return dump;
	}
}
