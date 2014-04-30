/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2014
 * 
*/
package hd3gtv.mydmam.taskqueue.demo;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class DemoWorker extends Worker {
	
	public static final Profile demo_profile = new Profile("test", "debug1");
	
	/**
	 * @param duration in ms
	 * @return new task key
	 */
	public static String createTask(long duration) throws ConnectionException {
		JSONObject jo = new JSONObject();
		jo.put("sleep", duration);
		return Broker.publishTask("Task demo", demo_profile, jo, DemoWorker.class, false, 0, null, false);
	}
	
	public void process(Job job) throws Exception {
		Thread.sleep((Long) job.getContext().get("sleep"));
	}
	
	public String getShortWorkerName() {
		return "debug-worker";
	}
	
	@Override
	public String getLongWorkerName() {
		return "Debug worker";
	}
	
	public List<Profile> getManagedProfiles() {
		ArrayList<Profile> profile = new ArrayList<Profile>();
		profile.add(demo_profile);
		return profile;
	}
	
	public void forceStopProcess() throws Exception {
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return Configuration.global.getValueBoolean("service", "demo_worker");
	}
	
}
