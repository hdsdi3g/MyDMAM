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

import hd3gtv.mydmam.db.orm.CrudOrmEngine;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import models.UserProfile;

public final class UAWorker extends Worker {
	
	private List<UAFunctionality> functionalities_list;
	private HashMap<String, UAFunctionality> functionalities_map;
	private List<Profile> managed_profiles;
	
	public UAWorker(List<UAFunctionality> functionalities_list) throws NullPointerException {
		this.functionalities_list = functionalities_list;
		if (functionalities_list == null) {
			throw new NullPointerException("\"process\" can't to be null");
		}
		functionalities_map = new HashMap<String, UAFunctionality>();
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			functionalities_map.put(functionalities_list.get(pos).getName(), functionalities_list.get(pos));
		}
		managed_profiles = new ArrayList<Profile>();
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			managed_profiles.addAll(functionalities_list.get(pos).getUserActionProfiles());
		}
	}
	
	public List<UAFunctionality> getFunctionalities_list() {
		return functionalities_list;
	}
	
	public HashMap<String, UAFunctionality> getFunctionalities_map() {
		return functionalities_map;
	}
	
	private UAJobProcess current_process;
	
	/**
	 * @see UAFinisherWorker.process()
	 */
	public void process(Job job) throws Exception {
		UAJobContext context = UAJobContext.importFromJob(job.getContext());
		
		if (context == null) {
			throw new NullPointerException("No \"context\" for job");
		}
		
		if (context.functionality_name == null) {
			throw new NullPointerException("\"context.functionality_name\" can't to be null");
		}
		
		UAFunctionality functionality = functionalities_map.get(context.functionality_name);
		if (functionality == null) {
			throw new NullPointerException("Can't found declared functionality " + context.functionality_name);
		}
		
		UAFinisherConfiguration finisher = context.finisher;
		UARange range = context.range;
		
		UAConfigurator user_configuration = context.user_configuration;
		if (user_configuration == null) {
			if (functionality.hasOneClickDefault() == false) {
				throw new NullPointerException("Can't found declared user_configuration in context and One Click is disabled");
			}
			user_configuration = functionality.createOneClickDefaultUserConfiguration();
			finisher = functionality.getFinisherForOneClick();
			range = functionality.getRangeForOneClick();
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
		
		UAJobProgress progress = new UAJobProgress(job);
		
		current_process = functionality.createProcess();
		current_process.process(progress, user_profile, user_configuration, elements);
		current_process = null;
		
		if ((finisher != null) & (range != null)) {
			if (range == UARange.ONE_USER_ACTION_BY_TASK) {
				job.last_message = "Finish current job";
				job.progress = 0;
				job.progress_size = 1;
				job.step_count++;
				UAFinisherWorker.doFinishUserAction(elements, user_profile, context.basket_name, explorer, finisher);
				job.last_message = "Finish terminated";
				job.progress = 1;
				job.step++;
			}
		}
	}
	
	public String getShortWorkerName() {
		return "User Action";
	}
	
	public String getLongWorkerName() {
		return "Process User actions";
	}
	
	public List<Profile> getManagedProfiles() {
		return managed_profiles;
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return (functionalities_list.isEmpty() == false);
	}
	
	public synchronized void forceStopProcess() throws Exception {
		if (current_process == null) {
			return;
		}
		current_process.forceStopProcess();
		current_process = null;
	}
	
}
