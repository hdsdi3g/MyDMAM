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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.pathindexing;

import java.io.File;

import hd3gtv.mydmam.cli.CLIDefinition;
import hd3gtv.tools.ApplicationArgs;

public class CLICopyDirStruct implements CLIDefinition {
	
	public String getCliModuleName() {
		return "copydirstruct";
	}
	
	public String getCliModuleShortDescr() {
		return "Copy directory structure";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-storage") == false) {
			throw new NullPointerException("-storage");
		}
		if (args.getParamExist("-dest") == false) {
			throw new NullPointerException("-dest");
		}
		final String dest_dir_path = (new File(args.getSimpleParamValue("-dest"))).getCanonicalPath();
		
		Explorer explorer = new Explorer();
		
		explorer.getAllDirectoriesStorage(args.getSimpleParamValue("-storage"), element -> {
			File file = new File(dest_dir_path + element.currentpath);
			System.out.println(file.getPath());
			file.mkdirs();
			return true;
		});
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage : " + getCliModuleName() + " -storage storageindexname -dest /path/to/dir");
	}
	
}
