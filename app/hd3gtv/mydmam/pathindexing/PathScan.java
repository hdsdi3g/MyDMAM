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
package hd3gtv.mydmam.pathindexing;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.CyclicCreateTasks;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class PathScan extends Worker implements CyclicCreateTasks {
	
	private HashMap<String, PathElementConfiguration> scanelements;
	
	private ImporterStorage importer;
	private int min_period = Integer.MAX_VALUE;
	
	private static final int grace_time_ttl = 5; // ttl = (grace_time_ttl * period)
	public static final String PROFILE_CATEGORY = "pathscan";
	
	private class PathElementConfiguration {
		/**
		 * In sec
		 */
		int period;
		String storage;
		long last_create_task;
		Profile profile;
		String label;
	}
	
	public PathScan() throws IOException {
		if (Configuration.global.isElementExists("storageindex_scan") == false) {
			return;
		}
		
		HashMap<String, ConfigurationItem> ps_configuration = Configuration.global.getElement("storageindex_scan");
		scanelements = new HashMap<String, PathElementConfiguration>();
		
		String label;
		
		for (Map.Entry<String, ConfigurationItem> entry : ps_configuration.entrySet()) {
			LinkedHashMap<String, ?> element = entry.getValue().content;
			PathElementConfiguration pec = new PathElementConfiguration();
			pec.label = (String) element.get("label");
			pec.storage = entry.getKey();
			pec.period = (Integer) element.get("period");
			
			label = pec.label.toLowerCase();
			pec.profile = new Profile(PROFILE_CATEGORY, label);
			
			scanelements.put(label, pec);
			
			if (pec.period < min_period) {
				min_period = pec.period;
			}
		}
		
	}
	
	public void process(Job job) throws Exception {
		PathElementConfiguration pec = scanelements.get(job.getProfile().getName());
		if (pec == null) {
			throw new IOException("Can't found pathindex storage name for " + job.getProfile().getName());
		}
		
		Log2Dump dump = new Log2Dump();
		dump.add("storage", pec.storage);
		dump.add("label", pec.label);
		Log2.log.info("Indexing Storage", dump);
		
		importer = new ImporterStorage(pec.storage, pec.label, 1000 * pec.period * grace_time_ttl);
		importer.index();
		// } catch (NoNodeAvailableException e) {
		// Log2.log.error("Elasticsearch transport trouble, cancel analysis.", e);
		// return;
		importer = null;
	}
	
	public String getShortWorkerName() {
		return "storagescanindex";
	}
	
	public String getLongWorkerName() {
		return "Storage indexing";
	}
	
	ArrayList<Profile> profiles_list;
	
	public List<Profile> getManagedProfiles() {
		if (profiles_list == null) {
			profiles_list = new ArrayList<Profile>();
			for (Map.Entry<String, PathScan.PathElementConfiguration> entry : scanelements.entrySet()) {
				profiles_list.add(entry.getValue().profile);
			}
		}
		return profiles_list;
	}
	
	public void forceStopProcess() throws Exception {
		if (importer != null) {
			importer.stopScan();
		}
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return Configuration.global.isElementExists("storageindex_scan");
	}
	
	public void createTasks() throws ConnectionException {
		PathElementConfiguration pec;
		for (Map.Entry<String, PathScan.PathElementConfiguration> entry : scanelements.entrySet()) {
			pec = entry.getValue();
			if ((pec.last_create_task + (long) (pec.period * 1000)) < System.currentTimeMillis()) {
				Broker.publishTask("Path scan", pec.profile, null, this, false, System.currentTimeMillis() + (long) (8 * 3600 * 1000), null, false);
				pec.last_create_task = System.currentTimeMillis();
			}
		}
	}
	
	public long getInitialCyclicPeriodTasks() {
		return min_period * 1000;
	}
	
	@Override
	public String getShortCyclicName() {
		return getClass().getSimpleName() + "-cyclic";
	}
	
	public String getLongCyclicName() {
		return "Regular storage indexing";
	}
	
	@Override
	public boolean isCyclicConfigurationAllowToEnabled() {
		return isConfigurationAllowToEnabled();
	}
	
	public boolean isPeriodDurationForCreateTasksCanChange() {
		return false;
	}
	
}
