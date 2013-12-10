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

import hd3gtv.mydmam.taskqueue.Broker;
import hd3gtv.tools.ApplicationArgs;

public class CliModuleBroker implements CliModule {
	
	public String getCliModuleName() {
		return "broker";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on Task queue system";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-truncate")) {
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("workers")) {
				Broker.truncateWorkergroups();
				return;
			}
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("queue")) {
				Broker.truncateTaskqueue();
				return;
			}
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("all")) {
				Broker.truncateWorkergroups();
				Broker.truncateTaskqueue();
				return;
			}
		}
		if (args.getParamExist("-delete")) {
			Broker.deleteTaskJob(args.getSimpleParamValue("-delete"));
			System.out.println("Don't forget to refresh web client");
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		System.out.println(" * truncate worker list: " + getCliModuleName() + " -truncate workers");
		System.out.println(" * truncate task/job queue: " + getCliModuleName() + " -truncate queue");
		System.out.println(" * truncate worker list and queue: " + getCliModuleName() + " -truncate all");
		System.out.println(" * delete task/job: " + getCliModuleName() + " -delete task:id");
	}
	
}
