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
 * Copyright (C) hdsdi3g for hd3g.tv 2016
 * 
*/
package hd3gtv.mydmam.cli;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;

import hd3gtv.mydmam.metadata.MetadataIndexingLimit;
import hd3gtv.mydmam.metadata.MetadataIndexingOperation;
import hd3gtv.mydmam.metadata.container.Container;
import hd3gtv.mydmam.transcode.ProcessingKit;
import hd3gtv.mydmam.transcode.ProcessingKitEngine;
import hd3gtv.mydmam.transcode.ProcessingKitInstance;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.CopyMove;

public class CliModuleProcessKit implements CliModule {
	
	public String getCliModuleName() {
		return "processkit";
	}
	
	public String getCliModuleShortDescr() {
		return "Execute a Process Kit on a file";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-h")) {
			showFullCliModuleHelp();
			return;
		}
		
		if (args.getParamExist("-class") == false) {
			System.err.println();
			System.err.println("Error: you must set the ProcessKit Java Class with -class");
			System.err.println();
			showFullCliModuleHelp();
			return;
		}
		
		ProcessingKitEngine pkite = new ProcessingKitEngine();
		ProcessingKit pkit = pkite.get(args.getSimpleParamValue("-class"));
		
		System.out.println("Using ProcessKit: " + pkit.toString());
		
		if (args.getParamExist("-source") == false) {
			System.err.println();
			System.err.println("Error: you must set the source file with -source");
			System.err.println();
			showFullCliModuleHelp();
			return;
		}
		File source_file = new File(args.getSimpleParamValue("-source")).getCanonicalFile();
		CopyMove.checkExistsCanRead(source_file);
		
		Container indexing_result = new MetadataIndexingOperation(source_file).setLimit(MetadataIndexingLimit.FAST).doIndexing();
		
		if (pkit.validateItem(indexing_result) == false) {
			System.err.println();
			System.err.println("Error: this ProcessKit can't works with this file");
			System.err.println();
			showFullCliModuleHelp();
			return;
		}
		
		File temp_directory = new File(".");
		if (args.getParamExist("-temp")) {
			temp_directory = new File(args.getSimpleParamValue("-temp")).getCanonicalFile();
		}
		
		ProcessingKitInstance instance = pkit.createInstance(temp_directory);
		
		File dest_directory = temp_directory;
		if (args.getParamExist("-dest")) {
			dest_directory = new File(args.getSimpleParamValue("-dest")).getCanonicalFile();
			FileUtils.forceMkdir(dest_directory);
		}
		instance.setDestDirectory(dest_directory);
		
		List<File> created_files = instance.process(source_file, indexing_result);
		
		if (args.getParamExist("-notclean") == false) {
			instance.cleanTempFiles();
		}
		
		if (created_files != null) {
			if (created_files.isEmpty() == false) {
				System.out.println("Created files: ");
				created_files.forEach(f -> {
					System.out.println(" - " + f.getAbsolutePath());
				});
			}
		}
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println("  " + getCliModuleName() + " -class package.Class -source file.ext [-temp /tmp] [-dest /var/data] [-notclean]");
		System.out.println("With");
		System.out.println("  -class");
		System.out.println("     Absolute Java Class name (case sensitive). This class must extends ProcessingKit abstract class.");
		System.out.println("  -source");
		System.out.println("     Source file to process. It must be a local file.");
		System.out.println("  -temp");
		System.out.println("     Temp directory, " + new File(".").getAbsoluteFile().getParent() + " by default");
		System.out.println("  -dest");
		System.out.println("     Destination directory, " + new File(".").getAbsoluteFile().getParent() + " by default");
		System.out.println("  -notclean");
		System.out.println("     Set it for don't execute clean operation and keep temp files");
	}
	
}
