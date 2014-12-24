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

public class Dummy3WorkerNG extends WorkerNG {
	
	public WorkerCategory getWorkerCategory() {
		return WorkerCategory.INTERNAL;
	}
	
	public String getWorkerLongName() {
		return "Dummy worker 3";
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
				return Dummy3Context.class;
			}
		});
		return wc;
	}
	
	boolean stop;
	
	protected void workerProcessJob(JobProgression progression, JobContext context) throws Exception {
		progression.update("Start dummy3 process");
		stop = false;
		for (int pos = 0; pos < 3000; pos++) {
			progression.updateProgress(pos, 3000);
			Thread.sleep(1000);
			if (stop) {
				return;
			}
		}
		progression.update("Dummy3 process is ended");
	}
	
	protected void forceStopProcess() throws Exception {
		stop = true;
	}
	
	protected boolean isActivated() {
		return true;
	}
	
}
