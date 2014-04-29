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
package hd3gtv.mydmam.mail.notification;

import hd3gtv.mydmam.taskqueue.Job;
import hd3gtv.mydmam.taskqueue.Profile;
import hd3gtv.mydmam.taskqueue.TriggerWorker;
import hd3gtv.mydmam.taskqueue.Worker;

import java.util.List;

import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;

public class NotificationWorker extends Worker implements TriggerWorker {
	
	@Override
	public boolean isTriggerWorkerConfigurationAllowToEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public List<Profile> plugToProfiles() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getTriggerShortName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getTriggerLongName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void triggerCreateTasks(Profile profile) throws ConnectionException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void process(Job job) throws Exception {
		// TODO Auto-generated method stub
	}
	
	@Override
	public String getShortWorkerName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getLongWorkerName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Profile> getManagedProfiles() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void forceStopProcess() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean isConfigurationAllowToEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
