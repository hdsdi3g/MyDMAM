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

import java.util.List;

import hd3gtv.mydmam.accesscontrol.AccessControlEntry;
import hd3gtv.tools.ApplicationArgs;

public class CliModuleAccessControl implements CliModule {
	
	public String getCliModuleName() {
		return "actrl";
	}
	
	public String getCliModuleShortDescr() {
		return "Access Control blocked IPs";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-x")) {
			String addr = args.getSimpleParamValue("-x");
			AccessControlEntry.delete(addr);
			return;
		}
		List<AccessControlEntry> items = AccessControlEntry.getAll();
		if (items.isEmpty() == false) {
			System.out.println("============= Access control blocked IPs list =============");
			for (int pos = 0; pos < items.size(); pos++) {
				System.out.print(" ");
				System.out.print(items.get(pos).getAddress());
				System.out.print(" > ");
				System.out.println(items.get(pos).statusForThisIP());
				System.out.print("     ");
				System.out.println(items.get(pos).toString());
			}
			System.out.println("===========================================================");
			System.out.println();
			showFullCliModuleHelp();
		} else {
			System.out.println("Access control has not blocked IPs");
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage for liberate a blocked IP adress: ");
		System.out.println(" -x <blocked ip adress>");
	}
	
}
