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
import hd3gtv.mydmam.mail.notification.NotificationWorker;
import hd3gtv.mydmam.manager.dummy.Dummy1Context;
import hd3gtv.mydmam.manager.dummy.Dummy1WorkerNG;
import hd3gtv.mydmam.manager.dummy.Dummy2Context;
import hd3gtv.mydmam.manager.dummy.Dummy2WorkerNG;
import hd3gtv.mydmam.manager.dummy.Dummy3WorkerNG;
import hd3gtv.mydmam.metadata.WorkerIndexer;
import hd3gtv.mydmam.metadata.WorkerRenderer;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.PathScan;
import hd3gtv.mydmam.transcode.Publish;

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
		
		new NotificationWorker(manager);
		manager.workerRegister(new Publish());
		manager.workerRegister(new PathScan().cyclicJobsRegister(manager));
		
		WorkerIndexer mwi = new WorkerIndexer(manager);
		manager.workerRegister(mwi);
		manager.workerRegister(new WorkerRenderer(mwi));
		
		MyDMAMModulesManager.declareAllModuleWorkerElement(manager);
		
		manager.workerRegister(new Dummy1WorkerNG());
		manager.workerRegister(new Dummy2WorkerNG());
		manager.workerRegister(new Dummy3WorkerNG());
		
		JobNG n1 = AppManager.createJob(new Dummy1Context()).setName("Test 1").setPostponed();
		n1.publish();
		JobNG n2 = AppManager.createJob(new Dummy2Context()).setName("Test 2 rq test 1").setRequireCompletedJob(n1);
		n2.publish();
		
		/*CyclicJobCreator cyclic_creator = new CyclicJobCreator(manager, 1, TimeUnit.MINUTES, false);
		cyclic_creator.setOptions(ServiceNGProbe.class, "Dummy cyclic test", "MyDMAM Test classes");
		cyclic_creator.add("Regular job", );
		// manager.cyclicJobsRegister(cyclic_creator);
		
		/*TriggerJobCreator trigger_creator = new TriggerJobCreator(getManager(), new Dummy1Context());
		trigger_creator.add("Trig job", new Dummy2Context());
		trigger_creator.setOptions(ServiceNGProbe.class, "Dummy trigger test", "MyDMAM Test classes");
		// manager.triggerJobsRegister(trigger_creator);
		
		CyclicJobCreator cyclic_creator2 = new CyclicJobCreator(manager, 1, TimeUnit.HOURS, false);
		cyclic_creator2.setOptions(ServiceNGProbe.class, "Dummy cyclic test 2", "MyDMAM Test classes");
		cyclic_creator2.add("Regular long job", new Dummy3Context());
		manager.cyclicJobsRegister(cyclic_creator2);*/
	}
	
	protected void stopService() throws Exception {
	}
	
}
