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
	}
	
	protected void stopService() throws Exception {
	}
	
}
