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

import hd3gtv.mydmam.db.orm.CrudOrmModel;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
			managed_profiles.addAll(functionalities_list.get(pos).getProfiles());
		}
	}
	
	public List<UAFunctionality> getFunctionalities_list() {
		return functionalities_list;
	}
	
	public HashMap<String, UAFunctionality> getFunctionalities_map() {
		return functionalities_map;
	}
	
	private UAProcess current_process;
	
	public void process(Job job) throws Exception {
		// TODO job.getContext().toJSONString()
		String functionality_name = null;// TODO from context
		UAFunctionality functionality = functionalities_map.get(functionality_name);
		if (functionality == null) {
			throw new NullPointerException("Can't found declared functionality " + functionality_name);
		}
		CrudOrmModel user_configuration = null;// TODO from context
		if (user_configuration == null) {
			user_configuration = functionality.createOneClickDefaultUserConfiguration();
		}
		
		// TODO get from context creator UserProfile and creator group name
		// TODO get from context pathindex keys items
		
		UACapability capability = functionality.getCapabilityForInstance();
		// capability.checkValidity();// TODO check items
		// if (checkValidityGroupName(...)==false) {
		
		current_process = functionality.createProcess();
		current_process.process(job, user_configuration);
		current_process = null;
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
	
	public void forceStopProcess() throws Exception {
		if (current_process == null) {
			return;
		}
		current_process.forceStopProcess();
		current_process = null;
	}
	
}
