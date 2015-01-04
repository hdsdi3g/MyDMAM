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
package hd3gtv.mydmam.useraction;

import hd3gtv.log2.Log2;
import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.UserProfile;

public final class UAWorker extends WorkerNG {
	
	private List<UAFunctionalityContext> functionalities_list;
	private HashMap<Class<? extends UAFunctionalityContext>, UAFunctionalityContext> functionalities_map;
	private List<WorkerCapablities> managed_worker_capablities;
	private boolean stop;
	
	UAWorker(List<UAFunctionalityContext> functionalities_list) throws NullPointerException {
		this.functionalities_list = functionalities_list;
		if (functionalities_list == null) {
			throw new NullPointerException("\"process\" can't to be null");
		}
		functionalities_map = new HashMap<Class<? extends UAFunctionalityContext>, UAFunctionalityContext>();
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			functionalities_map.put(functionalities_list.get(pos).getClass(), functionalities_list.get(pos));
		}
		managed_worker_capablities = new ArrayList<WorkerCapablities>();
		WorkerCapablities wc;
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			wc = functionalities_list.get(pos).getUserActionWorkerCapablities();
			if (wc != null) {
				managed_worker_capablities.add(wc);
			}
		}
	}
	
	public List<UAFunctionalityContext> getFunctionalities_list() {
		return functionalities_list;
	}
	
	private UAJobProcess current_process;
	
	/**
	 * @see UAFinisherWorker.process()
	 */
	protected void workerProcessJob(JobProgression progression, JobContext jobcontext) throws Exception {
		stop = false;
		UAJobFunctionalityContextContent context = ((UAFunctionalityContext) jobcontext).content;
		
		if (context.functionality_class == null) {
			throw new NullPointerException("\"context.functionality_classname\" can't to be null");
		}
		
		UAFunctionalityContext functionality = functionalities_map.get(context.functionality_class);
		if (functionality == null) {
			throw new NullPointerException("Can't found declared functionality " + context.functionality_class.getName());
		}
		
		UAConfigurator user_configuration = context.user_configuration;
		if (user_configuration == null) {
			user_configuration = functionality.createEmptyConfiguration();
		}
		
		if (context.creator_user_key == null) {
			throw new NullPointerException("\"context.creator_user_key\" can't to be null");
		}
		UserProfile user_profile = new UserProfile();
		CrudOrmEngine<UserProfile> engine = new CrudOrmEngine<UserProfile>(user_profile);
		if (engine.exists(context.creator_user_key)) {
			user_profile = engine.read(context.creator_user_key);
		}
		
		HashMap<String, SourcePathIndexerElement> elements = new HashMap<String, SourcePathIndexerElement>(1);
		Explorer explorer = new Explorer();
		if (context.items != null) {
			elements = explorer.getelementByIdkeys(context.items);
		}
		
		current_process = functionality.createProcess();
		current_process.process(progression, user_profile, user_configuration, elements);
		current_process = null;
		
		progression.update("Finish current job");
		progression.updateProgress(0, 1);
		
		if (context.remove_user_basket_item) {
			progression.incrStepCount();
			try {
				Basket basket = new Basket(user_profile.key);
				List<String> basket_content = basket.getContent(context.basket_name);
				for (Map.Entry<String, SourcePathIndexerElement> entry : elements.entrySet()) {
					basket_content.remove(entry.getKey());
				}
				basket.setContent(context.basket_name, basket_content);
			} catch (NullPointerException e) {
				Log2.log.error("Invalid finishing", e);
			}
			progression.incrStep();
		}
		
		if (context.soft_refresh_source_storage_index_item | context.force_refresh_source_storage_index_item) {
			progression.incrStepCount();
			List<SourcePathIndexerElement> items = new ArrayList<SourcePathIndexerElement>();
			
			for (Map.Entry<String, SourcePathIndexerElement> entry : elements.entrySet()) {
				items.add(entry.getValue());
			}
			
			if (context.soft_refresh_source_storage_index_item) {
				explorer.refreshStoragePath(items, false);
			}
			
			if (context.force_refresh_source_storage_index_item) {
				explorer.refreshStoragePath(items, true);
			}
			progression.incrStep();
		}
		
		progression.update("Finish terminated");
		progression.updateProgress(1, 1);
	}
	
	public synchronized void forceStopProcess() throws Exception {
		stop = true;
		if (current_process == null) {
			return;
		}
		current_process.forceStopProcess();
		current_process = null;
	}
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.USERACTION;
	}
	
	public String getWorkerLongName() {
		return "Process User actions";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Internal";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		return managed_worker_capablities;
	}
	
	protected boolean isActivated() {
		return (functionalities_list.isEmpty() == false);
	}
	
	public boolean wantToStop() {
		return stop;
	}
	
}
