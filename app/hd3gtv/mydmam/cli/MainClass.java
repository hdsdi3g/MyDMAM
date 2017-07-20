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

import java.util.ArrayList;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import hd3gtv.configuration.CLIProject;
import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.accesscontrol.CLIAccessControl;
import hd3gtv.mydmam.auth.CLIAuth;
import hd3gtv.mydmam.bcastautomation.CLIBCA;
import hd3gtv.mydmam.db.CLIOperateDatabase;
import hd3gtv.mydmam.factory.CLIJS;
import hd3gtv.mydmam.manager.CLIBroker;
import hd3gtv.mydmam.manager.ServiceNG;
import hd3gtv.mydmam.metadata.CLIMetadata;
import hd3gtv.mydmam.pathindexing.CLICDFinderPathIndexer;
import hd3gtv.mydmam.pathindexing.CLICopyDirStruct;
import hd3gtv.mydmam.ssh.CLISsh;
import hd3gtv.mydmam.transcode.kit.CLIProcessKit;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.TableList;

public class MainClass {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		ApplicationArgs appargs = new ApplicationArgs(args);
		
		Logger.getRootLogger().removeAllAppenders();
		ConsoleAppender console_appender = new ConsoleAppender();
		console_appender.setTarget(ConsoleAppender.SYSTEM_OUT);
		
		if (appargs.getParamExist("-verbose")) {
			/**
			 * always show caller and thread
			 */
			console_appender.setLayout(new PatternLayout("%-5p ‹%t› “%m”%n ‣ %C.%M(%F:%L)%n"));
		} else {
			console_appender.setLayout(new PatternLayout("%-5p %m%n"));
		}
		
		console_appender.activateOptions();
		Logger.getRootLogger().addAppender(console_appender);
		
		if (appargs.getParamExist("-trace")) {
			/**
			 * enable trace mode (debug level)
			 */
			Loggers._MyDMAM_Root.setLevel(Level.TRACE);
			Logger.getRootLogger().setLevel(Level.INFO);
		} else if (appargs.getParamExist("-quiet")) {
			/**
			 * never show a normal log message (debug level)
			 */
			Loggers._MyDMAM_Root.setLevel(Level.ERROR);
			Logger.getRootLogger().setLevel(Level.ERROR);
		} else {
			Loggers._MyDMAM_Root.setLevel(Level.INFO);
			Logger.getRootLogger().setLevel(Level.WARN);
		}
		
		ArrayList<CLIDefinition> modules = new ArrayList<CLIDefinition>();
		modules.add(new ServiceNG.PlayInCli());
		modules.add(new ServiceNG.BackgroundServicesInCli());
		modules.add(new ServiceNG.FTPServerInCli());
		modules.add(new CLIAuth());
		modules.add(new CLIAccessControl());
		modules.add(new CLISsh());
		modules.add(new CLICDFinderPathIndexer());
		modules.add(new CLIOperateDatabase());
		modules.add(new CLICopyDirStruct());
		modules.add(new CLIBroker());
		modules.add(new CLIMetadata());
		modules.add(new CLIProcessKit());
		modules.add(new CLIBCA());
		modules.add(new CLIJS());
		modules.add(new CLIProject());
		
		String modulename = appargs.getFirstAction();
		
		if (modulename == null) {
			System.out.println("MyDMAM Command line interface");
			System.out.println("=============================");
			System.out.println("Available modules:");
			
			TableList table = new TableList();
			
			for (int pos = 0; pos < modules.size(); pos++) {
				if (modules.get(pos).isFunctionnal() == false) {
					continue;
				}
				table.addRow(" * " + modules.get(pos).getCliModuleName(), modules.get(pos).getCliModuleShortDescr());
			}
			table.print();
			System.out.println("");
			System.out.println("To show help: modulename -help or modulename -h");
			System.out.println(" -verbose for verbose mode (always show caller and thread)");
			System.out.println(" -trace for enable trace mode (debug level)");
			System.out.println(" -quiet never show a normal log message (debug level)");
			System.exit(1);
		}
		
		if (appargs.getParamExist("-help") | appargs.getParamExist("-h")) {
			System.out.println("MyDMAM Command line interface");
			System.out.println("=============================");
			for (int pos = 0; pos < modules.size(); pos++) {
				if (modules.get(pos).isFunctionnal() == false) {
					continue;
				}
				if (modules.get(pos).getCliModuleName().equalsIgnoreCase(modulename)) {
					System.out.println("Help for module " + modulename);
					System.out.println();
					modules.get(pos).showFullCliModuleHelp();
					System.exit(0);
				}
			}
			System.err.println("Can't found CLI module " + modulename);
			System.exit(2);
		}
		
		for (int pos = 0; pos < modules.size(); pos++) {
			if (modules.get(pos).isFunctionnal() == false) {
				continue;
			}
			if (modules.get(pos).getCliModuleName().equalsIgnoreCase(modulename)) {
				modules.get(pos).execCliModule(appargs);
				System.exit(0);
			}
		}
		System.out.println("MyDMAM Command line interface");
		System.out.println("=============================");
		System.err.println("Can't found CLI module " + modulename);
		System.exit(2);
		
	}
}
