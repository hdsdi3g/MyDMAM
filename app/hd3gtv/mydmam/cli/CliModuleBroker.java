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

import hd3gtv.mydmam.manager.DatabaseLayer;
import hd3gtv.mydmam.manager.JobNG;
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
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("instances")) {
				DatabaseLayer.truncateAll();
				return;
			}
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("queue")) {
				JobNG.Utility.truncateAllJobs();
				return;
			}
			if (args.getSimpleParamValue("-truncate").equalsIgnoreCase("all")) {
				DatabaseLayer.truncateAll();
				JobNG.Utility.truncateAllJobs();
				return;
			}
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		System.out.println(" * truncate instances list: " + getCliModuleName() + " -truncate instances");
		System.out.println(" * truncate job queue: " + getCliModuleName() + " -truncate queue");
		System.out.println(" * truncate instances list and queue: " + getCliModuleName() + " -truncate all");
	}
	
}
