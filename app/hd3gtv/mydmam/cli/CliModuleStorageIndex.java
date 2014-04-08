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

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2FilterType;
import hd3gtv.log2.Log2Level;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.IndexingEvent;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;

public class CliModuleStorageIndex implements CliModule {
	
	public String getCliModuleName() {
		return "storageindex";
	}
	
	@Override
	public String getCliModuleShortDescr() {
		return "Storage Indexing operations";
	}
	
	@Override
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-list")) {
			if (args.getParamExist("-storage") == false) {
				throw new NullPointerException("-storage param must to be set");
			}
			Log2.log.createFilter("", Log2Level.ERROR, Log2FilterType.DEFAULT);
			
			Explorer explorer = new Explorer();
			
			for (int pos = 0; pos < SourcePathIndexerElement.TOSTRING_HEADERS.length; pos++) {
				System.err.print(SourcePathIndexerElement.TOSTRING_HEADERS[pos]);
				System.err.print("\t");
			}
			System.err.println();
			
			IndexingEvent found_elements_observer = new IndexingEvent() {
				@Override
				public boolean onFoundElement(SourcePathIndexerElement element) throws Exception {
					System.out.println(element.toString("\t"));
					return true;
				}
			};
			explorer.getAllStorage(args.getSimpleParamValue("-storage"), found_elements_observer);
		}
	}
	
	@Override
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println("  export list (dir list) : " + getCliModuleName() + " -list -storage storageindexname");
	}
	
}
