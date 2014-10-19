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

import hd3gtv.mydmam.db.BackupDb;
import hd3gtv.mydmam.db.CassandraDb;
import hd3gtv.tools.ApplicationArgs;

import java.io.File;

public class CliModuleDumpDatabase implements CliModule {
	
	public String getCliModuleName() {
		return "dumpdb";
	}
	
	public String getCliModuleShortDescr() {
		return "Dump Cassandra CF and ElasticSearch indexes to XML files";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		BackupDb.mode_debug = args.getParamExist("-debug");
		String prefix = args.getSimpleParamValue("-prefix");
		if (prefix == null) {
			prefix = "backup";
		}
		
		BackupDb bdb = new BackupDb(args.getParamExist("-c"), args.getParamExist("-e"));
		if (args.getParamExist("-import")) {
			File outfile = new File(args.getSimpleParamValue("-import"));
			bdb.restore(outfile, CassandraDb.getkeyspace().getKeyspaceName(), args.getParamExist("-purgebefore"));
		} else {
			bdb.backup(prefix);
		}
		
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage for export: " + getCliModuleName() + " [-prefix pathToFile/prefix] [-debug] [-c | -e]");
		System.out.println("                  with -c for Cassandra export only");
		System.out.println("                  with -e for ElasticSearch export only");
		System.out.println("                  default : Cassandra and ElasticSearch export");
		System.out.println("Usage for import: " + getCliModuleName() + " -import dumpfile.xml [-purgebefore]");
	}
	
}
