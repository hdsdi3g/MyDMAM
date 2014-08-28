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
 * Copyright (C) hdsdi3g for hd3g.tv 2012-2014
 * 
*/
package hd3gtv.mydmam.probe;

import hd3gtv.configuration.GitInfo;
import hd3gtv.javasimpleservice.ServiceInformations;
import hd3gtv.javasimpleservice.ServiceManager;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.mail.notification.NotificationWorker;
import hd3gtv.mydmam.metadata.WorkerIndexer;
import hd3gtv.mydmam.metadata.WorkerRenderer;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.PathScan;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.WorkerGroup;
import hd3gtv.mydmam.taskqueue.demo.DemoWorker;
import hd3gtv.mydmam.transcode.Publish;
import hd3gtv.mydmam.transcode.TranscodeProfile;
import hd3gtv.mydmam.useraction.UAManager;
import hd3gtv.storage.StorageManager;

public class MyDMAMProbeService extends ServiceManager implements ServiceInformations {
	
	public MyDMAMProbeService(String[] args, ServiceInformations serviceinformations) {
		super(args, serviceinformations);
	}
	
	public Log2Dump getLog2Dump() {
		Log2Dump dump = new Log2Dump();
		dump.add("applicationname", getApplicationName());
		dump.add("version", getApplicationVersion());
		return dump;
	}
	
	public String getApplicationName() {
		return "MyDMAM - Probe service";
	}
	
	public String getApplicationVersion() {
		GitInfo git = GitInfo.getFromRoot();
		if (git != null) {
			return git.getBranch() + " " + git.getCommit();
		} else {
			return "noset";
		}
	}
	
	public String getApplicationCopyright() {
		return "Copyright (C) hdsdi3g for hd3g.tv 2012-2014";
	}
	
	public String getApplicationShortName() {
		return "MyDMAM-Probe";
	}
	
	protected void postClassInit() throws Exception {
	}
	
	protected void startApplicationService() throws Exception {
		StorageManager.getGlobalStorage();
		Elasticsearch.refeshconfiguration();
		TranscodeProfile.isConfigured();
		
		Broker broker = new Broker(this);
		workergroup = new WorkerGroup(broker);
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
		
		UAManager.createWorkers(workergroup);
		
		workergroup.start();
	}
	
	protected void stopApplicationService() throws Exception {
		if (workergroup != null) {
			workergroup.requestStop();
		}
	}
	
}
