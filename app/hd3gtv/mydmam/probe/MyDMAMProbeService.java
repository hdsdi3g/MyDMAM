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
import hd3gtv.mydmam.analysis.AnalysingWorker;
import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.PathScan;
import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.mydmam.taskqueue.WorkerGroup;
import hd3gtv.mydmam.transcode.Publish;
import hd3gtv.mydmam.transcode.TranscodeProfileManager;
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
		return GitInfo.getActualRepositoryInformation();
	}
	
	public String getApplicationCopyright() {
		return "Copyright (C) hdsdi3g for hd3g.tv 2012-2013";
	}
	
	public String getApplicationShortName() {
		return "MyDMAM-Probe";
	}
	
	protected void postClassInit() throws Exception {
	}
	
	private WorkerGroup workergroup;
	
	protected void startApplicationService() throws Exception {
		StorageManager.getGlobalStorage();
		TranscodeProfileManager.refreshProfileslist();
		Elasticsearch.refeshconfiguration();
		
		Broker broker = new Broker(this);
		workergroup = new WorkerGroup(broker);
		workergroup.addWorker(new Publish());
		
		PathScan ps = new PathScan();
		workergroup.addWorker(ps);
		workergroup.addCyclicWorker(ps);
		
		AnalysingWorker aw = new AnalysingWorker();
		workergroup.addWorker(aw);
		workergroup.addTriggerWorker(aw);
		
		MyDMAMModulesManager.declareAllModuleWorkerElement(workergroup);
		
		workergroup.start();
		
		setWorkergroup(workergroup);
	}
	
	protected void stopApplicationService() throws Exception {
		if (workergroup != null) {
			workergroup.requestStop();
		}
	}
	
}
