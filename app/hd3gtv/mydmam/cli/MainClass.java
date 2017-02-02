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

import hd3gtv.mydmam.Loggers;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.tools.ApplicationArgs;

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
		
		MyDMAM.testIllegalKeySize();
		
		ArrayList<CliModule> modules = new ArrayList<CliModule>();
		modules.add(new CliModuleAuth());
		modules.add(new CliModuleAccessControl());
		modules.add(new CliModuleSsh());
		modules.add(new CliModuleStorageManager());
		modules.add(new CliModuleCDFinderPathIndexer());
		modules.add(new CliModuleDumpDatabase());
		modules.add(new CliModuleOperateDatabase());
		modules.add(new CliModuleStorageIndex());
		modules.add(new CliModuleCopyDirStruct());
		modules.add(new CliModuleBroker());
		modules.add(new CliModuleMetadata());
		modules.add(new CliModuleProcessKit());
		
		String modulename = appargs.getFirstAction();
		
		if (modulename == null) {
			System.out.println("MyDMAM Command line interface");
			System.out.println("=============================");
			System.out.println("Available modules:");
			for (int pos = 0; pos < modules.size(); pos++) {
				if (modules.get(pos).isFunctionnal() == false) {
					continue;
				}
				System.out.print(" * ");
				System.out.print(modules.get(pos).getCliModuleName());
				System.out.print(" (");
				System.out.print(modules.get(pos).getCliModuleShortDescr());
				System.out.print(")");
				System.out.println();
			}
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
			System.err.println("Can't found module " + modulename);
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
		System.err.println("Can't found module " + modulename);
		System.exit(2);
		
	}
}
