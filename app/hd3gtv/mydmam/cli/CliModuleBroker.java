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
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.cli;

import hd3gtv.mydmam.manager.InstanceStatus;
import hd3gtv.mydmam.manager.JobNG;
import hd3gtv.mydmam.manager.JobNG.JobStatus;
import hd3gtv.mydmam.manager.WorkerExporter;
import hd3gtv.tools.ApplicationArgs;

import java.util.List;

import com.google.gson.JsonObject;

public class CliModuleBroker implements CliModule {
	
	public String getCliModuleName() {
		return "broker";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on Queue system";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-truncate")) {
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("instances")) {
				WorkerExporter.truncate();
				InstanceStatus.truncate();
				return;
			}
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("queue")) {
				JobNG.Utility.truncateAllJobs();
				return;
			}
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("all")) {
				WorkerExporter.truncate();
				InstanceStatus.truncate();
				JobNG.Utility.truncateAllJobs();
				return;
			}
		} else if (args.getParamExist("-remove")) {
			try {
				JobStatus status = JobStatus.valueOf(args.getSimpleParamValue("-remove").toUpperCase());
				List<JsonObject> jobs = JobNG.Utility.deleteJobsByStatus(status);
				for (int pos = 0; pos < jobs.size(); pos++) {
					System.out.println(jobs.get(pos).toString());
				}
				return;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.err.println("Invalid job status name.");
				System.err.println("Valid names: " + sb_jobstatus.toString());
				return;
			}
		}
		showFullCliModuleHelp();
	}
	
	private static final StringBuilder sb_jobstatus = new StringBuilder();
	static {
		for (int i = 0; i < JobStatus.values().length; i++) {
			sb_jobstatus.append(JobStatus.values()[i].name().toLowerCase());
			if (i + 1 < JobStatus.values().length) {
				sb_jobstatus.append("|");
			}
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		System.out.println(" * truncate instances list: " + getCliModuleName() + " -truncate instances");
		System.out.println(" * truncate job queue: " + getCliModuleName() + " -truncate queue");
		System.out.println(" * truncate instances list and queue: " + getCliModuleName() + " -truncate all");
		
		System.out.println(" * remove jobs by status: " + getCliModuleName() + " -remove [" + sb_jobstatus.toString() + "]");
	}
	
}
