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
package hd3gtv.mydmam.cli;

import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;

public class CliModuleCopyDirStruct implements CliModule {
	
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
		
		IndexingEvent found_elements_observer = new IndexingEvent() {
			@Override
			public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
				File file = new File(dest_dir_path + element.currentpath);
				System.out.println(file.getPath());
				file.mkdirs();
				return true;
			}
			
			public void onRemoveFile(String storagename, String path) throws Exception {
			}
		};
		explorer.getAllDirectoriesStorage(args.getSimpleParamValue("-storage"), found_elements_observer);
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage : " + getCliModuleName() + " -storage storageindexname -dest /path/to/dir");
	}
	
}
