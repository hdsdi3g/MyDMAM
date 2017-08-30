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
 * Copyright (C) hdsdi3g for hd3g.tv 2017
 * 
*/
package hd3gtv.mydmam.assetsxcross;

import hd3gtv.configuration.Configuration;
import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.TableList;

public class CLIAssetsXcross implements CLIDefinition {
	
	public String getCliModuleName() {
		return "assetsxcross";
	}
	
	public String getCliModuleShortDescr() {
		return "Manipulate Assets in Interplay database via Vantage and ACAPI";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-ar")) {
			String mydmam_id = args.getSimpleParamValue("-ar").trim();
			String interplay_path = args.getSimpleParamValue("-path");
			if (interplay_path == null) {
				interplay_path = "/";
			}
			String base_task_name = args.getSimpleParamValue("-tn");
			if (base_task_name == null) {
				base_task_name = "";
			}
			
			RestoreInterplayVantageAC rivac = RestoreInterplayVantageAC.createFromConfiguration();
			RestoreJob rj = rivac.restore(mydmam_id, interplay_path, base_task_name);
			if (rj == null) {
				System.err.println("Can't found " + mydmam_id + " in " + interplay_path);
				return;
			}
			
			while (rj.isDone() == false) {
				TableList table = new TableList();
				rj.globalStatus(table);
				table.print();
				System.out.println();
				Thread.sleep(3000);
			}
			
			return;
		} else if (args.getParamExist("-tagfshr")) {
			RestoreInterplayVantageAC rivac = RestoreInterplayVantageAC.createFromConfiguration();
			String interplay_path = args.getSimpleParamValue("-tagfshr");
			int since_update_month = args.getSimpleIntegerParamValue("-upd", 0);
			if (since_update_month == 0) {
				throw new IndexOutOfBoundsException("You must provide an -upd month count");
			}
			int since_used_month = args.getSimpleIntegerParamValue("-used", 0);
			if (since_used_month == 0) {
				throw new IndexOutOfBoundsException("You must provide an -used month count");
			}
			rivac.tagForShred(interplay_path, since_update_month, since_used_month);
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage:");
		System.out.println("* Asset(s) restauration in Interplay database:");
		System.out.println("  " + getCliModuleName() + " -ar MyDMAMId -path /Interplay/Directory -tn VantageTaskName");
		System.out.println("with:");
		System.out.println(" -ar the MyDMAM id to search in the Interplay database");
		System.out.println(" -path the root path in Interplay database for found the asset");
		System.out.println(" -tn the vantage task (base) name to use during task creation");
		System.out.println("* Tag for shred function in Interplay database:");
		System.out.println("  " + getCliModuleName() + " -tagfshr /Interplay/Directory -upd 6 -used 3");
		System.out.println(" with:");
		System.out.println(" -tagfshr the root path in Interplay database for found the masterclips to scan");
		System.out.println(" -upd X (since update month) search only masterclip not modified since X months");
		System.out.println(" -used X (since used month) search only masterclip relatives to sequences not modified since X months");
		
	}
	
	// TODO simple file destage + fxp
	
	public boolean isFunctionnal() {
		return Configuration.global.isElementKeyExists("assetsxcross", "interplay_restore");
	}
}
