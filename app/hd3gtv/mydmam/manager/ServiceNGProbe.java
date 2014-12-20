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
package hd3gtv.mydmam.manager;

import hd3gtv.mydmam.db.status.ClusterStatusEvents;
import hd3gtv.mydmam.db.status.ClusterStatusService;
import hd3gtv.mydmam.manager.dummy.Dummy1Context;
import hd3gtv.mydmam.manager.dummy.Dummy1WorkerNG;
import hd3gtv.mydmam.manager.dummy.Dummy2Context;
import hd3gtv.mydmam.manager.dummy.Dummy2WorkerNG;

import java.util.concurrent.TimeUnit;

public class ServiceNGProbe extends ServiceNG implements ClusterStatusEvents {
	
	private ClusterStatusService cluster_status_service;
	
	public ServiceNGProbe(String[] args) throws Exception {
		super(args, "Probe service");
		
		cluster_status_service = new ClusterStatusService(getManager());
		cluster_status_service.addCallbackEvent(this);
	}
	
	public void clusterHasAGraveState(String reasons) {
		stopAllServices();
	}
	
	public void clusterHasAWarningState(String reasons) {
	}
	
	public void clusterIsFunctional(String reasons) {
		startAllServices();
	}
	
	@Override
	protected void startService() throws Exception {
		JobNG.Utility.truncateAllJobs();// TODO phase 2, remove
		
		AppManager manager = getManager();
		manager.workerRegister(new Dummy1WorkerNG());
		manager.workerRegister(new Dummy2WorkerNG());
		
		CyclicJobCreator cyclic_creator = new CyclicJobCreator(manager, 1, TimeUnit.MINUTES, false);
		cyclic_creator.setOptions(ServiceNGProbe.class, "Dummy cyclic test", "MyDMAM Test classes");
		cyclic_creator.add("Regular job", new Dummy1Context());
		manager.cyclicJobsRegister(cyclic_creator);
		
		TriggerJobCreator trigger_creator = new TriggerJobCreator(getManager(), new Dummy1Context());
		trigger_creator.add("Trig job", new Dummy2Context());
		trigger_creator.setOptions(ServiceNGProbe.class, "Dummy trigger test", "MyDMAM Test classes");
		manager.triggerJobsRegister(trigger_creator);
		
		/*
		// TODO phase 2, startService
		StorageManager.getGlobalStorage();
		TranscodeProfile.isConfigured();

		workergroup.addWorker(new Publish());
		
		PathScan ps = new PathScan();
		workergroup.addWorker(ps);
		workergroup.addCyclicWorker(ps);
		
		WorkerIndexer mwi = new WorkerIndexer();
		workergroup.addWorker(mwi);
		workergroup.addTriggerWorker(mwi);
		
		WorkerRenderer mwr = new WorkerRenderer(mwi);
		workergroup.addWorker(mwr);
		
		new NotificationWorker(workergroup);
		
		workergroup.addWorker(new DemoWorker());
		
		MyDMAMModulesManager.declareAllModuleWorkerElement(workergroup);
		
		UAManager.createWorkers(workergroup);*/
	}
	
	protected void stopService() throws Exception {
	}
	
}
