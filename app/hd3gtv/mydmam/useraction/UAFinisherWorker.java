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

import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import models.UserProfile;

public class UAFinisherWorker extends Worker {
	
	private UAWorker referer;
	private List<Profile> managed_profiles;
	
	public UAFinisherWorker(UAWorker referer) {
		this.referer = referer;
		if (referer == null) {
			throw new NullPointerException("\"referer\" can't to be null");
		}
		
		managed_profiles = new ArrayList<Profile>();
		List<UAFunctionality> functionalities_list = referer.getFunctionalities_list();
		for (int pos = 0; pos < functionalities_list.size(); pos++) {
			managed_profiles.addAll(functionalities_list.get(pos).getFinisherProfiles());
		}
	}
	
	@Override
	public void process(Job job) throws Exception {
		// TODO Auto-generated method stub
	}
	
	static void doFinishUserAction(HashMap<String, SourcePathIndexerElement> elements, UserProfile user_profile, String basket_name, Explorer explorer, UAFinisherConfiguration configuration)
			throws Exception {
		// TODO
	}
	
	public String getShortWorkerName() {
		return "useraction_finisher";
	}
	
	public String getLongWorkerName() {
		return "Finish User Action jobs";
	}
	
	public List<Profile> getManagedProfiles() {
		return managed_profiles;
	}
	
	public void forceStopProcess() throws Exception {
	}
	
	public boolean isConfigurationAllowToEnabled() {
		return referer.isConfigurationAllowToEnabled();
	}
	
}
