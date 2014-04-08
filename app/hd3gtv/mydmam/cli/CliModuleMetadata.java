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
import hd3gtv.mydmam.metadata.MetadataCenter;
import hd3gtv.mydmam.metadata.analysing.Analyser;
import hd3gtv.mydmam.metadata.indexing.MetadataIndexerResult;
import hd3gtv.mydmam.metadata.rendering.FuturePrepareTask;
import hd3gtv.mydmam.metadata.rendering.Renderer;
import hd3gtv.mydmam.module.MyDMAMModulesManager;
import hd3gtv.mydmam.pathindexing.Explorer;
import hd3gtv.mydmam.pathindexing.SourcePathIndexerElement;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CliModuleMetadata implements CliModule {
	
	public String getCliModuleName() {
		return "mtd";
	}
	
	public String getCliModuleShortDescr() {
		return "Operate on metadatas and file analysis";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		MetadataCenter metadata_center = new MetadataCenter();
		MetadataCenter.addAllInternalsProviders(metadata_center);
		MyDMAMModulesManager.addAllExternalMetadataProviders(metadata_center);
		
		boolean verbose = args.getParamExist("-v");
		boolean prettify = args.getParamExist("-vv");
		
		if (args.getParamExist("-a")) {
			MetadataCenter.addAllInternalsProviders(metadata_center);
			MyDMAMModulesManager.addAllExternalMetadataProviders(metadata_center);
			
			File dir_testformats = new File(args.getSimpleParamValue("-a"));
			if (dir_testformats.exists() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			if (dir_testformats.isDirectory() == false) {
				throw new FileNotFoundException(args.getSimpleParamValue("-a"));
			}
			
			/**
			 * Never be executed here (from CLI)
			 */
			List<FuturePrepareTask> current_create_task_list = new ArrayList<FuturePrepareTask>();
			
			MetadataIndexerResult result;
			File[] files = dir_testformats.listFiles();
			for (int pos = 0; pos < files.length; pos++) {
				if (files[pos].isDirectory()) {
					continue;
				}
				if (files[pos].isHidden()) {
					continue;
				}
				SourcePathIndexerElement spie = new SourcePathIndexerElement();
				spie.currentpath = "/execCli/" + System.currentTimeMillis();
				spie.date = System.currentTimeMillis();
				spie.dateindex = spie.date;
				spie.directory = false;
				spie.parentpath = "/execCli";
				spie.size = 0;
				spie.storagename = "Test_MyDMAM_CLI";
				
				result = metadata_center.standaloneIndexing(files[pos], spie, current_create_task_list);
				System.out.print(result.getOrigin());
				System.out.print("\t");
				System.out.print(result.getMimetype());
				System.out.print("\t");
				if (result.master_as_preview) {
					System.out.print("MasterAsPreview");
				}
				System.out.print("\t");
				if ((result.getAnalysis_results() != null) & (verbose | prettify)) {
					for (Map.Entry<Analyser, JSONObject> entry : result.getAnalysis_results().entrySet()) {
						System.out.println();
						System.out.print("\t\t");
						System.out.print(entry.getKey().getLongName());
						System.out.print(" [");
						System.out.print(entry.getKey().getElasticSearchIndexType());
						System.out.print("]");
						System.out.print("\t");
						if (prettify) {
							System.out.print(MetadataCenter.json_prettify(entry.getValue()));
						} else {
							System.out.print(entry.getValue().toJSONString());
						}
					}
				}
				
				if (verbose | prettify) {
					LinkedHashMap<Renderer, JSONArray> rendering_results = result.makeJSONRendering_results();
					if (rendering_results != null) {
						for (Map.Entry<Renderer, JSONArray> entry : rendering_results.entrySet()) {
							System.out.println();
							System.out.print("\t\t");
							System.out.print(entry.getKey().getLongName());
							System.out.print(" [");
							System.out.print(entry.getKey().getElasticSearchIndexType());
							System.out.print("]");
							System.out.print("\t");
							if (prettify) {
								System.out.print(MetadataCenter.json_prettify(entry.getValue()));
							} else {
								System.out.print(entry.getValue().toJSONString());
							}
						}
					}
				}
				
				System.out.println();
			}
			
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
			metadata_center.performAnalysis(root_indexing.storagename, root_indexing.currentpath, 0, true);
			return;
		}
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage");
		System.out.println(" * standalone directory analysis: ");
		System.out.println("   " + getCliModuleName() + " -a /full/path [-v | -vv]");
		System.out.println("   -v verbose");
		System.out.println("   -vv verbose and prettify");
		System.out.println(" * force re-indexing metadatas for a directory: ");
		System.out.println("   " + getCliModuleName() + " -refresh storagename:/pathindexrelative");
	}
	
}
