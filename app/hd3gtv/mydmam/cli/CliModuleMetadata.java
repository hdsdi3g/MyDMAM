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
package hd3gtv.mydmam.cli;

import hd3gtv.log2.Log2;
import hd3gtv.log2.Log2Dump;
import hd3gtv.mydmam.metadata.MetadataIndexer;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;
import java.io.FileNotFoundException;

public class CliModuleMetadata implements CliModule {
	
	public String getCliModuleName() {
		return "mtd";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on metadatas and file analysis";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-a")) {
			File dir_testformats = new File(args.getSimpleParamValue("-a"));
			if (dir_testformats.exists() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			if (dir_testformats.isDirectory() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			
			if (args.getParamExist("-ptt")) {
				ContainerOperations.setGsonPrettyPrinting();
			}
			
			Container result;
			File[] files = dir_testformats.listFiles();
			Log2Dump dump = new Log2Dump();
			for (int pos = 0; pos < files.length; pos++) {
				if (files[pos].isDirectory()) {
					continue;
				}
				if (files[pos].isHidden()) {
					continue;
				}
				SourcePathIndexerElement spie = new SourcePathIndexerElement();
				spie.currentpath = "/cli-request/" + System.currentTimeMillis();
				spie.date = files[pos].lastModified();
				spie.dateindex = System.currentTimeMillis();
				spie.directory = false;
				spie.parentpath = "/cli-request";
				spie.size = files[pos].length();
				spie.storagename = "MyDMAM-CLI-Request";
				
				result = new MetadataIndexingOperation(files[pos]).setLimit(MetadataIndexingLimit.ANALYST).doIndexing();
				dump.add("Item", files[pos]);
				dump.addAll(result);
			}
			Log2.log.info("Result", dump);
			
			return;
		} else if (args.getParamExist("-refresh")) {
			String raw_path = args.getSimpleParamValue("-refresh");
			
			if (raw_path.indexOf(":") <= 0) {
				System.err.println("Error ! Use storage:/path syntax");
				showFullCliModuleHelp();
				return;
			}
			
			SourcePathIndexerElement root_indexing = new SourcePathIndexerElement();
			
			root_indexing.storagename = raw_path.substring(0, raw_path.indexOf(":"));
			root_indexing.currentpath = raw_path.substring(raw_path.indexOf(":") + 1, raw_path.length());
			if (root_indexing.currentpath.startsWith("/") == false) {
				root_indexing.currentpath = "/" + root_indexing.currentpath;
			}
			
			Explorer explorer = new Explorer();
			
			if (explorer.countDirectoryContentElements(root_indexing.prepare_key()) == 0) {
				Log2Dump dump = new Log2Dump();
				dump.addAll(root_indexing);
				Log2.log.info("Empty/not found element to scan metadatas", dump);
				return;
			}
			MetadataIndexer metadataIndexer = new MetadataIndexer(true);
			metadataIndexer.process(root_indexing, 0);
			return;
		} else if (args.getParamExist("-clean")) {
			Log2.log.info("Start clean operations");
			ContainerOperations.purge_orphan_metadatas();
			return;
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println(" * standalone directory analysis: ");
		System.out.println("   " + getCliModuleName() + " -a /full/path [-ptt]");
		System.out.println("   -ptt prettify json for human reading");
		System.out.println(" * force re-indexing metadatas for a directory:");
		System.out.println("   " + getCliModuleName() + " -refresh storagename:/pathindexrelative");
		System.out.println(" * do clean operation (remove orphan metadatas):");
		System.out.println("   " + getCliModuleName() + " -clean");
	}
	
}
