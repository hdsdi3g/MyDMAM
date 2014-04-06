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
package hd3gtv.mydmam.metadata.indexing;

import hd3gtv.configuration.Configuration;
import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.PathScan;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.TriggerWorker;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Client;
import org.json.simple.JSONObject;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class MetadataIndexerWorker extends Worker implements TriggerWorker {
	
	static final String PROFILE_CATEGORY = "analyst";
	
	private MetadataCenter metadata_center;
	
	/**
	 * Plugged profiles
	 */
	private ArrayList<Profile> managed_profiles_worker;
	
	/**
	 * Profiles to plug
	 */
	private ArrayList<Profile> managed_profiles_trigger;
	
	/**
	 * profile name -> AnalysingConfiguration
	 */
	private HashMap<String, AnalysingConfiguration> analysing_storageindexes_map;
	
	private class AnalysingConfiguration {
		String pathindexstoragename;
		String path;
	}
	
	public MetadataIndexerWorker() {
		if (Configuration.global.isElementExists("storageindex_bridge") == false) {
			return;
		}
		metadata_center = new MetadataCenter();
		MetadataCenter.addAllInternalsProviders(metadata_center);
		MyDMAMModulesManager.addAllExternalMetadataProviders(metadata_center);
		
		managed_profiles_trigger = new ArrayList<Profile>();
		analysing_storageindexes_map = new HashMap<String, AnalysingConfiguration>();
		
		if (Configuration.global.isElementExists("analysing_storageindexes")) {
			LinkedHashMap<String, String> s_bridge = Configuration.global.getValues("analysing_storageindexes");
			for (Map.Entry<String, String> entry : s_bridge.entrySet()) {
				managed_profiles_trigger.add(new Profile(PathScan.PROFILE_CATEGORY, entry.getKey()));
				AnalysingConfiguration ac = new AnalysingConfiguration();
				ac.pathindexstoragename = entry.getKey();
				ac.path = entry.getValue();
				analysing_storageindexes_map.put(entry.getKey().toLowerCase(), ac);
			}
		}
	}
	
	public String getShortWorkerName() {
		return "metadata-indexer";
	}
	
	public String getLongWorkerName() {
		return "Metadata Indexer";
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return Configuration.global.isElementExists("storageindex_bridge") & Configuration.global.isElementExists("analysing_storageindexes");
	}
	
	public void process(Job job) throws Exception {
		Client client = Elasticsearch.createClient();
		JSONObject context = job.getContext();
		if (context == null) {
			throw new NullPointerException("No context");
		}
		if (context.isEmpty()) {
			throw new NullPointerException("No context");
		}
		String storagename = (String) context.get("storage");
		String currentpath = (String) context.get("path");
		long min_index_date = ((Number) context.get("minindexdate")).longValue();
		boolean force_refresh = (Boolean) context.get("force_refresh");
		
		metadata_center.performAnalysis(client, storagename, currentpath, min_index_date, force_refresh);
		client.close();
	}
	
	public List<Profile> getManagedProfiles() {
		if (managed_profiles_worker == null) {
			ArrayList<String> bridged_list = Explorer.getBridgedStoragesName();
			managed_profiles_worker = new ArrayList<Profile>();
			for (int pos = 0; pos < bridged_list.size(); pos++) {
				managed_profiles_worker.add(new Profile(PROFILE_CATEGORY, bridged_list.get(pos)));
			}
		}
		return managed_profiles_worker;
	}
	
	public void forceStopProcess() throws Exception {
		metadata_center.stopAnalysis();
	}
	
	public boolean isTriggerWorkerConfigurationAllowToEnabled() {
		return Configuration.global.isElementExists("storageindex_bridge") & Configuration.global.isElementExists("analysing_storageindexes");
	}
	
	public List<Profile> plugToProfiles() {
		return managed_profiles_trigger;
	}
	
	public String getTriggerShortName() {
		return "pathindex-metadata-trigger";
	}
	
	public String getTriggerLongName() {
		return "Pathindex metadata indexer";
	}
	
	private HashMap<Profile, Long> lastindexeddatesforprofiles;
	
	public void triggerCreateTasks(Profile profile) throws ConnectionException {
		String profilename = profile.getName();
		
		AnalysingConfiguration ac = analysing_storageindexes_map.get(profilename);
		if (ac == null) {
			Log2.log.error("No configuration for this Profile", new NullPointerException(profilename));
			return;
		}
		
		if (lastindexeddatesforprofiles == null) {
			lastindexeddatesforprofiles = new HashMap<Profile, Long>();
		}
		
		long lastdate = 0;
		if (lastindexeddatesforprofiles.containsKey(profile)) {
			lastdate = lastindexeddatesforprofiles.get(profile);
		}
		
		JSONObject context = new JSONObject();
		context.put("storage", ac.pathindexstoragename);
		context.put("path", ac.path);
		context.put("minindexdate", lastdate);
		context.put("force_refresh", false);
		Broker.publishTask("Analyst directory", new Profile(PROFILE_CATEGORY, profilename), context, this, false, 0, null, false);
		
		lastindexeddatesforprofiles.put(profile, System.currentTimeMillis());
	}
	
	public MetadataCenter getMetadata_center() {
		return metadata_center;
	}
	
}
