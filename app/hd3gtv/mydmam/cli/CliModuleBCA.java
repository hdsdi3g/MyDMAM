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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import hd3gtv.configuration.Configuration;
import hd3gtv.configuration.ConfigurationItem;
import hd3gtv.mydmam.MyDMAM;
import hd3gtv.mydmam.bcastautomation.BCACatch;
import hd3gtv.mydmam.bcastautomation.BCACatchHandler;
import hd3gtv.mydmam.bcastautomation.BCAEngine;
import hd3gtv.mydmam.bcastautomation.BCAWatcher;
import hd3gtv.tools.ApplicationArgs;
import hd3gtv.tools.TableList;

public class CliModuleBCA implements CliModule {
	
	public String getCliModuleName() {
		return "bca";
	}
	
	public String getCliModuleShortDescr() {
		return "Broadcast automation";
	}
	
	public void execCliModule(ApplicationArgs args) throws Exception {
		if (args.getParamExist("-parse")) {
			File file_to_parse = new File(args.getSimpleParamValue("-parse"));
			
			BCAEngine engine = BCAWatcher.getEngine();
			
			TableList tl = new TableList(4);
			
			final HashMap<String, ConfigurationItem> import_other_properties_configuration = Configuration.getElement(Configuration.global.getElement("broadcast_automation"),
					"import_other_properties");
			
			SimpleDateFormat date_format = new SimpleDateFormat("HH:mm:ss");
			
			engine.processScheduleFile(file_to_parse, event -> {
				tl.addRow(date_format.format(new Date(event.getStartDate())), event.getName(), event.getDuration().toString(),
						event.getOtherProperties(import_other_properties_configuration).toString());
			});
			
			tl.print();
			
			return;
		} else if (args.getParamExist("-propertycatch")) {
			BCACatch bcacatch = new BCACatch();
			Class<?> engine_class = Class.forName(Configuration.global.getValue("broadcast_automation", "catch_handler", null));
			MyDMAM.checkIsAccessibleClass(engine_class, false);
			BCACatchHandler handler = (BCACatchHandler) engine_class.newInstance();
			
			bcacatch.parsePlaylist(handler).save();
			return;
		}
		
		showFullCliModuleHelp();
	}
	
	public void showFullCliModuleHelp() {
		System.out.println("Usage (with no confirm)");
		System.out.println(" * just parse playlist/asrun: " + getCliModuleName() + " -parse file.sch");
		System.out.println(" * get property from playlist and do actions on match events: " + getCliModuleName() + " -propertycatch");
	}
	
	public boolean isFunctionnal() {
		return Configuration.global.isElementExists("broadcast_automation");
	}
}
