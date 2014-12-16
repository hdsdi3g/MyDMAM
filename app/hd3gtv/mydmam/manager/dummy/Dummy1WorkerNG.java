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
package hd3gtv.mydmam.manager.dummy;

import hd3gtv.mydmam.manager.JobContext;
import hd3gtv.mydmam.manager.JobProgression;
import hd3gtv.mydmam.manager.WorkerCapablities;
import hd3gtv.mydmam.manager.WorkerNG;
import hd3gtv.mydmam.pathindexing.Explorer;

import java.util.ArrayList;
import java.util.List;

public class Dummy1WorkerNG extends WorkerNG {
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Dummy worker 1";
	}
	
	public String getWorkerVendorName() {
		return "MyDMAM Test classes";
	}
	
	public List<WorkerCapablities> getWorkerCapablities() {
		ArrayList<WorkerCapablities> wc = new ArrayList<WorkerCapablities>(1);
		wc.add(new WorkerCapablities() {
			
			public List<String> getStoragesAvaliable() {
				return Explorer.getBridgedStoragesName();
			}
			
			public Class<? extends JobContext> getJobContextClass() {
				return Dummy1Context.class;
			}
		});
		return wc;
	}
	
	private boolean stop_process;
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		stop_process = false;
		progression.update("Start dummy process");
		progression.updateStep(1, 3);
		
		Thread.sleep(context.contextToJson().get("sleep").getAsLong());
		
		progression.update("Dummy process is on live");
		progression.updateStep(2, 3);
		
		for (int pos = 0; pos < 1000; pos++) {
			if (stop_process) {
				return;
			}
			progression.updateProgress(pos, 1000);
			Thread.sleep(10);
		}
		
		progression.update("Dummy process is ended");
		progression.updateStep(3, 3);
	}
	
	protected void forceStopProcess() throws Exception {
		stop_process = true;
	}
	
	protected boolean isActivated() {
		return true;
	}
	
}
