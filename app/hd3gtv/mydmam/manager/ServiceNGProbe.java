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
import hd3gtv.mydmam.manager.dummy.Dummy1WorkerNG;

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
		AppManager manager = getManager();
		manager.workerRegister(new Dummy1WorkerNG());
		
		/*
		// TODO startService
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
	
	@Override
	protected void stopService() throws Exception {
		// TODO stopService
		/*
			broker.stop();
			workergroup.requestStop();
		 * */
	}
	
}
