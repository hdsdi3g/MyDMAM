/*
 * This file is part of MyDMAM
 * 
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2013
 * 
*/
package hd3gtv.mydmam.cli;

import hd3gtv.mydmam.storage.Storage;
import hd3gtv.tools.ApplicationArgs;

class CliModuleStorageManager implements CliModule {
	
	public String getCliModuleName() {
		return "storage";
	}
	
	public String getCliModuleShortDescr() {
		return "Test configuration storage";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		
		String storagename = args.getSimpleParamValue("-testio");
		if (storagename != null) {
			Storage.getByName(storagename).testStorageOperations();
		} else {
			Storage.testAllStoragesConnection();
		}
		
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Autotest by default");
		System.out.println("You can add -testio storagename to do a storage writing test.");
	}
	
}
