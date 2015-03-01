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
 * Copyright (C) hdsdi3g for hd3g.tv 2013-2014
 * 
*/
package hd3gtv.mydmam.cli;

import hd3gtv.mydmam.db.Elasticsearch;
import hd3gtv.mydmam.db.ElasticsearchBulkOperation;
import hd3gtv.mydmam.pathindexing.ImporterCDFinder;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;
import java.io.FileNotFoundException;

public class CliModuleCDFinderPathIndexer implements CliModule {
	
	public String getCliModuleName() {
		return "cdfinderimporter";
	}
	
	public String getCliModuleShortDescr() {
		return "CDFinder text/CSV database importer to ElasticSearch";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		String pool = args.getSimpleParamValue("-pool");
		String filename = args.getSimpleParamValue("-file");
		
		if (pool == null | filename == null) {
			showFullCliModuleHelp();
			return;
		}
		
		File file = new File(filename);
		if (file.exists() == false) {
			throw new FileNotFoundException(filename);
		}
		
		ImporterCDFinder cdf_pi = new ImporterCDFinder(file, pool);
		ElasticsearchBulkOperation bulk = Elasticsearch.prepareBulk();
		cdf_pi.index(bulk);
		bulk.terminateBulk();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage : " + getCliModuleName() + " -pool StoragePoolName -file export.txt");
		System.out.println("");
		System.out.println("== CDFinder export params ==");
		System.out.println("Text format : UTF-8");
		System.out.println("Include All levels");
		System.out.println("Date format : YYYY-MM-DD HH-MN-SS");
		System.out.println("No STR#1100:292");
		System.out.println("No cat. metadata");
		System.out.println("No Create cols headers");
		System.out.println("Include ONLY Name, Path, Size, Cat name, Edited (date), Type");
	}
	
}
