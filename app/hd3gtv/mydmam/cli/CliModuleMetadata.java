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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.MetadataExtractor;
import hd3gtv.mydmam.metadata.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.MetadataStorageIndexer;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.metadata.container.ContainerOperations;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;
import hd3gtv.tools.TableList;

public class CliModuleMetadata implements CliModule {
	
	public String getCliModuleName() {
		return "mtd";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on metadatas and file analysis";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-ptt")) {
			MyDMAM.gson_kit.setFullPrettyPrinting();
		}
		
		if (args.getParamExist("-a")) {
			File dir_testformats = new File(args.getSimpleParamValue("-a"));
			if (dir_testformats.exists() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			if (dir_testformats.isDirectory() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			
			Container result;
			File[] files = dir_testformats.listFiles();
			
			for (int pos = 0; pos < files.length; pos++) {
				if (files[pos].isDirectory()) {
					continue;
				}
				if (CopyMove.isHidden(files[pos])) {
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
				
				result = new MetadataIndexingOperation(files[pos]).setLimit(MetadataIndexingLimit.FAST).doIndexing();
				System.out.println(" == " + files[pos] + " == ");
				System.out.println(result);
				System.out.println();
			}
			
			return;
		} else if (args.getParamExist("-index")) {
			String raw_path = args.getSimpleParamValue("-index");
			
			if (raw_path.indexOf(":") <= 0) {
				System.err.println("Error ! Use storage:/path syntax");
				showFullCliModuleHelp();
				return;
			}
			String storagename = raw_path.substring(0, raw_path.indexOf(":"));
			String _currentpath = raw_path.substring(raw_path.indexOf(":") + 1, raw_path.length());
			if (_currentpath.startsWith("/") == false) {
				_currentpath = "/" + _currentpath;
			}
			final String currentpath = _currentpath;
			
			Explorer explorer = new Explorer();
			
			MetadataStorageIndexer metadataStorageIndexer = new MetadataStorageIndexer(args.getParamExist("-refresh"), args.getParamExist("-npz"));
			int since = args.getSimpleIntegerParamValue("-since", 0);
			long _min_index_date = 0;
			if (since > 0) {
				_min_index_date = System.currentTimeMillis() - ((long) since * 3600l * 1000l);
			}
			final long min_index_date = _min_index_date;
			
			String reprocess_extractor_basename = args.getSimpleParamValue("-reprocess");
			if (reprocess_extractor_basename != null) {
				List<MetadataExtractor> reprocess_extractors = MetadataCenter.getExtractors().stream().filter(ex -> {
					return reprocess_extractor_basename.equalsIgnoreCase(ex.getClass().getSimpleName());
				}).collect(Collectors.toList());
				
				if (reprocess_extractors.isEmpty()) {
					throw new ClassNotFoundException("Can't found -reprocess " + reprocess_extractor_basename + " in all enabled extractors");
				}
				
				reprocess_extractors.forEach(extr -> {
					metadataStorageIndexer.setMetadataExtractorToReprocess(extr, args.getParamExist("-noadd"), args.getParamExist("-norefresh"));
					try {
						Loggers.CLI.info("Reprocess with " + extr.getLongName());
						metadataStorageIndexer.process(explorer.getelementByIdkey(Explorer.getElementKey(storagename, currentpath)), min_index_date, null);
					} catch (Exception e) {
						Loggers.CLI.error("Can't reprocess with " + extr.getClass().getName(), e);
					}
				});
				System.exit(0);
			}
			
			metadataStorageIndexer.process(explorer.getelementByIdkey(Explorer.getElementKey(storagename, currentpath)), min_index_date, null);
			
			return;
		} else if (args.getParamExist("-clean")) {
			Loggers.CLI.info("Start clean operations");
			ContainerOperations.purge_orphan_metadatas(args.getParamExist("-all"));
			return;
		} else if (args.getParamExist("-list")) {
			if (args.getParamExist("-verbose")) {
				TableList tl = new TableList();
				tl.addRow("Name", "Description", "Class");
				MetadataCenter.getExtractors().forEach(ex -> {
					tl.addRow(ex.getClass().getSimpleName().toLowerCase(), ex.getLongName(), ex.getClass().getName());
				});
				tl.print();
			} else {
				TableList tl = new TableList();
				tl.addRow("Name", "Description");
				MetadataCenter.getExtractors().forEach(ex -> {
					tl.addRow(ex.getClass().getSimpleName().toLowerCase(), ex.getLongName());
				});
				tl.print();
			}
			return;
		}
		showFullCliModuleHelp();
		
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println(" * Generic usage:");
		System.out.println("    " + getCliModuleName() + " <options> [-ptt]");
		System.out.println("      with -ptt prettify json for human reading");
		System.out.println(" * Standalone directory analysis:");
		System.out.println("    " + getCliModuleName() + " -a /full/path");
		System.out.println(" * Indexing metadatas for a directory:");
		System.out.println("    " + getCliModuleName() + " -index storagename:/pathindexrelative [-refresh] [-since x] [-npz] [<reprocess>]");
		System.out.println("      with -refresh to force re-indexing metadatas and");
		System.out.println("      with -since the number of hours to select the recent updated files.");
		System.out.println("      with -npz to not let to paralleling analysis.");
		System.out.println("    Options for reprocess metadatas: -reprocess extractor_name [-noadd] [-norefresh]");
		System.out.println("      with extractor_name, a String listed by -list");
		System.out.println("      with -noadd don't create metadatas if the item does not");
		System.out.println("      with -norefresh don't reprocess this extractor if the element already have it");
		System.out.println("      -noadd and -norefresh can be wanted if you just want create missing metadatas without process new files");
		System.out.println(" * List all enabled metadata extractors:");
		System.out.println("    " + getCliModuleName() + " -list [-verbose]");
		System.out.println(" * Do clean operation (remove orphan metadatas):");
		System.out.println("    " + getCliModuleName() + " -clean [-all]");
		System.out.println("      with -all for remove all metadatas from empty storages and removed storages.");
	}
	
}
